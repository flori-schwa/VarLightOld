package me.shawlaf.varlight.spigot;

import me.shawlaf.varlight.util.NumericMajorMinorVersion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class UpdateCheck implements Runnable {

    private static final String BASE_URL_FORMAT = "https://api.spigotmc.org/legacy/update.php?resource=%s";
    private static final String PROJECT_ID = "65268";
    private final NumericMajorMinorVersion currentVersion;
    private final Logger logger;

    public UpdateCheck(Logger logger, NumericMajorMinorVersion currentVersion) {
        this.currentVersion = currentVersion;
        this.logger = logger;
    }

    @Override
    public void run() {
        String checkUrl = String.format(BASE_URL_FORMAT, PROJECT_ID);

        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(checkUrl).openConnection();
            NumericMajorMinorVersion latestVerison;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
                latestVerison = new NumericMajorMinorVersion(reader.readLine());
            }

            logger.info("------------------------------------------------------");

            if (latestVerison.newerThan(currentVersion)) {
                logger.info("A new Version of VarLight is available: " + latestVerison.toString());
            } else {
                logger.info("You are running the latest version of VarLight");
            }

            logger.info("------------------------------------------------------");

            urlConnection.disconnect();
        } catch (IOException e) {
            logger.warning("Failed to check for updates: " + e.getMessage());
        }
    }
}
