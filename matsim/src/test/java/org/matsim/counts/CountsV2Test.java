package org.matsim.counts;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
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

public class CountsV2Test {

	private final SplittableRandom random = new SplittableRandom(1234);
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void test_general_handling() throws IOException {

		MultiModeCounts<Link> multiModeCounts = new MultiModeCounts<>();
		multiModeCounts.setName("test");
		multiModeCounts.setYear(2100);
		multiModeCounts.setDescription("Test counts for several transport modes.");
		multiModeCounts.setSource("unit4");
		multiModeCounts.getAttributes().putAttribute("generationType", "random");

		generateDummyCounts(multiModeCounts);

		CountsWriterHandlerImplV2 writer = new CountsWriterHandlerImplV2(multiModeCounts);
		String filename = utils.getOutputDirectory() + "test_counts.xml";

		writer.write(filename);
		assertThat(Files.exists(Path.of(filename))).isTrue();
	}

	@Test
	public void test_reader_writer() throws IOException {

		String filename = utils.getOutputDirectory() + "test_counts.xml";

		MultiModeCounts<Link> dummyCounts = new MultiModeCounts<>();
		generateDummyCounts(dummyCounts);

		CountsWriterHandlerImplV2 writer = new CountsWriterHandlerImplV2(dummyCounts);
		writer.write(filename);

		MultiModeCounts<Link> counts = new MultiModeCounts<>();
		CountsReaderMatsimV2 reader = new CountsReaderMatsimV2(counts, Link.class);

		Assertions.assertThatNoException().isThrownBy(() -> reader.readFile(filename));

		Map<Id<Link>, MeasurementLocation<Link>> countMap = counts.getMeasureLocations();
		Assert.assertEquals(21, countMap.size());

		boolean onlyDailyValues = countMap.get(Id.create("12", Link.class)).getMeasurableForMode(Measurable.VOLUMES, TransportMode.car).getInterval() == 24 * 60;
		Assert.assertFalse(onlyDailyValues);

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


	@Test(expected = IllegalArgumentException.class)
	public void test_illegal() {

		MultiModeCounts<Link> dummyCounts = new MultiModeCounts<>();

		MeasurementLocation<Link> station = dummyCounts.createAndAddMeasureLocation(Id.create("12", Link.class), "12_test");
		Measurable volume = station.createVolume(TransportMode.car, Measurable.HOURLY);

		volume.setAtHour(0, 500);

	}

	public void generateDummyCounts(MultiModeCounts<Link> multiModeCounts) {
		Set<String> modes = Set.of(TransportMode.car, TransportMode.bike, TransportMode.drt);

		URL berlin = ExamplesUtils.getTestScenarioURL("berlin");

		Network network = NetworkUtils.readNetwork(berlin + "network.xml.gz");

		int counter = 0;

		for (Id<Link> id : network.getLinks().keySet()) {
			if (counter++ > 20)
				break;

			if (id.toString().equals("12")) {
				MeasurementLocation<Link> count = multiModeCounts.createAndAddMeasureLocation(id, id + "_test");
				Measurable volume = count.createVolume(TransportMode.car, Measurable.HOURLY);

				for (int i = 1; i <= 24; i++) {
					volume.setAtHour(i, random.nextInt(300, 800));
				}
				continue;
			}

			MeasurementLocation<Link> count = multiModeCounts.createAndAddMeasureLocation(id, id + "_test");

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
					for (int i = 1; i <= 24; i++)
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
