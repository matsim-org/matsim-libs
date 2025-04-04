package org.matsim.application.prepare.counts;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsUtils;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import static org.matsim.counts.CountsUtils.mergeCounts;

/**
 * Command line tool that merges two counts files into a single counts file.
 *
 * <p>This class reads two separate MATSim counts files (in .xml or .xml.gz format)
 * and merges them into one counts file. The merge is performed based on the key (refId)
 * for each measurement location. If both files contain a location with the same key, the
 * duplicate locations are merged according to the specified duplicate handling mode.
 *
 * <p>The duplicate handling mode can be one of the following:
 * <ul>
 *   <li>SKIP - Keep the value from the base counts file, ignoring the duplicate.
 *   <li>OVERRIDE - Replace the base measurable with the one from the duplicate file.
 *   <li>SUM - Aggregate (sum) the values from both counts files.
 * </ul>
 *
 * <p>This class makes use of the MATSim counts API to read and write counts, and
 * supports merging measurable values (such as traffic volumes) for each measurement location.
 */
@CommandLine.Command(name = "merge-counts", description = "Merges two MATSim counts files into one merged counts file.")
public class MergeCounts implements Callable<Integer> {

	private final static Logger log = LogManager.getLogger(MergeCounts.class);

	@CommandLine.Option(names = "--input1", required = true,
		description = "Path to the first counts file (.xml or .xml.gz).")
	private Path input1;

	@CommandLine.Option(names = "--input2", required = true,
		description = "Path to the second counts file (.xml or .xml.gz).")
	private Path input2;

	@CommandLine.Option(names = "--output", required = true,
		description = "Output path for the merged counts file.")
	private Path output;

	// TODO: default: error
	@CommandLine.Option(names = "--duplicate",
		description = "Duplicate handling mode: skip, override, or sum (default: sum).",
		defaultValue = "error")
	private CountsUtils.DuplicateHandling duplicateMode;

	/**
	 * Main method to start the MergeCounts command line tool.
	 *
	 * @param args Command line arguments specifying input files, output file, and duplicate handling mode.
	 */
	public static void main(String[] args) {
		int exitCode = new CommandLine(new MergeCounts()).execute(args);
		System.exit(exitCode);
	}

	/**
	 * Executes the merge operation.
	 *
	 * <p>The method performs the following steps:
	 * <ol>
	 *   <li>Reads the counts data from the first file into a Counts object.
	 *   <li>Reads the counts data from the second file into another Counts object.
	 *   <li>Determines the duplicate handling mode based on command line input.
	 *   <li>Merges the second counts file into the first file by checking the measurement location keys.
	 *   <li>Logs the keys present in each file.
	 *   <li>Writes the merged counts data to the specified output file.
	 * </ol>
	 *
	 * @return 0 if the merge is successful; otherwise an Exception is thrown.
	 * @throws Exception if any error occurs during file reading, merging, or writing.
	 */
	@Override
	public Integer call() throws Exception {
		// Load first counts file
		Counts<Link> counts1 = new Counts<>();
		MatsimCountsReader reader1 = new MatsimCountsReader(counts1);
		reader1.readFile(input1.toString());

		// Load second counts file
		Counts<Link> counts2 = new Counts<>();
		MatsimCountsReader reader2 = new MatsimCountsReader(counts2);
		reader2.readFile(input2.toString());

		// Get duplicate handling mode after trimming and converting to upper case
		CountsUtils.DuplicateHandling mode = duplicateMode;

		// Merge counts2 into counts1; merging is based on the key (measurement location's refId)
		mergeCounts(counts1, counts2, mode);

		log.info("Base counts locations: " + counts1.getMeasureLocations());
		log.info("Addition counts locations: " + counts2.getMeasureLocations());

		// Write merged counts file
		CountsWriter writer = new CountsWriter(counts1);
		writer.write(output.toString());

		return 0;
	}
}
