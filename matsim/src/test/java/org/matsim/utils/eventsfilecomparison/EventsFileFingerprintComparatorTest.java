package org.matsim.utils.eventsfilecomparison;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.events.EventsUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class EventsFileFingerprintComparatorTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testEqual() {


		assertThat(EventsUtils.createAndCompareEventsFingerprint(
				new File(utils.getClassInputDirectory(), "events_correct_fp.zst").toString(),
				new File(utils.getClassInputDirectory(), "output_events.xml").toString()
		)).isEqualTo(ComparisonResult.FILES_ARE_EQUAL);

		assertThat(EventsUtils.createAndCompareEventsFingerprint(
				new File(utils.getClassInputDirectory(), "events_correct_fp.zst").toString(),
				new File(utils.getClassInputDirectory(), "output_events.xml").toString()
		)).isEqualTo(ComparisonResult.FILES_ARE_EQUAL);

	}

	@Test
	void testDiffTimesteps() {

		assertThat(EventsUtils.createAndCompareEventsFingerprint(
				new File(utils.getClassInputDirectory(), "events_correct_fp.zst").toString(),
				new File(utils.getClassInputDirectory(), "output_events.diff-num-timestamps.xml").toString()
		)).isEqualTo(ComparisonResult.DIFFERENT_TIMESTEPS);


	}

	@Test
	void testDiffCounts() {

		assertThat(EventsUtils.createAndCompareEventsFingerprint(
				new File(utils.getClassInputDirectory(), "events_correct_fp.zst").toString(),
				new File(utils.getClassInputDirectory(), "output_events.diff-type-count.xml").toString()
		)).isEqualTo(ComparisonResult.WRONG_EVENT_COUNT);

	}

	@Test
	void testDiffContent() {
		assertThat(EventsUtils.createAndCompareEventsFingerprint(
				new File(utils.getClassInputDirectory(), "events_correct_fp.zst").toString(),
				new File(utils.getClassInputDirectory(), "output_events.diff-hash.xml").toString()
		)).isEqualTo(ComparisonResult.DIFFERENT_EVENT_ATTRIBUTES);
	}

}
