package org.matsim.application.analysis.population;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TripChoiceAnalysisTest {

	/**
	 * Create confusion matrix.
	 */
	public static List<DoubleList> cm(String... entries) {
		List<DoubleList> rows = new ArrayList<>();
		List<String> distinct = Arrays.stream(entries).distinct().toList();
		Object2DoubleMap<TripChoiceAnalysis.Pair> pairs = new Object2DoubleOpenHashMap<>();

		for (int i = 0; i < entries.length; i += 2) {
			TripChoiceAnalysis.Pair pair = new TripChoiceAnalysis.Pair(entries[i], entries[i + 1]);
			pairs.mergeDouble(pair, 1, Double::sum);
		}

		for (String d1 : distinct) {
			DoubleArrayList row = new DoubleArrayList();
			for (String d2 : distinct) {
				row.add(pairs.getDouble(new TripChoiceAnalysis.Pair(d1, d2)));
			}
			rows.add(row);
		}

		return rows;
	}

	@Test
	void cohenKappa() {

		double ck = TripChoiceAnalysis.computeCohenKappa(List.of());
		assertThat(ck).isEqualTo(1);

		ck = TripChoiceAnalysis.computeCohenKappa(cm(
			"a", "a",
			"b", "b",
			"b", "b",
			"c", "c")
		);

		assertThat(ck).isEqualTo(1.0);
		ck = TripChoiceAnalysis.computeCohenKappa(cm(
			"a", "c",
			"d", "e",
			"a", "b",
			"b", "d"
		));

		assertThat(ck).isLessThan(0.0);

		// These have been verified with sklearn
		ck = TripChoiceAnalysis.computeCohenKappa(cm(
			"negative", "negative",
			"positive", "positive",
			"negative", "negative",
			"neutral", "neutral",
			"positive", "negative"
		));

		assertThat(ck).isEqualTo(0.6875);

		ck = TripChoiceAnalysis.computeCohenKappa(cm(
			"negative", "positive",
			"positive", "neutral",
			"negative", "negative",
			"neutral", "neutral",
			"positive", "negative"
		));

		assertThat(ck).isEqualTo( 0.11764705882352955, Offset.offset(1e-5));

	}
}
