package org.matsim.contrib.sumo;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

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

		Path fts = Path.of(output.toString().replace(".xml", "-ft.csv"));

		assert Files.exists(fts) : "Features must exists";

	}
}
