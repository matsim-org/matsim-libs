package org.matsim.contrib.drt.extension.alonso_mora.example;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;

public class ConvertDemand {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("demand-path", "network-path", "output-path") //
				.allowOptions("sampling-rate") //
				.build();

		String line = null;

		int indexPickupTime = -1;
		int indexOriginX = -1;
		int indexOriginY = -1;
		int indexDestinationX = -1;
		int indexDestinationY = -1;
		int indexTravelTime = -1;

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(cmd.getOptionStrict("demand-path"))));

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(cmd.getOptionStrict("network-path"));

		Population population = scenario.getPopulation();
		PopulationFactory factory = population.getFactory();
		Network network = scenario.getNetwork();

		List<String> accessibleTypes = Arrays.asList("primary", "secondary", "tertiary", "residential", "unclassified",
				"living_street", "road");

		double[] dimensions = NetworkUtils.getBoundingBox(network.getNodes().values());
		QuadTree<List<Link>> spatialIndex = new QuadTree<>(dimensions[0], dimensions[1], dimensions[2], dimensions[3]);

		for (Node node : network.getNodes().values()) {
			List<Link> candidates = new LinkedList<>();

			for (Link link : node.getInLinks().values()) {
				if (accessibleTypes.contains(link.getAttributes().getAttribute("type"))) {
					candidates.add(link);
				}
			}

			if (candidates.size() > 0) {
				spatialIndex.put(node.getCoord().getX(), node.getCoord().getY(), candidates);
			}
		}

		int tripIndex = 0;

		Random random = new Random(0);
		double samplingRate = cmd.getOption("sampling-rate").map(Double::parseDouble).orElse(1.0);

		while ((line = reader.readLine()) != null) {
			List<String> row = Arrays.asList(line.split(";"));

			if (indexPickupTime == -1) {
				indexPickupTime = row.indexOf("time");
				indexOriginX = row.indexOf("origin_x");
				indexOriginY = row.indexOf("origin_y");
				indexDestinationX = row.indexOf("destination_x");
				indexDestinationY = row.indexOf("destination_y");
				indexTravelTime = row.indexOf("travel_time");
			} else {
				double time = Double.parseDouble(row.get(indexPickupTime));
				double originX = Double.parseDouble(row.get(indexOriginX));
				double originY = Double.parseDouble(row.get(indexOriginY));
				double destinationX = Double.parseDouble(row.get(indexDestinationX));
				double destinationY = Double.parseDouble(row.get(indexDestinationY));
				double travelTime = Double.parseDouble(row.get(indexTravelTime));

				Coord originCoord = new Coord(originX, originY);
				Coord destinationCoord = new Coord(destinationX, destinationY);

				List<Link> originCandidates = spatialIndex.getClosest(originX, originY);
				List<Link> destinationCandidates = spatialIndex.getClosest(destinationX, destinationY);

				double originDistance = CoordUtils.calcEuclideanDistance(originCoord, new Coord(originX, originY));
				double destinationDistance = CoordUtils.calcEuclideanDistance(destinationCoord,
						new Coord(destinationX, destinationY));

				if (originDistance < 100 && destinationDistance < 100) {
					Link originLink = originCandidates.get(random.nextInt(originCandidates.size()));
					Link destinationLink = destinationCandidates.get(random.nextInt(destinationCandidates.size()));

					if (originLink != destinationLink) {
						if (random.nextDouble() <= samplingRate) {
							Person person = factory.createPerson(Id.createPersonId(tripIndex));
							population.addPerson(person);

							Plan plan = factory.createPlan();
							person.addPlan(plan);

							Activity originActivity = factory.createActivityFromCoord("generic", originLink.getCoord());
							originActivity.setLinkId(originLink.getId());
							originActivity.setEndTime(time);
							plan.addActivity(originActivity);

							Leg leg = factory.createLeg("drt");
							leg.setTravelTime(travelTime);
							plan.addLeg(leg);

							Activity destinationActviity = factory.createActivityFromCoord("generic",
									destinationLink.getCoord());
							destinationActviity.setLinkId(destinationLink.getId());
							plan.addActivity(destinationActviity);

							tripIndex++;
						}
					}
				}
			}
		}

		reader.close();

		new PopulationWriter(population).write(cmd.getOptionStrict("output-path"));
	}
}
