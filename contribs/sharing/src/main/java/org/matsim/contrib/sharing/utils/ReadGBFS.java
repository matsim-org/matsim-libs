package org.matsim.contrib.sharing.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.sharing.io.DefaultSharingServiceSpecification;
import org.matsim.contrib.sharing.io.ImmutableSharingStationSpecification;
import org.matsim.contrib.sharing.io.ImmutableSharingVehicleSpecification;
import org.matsim.contrib.sharing.io.SharingServiceSpecification;
import org.matsim.contrib.sharing.io.SharingServiceWriter;
import org.matsim.contrib.sharing.service.SharingStation;
import org.matsim.contrib.sharing.service.SharingVehicle;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * 
 * This class allows to read GBFS and convert it to the input file 
 * needed by the sharing contrib. Currently this is only applicable
 * to the station-based services.
 *
 */
public class ReadGBFS {
	static public void main(String[] args) throws IOException, ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("url", "network-path", "crs", "network-modes", "output-path") //
				.build();

		// Define transformation

		CoordinateTransformation transformation = new GeotoolsTransformation( //
				"EPSG:4326", //
				cmd.getOptionStrict("crs"));

		// Filter network according to modes

		Network fullNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader(fullNetwork).readFile(cmd.getOptionStrict("network-path"));

		Set<String> modes = Arrays.asList(cmd.getOptionStrict("network-modes").split(",")).stream().map(String::trim)
				.collect(Collectors.toSet());

		Network network = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(fullNetwork).filter(network, modes);

		// Load main feed

		URL mainUrl = new URL(cmd.getOptionStrict("url"));
		Map<String, URL> feeds = new HashMap<>();

		{
			BufferedReader reader = IOUtils.getBufferedReader(mainUrl);
			ObjectMapper mapper = new ObjectMapper();

			JsonNode rootNode = mapper.readTree(reader);
			JsonNode feedsNode = rootNode.findPath("feeds");

			if (feedsNode.isMissingNode()) {
				throw new IllegalStateException("URL does not seem to contain 'feeds' node");
			}

			if (feedsNode.isArray()) {
				for (JsonNode feedNode : feedsNode) {
					String feedName = feedNode.findValue("name").asText();
					URL feedUrl = new URL(feedNode.findValue("url").asText());

					feeds.put(feedName, feedUrl);
				}
			}
		}

		SharingServiceSpecification service = new DefaultSharingServiceSpecification();
		Map<Id<SharingStation>, Id<Link>> stationLinks = new HashMap<>();

		{
			BufferedReader reader = IOUtils.getBufferedReader(feeds.get("station_information"));
			ObjectMapper mapper = new ObjectMapper();

			JsonNode rootNode = mapper.readTree(reader);
			JsonNode stationsNode = rootNode.findPath("stations");

			if (stationsNode.isArray()) {
				for (JsonNode stationNode : stationsNode) {
					String stationId = stationNode.findValue("station_id").asText();
					int capacity = stationNode.findValue("capacity").asInt();

					Coord coord = new Coord(stationNode.findValue("lon").asDouble(),
							stationNode.findValue("lat").asDouble());
					coord = transformation.transform(coord);

					Link link = NetworkUtils.getNearestLink(network, coord);

					service.addStation(ImmutableSharingStationSpecification.newBuilder() //
							.id(Id.create(stationId, SharingStation.class)) //
							.capacity(capacity) //
							.linkId(link.getId()) //
							.build());

					stationLinks.put(Id.create(stationId, SharingStation.class), link.getId());
				}
			}
		}

		{
			BufferedReader reader = IOUtils.getBufferedReader(feeds.get("station_status"));
			ObjectMapper mapper = new ObjectMapper();

			JsonNode rootNode = mapper.readTree(reader);
			JsonNode stationsNode = rootNode.findPath("stations");

			int vehicleIndex = 0;

			if (stationsNode.isArray()) {
				for (JsonNode stationNode : stationsNode) {
					String stationId = stationNode.findValue("station_id").asText();
					int numberOfBikes = stationNode.findValue("num_bikes_available").asInt();

					for (int k = 0; k < numberOfBikes; k++) {
						service.addVehicle(ImmutableSharingVehicleSpecification.newBuilder() //
								.id(Id.create(vehicleIndex, SharingVehicle.class)) //
								.startStationId(Id.create(stationId, SharingStation.class)) //
								.startLinkId(stationLinks.get(Id.create(stationId, SharingStation.class))) //
								.build());

						vehicleIndex++;
					}
				}
			}
		}

		String outputPath = cmd.getOptionStrict("output-path");
		new SharingServiceWriter(service).write(outputPath);
	}
}
