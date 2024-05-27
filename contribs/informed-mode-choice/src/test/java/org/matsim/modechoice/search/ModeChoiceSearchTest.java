package org.matsim.modechoice.search;

import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ModeChoiceSearchTest {


	@Test
	void order() {

		ModeChoiceSearch search = new ModeChoiceSearch(3, 3);

		search.addEstimates("A", new double[]{1, 4, 3});
		search.addEstimates("B", new double[]{4, 3, 1});
		search.addEstimates("C", new double[]{6, 2, 4});

		System.out.printf(search.toString());

		String[] result = new String[3];

		DoubleIterator iter = search.iter(result);

		assertThat(iter).toIterable()
				.startsWith(14d)
				.endsWith(4d);


		iter = search.iter(result);
		iter.nextDouble();

		assertThat(result)
				.containsExactly("C", "A", "C");

		iter.nextDouble();

		assertThat(result)
				.containsExactly("C", "B", "C");

	}


	@Test
	void negative() {

		ModeChoiceSearch search = new ModeChoiceSearch(3, 3);

		search.addEstimates("A", new double[]{1, -4, 3});
		search.addEstimates("B", new double[]{-4, 3, 1});
		search.addEstimates("C", new double[]{6, 2, -4});

		String[] result = new String[3];

		DoubleIterator iter = search.iter(result);

		assertThat(iter).toIterable()
				.startsWith(12d)
				.endsWith(-12d);

		iter = search.iter(result);
		iter.nextDouble();

		assertThat(result)
				.containsExactly("C", "B", "A");
	}


	@Test
	void nullValues() {

		ModeChoiceSearch search = new ModeChoiceSearch(3, 3);

		search.addEstimates("A", new double[]{1, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY});
		search.addEstimates("B", new double[]{Double.NEGATIVE_INFINITY, 3, Double.NEGATIVE_INFINITY});
		search.addEstimates("C", new double[]{6, 2, Double.NEGATIVE_INFINITY});

		String[] result = new String[3];

		DoubleIterator iter = search.iter(result);

		assertThat(iter).toIterable()
				.startsWith(9d)
				.endsWith(3d);

		iter = search.iter(result);
		iter.nextDouble();

		assertThat(result)
				.containsExactly("C", "B", null);

		iter.nextDouble();

		assertThat(result)
				.containsExactly("C", "C", null);

	}

}