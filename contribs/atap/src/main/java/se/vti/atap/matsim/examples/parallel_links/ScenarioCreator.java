/**
 * se.vti.atap
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.atap.matsim.examples.parallel_links;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;

import se.vti.utils.misc.Units;

/**
 * 
 * The {@link ScenarioCreator} builds a network of parallel links
 * and a corresponding population.
 * 
 * The number of parallel links is configurable, so are their parameters. The
 * population is built such that travel occurrs from upstream origin links to
 * downstream destination links that are connected to individually configurable
 * parallel network links. The links connecting origins and destination to the
 * parallel links network are automatically configured such that all origins
 * reach the parallel links at the same time. If there is a chance that
 * congestion spills back into upstream diverges, an exception is thrown and
 * recommendations for redimensioning the system are given.
 * 
 * See {link ParallelLinkExampleRunner} for examples.
 * 
 * @author GunnarF
 *
 */
public class ScenarioCreator {

	// -------------------- CONSTANTS --------------------

	private final double sizeFactor;
	private final double bottleneckXSpacing_m;
	private final double entryNodeYCoord_m;
	private final double divergeNodeYCoord_m;
	private final double bottleneckTailYCoord_m;
	private final double bottleneckHeadYCoord_m;
	private final double mergeNodeYCoord_m;
	private final double exitNodeYCoord_m;

	private final double freeSpeed_m_s = Units.M_S_PER_KM_H * 100.0;

	private final double inflowDuration_s;
	private final double inflowDuration_h;

	// -------------------- MEMBERS --------------------

	private final Map<Integer, Double> bottleneck2capacity_veh_h = new LinkedHashMap<>();
	private final Map<List<Integer>, Double> od2demand_veh_h = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public ScenarioCreator(double inflowDuration_s, double sizeFactor) {
		this.inflowDuration_s = inflowDuration_s;
		this.inflowDuration_h = Units.H_PER_S * inflowDuration_s;

		this.sizeFactor = sizeFactor;
		this.bottleneckXSpacing_m = sizeFactor * 250.0;
		this.entryNodeYCoord_m = sizeFactor * 0.0;
		this.divergeNodeYCoord_m = sizeFactor * 100.0;
		this.bottleneckTailYCoord_m = sizeFactor * 1100.0;
		this.bottleneckHeadYCoord_m = sizeFactor * 1200.0;
		this.mergeNodeYCoord_m = sizeFactor * 2200.0;
		this.exitNodeYCoord_m = sizeFactor * 2300.0;
	}

	// -------------------- INTERNALS --------------------

	private Id<Link> linkId(Node tailNode, Node headNode) {
		return Id.createLinkId(tailNode.getId() + "->" + headNode.getId());
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setBottleneck(int bottleneckIndex, double capacity_veh_h) {
		this.bottleneck2capacity_veh_h.put(bottleneckIndex, capacity_veh_h);
	}

	public void setOD(double demand_veh_h, int... linkIndices) {
		List<Integer> od = Arrays.stream(linkIndices).boxed().toList();
		this.od2demand_veh_h.put(od, demand_veh_h);
	}

	public Config createConfig() {
		Config config = ConfigUtils.createConfig();
		config.qsim().setTrafficDynamics(TrafficDynamics.queue);
		config.replanning()
				.addStrategySettings(new StrategySettings().setStrategyName(DefaultStrategy.ReRoute).setWeight(1.0));
		config.scoring().addActivityParams(new ActivityParams("start").setScoringThisActivityAtAll(false));
		config.scoring().addActivityParams(new ActivityParams("end").setScoringThisActivityAtAll(false));
		config.scoring().addModeParams(new ModeParams(TransportMode.car).setMarginalUtilityOfTraveling(-1.0));
		return config;
	}

	public Scenario createScenario(Config config) {
		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getNetwork().setCapacityPeriod(3600.0);

		// CHECK BOTTLENECK INDEXING

		int minIndex = this.bottleneck2capacity_veh_h.keySet().stream().mapToInt(b -> b).min().getAsInt();
		int maxIndex = this.bottleneck2capacity_veh_h.keySet().stream().mapToInt(b -> b).max().getAsInt();
		int bottleneckCnt = this.bottleneck2capacity_veh_h.size();
		if (minIndex != 0 || maxIndex != bottleneckCnt - 1) {
			throw new RuntimeException("Bottlenecks need to correspond to consecutive indices 0,1,2...");
		}

		// CREATE BOTTLENECKS

		List<Link> allBottlenecks = new ArrayList<>(bottleneckCnt);
		for (int index = 0; index < bottleneckCnt; index++) {
			double xCoord_m = index * this.bottleneckXSpacing_m;
			Node tail = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId("tail" + index),
					new Coord(xCoord_m, this.bottleneckTailYCoord_m));
			Node head = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId("head" + index),
					new Coord(xCoord_m, this.bottleneckHeadYCoord_m));
			allBottlenecks.add(NetworkUtils.createAndAddLink(scenario.getNetwork(), this.linkId(tail, head), tail, head,
					NetworkUtils.getEuclideanDistance(tail.getCoord(), head.getCoord()), this.freeSpeed_m_s,
					this.bottleneck2capacity_veh_h.get(index), 1.0, null, null));
		}

		// CREATE (FIRST HALF OF) ODS

		Map<Link, Double> bottleneck2maxArrival_veh_h = new LinkedHashMap<>();

		Map<List<Integer>, Node> od2entryNode = new LinkedHashMap<>();
		Map<List<Integer>, Node> od2divergeNode = new LinkedHashMap<>();
		Map<List<Integer>, Node> od2mergeNode = new LinkedHashMap<>();
		Map<List<Integer>, Node> od2exitNode = new LinkedHashMap<>();
		Map<List<Integer>, Link> od2entryLink = new LinkedHashMap<>();
		Map<List<Integer>, Link> od2exitLink = new LinkedHashMap<>();

		double maxApproachTime_s = Double.NEGATIVE_INFINITY;

		for (List<Integer> od : this.od2demand_veh_h.keySet()) {
			String odName = "OD(" + od.stream().map(i -> Integer.toString(i)).collect(Collectors.joining(",")) + ")";
			List<? extends Link> usedBottlenecks = od.stream().map(i -> allBottlenecks.get(i)).toList();

			double odFlow_veh_h = this.od2demand_veh_h.get(od);
			for (Link bottleneck : usedBottlenecks) {
				bottleneck2maxArrival_veh_h.compute(bottleneck, (b, a) -> a == null ? odFlow_veh_h : a + odFlow_veh_h);
			}

			double xCoord_m = usedBottlenecks.stream().mapToDouble(l -> l.getFromNode().getCoord().getX()).average()
					.getAsDouble();
			Node entryNode = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId(odName + "entry"),
					new Coord(xCoord_m, this.entryNodeYCoord_m));
			Node divergeNode = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId(odName + "diverge"),
					new Coord(xCoord_m, this.divergeNodeYCoord_m));
			Node mergeNode = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId(odName + "merge"),
					new Coord(xCoord_m, this.mergeNodeYCoord_m));
			Node exitNode = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId(odName + "exit"),
					new Coord(xCoord_m, this.exitNodeYCoord_m));
			od2entryNode.put(od, entryNode);
			od2divergeNode.put(od, divergeNode);
			od2mergeNode.put(od, mergeNode);
			od2exitNode.put(od, exitNode);

			od2entryLink.put(od,
					NetworkUtils.createAndAddLink(scenario.getNetwork(), this.linkId(entryNode, divergeNode), entryNode,
							divergeNode,
							NetworkUtils.getEuclideanDistance(entryNode.getCoord(), divergeNode.getCoord()),
							this.freeSpeed_m_s, 2.0 * odFlow_veh_h, 1, null, null));
			od2exitLink.put(od,
					NetworkUtils.createAndAddLink(scenario.getNetwork(), this.linkId(mergeNode, exitNode), mergeNode,
							exitNode, NetworkUtils.getEuclideanDistance(mergeNode.getCoord(), exitNode.getCoord()),
							this.freeSpeed_m_s, 2.0 * odFlow_veh_h, 1, null, null));

			for (Link bottleneck : usedBottlenecks) {
				double approachDist_m = NetworkUtils.getEuclideanDistance(divergeNode.getCoord(),
						bottleneck.getFromNode().getCoord());
				maxApproachTime_s = Math.max(maxApproachTime_s, approachDist_m / this.freeSpeed_m_s);
			}
		}

		// CREATE (SECOND HALF OF) ODS

		for (List<Integer> od : this.od2demand_veh_h.keySet()) {
			List<? extends Link> usedBottlenecks = od.stream().map(i -> allBottlenecks.get(i)).toList();
			Node divergeNode = od2divergeNode.get(od);
			Node mergeNode = od2mergeNode.get(od);

			List<Link> divergeLinks = new ArrayList<>(od.size());
			List<Link> mergeLinks = new ArrayList<>(od.size());
			for (Link bottleneck : usedBottlenecks) {
				double approachDist_m = NetworkUtils.getEuclideanDistance(divergeNode.getCoord(),
						bottleneck.getFromNode().getCoord());
				double approachSpeed_m_s = approachDist_m / maxApproachTime_s;
				double capacity_veh_h = bottleneck2maxArrival_veh_h.get(bottleneck);
				divergeLinks.add(NetworkUtils.createAndAddLink(scenario.getNetwork(),
						this.linkId(divergeNode, bottleneck.getFromNode()), divergeNode, bottleneck.getFromNode(),
						approachDist_m, approachSpeed_m_s, capacity_veh_h, 1, null, null));
				mergeLinks.add(NetworkUtils.createAndAddLink(scenario.getNetwork(),
						this.linkId(bottleneck.getToNode(), mergeNode), bottleneck.getToNode(), mergeNode,
						approachDist_m, approachSpeed_m_s, capacity_veh_h, 1, null, null));
			}

			Id<Link> entryLinkId = od2entryLink.get(od).getId();
			Id<Link> routeLinkId1 = divergeLinks.get(0).getId();
			Id<Link> routeLinkId2 = usedBottlenecks.get(0).getId();
			Id<Link> routeLinkId3 = mergeLinks.get(0).getId();
			Id<Link> exitLinkId = od2exitLink.get(od).getId();

			PopulationFactory factory = scenario.getPopulation().getFactory();

			double agentCnt = this.inflowDuration_h * this.od2demand_veh_h.get(od);
			double delta_s = this.inflowDuration_s / agentCnt;
			double time_s = 0.0;
			for (int n = 0; n < agentCnt; n++) {

				Plan plan = factory.createPlan();
				Activity start = factory.createActivityFromLinkId("start", entryLinkId);
				start.setEndTime(time_s);
				plan.addActivity(start);
				Leg leg = factory.createLeg(TransportMode.car);
				Route route = RouteUtils.createLinkNetworkRouteImpl(entryLinkId,
						Arrays.asList(routeLinkId1, routeLinkId2, routeLinkId3), exitLinkId);
				leg.setRoute(route);
				plan.addLeg(leg);
				plan.addActivity(factory.createActivityFromLinkId("end", exitLinkId));

				Person person = factory.createPerson(Id.createPersonId(scenario.getPopulation().getPersons().size()));
				person.addPlan(plan);
				scenario.getPopulation().addPerson(person);

				time_s += delta_s;
			}
		}

		// ENSURE THAT THERE IS NO SPILLBACK INTO UPSTREAM DIVERGES

		Logger log = LogManager.getLogger(this.getClass());
		for (int bottleneckIndex = 0; bottleneckIndex < bottleneckCnt; bottleneckIndex++) {
			Link bottleneck = allBottlenecks.get(bottleneckIndex);
			double maxQueueSize_veh = Math.max(0,
					bottleneck2maxArrival_veh_h.get(bottleneck) - this.bottleneck2capacity_veh_h.get(bottleneckIndex))
					* Units.H_PER_S * this.inflowDuration_s;
			double maxQueueLength_m = scenario.getNetwork().getEffectiveCellSize() * maxQueueSize_veh;
			log.info("Maximum queue length upstream of bottleneck " + bottleneck.getId() + " is " + maxQueueLength_m
					+ "m");
			for (Link approach : bottleneck.getFromNode().getInLinks().values()) {
				log.info("  Length of upstream link " + approach.getId() + " is " + approach.getLength() + "m.");
				if (approach.getLength() < maxQueueLength_m) {
					throw new RuntimeException("Maximum queue length upstream of bottleneck " + bottleneck.getId()
							+ " is " + maxQueueLength_m + "m but length of upstream link " + approach.getId()
							+ " is just " + approach.getLength() + "m.\nPossible solution: Increase size factor to "
							+ (this.sizeFactor * maxQueueLength_m / approach.getLength()) + ".");
				}
			}
		}

		return scenario;
	}
}
