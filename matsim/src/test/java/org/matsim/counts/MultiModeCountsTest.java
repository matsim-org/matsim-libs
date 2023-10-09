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

public class MultiModeCountsTest {

	SplittableRandom random = new SplittableRandom(1234);

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
		writer.write(utils.getOutputDirectory() + "test_counts.xml");

		Assertions.assertThat(Files.exists(Path.of(utils.getOutputDirectory() + "test_counts.xml"))).isTrue();
	}

	@Test
	public void test_reader() throws IOException {

		MultiModeCounts<Link> dummyCounts = new MultiModeCounts<>();
		generateDummyCounts(dummyCounts);

		CountsWriterHandlerImplV2 writer = new CountsWriterHandlerImplV2(dummyCounts);
		writer.write("test_counts.xml");

		MultiModeCounts<Link> counts = new MultiModeCounts<>();
		CountsReaderMatsimV2 reader = new CountsReaderMatsimV2(counts, Link.class);

		Assertions.assertThatNoException().isThrownBy(() -> {
			reader.readFile("test_counts.xml");
		});

		Map<Id<Link>, MeasurementLocation<Link>> countMap = counts.getMeasureLocations();
		Assert.assertEquals(21, countMap.size());

		boolean onlyDailyValues = countMap.get(Id.create("12", Link.class)).getMeasurable(Measurable.VOLUMES, TransportMode.car).getInterval() == 24 * 60;
		Assert.assertFalse(onlyDailyValues);
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
				MeasurementLocation<Link> count = multiModeCounts.createAndAddCount(id, id + "_test");
				Measurable volume = count.createVolume(TransportMode.car);

				for (int i = 0; i < 24; i++) {
					volume.setAtHour(i, random.nextInt(0, 800));
				}
				continue;
			}

			MeasurementLocation<Link> count = multiModeCounts.createAndAddCount(id, id + "_test");
			for (String mode : modes) {
				boolean dailyValuesOnly = random.nextBoolean();
				Measurable volume;
				if (dailyValuesOnly) {
					volume = count.addMeasurable(Measurable.VOLUMES, mode, 24 * 60);
					volume.setDailyValue(random.nextInt(0, 10000));
				} else {
					volume = count.createVolume(mode);
					for (int i = 0; i < 24; i++)
						volume.setAtHour(i, random.nextInt(0, 800));

				}

				if (random.nextBoolean()) {
					Measurable velocity = count.createVelocity(mode);
					velocity.setDailyValue(random.nextDouble(27.78));
				}
			}
		}
	}
}
