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
				new File(utils.getClassInputDirectory(), "events_correct.xml"),
				new File(utils.getClassInputDirectory(), "correct.fp.zst").toString()
		)).isEqualTo(ComparisonResult.FILES_ARE_EQUAL);

		// Check if file has been created
		assertThat(new File(utils.getClassInputDirectory(), "events_correct.fp.zst"))
			.exists()
			.size().isGreaterThan(0);

	}

	@Test
	void testDiffTimesteps() {

		assertThat(EventsUtils.createAndCompareEventsFingerprint(
				new File(utils.getClassInputDirectory(), "events.diff-num-timestamps.xml"),
				new File(utils.getClassInputDirectory(), "correct.fp.zst").toString()
		)).isEqualTo(ComparisonResult.DIFFERENT_TIMESTEPS);


	}

	@Test
	void testDiffCounts() {

		assertThat(EventsUtils.createAndCompareEventsFingerprint(
				new File(utils.getClassInputDirectory(), "events.diff-type-count.xml"),
				new File(utils.getClassInputDirectory(), "correct.fp.zst").toString()
		)).isEqualTo(ComparisonResult.WRONG_EVENT_COUNT);

	}

	@Test
	void testDiffContent() {
		assertThat(EventsUtils.createAndCompareEventsFingerprint(
				new File(utils.getClassInputDirectory(), "events.diff-hash.xml"),
				new File(utils.getClassInputDirectory(), "correct.fp.zst").toString()
		)).isEqualTo(ComparisonResult.DIFFERENT_EVENT_ATTRIBUTES);
	}

	@Test
	void testAdditionalEvent() {
		assertThat(EventsUtils.createAndCompareEventsFingerprint(
				new File(utils.getClassInputDirectory(), "events.one-more-event.xml"),
				new File(utils.getClassInputDirectory(), "correct.fp.zst").toString()
		)).isEqualTo(ComparisonResult.DIFFERENT_TIMESTEPS);
	}

	@Test
	void testAttributeOrder() {
		assertThat(EventsUtils.createAndCompareEventsFingerprint(
				new File(utils.getClassInputDirectory(), "events.attribute-order.xml"),
				new File(utils.getClassInputDirectory(), "correct.fp.zst").toString()
		)).isEqualTo(ComparisonResult.FILES_ARE_EQUAL);
	}

	@Test
	void testEventOrder() {
		assertThat(EventsUtils.compareEventsFiles(
				new File(utils.getClassInputDirectory(), "events_correct.xml").toString(),
				new File(utils.getClassInputDirectory(), "events.event-order-wrong_logic.xml").toString()
		)).isEqualTo(ComparisonResult.FILES_ARE_EQUAL);

		assertThat(EventsUtils.createAndCompareEventsFingerprint(
				new File(utils.getClassInputDirectory(), "events.event-order-wrong_logic.xml"),
				new File(utils.getClassInputDirectory(), "correct.fp.zst").toString()
		)).isEqualTo(ComparisonResult.FILES_ARE_EQUAL);

	}


	@Test
	void testEqualFingerprints() {

		EventsUtils.createEventsFingerprint(new File(utils.getClassInputDirectory(), "events_correct.xml").toString(), new File(utils.getClassInputDirectory(), "events.fp.zst").toString());

		assertThat(EventsFileFingerprintComparator.compareFingerprints(
				new File(utils.getClassInputDirectory(), "events.fp.zst").toString(),
				new File(utils.getClassInputDirectory(), "correct.fp.zst").toString()
		)).isEqualTo(ComparisonResult.FILES_ARE_EQUAL);


	}

	@Test
	void testDiffTimestepsFingerprints() {

		assertThat(EventsFileFingerprintComparator.compareFingerprints(
				new File(utils.getClassInputDirectory(), "events.diff-num-timestamps.fp.zst").toString(),
				new File(utils.getClassInputDirectory(), "correct.fp.zst").toString()
		)).isEqualTo(ComparisonResult.DIFFERENT_NUMBER_OF_TIMESTEPS);


	}

	@Test
	void testDiffCountsFingerprints() {

		assertThat(EventsFileFingerprintComparator.compareFingerprints(
				new File(utils.getClassInputDirectory(), "events.diff-type-count.fp.zst").toString(),
				new File(utils.getClassInputDirectory(), "correct.fp.zst").toString()
		)).isEqualTo(ComparisonResult.WRONG_EVENT_COUNT);

	}

	@Test
	void assertion() {

		AssertionError err = assertThrows(AssertionError.class, () -> {
			EventsUtils.assertEqualEventsFingerprint(
					new File(utils.getClassInputDirectory(), "events.diff-num-timestamps.xml"),
					new File(utils.getClassInputDirectory(), "correct.fp.zst").toString()
			);
		});

		assertThat(err).message().isEqualTo("""
				Difference occurred in this event time=48067.0 | link=1 | type=entered link | vehicle=5 |\s
				Count for event type 'entered link' differs: 2 (in fingerprint) != 1 (in events)
				Count for event type 'vehicle leaves traffic' differs: 1 (in fingerprint) != 0 (in events)""");

	}
}
