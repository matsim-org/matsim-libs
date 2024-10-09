package org.matsim.application.prepare.network.params;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.sumo.SumoNetworkConverter;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplyNetworkParamsTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void apply() throws Exception {

		Path networkPath = Path.of(utils.getPackageInputDirectory()).resolve("osm.net.xml");

		Path output = Path.of(utils.getOutputDirectory());

		SumoNetworkConverter converter = SumoNetworkConverter.newInstance(List.of(networkPath),
			output.resolve("network.xml"),
			"EPSG:4326", "EPSG:4326");

		converter.call();

		assertThat(output.resolve("network.xml")).exists();
		assertThat(output.resolve("network-ft.csv")).exists();

		new ApplyNetworkParams().execute(
			"capacity", "freespeed",
			"--network", output.resolve("network.xml").toString(),
			"--input-features", output.resolve("network-ft.csv").toString(),
			"--output", output.resolve("network-opt.xml").toString(),
			"--model", "org.matsim.application.prepare.network.params.ref.GermanyNetworkParams"
		);

		assertThat(output.resolve("network-opt.xml")).exists();
	}
}
