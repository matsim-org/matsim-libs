package org.matsim.application.analysis.accessibility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.pt.transitSchedule.api.*;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.matsim.contrib.accessibility.AccessibilityModule.CONFIG_FILENAME_ACCESSIBILITY;
import static org.matsim.core.scenario.ScenarioUtils.createScenario;

@CommandLine.Command(
	name = "prepare-transit-schedule", description = "Prepare Transit Schedule.",
	mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
	produces = {
		"transit-schedule-reduced.xml.gz",
		"network-reduced.xml.gz"
	}
)


public class PrepareTransitSchedule implements MATSimAppCommand {
	private static final Logger log = LogManager.getLogger(PrepareTransitSchedule.class);

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(PrepareTransitSchedule.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(PrepareTransitSchedule.class);
	public SimpleFeatureBuilder builder;


	public static void main(String[] args) {
		new PrepareTransitSchedule().execute(args);
	}

	@Override
	public Integer call() throws Exception {


		String transportScheduleFile = ApplicationUtils.matchInput("output_transitSchedule.xml.gz", input.getRunDirectory()).toString();
		String networkFile = ApplicationUtils.matchInput("output_network.xml.gz", input.getRunDirectory()).toString();

		// load transit schedule
		Scenario scenario = createScenario(ConfigUtils.createConfig());
		TransitScheduleReader transitScheduleReader = new TransitScheduleReader(scenario);
		transitScheduleReader.readFile(transportScheduleFile);

		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);


		// new transit schedule
		Scenario scenarioNew = createScenario(ConfigUtils.createConfig());
		TransitSchedule transitScheduleNew = scenarioNew.getTransitSchedule();

		// new network

		Network networkNew = scenarioNew.getNetwork();


		// get accessibility config
		AccessibilityConfigGroup acg = getAccessibilityConfig();


		// filter stops and add to new transit schedule
		Set<Id<TransitStopFacility>> stopsInBox = new HashSet<>();
		for (TransitStopFacility stop : scenario.getTransitSchedule().getFacilities().values()) {
			if (stop.getCoord().getX() > acg.getBoundingBoxRight() || stop.getCoord().getX() < acg.getBoundingBoxLeft() || stop.getCoord().getY() > acg.getBoundingBoxTop() || stop.getCoord().getY() < acg.getBoundingBoxBottom()) {
				// do nothing
			} else {
				stopsInBox.add(stop.getId());
				transitScheduleNew.addStopFacility(stop);
			}
		}

		System.out.println("Number of stops in bounding box: " + stopsInBox.size());
		System.out.println("Number of all stops: " + scenario.getTransitSchedule().getFacilities().size());


		// filter lines and add to new transit schedule
		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			routeLoop:
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					if (stopsInBox.contains(stop.getStopFacility().getId())) {
						// add route to new transit schedule
						transitScheduleNew.addTransitLine(line);
						break routeLoop; // no need to check further stops for this route
					}
				}
			}
		}

		new TransitScheduleWriter(transitScheduleNew).writeFile(
			input.getRunDirectory().resolve("analysis/accessibility/transit-schedule-reduced.xml.gz").toString()
		);


		// fill new network
		for (TransitLine line : transitScheduleNew.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Id<Link> linkId : route.getRoute().getLinkIds()) {
					Link linkToAdd = scenario.getNetwork().getLinks().get(linkId);
					if(networkNew.getLinks().containsKey(linkToAdd.getId())) {
						continue; // link already exists in new network
					}

					Node fromNode = linkToAdd.getFromNode();
					if(!networkNew.getNodes().containsKey(fromNode.getId())) {
						networkNew.addNode(fromNode);
					}

					Node toNode = linkToAdd.getToNode();
					if(!networkNew.getNodes().containsKey(toNode.getId())) {
						networkNew.addNode(toNode);
					}


					networkNew.addLink(linkToAdd);
				}
			}
		}

		new NetworkWriter(networkNew).write(
			input.getRunDirectory().resolve("analysis/accessibility/network-reduced.xml.gz").toString()
		);

		return 0;
	}

	private AccessibilityConfigGroup getAccessibilityConfig() {
		List<String> activityOptions = null;
		try {
			activityOptions = Files.list(input.getRunDirectory().resolve("analysis/accessibility/"))
				.filter(Files::isDirectory)
				.map(Path::getFileName)
				.map(Path::toString).
				collect(Collectors.toList());
		} catch (IOException e) {
			e.printStackTrace();
		}


		assert activityOptions != null;

		Config config = ConfigUtils.loadConfig(input.getRunDirectory() + "/analysis/accessibility/" + activityOptions.getFirst() + "/" + CONFIG_FILENAME_ACCESSIBILITY);
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);

		return acg;
	}


}
