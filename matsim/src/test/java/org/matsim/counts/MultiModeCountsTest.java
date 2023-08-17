package org.matsim.counts;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

public class MultiModeCountsTest {

	SplittableRandom random = new SplittableRandom(1234);

	@Test
	public void test_general_handling() {

		MatsimTestUtils utils = new MatsimTestUtils();

		MultiModeCounts multiModeCounts = new MultiModeCounts();
		multiModeCounts.setName("test");
		multiModeCounts.setYear(2100);
		multiModeCounts.setDescription("Test counts for several transport modes.");
		multiModeCounts.setSource("unit4");
		multiModeCounts.getAttributes().putAttribute("generationType", "random");

		generateDummyCounts(multiModeCounts);

		MultiModeCountsWriter writer = new MultiModeCountsWriter(multiModeCounts);
		writer.write(utils.getOutputDirectory() + "test_counts.xml.gz");

		Assertions.assertThat(Files.exists(Path.of(utils.getOutputDirectory() + "test_counts.xml.gz"))).isTrue();
	}

	@Test
	public void test_reader(){
		MatsimTestUtils utils = new MatsimTestUtils();

		MultiModeCounts dummyCounts = new MultiModeCounts(Link.class);
		generateDummyCounts(dummyCounts);

		MultiModeCountsWriter writer = new MultiModeCountsWriter(dummyCounts);
		writer.write("test_counts.xml.gz");

		MultiModeCounts counts = new MultiModeCounts(Link.class);
		MultiModeCountsReader reader = new MultiModeCountsReader(counts);

		Assertions.assertThatNoException().isThrownBy(() -> {
			reader.readFile("test_counts.xml.gz");
		});

		Map<Id<? extends Identifiable>, MultiModeCount> countMap = counts.getCounts();
		Assert.assertEquals(21, countMap.size());

		boolean onlyDailyValues = countMap.get(Id.create("12", Link.class)).getMeasurable(Measurable.VOLUMES, TransportMode.car).hasOnlyDailyValues();
		Assert.assertFalse(onlyDailyValues);
	}

	public void generateDummyCounts(MultiModeCounts multiModeCounts){
		Set<String> modes = Set.of(TransportMode.car, TransportMode.bike, TransportMode.drt);

		URL berlin = ExamplesUtils.getTestScenarioURL("berlin");

		Network network = NetworkUtils.readNetwork(berlin + "network.xml.gz");

		int counter = 0;

		for (Id<Link> id : network.getLinks().keySet()) {
			if(counter++ > 20)
				break;

			if(id.toString().equals("12")){
				MultiModeCount count = multiModeCounts.createAndAddCount(id, id + "_test", 2100);
				Measurable volume = count.createVolume(TransportMode.car, false);

				for (int i = 0; i < 24; i++) {
					volume.addAtHour(i, random.nextInt(0, 800));
				}
				continue;
			}

			MultiModeCount count = multiModeCounts.createAndAddCount(id, id + "_test", 2100);
			for (String mode : modes) {
				boolean b = random.nextBoolean();
				Measurable volume = count.createVolume(mode, b);
				if (b) {
					volume.setDailyValue(random.nextInt(0, 10000));
				} else {

					for (int i = 0; i < 24; i++) {
						volume.addAtHour(i, random.nextInt(0, 800));
					}
				}

				if(random.nextBoolean()){
					Measurable velocity = count.createVelocity(mode, true);
					velocity.setDailyValue(random.nextDouble(27.78));
				}
			}
		}
	}
}
