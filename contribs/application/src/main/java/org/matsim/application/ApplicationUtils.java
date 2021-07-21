package org.matsim.application;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

public class ApplicationUtils {

	private static final Logger log = LogManager.getLogger(ApplicationUtils.class);

	/**
	 * Helper function to glob for a required file.
	 *
	 * @throws IllegalStateException if no file was matched
	 */
	public static Path globFile(Path path, String pattern) {

		PathMatcher m = path.getFileSystem().getPathMatcher("glob:" + pattern);

		try {
			return Files.list(path)
					.filter(p -> m.matches(p.getFileName()))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("No " + pattern + " file found."));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String globFile(Path path, String runId, String name) {
		var filePath = globFile(path, runId + ".*" + name + ".*").toString();

		log.info("Using {} file: {}", name, filePath);

		return filePath;
	}
}
