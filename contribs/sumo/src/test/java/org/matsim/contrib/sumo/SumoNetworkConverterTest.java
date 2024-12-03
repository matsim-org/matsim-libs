package org.matsim.contrib.sumo;

import com.google.common.io.Resources;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class SumoNetworkConverterTest {


	@Test
	void convert() throws Exception {

		Path input = Files.createTempFile("sumo", ".xml");
		Path output = Files.createTempFile("matsim", ".xml");

		Files.copy(Resources.getResource("osm.net.xml").openStream(), input, StandardCopyOption.REPLACE_EXISTING);

		SumoNetworkConverter converter = SumoNetworkConverter.newInstance(List.of(input), output, "EPSG:4326", "EPSG:4326");

		converter.call();

		Network network = NetworkUtils.readNetwork(output.toString());

		assert network.getNodes().size() == 21 : "Must contain 21 nodes";
		assert network.getNodes().containsKey(Id.createNodeId("251106770")) : "Must contain specific id";

		Link link = network.getLinks().get(Id.createLinkId("-461905066#1"));

		List<List<Id<Link>>> disallowed = NetworkUtils.getDisallowedNextLinks(link).getDisallowedLinkSequences(TransportMode.car);
		assert disallowed.contains(List.of(Id.createLinkId("461905066#0"))) : "Must contain disallowed link sequence";

		Path geometry = Path.of(output.toString().replace(".xml", "-linkGeometries.csv"));

		assert Files.exists(geometry) : "Geometries must exist";

		String csv = output.toString().replace(".xml", "-ft.csv");
		Path fts = Path.of(csv);

		assert Files.exists(fts) : "Features must exists";

		CSVParser parser = CSVParser.parse(new File(csv), StandardCharsets.UTF_8, CSVFormat.DEFAULT.builder().setHeader().setHeader().build());

		List<String> header = parser.getHeaderNames();
		Assertions.assertEquals("linkId", header.get(0));
		Assertions.assertEquals("highway_type", header.get(1));

	}
}
