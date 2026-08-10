package org.matsim.application.prepare.counts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Measurable;
import org.matsim.counts.MeasurementLocation;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Documents how {@link MergeCounts} combines counts files that each cover their own part of the network, and what it
 * does when two of them measure the same thing.
 */
public class MergeCountsTest {

	@TempDir
	private Path tempDir;

	/**
	 * Writes a counts file holding one car volume per given link, with the hour of the day as the value, so that the
	 * origin of a value can be told from its size.
	 */
	private Path counts(String file, int year, double factor, String... linkIds) {

		Counts<Link> counts = new Counts<>();
		counts.setName(file);
		counts.setYear(year);

		for (String linkId : linkIds) {
			MeasurementLocation<Link> location = counts.createAndAddMeasureLocation(Id.createLinkId(linkId), "station-" + linkId);
			location.setCoordinates(new Coord(1, 2));

			Measurable volume = location.createVolume();
			for (int hour = 0; hour < 24; hour++) {
				volume.setAtHour(hour, hour * factor);
			}
		}

		Path path = tempDir.resolve(file);
		new CountsWriter(counts).write(path.toString());

		return path;
	}

	private Counts<Link> read(Path path) {

		Counts<Link> counts = new Counts<>();
		new MatsimCountsReader(counts).readFile(path.toString());

		return counts;
	}

	@Test
	public void disjointFilesAreCombined() {

		Path a = counts("a.xml", 2022, 1, "1", "2");
		Path b = counts("b.xml", 2022, 1, "3");
		Path out = tempDir.resolve("merged.xml");

		new MergeCounts().execute(a.toString(), b.toString(), "--output", out.toString());

		//The usual case: two sources covering their own links, so the result simply holds all of them
		Counts<Link> merged = read(out);
		assertThat(merged.getMeasureLocations()).containsOnlyKeys(
			Id.createLinkId("1"), Id.createLinkId("2"), Id.createLinkId("3"));

		//The values survive the round trip, and so does the station name of the source
		MeasurementLocation<Link> location = merged.getMeasureLocation(Id.createLinkId("3"));
		assertThat(location.getStationName()).isEqualTo("station-3");
		assertThat(location.getVolumesForMode(TransportMode.car).getAtHour(5)).hasValue(5);
	}

	@Test
	public void metadataComesFromTheFirstInput() {

		Path a = counts("a.xml", 2022, 1, "1");
		Path b = counts("b.xml", 2019, 1, "2");
		Path out = tempDir.resolve("merged.xml");

		//The inputs disagree on the year, which is only warned about, because the command cannot decide which one the
		//merged file should carry
		new MergeCounts().execute(a.toString(), b.toString(), "--output", out.toString());

		assertThat(read(out).getYear()).isEqualTo(2022);
		assertThat(read(out).getName()).isEqualTo("a.xml");

		//... unless it is told explicitly
		new MergeCounts().execute(a.toString(), b.toString(), "--output", out.toString(),
			"--year", "2023", "--name", "berlin");

		assertThat(read(out).getYear()).isEqualTo(2023);
		assertThat(read(out).getName()).isEqualTo("berlin");
	}

	@Test
	public void differentModesOnTheSameLinkAreCombined() {

		Counts<Link> freight = new Counts<>();
		MeasurementLocation<Link> location = freight.createAndAddMeasureLocation(Id.createLinkId("1"), "freight-station");
		location.createVolume(TransportMode.truck).setAtHour(0, 42);

		Path b = tempDir.resolve("freight.xml");
		new CountsWriter(freight).write(b.toString());

		Path a = counts("a.xml", 2022, 1, "1");
		Path out = tempDir.resolve("merged.xml");

		//Car and freight for the same link are not a conflict, they are two measurables of one location. This is what
		//makes merging per measurable rather than per location worthwhile
		new MergeCounts().execute(a.toString(), b.toString(), "--output", out.toString());

		MeasurementLocation<Link> merged = read(out).getMeasureLocation(Id.createLinkId("1"));
		assertThat(merged.getVolumesForMode(TransportMode.car).getAtHour(5)).hasValue(5);
		assertThat(merged.getVolumesForMode(TransportMode.truck).getAtHour(0)).hasValue(42);

		//The location was created by the first file, so its station name is the one that is kept
		assertThat(merged.getStationName()).isEqualTo("station-1");
	}

	@Test
	public void conflictFailsByDefault() {

		Path a = counts("a.xml", 2022, 1, "1");
		Path b = counts("b.xml", 2022, 10, "1");
		Path out = tempDir.resolve("merged.xml");

		//Both files hold car volumes for link 1. Counts drive the calibration, so one of the two measurements is not
		//dropped without the user saying which
		assertThatThrownBy(() -> new MergeCounts().execute(a.toString(), b.toString(), "--output", out.toString()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("--on-conflict");

		//Nothing is written, the inputs are read completely before the first line goes out
		assertThat(out).doesNotExist();
	}

	@Test
	public void conflictResolvedByInputOrder() {

		Path a = counts("a.xml", 2022, 1, "1");
		Path b = counts("b.xml", 2022, 10, "1");
		Path out = tempDir.resolve("merged.xml");

		//The inputs are a priority order: KEEP_FIRST lets the file named first win the contested measurable
		new MergeCounts().execute(a.toString(), b.toString(), "--output", out.toString(), "--on-conflict", "KEEP_FIRST");
		assertThat(read(out).getMeasureLocation(Id.createLinkId("1")).getVolumesForMode(TransportMode.car).getAtHour(3))
			.hasValue(3);

		//KEEP_LAST is the other way round, e.g. to patch an older file with a newer delivery
		new MergeCounts().execute(a.toString(), b.toString(), "--output", out.toString(), "--on-conflict", "KEEP_LAST");
		assertThat(read(out).getMeasureLocation(Id.createLinkId("1")).getVolumesForMode(TransportMode.car).getAtHour(3))
			.hasValue(30);
	}

	@Test
	public void keepLastReplacesTheWholeMeasurable() {

		//The overriding file measures fewer hours than the one it overrides
		Counts<Link> partial = new Counts<>();
		partial.createAndAddMeasureLocation(Id.createLinkId("1"), "partial").createVolume().setAtHour(0, 999);

		Path b = tempDir.resolve("partial.xml");
		new CountsWriter(partial).write(b.toString());

		Path a = counts("a.xml", 2022, 1, "1");
		Path out = tempDir.resolve("merged.xml");

		new MergeCounts().execute(a.toString(), b.toString(), "--output", out.toString(), "--on-conflict", "KEEP_LAST");

		//A measurable is replaced as a whole, not hour by hour, so the result is the winner's profile and not a
		//mixture of two daily curves that no vehicle ever drove
		Measurable volumes = read(out).getMeasureLocation(Id.createLinkId("1")).getVolumesForMode(TransportMode.car);
		assertThat(volumes.getAtHour(0)).hasValue(999);
		assertThat(volumes.getAtHour(5)).isEmpty();
		assertThat(volumes.size()).isEqualTo(1);
	}

	@Test
	public void inputHasToExist() {

		Path a = counts("a.xml", 2022, 1, "1");
		Path out = tempDir.resolve("merged.xml");

		assertThatThrownBy(() -> new MergeCounts().execute(
			a.toString(), tempDir.resolve("nope.xml").toString(), "--output", out.toString()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("does not exist");
	}
}
