package org.matsim.contribs.discrete_mode_choice.model.utilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.DefaultTourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * UtilityWriter is responsible for logging utility candidates and their selection status
 * to a CSV file in a thread-safe manner.
 */
public class UtilityWriter {

	private final static Logger logger = LogManager.getLogger(UtilityWriter.class);

	private static final Object LOCK = new Object();
	private static BufferedWriter writer = null;
	private static final AtomicInteger selectionCounter = new AtomicInteger(0);
	private static String csvFilePath = null;

	/**
	 * Initializes the writer by creating a BufferedWriter for the given file path.
	 *
	 * @param filePath the path to the output CSV file
	 */
	public static void init(String filePath) {
		if (filePath == null) {
			return;
		}

		synchronized (LOCK) {
			if (filePath.equals(csvFilePath)) {return;} // if it is the same file, do nothing.
			try {
				Path path = Path.of(filePath);
				writer = Files.newBufferedWriter(path);
				logger.info("Writer initialized for file: " + path.toAbsolutePath());
				writeHeader();
				csvFilePath = filePath;
			} catch (IOException e) {
				throw new RuntimeException("Failed to initialize CSV writer", e);
			}
		}
	}

	public static boolean isWriterInitialized() {
		return writer != null;
	}

	private static void writeHeader() throws IOException {
		writer.write("person_id;trips_index;selection_id;candidate_mode;utilities;utility;selected\n");
		writer.flush();
		logger.info("Head is written");
	}

	/**
	 * Writes a list of utility candidates to the CSV file, indicating which candidate was selected.
	 *
	 * @param personId     the ID of the person
	 * @param tourTrips    the list of trips in the tour
	 * @param candidates   the list of utility candidates
	 * @param selectedIndex   the index of the selected candidate in the list of candidates
	 */
	public static void writeCandidate(Id<Person> personId, List<DiscreteModeChoiceTrip> tourTrips,
									  List<UtilityCandidate> candidates, int selectedIndex) {
		if (writer == null) return;

		int selectionId = selectionCounter.incrementAndGet();
		UtilityCandidate selectedCandidate = candidates.get(selectedIndex);

		synchronized (LOCK) {
			try {
				String index = tourTrips.stream()
					.map(t -> String.valueOf(t.getIndex()))
					.collect(Collectors.joining(","));

				for (UtilityCandidate candidate : candidates) {
					String line = String.format("%s;%s;%d;%s;%s;%.4f;%b\n",
						personId.toString(),
						// startTime,
						index,
						selectionId,
						getModes(candidate),
						getUtilities(candidate),
						candidate.getUtility(),
						candidate.equals(selectedCandidate));
					writer.write(line);
				}
				writer.flush(); // Consider buffering writes for large volumes
			} catch (IOException e) {
				logger.error("Failed to write to CSV: " + e.getMessage());
			}
		}
	}

	public static String getModes(UtilityCandidate candidate){
		if (candidate instanceof DefaultTourCandidate){
			List<String> modes = ((DefaultTourCandidate) candidate).getTripCandidates().stream()
				.map(TripCandidate::getMode)
				.collect(Collectors.toList());
			return String.join(",", modes);
		}
		if (candidate instanceof DefaultTripCandidate){
			return ((DefaultTripCandidate) candidate).getMode();
		}
		return "";
	}

	public static String getUtilities(UtilityCandidate candidate){
		if (candidate instanceof DefaultTourCandidate){
			return ((DefaultTourCandidate) candidate).getTripCandidates().stream()
				.map(t -> String.format("%.4f", t.getUtility())) // Formats to 3 decimal places
				.collect(Collectors.joining(","));
		}
		if (candidate instanceof DefaultTripCandidate){
			return Double.toString(((DefaultTripCandidate) candidate).getUtility());
		}
		return "";
	}

	/**
	 * Closes the writer, releasing any system resources.
	 */
	public static void close() {
		synchronized (LOCK) {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					logger.error("Failed to close CSV writer: " + e.getMessage());
				}
			}
		}
	}
}
