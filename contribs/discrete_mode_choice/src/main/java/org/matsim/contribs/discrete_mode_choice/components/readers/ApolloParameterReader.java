package org.matsim.contribs.discrete_mode_choice.components.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

public class ApolloParameterReader {
	private final static Logger logger = LogManager.getLogger(ApolloParameterReader.class);

	public ApolloParameters read(File path) throws MalformedURLException, IOException {
		return read(path.toURI().toURL());
	}

	public ApolloParameters read(URL url) throws IOException {
		logger.info(String.format("Reading Apollo parameters from: %s", url));
		BufferedReader reader = IOUtils.getBufferedReader(url);

		String line = null;
		boolean foundEstimates = false;
		boolean foundApollo = false;

		Map<String, Double> parameters = new HashMap<>();

		while ((line = reader.readLine()) != null) {
			if (line.contains("Apollo for R")) {
				foundApollo = true;
			}

			if (line.startsWith("Estimates:")) {
				foundEstimates = true;
			} else if (foundEstimates) {
				if (line.trim().length() == 0) {
					break;
				} else {
					if (!line.contains("Std.err.")) {
						// This must be a parameter
						String[] parts = line.split("\\s+");

						String name = parts[0];
						double value = Double.parseDouble(parts[1]);

						parameters.put(name, value);
						logger.info(String.format("  %s = %f", name, value));
					}
				}
			}
		}

		reader.close();

		if (!foundApollo) {
			throw new RuntimeException("File does not seem to be Apollo output.");
		}

		return new ApolloParameters(parameters);
	}
}
