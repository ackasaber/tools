package by.aveleshko.tools.recog;

import com.microsoft.credentialstorage.StorageProvider;
import com.microsoft.credentialstorage.StorageProvider.SecureOption;
import com.microsoft.credentialstorage.model.StoredToken;
import com.microsoft.credentialstorage.model.StoredTokenType;

import java.util.Arrays;

public class Recognizer {
    public static void main(String[] args) {
        System.out.println("Hello and welcome!");

        var tokenStore = StorageProvider.getTokenStorage(true, SecureOption.REQUIRED);

        if (tokenStore == null)
            throw new RuntimeException("No secure keystore available.");

        char[] tokenData = new char[]{'a', 'b', 'c', 'd', 'e'};
        var token = new StoredToken(tokenData, StoredTokenType.PERSONAL);
        tokenStore.add("https://rehand.ru", token);
        Arrays.fill(tokenData, '\0');
    }
}