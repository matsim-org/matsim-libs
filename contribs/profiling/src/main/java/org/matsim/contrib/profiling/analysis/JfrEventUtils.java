package org.matsim.contrib.profiling.analysis;

import jdk.jfr.Event;
import jdk.jfr.Name;

import java.awt.*;
import java.nio.file.Path;
import java.util.Optional;

public class JfrEventUtils {

	private JfrEventUtils() {}

	public static String getEventName(Class<? extends Event> event) {
		var nameAnnotation = event.getAnnotation(Name.class);
		if (nameAnnotation != null) {
			return nameAnnotation.value();
		}
		return event.getName();
	}

	/**
	 *
	 * @param args arguments of main method
	 * @param startDir start directory for the file dialog
	 * @return a path from given arguments, if not present, try to open a file dialog
	 */
	public static Optional<Path> getFilePath(String[] args, String... startDir) {
		if (args.length > 1) {
			if ("--path".equals(args[0])) {
				return Optional.of(Path.of(args[1]));
			}
		}
		if (!GraphicsEnvironment.isHeadless()) {
			return getFileFromDialog(startDir);
		}
		return Optional.empty();
	}

	/**
	 *
	 * @param startDir directory to open the filedialog in
	 * @return selected path or empty optional if the dialog was canceled
	 */
	public static Optional<Path> getFileFromDialog(String... startDir) {
		var frame = new Frame();
		try {
			var fileChooser = new FileDialog(frame);
			fileChooser.setDirectory(System.getProperty("user.dir"));
			var testPath = Path.of(System.getProperty("user.dir"), startDir);
			if (testPath.toFile().exists()) {
				fileChooser.setDirectory(testPath.toString());
			}
			fileChooser.show();

			var file = fileChooser.getFile();
			if (file == null) {
				return Optional.empty();
			}

			var filePath = Path.of(fileChooser.getDirectory(), file);
			return Optional.of(filePath);
		} finally {
			frame.dispose();
		}
	}

}
