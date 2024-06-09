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
import java.util.*;

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

	/**
	 * Contains predication result for each mode.
	 */
	private final Map<String, Counts> counts = new HashMap<>();

	public TripChoiceAnalysis(Table persons, Table trips, List<String> modeOrder) {
		persons = persons.where(persons.stringColumn("ref_modes").isNotEqualTo(""));
		trips = new DataFrameJoiner(trips, "person").inner(persons);
		this.modeOrder = modeOrder;

		log.info("Analyzing mode choices for {} persons", persons.rowCount());

		boolean hasWeight = persons.containsColumn(TripAnalysis.ATTR_REF_WEIGHT);

		for (Row trip : trips) {

			String person = trip.getText("person");
			int n = trip.getInt("trip_number") - 1;
			double weight = hasWeight ? trip.getDouble(TripAnalysis.ATTR_REF_WEIGHT) : 1;

			String predMode = trip.getString("main_mode");
			String[] split = trip.getString(TripAnalysis.ATTR_REF_MODES).split("-");

			if (n < split.length) {
				String trueMode = split[n];
				data.add(new Entry(person, weight, n, trueMode, predMode));
			} else
				log.warn("Person {} trip {} does not match ref data ({})", person, n, split.length);
		}

		for (String mode : modeOrder) {
			counts.put(mode, countPredictions(mode, data));
		}
	}

	private static double precision(Counts c) {
		return c.tp / (c.tp + c.fp);
	}

	private static double recall(Counts c) {
		return c.tp / (c.tp + c.fn);
	}

	private static double f1(Counts c) {
		return 2 * c.tp / (2 * c.tp + c.fp + c.fn);
	}

	private Counts countPredictions(String mode, List<Entry> data) {
		double tp = 0, fp = 0, fn = 0, tn = 0;
		double total = 0;
		for (Entry e : data) {
			if (e.trueMode.equals(mode)) {
				if (e.predMode.equals(mode))
					tp += e.weight;
				else
					fn += e.weight;
			} else {
				if (e.predMode.equals(mode))
					fp += e.weight;
				else
					tn += e.weight;
			}
			total += e.weight;
		}

		return new Counts(tp, fp, fn, tn, total);
	}

	/**
	 * Writes all choices to csv.
	 */
	public void writeChoices(Path path) throws IOException {
		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(path), CSVFormat.DEFAULT)) {
			csv.printRecord("person", "weight", "n", "true_mode", "pred_mode");
			for (Entry e : data) {
				csv.printRecord(e.person, e.weight, e.n, e.trueMode, e.predMode);
			}
		}
	}

	/**
	 * Writes aggregated choices metrics.
	 */
	public void writeChoiceEvaluation(Path path) throws IOException {

		double tp = 0;
		double total = 0;
		double tpfp = 0;
		double tpfn = 0;
		for (Counts c : counts.values()) {
			tp += c.tp;
			tpfp += c.tp + c.fp;
			tpfn += c.tp + c.fn;
			total = c.total;
		}

		OptionalDouble precision = counts.values().stream().mapToDouble(TripChoiceAnalysis::precision).average();
		OptionalDouble recall = counts.values().stream().mapToDouble(TripChoiceAnalysis::recall).average();
		OptionalDouble f1 = counts.values().stream().mapToDouble(TripChoiceAnalysis::f1).average();

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(path), CSVFormat.DEFAULT)) {

			csv.printRecord("Info", "Value");

			csv.printRecord("Accuracy", tp / total);
			csv.printRecord("Precision (micro avg.)", tp / tpfp);
			csv.printRecord("Precision (macro avg.)", precision.orElse(0));
			csv.printRecord("Recall (micro avg.)", tp / tpfn);
			csv.printRecord("Recall (macro avg.)", recall.orElse(0));
			csv.printRecord("F1 Score (micro avg.)", 2 * tp / (tpfp + tpfn));
			csv.printRecord("F1 Score (macro avg.)", f1.orElse(0));
		}

		// TODO Cohen’s Kappa, Cross-Entropy, Mathews Correlation Coefficient (MCC)
	}

	/**
	 * Writes metrics per mode.
	 */
	public void writeChoiceEvaluationPerMode(Path path) throws IOException {

		// Precision in multi-class classification is the fraction of instances correctly classified as belonging to a specific class out of all instances the model predicted to belong to that class.

		// Recall in multi-class classification is the fraction of instances in a class that the model correctly classified out of all instances in that class.

		try (CSVPrinter csv = new CSVPrinter(Files.newBufferedWriter(path), CSVFormat.DEFAULT)) {

			csv.printRecord("Mode", "Precision", "Recall", "F1 Score");
			for (String m : modeOrder) {
				csv.print(m);

				Counts c = counts.get(m);

				csv.print(precision(c));
				csv.print(recall(c));
				csv.print(f1(c));
				csv.println();
			}
		}
	}

	// TODO: write confusion matrix

	// TODO Class Prediction Error

	private record Entry(String person, double weight, int n, String trueMode, String predMode) {
	}

	/**
	 * Contains true positive, false positive, false negative and true negative counts.
	 *
	 * @param tp correctly predicted this class
	 * @param fp incorrectly predicted this class
	 * @param fn incorrectly predicted different class
	 * @param tn correctly predicated different class
	 */
	private record Counts(double tp, double fp, double fn, double tn, double total) {

	}

}
