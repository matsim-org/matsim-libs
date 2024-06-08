package org.matsim.application.analysis.population;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.joining.DataFrameJoiner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to analyze trip choices from persons against reference data.
 * <a href="https://neptune.ai/blog/evaluation-metrics-binary-classification">Metrics for binary classification</a>
 * <a href="https://medium.com/apprentice-journal/evaluating-multi-class-classifiers-12b2946e755b">Evaluation multi-class classifiers</a>
 * <a href="https://www.kaggle.com/code/nkitgupta/evaluation-metrics-for-multi-class-classification">more</a>
 * <a href="https://www.evidentlyai.com/classification-metrics/multi-class-metrics">more</a>
 * <a href="https://towardsdatascience.com/multiclass-classification-evaluation-with-roc-curves-and-roc-auc-294fd4617e3a">...</a>
 */
final class TripChoiceAnalysis {

	private static final Logger log = LogManager.getLogger(TripChoiceAnalysis.class);

	private final List<String> modeOrder;

	/**
	 * Contains trip data with true and predicated (simulated) modes.
	 */
	private final List<Entry> data = new ArrayList<>();

	public TripChoiceAnalysis(Table persons, Table trips, List<String> modeOrder) {
		persons = persons.where(persons.stringColumn("ref_modes").isNotEqualTo(""));
		trips = new DataFrameJoiner(trips, "person").inner(persons);;
		this.modeOrder = modeOrder;

		log.info("Analyzing mode choices for {} persons", persons.rowCount());

		for (Row trip : trips) {

			String person = trip.getText("person");
			int n = trip.getInt("trip_number") - 1;
			double weight = trip.getDouble(TripAnalysis.ATTR_REF_WEIGHT);

			String predMode = trip.getString("main_mode");
			String[] split = trip.getString(TripAnalysis.ATTR_REF_MODES).split("-");

			if (n < split.length) {
				String trueMode = split[n];
				data.add(new Entry(person, weight, n, trueMode, predMode));
			} else
				log.warn("Person {} trip {} does not match ref data ({})", person, n, split.length);
		}
	}

	/**
	 * Writes all choices to csv.
	 */
	public void writeChoices(Path path) throws IOException {
		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(path), CSVFormat.DEFAULT)) {
			csv.printRecord("person", "weight", "true_mode", "pred_mode");
			for (Entry e : data) {
				csv.printRecord(e.person, e.weight, e.trueMode, e.predMode);
			}
		}
	}

	/**
	 * Writes aggregated choices metrics.
	 */
	public void writeChoiceEvaluation(Path path) throws IOException {

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(path), CSVFormat.DEFAULT)) {

			csv.printRecord("Info", "Value");

			csv.printRecord("Accuracy", "TODO");


		}


		// TODO: accuracy
		// macro and micro averaged precision, recall, f1
	}

	/**
	 * Writes metrics per mode.
	 */
	public void writeChoiceEvaluationPerMode(Path path) {

	}

	private record Entry(String person, double weight, int n, String trueMode, String predMode) {
	}
}
