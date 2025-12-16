package org.matsim.dsim;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.api.core.v01.messages.ShutDownMessage;
import org.matsim.api.core.v01.messages.StartUpMessage;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkPartitioning;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationPartition;
import org.matsim.core.communication.Communicator;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.serialization.SerializationProvider;
import org.matsim.dsim.executors.LPExecutor;

import java.util.List;

/**
 * Controller listener running during distributed simulation.
 */
public class DSimControllerListener implements StartupListener, ShutdownListener, BeforeMobsimListener, IterationEndsListener {

	private static final Logger log = LogManager.getLogger(DSimControllerListener.class);

	public static double PRIORITY = -100;

	@Inject
	private Scenario scenario;

	@Inject
	private Topology topology;

	@Inject
	private ComputeNode computeNode;

	@Inject
	private Communicator comm;

	@Inject
	private SerializationProvider serializer;

	@Inject
	private Injector injector;

	@Override
	public double priority() {
		return PRIORITY;
	}

	@Override
	public void notifyStartup(StartupEvent event) {

		// Right now every node is required to perform the same partitioning to that results are consistent
		// TODO: partitioning can be performed on one node only, and then broadcast to all nodes
		// TODO: one lp provider may want to access partition information of another lp
		NetworkDecomposition.partition(scenario.getNetwork(), scenario.getPopulation(), scenario.getConfig(), topology.getTotalPartitions());

		NetworkPartitioning partitioning = new NetworkPartitioning(computeNode, scenario.getNetwork());
		scenario.getNetwork().setPartitioning(partitioning);

		StartUpMessage msg = new StartUpMessage(
			Id.getAllIds(Link.class),
			Id.getAllIds(Node.class),
			Id.getAllIds(Person.class)
		);

		log.info("Sending startup messages...");

		// Check if Ids are consistent
		List<StartUpMessage> all = comm.allGather(msg, -1, serializer);
		for (StartUpMessage m : all) {
			Id.check(Link.class, m.linkIds());
			Id.check(Node.class, m.nodeIds());
			Id.check(Person.class, m.personIds());
		}
	}

	private void partitionPopulation(NetworkPartitioning partitioning) {
		LazyPopulationPartition population = (LazyPopulationPartition) injector.getInstance(PopulationPartition.class);
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Id<Link> startLink = PopulationAgentSource.getStartLink(scenario, person);
			if (partitioning.isLinkOnCurrentNode(startLink)) {
				population.addPerson(person.getId());
			}
		}

		log.info("Partition #{} contains {} persons", computeNode.getRank(), population.size());
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		if (topology.getNodesCount() > 1) {
			partitionPopulation(scenario.getNetwork().getPartitioning());
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {

		injector.getInstance(LPExecutor.class).shutdown();

		// Wait for all nodes to finish
		comm.allGather(new ShutDownMessage(), Integer.MIN_VALUE, serializer);

		log.info("Simulation finished");

	}

}
