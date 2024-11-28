package org.matsim.dsim.simulation;

import com.google.inject.Inject;
import com.google.inject.Injector;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.LP;
import org.matsim.api.LPProvider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkUtils;
import org.matsim.dsim.MessageBroker;
import org.matsim.dsim.QSimCompatibility;
import org.matsim.dsim.simulation.net.NetworkTrafficEngine;

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
	private Config config;

	@Inject
	private MessageBroker messageBroker;

	@Inject
	private EventsManager eventsManager;

	@Inject
	private Injector injector;


	private QSimCompatibility singletons;

	private static boolean isSplit(Link link) {
		var fromRank = (int) link.getFromNode().getAttributes().getAttribute(PARTITION_ATTR_KEY);
		var toRank = (int) link.getToNode().getAttributes().getAttribute(PARTITION_ATTR_KEY);
		return fromRank != toRank;
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

		// The first created partition will hold the singletons
		if (singletons == null)
			singletons = compat;

		SimStepMessaging messaging = SimStepMessaging.create(network, messageBroker, compat, neighbors, part);
		//ActivityEngineReimplementation activityEngine = createActivityEngine(part);
		DistributedTeleportationEngine teleportationEngine = new DistributedTeleportationEngine(eventsManager, messaging, compat);
		//var simNetwork = new SimNetwork(scenario.getNetwork(), scenario.getConfig(), this::handleVehicleIsFinished, part);
		NetworkTrafficEngine networkTrafficEngine = new NetworkTrafficEngine(scenario, compat, messaging, eventsManager, part);
		var simNetwork = networkTrafficEngine.getSimNetwork();
		//var qsimPtEngine = injector.getInstance(TransitQSimEngine.class);
		//var ptEngine = new DistributedPtEngine(scenario, qsimPtEngine, simNetwork);

		return new SimProcess(
			partition, messaging, compat, singletons == compat ? null : singletons,
			teleportationEngine, networkTrafficEngine,
			eventsManager, config);
	}
}
