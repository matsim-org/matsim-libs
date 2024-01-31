package org.matsim.counts;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CountsV2Test {

	private final SplittableRandom random = new SplittableRandom(1234);
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void test_general_handling() throws IOException {

		Counts<Link> counts = new Counts<>();
		counts.setName("test");
		counts.setYear(2100);
		counts.setDescription("Test counts for several transport modes.");
		counts.setSource("unit4");
		counts.getAttributes().putAttribute("generationType", "random");

		generateDummyCounts(counts);

		CountsWriterV2 writer = new CountsWriterV2(new IdentityTransformation(), counts);
		String filename = utils.getOutputDirectory() + "test_counts.xml";

		writer.write(filename);
		assertThat(Files.exists(Path.of(filename))).isTrue();
	}

	@Test
	void test_reader_writer() throws IOException {

		String filename = utils.getOutputDirectory() + "test_counts.xml";

		Counts<Link> dummyCounts = new Counts<>();
		generateDummyCounts(dummyCounts);

		CountsWriterV2 writer = new CountsWriterV2(new IdentityTransformation(), dummyCounts);
		writer.write(filename);

		Counts<Link> counts = new Counts<>();
		CountsReaderMatsimV2 reader = new CountsReaderMatsimV2(counts, Link.class);

		Assertions.assertDoesNotThrow(() -> reader.readFile(filename));

		Map<Id<Link>, MeasurementLocation<Link>> countMap = counts.getMeasureLocations();
		Assertions.assertEquals(21, countMap.size());

		boolean onlyDailyValues = countMap.get(Id.create("12", Link.class)).getMeasurableForMode(Measurable.VOLUMES, TransportMode.car).getInterval() == 24 * 60;
		Assertions.assertFalse(onlyDailyValues);

		assertThat(dummyCounts.getMeasurableTypes())
			.isEqualTo(counts.getMeasurableTypes());


		// Compare if all content is equal
		for (Map.Entry<Id<Link>, MeasurementLocation<Link>> e : dummyCounts.getMeasureLocations().entrySet()) {
			MeasurementLocation<Link> otherLocation = counts.getMeasureLocation(e.getKey());

			for (MeasurementLocation.TypeAndMode typeAndMode : otherLocation) {

				Measurable m = e.getValue().getMeasurableForMode(typeAndMode.type(), typeAndMode.mode());
				assertThat(m)
					.isEqualTo(otherLocation.getMeasurableForMode(typeAndMode.type(), typeAndMode.mode()));
			}
		}
	}


	@Test
	void test_illegal() {
		assertThrows(IllegalArgumentException.class, () -> {

			Counts<Link> dummyCounts = new Counts<>();

			MeasurementLocation<Link> station = dummyCounts.createAndAddMeasureLocation(Id.create("12", Link.class), "12_test");
			Measurable volume = station.createVolume(TransportMode.car, Measurable.HOURLY);

			volume.setAtHour(-1, 500);

		});

	}


	@Test
	void aggregate() {

		Counts<Link> counts = new Counts<>();

		MeasurementLocation<Link> station = counts.createAndAddMeasureLocation(Id.createLinkId(1), "test");
		Measurable volumes = station.createVolume(TransportMode.car, Measurable.QUARTER_HOURLY);

		volumes.setAtMinute(0, 100);
		volumes.setAtMinute(15, 100);
		volumes.setAtMinute(45, 100);
		volumes.setAtMinute(60, 100);
		volumes.setAtMinute(75, 100);

		assertThat(volumes.aggregateAtHour(0).orElse(-1))
			.isEqualTo(300);


		assertThat(volumes.aggregateAtHour(1).orElse(-1))
			.isEqualTo(200);

		assertThat(volumes.aggregateAtHour(2).isEmpty())
			.isTrue();

		assertThat(volumes.aggregateDaily())
			.isEqualTo(500);
	}

	public void generateDummyCounts(Counts<Link> counts) {
		Set<String> modes = Set.of(TransportMode.car, TransportMode.bike, TransportMode.drt);

		URL berlin = ExamplesUtils.getTestScenarioURL("berlin");

		Network network = NetworkUtils.readNetwork(berlin + "network.xml.gz");

		int counter = 0;

		for (Id<Link> id : network.getLinks().keySet()) {
			if (counter++ > 20)
				break;

			if (id.toString().equals("12")) {
				MeasurementLocation<Link> count = counts.createAndAddMeasureLocation(id, id + "_test");
				Measurable volume = count.createVolume(TransportMode.car, Measurable.HOURLY);

				for (int i = 0; i <= 23; i++) {
					volume.setAtHour(i, random.nextInt(300, 800));
				}
				continue;
			}

			MeasurementLocation<Link> count = counts.createAndAddMeasureLocation(id, id + "_test");

			if (random.nextBoolean())
				count.getAttributes().putAttribute("testAttribute", "test");

			for (String mode : modes) {
				boolean dailyValuesOnly = random.nextBoolean();
				Measurable volume;
				if (dailyValuesOnly) {
					volume = count.createMeasurable(Measurable.VOLUMES, mode, Measurable.DAILY);
					volume.setDailyValue(random.nextInt(1000, 10000));
				} else {
					volume = count.createVolume(mode, Measurable.HOURLY);
					for (int i = 0; i <= 23; i++)
						volume.setAtHour(i, random.nextInt(300, 800));

				}

				if (random.nextBoolean()) {
					Measurable velocity = count.createVelocity(mode, Measurable.DAILY);
					velocity.setDailyValue(random.nextDouble(27.78));
				}
			}
		}
	}
}
