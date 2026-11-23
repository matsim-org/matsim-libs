package org.matsim.contrib.common.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TimeIntervalTest {

	private static final TimeInterval EXISTING = new TimeInterval(100, 200);

	@ParameterizedTest(name = "[{0}, {1}) overlaps [100, 200) -> {2}")
	@CsvSource({
		// disjoint or touching
		"0,   50,  false",
		"0,   100, false",
		"200, 300, false",
		"250, 300, false",
		// partial overlap
		"50,  150, true",
		"150, 250, true",
		// shared boundary on the overlapping side
		"50,  200, true",
		"100, 250, true",
		// identical, containing, contained
		"100, 200, true",
		"50,  250, true",
		"120, 180, true",
		// zero-length inside / at the boundaries
		"150, 150, true",
		"100, 100, true",
		"200, 200, false",
	})
	void overlaps(double start, double end, boolean expected) {
		TimeInterval candidate = new TimeInterval(start, end);
		assertThat(candidate.overlaps(EXISTING)).isEqualTo(expected);
		assertThat(EXISTING.overlaps(candidate)).isEqualTo(expected);
	}

	@ParameterizedTest(name = "[{0}, {0}) overlaps [{1}, {1}) -> {2}")
	@CsvSource({
		"100, 100, true",
		"100, 101, false",
	})
	void instantsOverlapOnlyWhenEqual(double a, double b, boolean expected) {
		assertThat(new TimeInterval(a, a).overlaps(new TimeInterval(b, b))).isEqualTo(expected);
	}

	@Test
	void expandByWidensBothSides() {
		assertThat(EXISTING.expandBy(30)).isEqualTo(new TimeInterval(70, 230));
	}

	@Test
	void rejectsEndBeforeStart() {
		assertThatThrownBy(() -> new TimeInterval(200, 100)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsNaN() {
		assertThatThrownBy(() -> new TimeInterval(Double.NaN, 100)).isInstanceOf(IllegalArgumentException.class);
	}
}
