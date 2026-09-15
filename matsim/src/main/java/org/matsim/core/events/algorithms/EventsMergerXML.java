package org.matsim.core.events.algorithms;

import it.unimi.dsi.fastutil.doubles.DoubleObjectPair;
import org.apache.commons.io.FilenameUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class EventsMergerXML {

	/**
	 * Merge xml events into one file. This method detects all xml files in the iterations directory.
	 */
	public static void mergeEvents(String iterationPath, String eventFile, String outputFilename) throws IOException {

		System.out.println(iterationPath);

		String eventName = FilenameUtils.getName(eventFile);
		String iterName = FilenameUtils.getName(iterationPath);

		Path outputDir = Path.of(iterationPath).getParent().getParent();

		List<Path> iters = Files.list(outputDir)
			.filter(Files::isDirectory)
			.filter(p -> p.getFileName().toString().startsWith(Controler.DIRECTORY_ITERS))
			.toList();


		List<Path> files = new ArrayList<>();

		for (Path it : iters) {
			Path p = it.resolve(iterName).resolve(eventName);
			if (Files.exists(p)) {
				files.add(p);
			}
		}

		Pattern pattern = Pattern.compile("time=\"(\\d+\\.\\d+)\"", Pattern.CASE_INSENSITIVE);
		List<DoubleObjectPair<String>> lines = new ArrayList<>();

		for (Path file : files) {
			try (BufferedReader reader = IOUtils.getBufferedReader(file.toString())) {
				reader.lines().forEach(line -> {
					Matcher matcher = pattern.matcher(line);
					if (matcher.find()) {
						lines.add(DoubleObjectPair.of(Double.parseDouble(matcher.group(1)), line));
					}
				});
			}
		}

		// TODO: this reads all lines first and is not memory efficient
		lines.sort(Comparator.comparingDouble(DoubleObjectPair::firstDouble));

		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputFilename)) {
			writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<events version=\"1.0\">\n");
			for (DoubleObjectPair<String> line : lines) {
				writer.write(line.second());
				writer.write("\n");
			}
			writer.write("</events>");
		}
	}
}
