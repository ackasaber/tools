package by.aveleshko.tools.recog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.credentialstorage.SecretStore;
import com.microsoft.credentialstorage.StorageProvider;
import com.microsoft.credentialstorage.StorageProvider.SecureOption;
import com.microsoft.credentialstorage.model.StoredToken;
import com.microsoft.credentialstorage.model.StoredTokenType;
import org.apache.commons.cli.*;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.StatusLine;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Recognizer implements AutoCloseable {
    private final CloseableHttpClient http = HttpClients.createDefault();
    private final SecretStore<StoredToken> keystore;
    private final ObjectMapper jsonMapper;
    private final Options options;
    private final Option helpOption;
    private final Option inputOption;
    private final Option maskOption;
    private final Option outputOption;
    private final Option keyOption;
    private boolean showHelp;
    private Path inputDir;
    private String inputMask;
    private Path outputFile;
    private StoredToken apiKey;

    private final static String apiHost = "https://rehand.ru";
    //private final static String apiEndpoint = "http://localhost/post";
    private final static String apiEndpoint = apiHost + "/api/v1/upload";

    public static void main(String[] args) {
        try (var recognizer = new Recognizer()) {
            recognizer.parseCommandLine(args);

            if (recognizer.showHelp)
                recognizer.showHelp();
            else if (recognizer.apiKey == null)
                System.err.println("Please provide API key using the command line argument --key");
            else
                recognizer.run();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            System.exit(2);
        }
    }

    private void parseCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(helpOption)) {
            showHelp = true;
            return;
        }

        inputDir = Path.of("");
        inputMask = "*.jpg";
        apiKey = keystore.get(apiHost);
        outputFile = Path.of("recognized.txt");

        if (cmd.hasOption(inputOption))
            inputDir = pathValue(cmd, inputOption);

        if (cmd.hasOption(maskOption))
            inputMask = cmd.getOptionValue(maskOption);

        if (cmd.hasOption(outputOption))
            outputFile = pathValue(cmd, outputOption);

        if (cmd.hasOption(keyOption)) {
            if (apiKey != null)
                apiKey.clear();

            char[] token = cmd.getOptionValue(keyOption).toCharArray();
            apiKey = new StoredToken(token, StoredTokenType.PERSONAL);
            Arrays.fill(token, '\0');
            keystore.add(apiHost, apiKey);
        }
    }

    private Path pathValue(CommandLine line, Option option) {
        try {
            return Path.of(line.getOptionValue(option));
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Invalid path for the command line option -" + option.getOpt(), e);
        }
    }

    public Recognizer() {
        keystore = StorageProvider.getTokenStorage(true, SecureOption.REQUIRED);

        if (keystore == null)
            throw new RuntimeException("No secure keystore available.");

        jsonMapper = new ObjectMapper();
        options = new Options();

        helpOption = new Option("h", "help", false, "display help");
        options.addOption(helpOption);

        inputOption = Option.builder("i")
                .longOpt("input")
                .argName("input-dir")
                .hasArg()
                .desc("directory with input images")
                .build();
        options.addOption(inputOption);

        maskOption = Option.builder("m")
                .longOpt("mask")
                .argName("file-mask")
                .hasArg()
                .desc("input file mask")
                .build();
        options.addOption(maskOption);

        outputOption = Option.builder("o")
                .longOpt("output")
                .argName("output-file")
                .hasArg()
                .desc("output file name")
                .build();
        options.addOption(outputOption);

        keyOption = Option.builder("k")
                .longOpt("key")
                .argName("api-key")
                .hasArg()
                .desc("(re)define API key")
                .build();
        options.addOption(keyOption);
    }

    public void showHelp() {
        var helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("recognizer", options);
    }

    public void run() throws IOException {
        System.out.println("Working directory: " + inputDir);
        System.out.println("Mask: " + inputMask);
        System.out.println("Output file: " + outputFile);
        System.out.println("API key: " + String.valueOf(apiKey.getValue()));

        var inputs = collectInputs();

        if (inputs.isEmpty()) {
            System.err.println("No files found");
            System.exit(3);
        }

        System.out.println("Found " + inputs.size() + " input files");

        try (var output = Files.newOutputStream(inputDir.resolve(outputFile));
            var writer = new PrintWriter(output)) {
            int n = 0;
            for (Path image : inputs) {
                String responseFile = withSuffix(image.getFileName(), ".json");
                n++;
                System.out.println(n + ": " + image.getFileName());
                writer.println(recognize(image, responseFile));
            }
        }
    }

    private String withSuffix(Path filename, String suffix) {
        String asString = filename.toString();
        int dotPos = asString.lastIndexOf('.');

        if (dotPos == -1)
            return asString + suffix;

        return asString.substring(0, dotPos) + suffix;
    }

    private String recognize(Path image, String responseFile) {
        Path responsePath = image.resolveSibling(responseFile);

        try {
            if (!Files.isRegularFile(responsePath))
                postImage(image, responsePath);

            return parseResponse(responsePath);
        } catch (IOException ex) {
            throw new RecogException("Failed to upload the image to the recognition service", ex);
        }
    }

    private String parseResponse(Path responsePath) {
        RecogResponse parsed;
        try {
            parsed = jsonMapper.readValue(responsePath.toFile(), RecogResponse.class);
        } catch (IOException ex) {
            throw new RecogException("Failed to parse response file" , ex);
        }

        if (!"success".equals(parsed.status))
            throw new RecogException("The recognition service couldn't recognize the text: " + parsed.statusText);

        return parsed.outputText;
    }

    private void postImage(Path image, Path responseFile) throws IOException {
        HttpPost postRequest = new HttpPost(apiEndpoint);
        FileBody blob = new FileBody(image.toFile());
        StringBody type = new StringBody("handwriting", ContentType.TEXT_PLAIN);
        HttpEntity requestEntity = MultipartEntityBuilder.create()
                .addPart("file", blob)
                .addPart("type", type)
                .build();
        postRequest.setEntity(requestEntity);
        //postRequest.setHeader("Authorization", new String(apiKey.getValue()));

        http.execute(postRequest, response -> {
            if (response.getCode() != HttpStatus.SC_OK)
                throw new IOException("Recognition service rejected the upload of " +
                        image.getFileName() + ": " + new StatusLine(response));

            HttpEntity responseEntity = response.getEntity();

            if (responseEntity == null)
                throw new IOException("No response from the recognition service for " +
                        image.getFileName());

            try (var fileOut = Files.newOutputStream(responseFile)) {
                responseEntity.writeTo(fileOut);
            }

            return null;
        });
    }

    private List<Path> collectInputs() {
        var fs = FileSystems.getDefault();
        var matcher = fs.getPathMatcher("glob:" + inputMask);

        try (Stream<Path> stream = Files.list(inputDir)) {
            List<Path> inputs = stream
                    .filter(path -> matcher.matches(path.getFileName()) &&
                            Files.isRegularFile(path))
                    .collect(Collectors.toList());
            return inputs;
        } catch (NoSuchFileException e) {
            throw new UncheckedIOException("Working directory is not found: " + e.getFile(), e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        http.close();

        if (apiKey != null)
            apiKey.clear();
    }
}