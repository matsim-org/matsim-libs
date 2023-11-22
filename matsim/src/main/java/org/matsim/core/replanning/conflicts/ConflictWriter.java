package org.matsim.core.replanning.conflicts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.matsim.core.utils.io.IOUtils;

/**
 * Writes high-level statistics on the conflict resolution process per iteration
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ConflictWriter {
	private final File outputPath;

	public ConflictWriter(File outputPath) {
		this.outputPath = outputPath;
	}

	public void write(int iteration, int rejectedToInitial, int rejectedToRandom, Map<String, Integer> conflictCounts) {
		boolean writeHeader = !outputPath.exists();

		List<String> resolvers = new ArrayList<>(conflictCounts.keySet());
		Collections.sort(resolvers);

		try {
			BufferedWriter writer = IOUtils.getAppendingBufferedWriter(outputPath.getPath());

			if (writeHeader) {
				List<String> header = new ArrayList<>(
						Arrays.asList("iteration", "rejected_total", "switched_to_initial", "switched_to_random"));
				resolvers.stream().map(r -> "resolver:" + r).forEach(header::add);

				writer.write(String.join(";", header) + "\n");
			}

			List<String> row = new ArrayList<>(Arrays.asList(String.valueOf(iteration), //
					String.valueOf(rejectedToInitial + rejectedToRandom), //
					String.valueOf(rejectedToInitial), //
					String.valueOf(rejectedToRandom) //
			));

			for (String resolver : resolvers) {
				row.add(String.valueOf(conflictCounts.get(resolver)));
			}

			writer.write(String.join(";", row) + "\n");

			writer.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
