package org.matsim.application.prepare.network.zone_preparation;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import picocli.CommandLine;

import java.util.*;

public class PrepareMaxTravelTimeBasedZonalSystem implements MATSimAppCommand {
	@CommandLine.Option(names = "--input", required = true, description = "input network path")
	private String inputNetworkPath;

	@CommandLine.Option(names = "--output", required = true, description = "output network path")
	private String outputNetworkPath;

	@CommandLine.Option(names = "--max-travel-time", defaultValue = "300", description = "max time distance away from zone centroid [second]")
	private double maxTimeDistance;

	@CommandLine.Option(names = "--iterations", defaultValue = "20", description = "number of iterations reduce number of zones")
	private int iterations;

	@CommandLine.Option(names = "--network-modes", description = "filter the network based on the modes we are interested in", split = ",", defaultValue = "car")
	private List<String> networkModes;

	// Optional shp input for network filtering
	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	@Override
	public Integer call() throws Exception {
		Network fullNetwork = NetworkUtils.readNetwork(inputNetworkPath);

		// extract the subnetwork from the full network based on allowed modes
		Network subNetwork = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getNetwork();
		new TransportModeNetworkFilter(fullNetwork).filter(subNetwork, new HashSet<>(networkModes));

		// filter the subnetwork if shp is provided
		if (shp.isDefined()){
			filterNetworkWithShp(subNetwork);
		}

		// clean the network after the filter process
		new NetworkCleaner().run(subNetwork);

		// perform zone-generation on the subnetwork (zone information will be written directly to the attributes of the links in the subnetwork)
		MaxTravelTimeBasedZoneGenerator.Builder builder = new MaxTravelTimeBasedZoneGenerator.Builder(subNetwork);
		MaxTravelTimeBasedZoneGenerator generator = builder.setTimeRadius(maxTimeDistance).setZoneIterations(iterations).build();
		generator.compute();

		// add attribute related to zonal information to the full network
		for (Id<Link> linkId : subNetwork.getLinks().keySet()) {
			Link linkInSubNetwork = subNetwork.getLinks().get(linkId);
			Link linkInFullNetwork = fullNetwork.getLinks().get(linkId);
			for (Map.Entry<String, Object> attribute : linkInSubNetwork.getAttributes().getAsMap().entrySet()) {
				// if the newly written attribute (related to zone) does not exist in full network, then add this attribute to the link in full network
				if (linkInFullNetwork.getAttributes().getAttribute(attribute.getKey()) == null) {
					linkInFullNetwork.getAttributes().putAttribute(attribute.getKey(), attribute.getValue());
				}
			}
		}
		for (Id<Node> nodeId : subNetwork.getNodes().keySet()) {
			Node nodeInSubNetwork = subNetwork.getNodes().get(nodeId);
			Node nodeInFullNetwork = fullNetwork.getNodes().get(nodeId);
			for (Map.Entry<String, Object> attribute : nodeInSubNetwork.getAttributes().getAsMap().entrySet()) {
				// if the newly written attribute (related to zone) does not exist in full network, then add this attribute to the node in full network
				if (nodeInFullNetwork.getAttributes().getAttribute(attribute.getKey()) == null) {
					nodeInFullNetwork.getAttributes().putAttribute(attribute.getKey(), attribute.getValue());
				}
			}
		}


		// write down the processed full network to the output path
		new NetworkWriter(fullNetwork).write(outputNetworkPath);

		return 0;
	}

	public static void main(String[] args) {
		new PrepareMaxTravelTimeBasedZonalSystem().execute(args);
	}

	private void filterNetworkWithShp(Network network){
		Geometry areaToKeep = shp.getGeometry();
		List<Link> linksToRemove = new ArrayList<>();
		for (Link link : network.getLinks().values()) {
			Point from = MGC.coord2Point(link.getFromNode().getCoord());
			Point to = MGC.coord2Point(link.getToNode().getCoord());
			if (!from.within(areaToKeep) || !to.within(areaToKeep)) {
				linksToRemove.add(link);
			}
		}
		for (Link link : linksToRemove) {
			network.removeLink(link.getId());
		}
	}
}
