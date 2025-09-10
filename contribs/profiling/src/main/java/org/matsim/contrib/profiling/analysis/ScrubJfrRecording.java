package org.matsim.contrib.profiling.analysis;

import jdk.jfr.consumer.RecordingFile;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

/**
 * JFR recordings record quite a lot from the system environment.
 * When recorded on a personal machine, this might include personal data in
 * environment variables, file paths, or parallel running processes.
 * <p>This is an attempt to scrub the personal data from the recording.
 */
public class ScrubJfrRecording {


	public static void main(String[] args) throws IOException {
		var frame = new Frame();
		try {
			var fileChooser = new FileDialog(frame);
			fileChooser.setDirectory(System.getProperty("user.dir"));
			var testPath = Path.of(System.getProperty("user.dir"), "contribs", "profiling", "src", "test", "resources");
			if (testPath.toFile().exists()) {
				fileChooser.setDirectory(testPath.toString());
			}
			fileChooser.show();
			var filePath = Path.of(fileChooser.getDirectory(), fileChooser.getFile());
			System.out.println(filePath);

			try (var rf = new RecordingFile(filePath)) {
				rf.write(Path.of(fileChooser.getDirectory(), fileChooser.getFile().replace(".jfr", "_scrubbed.jfr")), e -> {

					var eventName = e.getEventType().getName();
					// commented events exposing local file paths as removing them breaks tooling trying to read the scrubbed file
					if (eventName.equals("jdk.InitialSystemProperty") && e.hasField("value") && e.hasField("key")) {
						return true;
						//return !"java.class.path".equals(e.getString("key")); // file paths
					}
					return !eventName.equals("jdk.InitialEnvironmentVariable")
						//&& !eventName.equals("jdk.JavaAgent") // file paths
						//&& !eventName.equals("jdk.JVMInformation") // file paths
						&& !eventName.equals("jdk.SystemProcess") // parallel running processes
						//&& !eventName.equals("jdk.ActiveRecording") // file paths
					;
				});
			}

		} catch (Exception e) {
			System.err.println(e.getMessage());
			throw e;
		} finally {
			frame.dispose();
		}
	}

}
