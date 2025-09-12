package org.matsim.contrib.profiling.analysis;

import jdk.jfr.consumer.RecordingFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * JFR recordings record quite a lot from the system environment.
 * When recorded on a personal machine, this might include personal data in
 * environment variables, file paths, or parallel running processes.
 * <p>This is an attempt to scrub the personal data from the recording.
 */
public final class ScrubJfrRecording {

	private ScrubJfrRecording() {}

	public static void main(String[] args) throws IOException {

		var optionalFilePath = JfrEventUtils.getFilePath(args, "contribs", "profiling", "src", "test", "resources");
		if  (optionalFilePath.isEmpty()) {
			System.err.println("No file provided");
			return;
		}
		var filePath = optionalFilePath.get();
		System.out.println(filePath);

		boolean scrubDestructible = Arrays.asList(args).contains("--all");

		try (var rf = new RecordingFile(filePath)) {
			rf.write(Path.of(filePath.toString().replace(".jfr", "_scrubbed.jfr")), e -> {
				// return true to keep, false to discard event

				var eventName = e.getEventType().getName();
				if (scrubDestructible) {
					// removing these them breaks some tooling trying to read the scrubbed file
					if (eventName.equals("jdk.InitialSystemProperty") && e.hasField("value") && e.hasField("key")) {
						return !"java.class.path".equals(e.getString("key")); // file paths
					}
					return !eventName.equals("jdk.JavaAgent") // file paths
						&& !eventName.equals("jdk.JVMInformation") // file paths
						&& !eventName.equals("jdk.ActiveRecording") // file paths
						;
				}

				return !eventName.equals("jdk.InitialEnvironmentVariable")
					&& !eventName.equals("jdk.SystemProcess") // parallel running processes
				;
			});
		}
	}

}
