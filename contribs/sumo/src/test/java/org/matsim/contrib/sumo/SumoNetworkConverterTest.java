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

	/**
	 * The bike-carrying highway types have no entry in
	 * {@code LinkProperties.createLinkProperties()}, so without the extra puts in
	 * {@link SumoNetworkConverter#convert(Network)} their edges are dropped with
	 * "Skipping unknown link type" and the cycling infrastructure on them is lost.
	 */
	@Test
	void convertsBikeCarryingHighwayTypes() throws Exception {

		Path input = Files.createTempFile("sumo-bike-types", ".xml");
		Path output = Files.createTempFile("matsim-bike-types", ".xml");

		Files.copy(Resources.getResource("bike-types.net.xml").openStream(), input, StandardCopyOption.REPLACE_EXISTING);

		SumoNetworkConverter.newInstance(List.of(input), output, "EPSG:25832", "EPSG:25832").call();

		Network network = NetworkUtils.readNetwork(output.toString());

		for (String type : List.of("footway", "pedestrian", "track", "cycleway")) {
			List<? extends Link> links = network.getLinks().values().stream()
				.filter(l -> ("highway." + type).equals(l.getAttributes().getAttribute(NetworkUtils.TYPE)))
				.toList();

			Assertions.assertFalse(links.isEmpty(), "No link of type highway." + type + " survived the conversion");

			for (Link link : links) {
				Assertions.assertTrue(link.getAllowedModes().contains(TransportMode.bike),
					"Link " + link.getId() + " (highway." + type + ") must allow bike");
				Assertions.assertTrue(link.getCapacity() > 0,
					"Link " + link.getId() + " (highway." + type + ") must have a positive capacity");
				Assertions.assertTrue(link.getFreespeed() > 0,
					"Link " + link.getId() + " (highway." + type + ") must have a positive freespeed");
			}
		}

		// the residential control keeps car access, so the added properties did not leak into it
		Assertions.assertTrue(network.getLinks().values().stream()
				.filter(l -> "highway.residential".equals(l.getAttributes().getAttribute(NetworkUtils.TYPE)))
				.allMatch(l -> l.getAllowedModes().contains(TransportMode.car)),
			"Residential control links must still allow car");
	}
}
