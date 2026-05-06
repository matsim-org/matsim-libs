package org.matsim.contrib.common.ntfy;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author sebhoerl
 */
public class Notifier {
    private final Logger logger = LogManager.getLogger(Notifier.class);

    private final String run;
    private final NtfyConfigGroup config;

    Notifier(NtfyConfigGroup config, String run) {
        this.config = config;
        this.run = run;
    }

    public void raw(String message) {
        try {
            URL url = new URI("https://ntfy.sh/" + config.topic).toURL();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            DataOutputStream stream = new DataOutputStream(connection.getOutputStream());
            stream.writeUTF(message);
            stream.close();

            int response = connection.getResponseCode();
            if (response != HttpURLConnection.HTTP_OK) {
                logger.warn("Response code " + response + ": " + connection.getResponseMessage());
            }
        } catch (IOException | URISyntaxException e) {
            logger.warn(e.getMessage());
        }
    }

    public void notify(String message) {
        String formatted = "MATSim";

        if (!isBlank(config.simulationName) || !isBlank(run)) {
            formatted += " (";

            if (!isBlank(config.simulationName)) {
                formatted += config.simulationName;
            }

            if (!isBlank(run)) {
                if (!isBlank(config.simulationName)) {
                    formatted += ", ";
                }

                formatted += run;
            }

            formatted += ")";
        }

        formatted += ": ";
        formatted += message;

        raw(formatted);
    }

    void notifyStartup() {
        notify("Simulation starts");
    }

    void notifyIterationStarts(int iteration) {
        notify(String.format("Iteration %d starts", iteration));
    }

    void notifyIterationEnds(int iteration) {
        notify(String.format("Iteration %d ends", iteration));
    }

    void notifyShutdown(String info) {
        notify("Simulation ends: " + info);
    }

    private boolean isBlank(String message) {
        return message == null || message.length() == 0;
    }
}
