package org.matsim.dsim.simulation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.LP;
import org.matsim.api.LPProvider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.SearchableNetwork;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.dsim.MessageBroker;
import org.matsim.dsim.QSimCompatibility;
import org.matsim.dsim.simulation.net.NetworkTrafficEngine;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.matsim.dsim.NetworkDecomposition.PARTITION_ATTR_KEY;

@Log4j2
public class SimProvider implements LPProvider {

	@Inject
	private Network network;

	// this assumes, that the entire scenario is loaded. This is fine as long as the scenario fits into
	// ram. The rust prototype already supports partial loading of the scenario. This has to be added here
	// as well, for VW's use cases.
	@Inject
	private Scenario scenario;

	@Inject
	private TimeInterpretation timeInterpretation;

	@Inject
	private Config config;

	@Inject
	private MessageBroker messageBroker;

	@Inject
	private EventsManager eventsManager;

	@Inject
	private Injector injector;

	private static boolean isSplit(Link link) {
		var fromRank = (int) link.getFromNode().getAttributes().getAttribute(PARTITION_ATTR_KEY);
		var toRank = (int) link.getToNode().getAttributes().getAttribute(PARTITION_ATTR_KEY);
		return fromRank != toRank;
	}


	private static boolean startOnPartition(Person person, int part, Network network, ActivityFacilities facilities) {
		var startLinkId = getStartLink(person.getSelectedPlan(), network, facilities);
		var startLink = network.getLinks().get(startLinkId);
		Object partition = NetworkUtils.getPartition(startLink);
		if (partition == null) {
			throw new IllegalStateException("Partition attribute not set for link " + startLinkId);
		}

		return (int) partition == part;
	}

	private static Id<Link> getStartLink(Plan plan, Network network, ActivityFacilities facilities) {
		var firstAct = firstActivity(plan);
		if (firstAct.getLinkId() != null) {
			return firstAct.getLinkId();
		} else if (firstAct.getCoord() != null) {
			Link link = ((SearchableNetwork) network).getLinkQuadTree().getNearest(firstAct.getCoord().getX(), firstAct.getCoord().getY());
			return link.getId();
		} else if (firstAct.getFacilityId() != null) {
			return facilities.getFacilities().get(firstAct.getFacilityId()).getLinkId();
		}
		{
			throw new RuntimeException("Could not determine start link.");
		}
	}

	/**
	 * Assign linkIds to all plan elements. These might be missing if facilities are used.
	 */
	private static Person assignLinkIds(Person person, Network network, ActivityFacilities activityFacilities) {

		Plan plan = person.getSelectedPlan();

		for (Activity act : TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities)) {

			if (act.getLinkId() == null) {
				if (act.getCoord() != null) {
					Link link = ((SearchableNetwork) network).getLinkQuadTree().getNearest(act.getCoord().getX(), act.getCoord().getY());
					act.setLinkId(link.getId());
				}

				if (act.getFacilityId() != null) {
					ActivityFacility facility = activityFacilities.getFacilities().get(act.getFacilityId());
					act.setLinkId(facility.getLinkId());
					if (act.getCoord() == null) {
						act.setCoord(facility.getCoord());
					}
				}
			}
		}

		return person;
	}

	private static Activity firstActivity(Plan plan) {
		// assuming that agents start at an activity
		return (Activity) plan.getPlanElements().getFirst();
	}

	@Override
	public LP create(int part) {

		NetworkPartition partition = network.getPartitioning().getPartition(part);

		// wire up all the qsim parts. This can probably also be done with injection
		// but keep things simple for now.
		IntSet neighbors = network.getLinks().values().stream()
			.filter(partition::containsNodesOfLink)
			.filter(SimProvider::isSplit)
			.flatMap(l -> Stream.of(l.getFromNode(), l.getToNode()))
			.filter(n -> !partition.containsNode(n.getId()))
			.map(NetworkUtils::getPartition)
			.collect(Collectors.toCollection(IntOpenHashSet::new));

		QSimCompatibility compat = injector.getInstance(QSimCompatibility.class);

		SimStepMessaging messaging = SimStepMessaging.create(network, messageBroker, compat, neighbors, part);
		//ActivityEngineReimplementation activityEngine = createActivityEngine(part);
		TeleportationEngine teleportationEngine = new TeleportationEngine(eventsManager, messaging, compat);
		NetworkTrafficEngine networkTrafficEngine = new NetworkTrafficEngine(scenario, compat, messaging, eventsManager, part);

		return new SimProcess(
			partition, messaging, compat, teleportationEngine, networkTrafficEngine,
			eventsManager,
			config);
	}

//	private ActivityEngineReimplementation createActivityEngine(int part) {
//		var personsOnPartition = scenario.getPopulation().getPersons().values().stream()
//			.filter(p -> startOnPartition(p, part, scenario.getNetwork(), scenario.getActivityFacilities()))
//			.map(p -> assignLinkIds(p, scenario.getNetwork(), scenario.getActivityFacilities()))
//			.map(SimPerson::new)
//			.toList();
//		return new ActivityEngineReimplementation(personsOnPartition, timeInterpretation, eventsManager);
//	}
}
