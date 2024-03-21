package by.aveleshko.tools.djvu;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class DjvuAssembler {

	private final Options options;
	private final Option helpOption;
	private final Option inputOption;
	private final Option maskOption;
	private final Option outputOption;
	private final Option tooldirOption;
	
	private Path inputPath;
	private String inputMask;
	private Path outputFile;
	private Path djvuLibrePath;
	private boolean showHelp;
	
	public static void main(String[] args) {
		var assembler = new DjvuAssembler();
		
		try {
            assembler.parseCommandLine(args);
			
			if (assembler.showHelp)
				assembler.showHelp();
			else
				assembler.assemble();
		} catch (InterruptedException e) {
			System.err.println("Interrupted");
			System.exit(1);
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
		
		checkOption(cmd, inputOption);
		checkOption(cmd, maskOption);
		checkOption(cmd, outputOption);
		checkOption(cmd, tooldirOption);
	
		inputPath = pathValue(cmd, inputOption);
		inputMask = cmd.getOptionValue(maskOption);
		outputFile = pathValue(cmd, outputOption);
		djvuLibrePath = pathValue(cmd, tooldirOption);
	}
	
	private Path pathValue(CommandLine line, Option option) {
		try {
			return Path.of(line.getOptionValue(option));
		} catch (InvalidPathException e) {
			throw new IllegalArgumentException("Invalid path for the command line option -" + option.getOpt(), e);
		}
	}
	
	public DjvuAssembler() {
		options = new Options();
		
		helpOption = new Option("h", "help", false, "display this message");
		options.addOption(helpOption);
		
		inputOption = Option.builder("i")
				.longOpt("input")
				.argName("directory")
				.hasArg()
				.desc("directory with the image files to convert")
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
				.argName("file")
				.hasArg()
				.desc("filename for the resulting file")
				.build();
		options.addOption(outputOption);
		
		tooldirOption = Option.builder("d")
				.longOpt("djvulibre")
				.argName("directory")
				.hasArg()
				.desc("path to the DjVuLibre installation")
				.build();
		options.addOption(tooldirOption);
	}
	
	public void showHelp() {
		var helpFormatter = new HelpFormatter();
		helpFormatter.printHelp("djvu-assembler", options);
	}
	
	private static void checkOption(CommandLine line, Option option) throws ParseException {
		if (!line.hasOption(option)) {
			throw new ParseException("Missing option -" + option.getOpt()
				+ " or --" + option.getLongOpt());
		}
	}
	
	public void assemble() throws InterruptedException {
		System.out.println("Input path: " + inputPath);
		System.out.println("Mask: " + inputMask);
		
		var inputs = collectInputs();
		
		if (inputs.isEmpty()) {
			System.err.println("No files found");
			return;
		}
		
		System.out.println("Found " + inputs.size() + " input files");
		
		Path first = inputs.getFirst();
		Path tempImage = resolveNearby(outputFile, ".page.djvu");
		convert(first, tempImage);
		System.out.println("Creating the first page...");
		createCollated(outputFile, tempImage);
		
		for (int i = 1; i < inputs.size(); i++) {
			Path image = inputs.get(i);
			convert(image, tempImage);
			System.out.println("Adding page " + (i+1) + "...");
			merge(outputFile, tempImage);
		}
		
		System.out.println("Done!");
		
		try {
			Files.delete(tempImage);
		} catch (IOException e) {
			System.err.println("Couldn't delete the temporary file " + tempImage);
		}
	}
	
	private static Path resolveNearby(Path anchor, String filename) {
		if (anchor.getParent() == null)
			return Path.of(filename);
		
		return anchor.getParent().resolve(filename);
	}
	
	private List<Path> collectInputs() {
		var fs = FileSystems.getDefault();
		var matcher = fs.getPathMatcher("glob:" + inputMask);

		try (Stream<Path> stream = Files.list(inputPath)) {
			List<Path> inputs = stream
					.filter(path -> matcher.matches(path.getFileName()) &&
					                Files.isRegularFile(path))
					.collect(Collectors.toList());
			return inputs;
		} catch (NoSuchFileException e) {
			throw new UncheckedIOException("Input path is not found: " + e.getFile(), e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	void createCollated(Path output, Path page) throws InterruptedException {
		runDjvm(output, page, "-c");
	}
	
	void merge(Path output, Path page) throws InterruptedException {
		runDjvm(output, page, "-i");
	}

	private void convert(Path image, Path target) throws InterruptedException {
		System.out.println("Converting " + image + "...");
		run(djvuLibrePath.resolve("c44.exe").toString(),
				image.toString(), target.toString());
	}
	
	void runDjvm(Path output, Path page, String key) throws InterruptedException {
		run(djvuLibrePath.resolve("djvm.exe").toString(),
			key, output.toString(), page.toString());
	}
	
	void run(String... args) throws InterruptedException {
		var processBuilder = new ProcessBuilder(args);
		processBuilder.redirectOutput(Redirect.INHERIT)
			.redirectError(Redirect.INHERIT);
		
		try {
			Process process = processBuilder.start();
			process.waitFor();
			
			if (process.exitValue() != 0)
				throw new RuntimeException("DjvuLibre utility finished with exit code " + process.exitValue());
		} catch (IOException e) {
			throw new UncheckedIOException("Couldn't start " + args[0], e);
		}
	}
}
