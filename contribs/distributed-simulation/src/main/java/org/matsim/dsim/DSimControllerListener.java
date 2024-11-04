package org.matsim.dsim;

import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.messages.ShutDownMessage;
import org.matsim.api.core.v01.messages.StartUpMessage;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.communication.Communicator;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.serialization.SerializationProvider;
import org.matsim.dsim.executors.LPExecutor;

import java.util.List;

/**
 * Controller listener running during distributed simulation.
 */
@Log4j2
public class DSimControllerListener implements StartupListener, ShutdownListener, BeforeMobsimListener {

	public static double PRIORITY = -100;

	@Inject
	private Scenario scenario;

	@Inject
	private Topology topology;

	@Inject
	private EventsManager manager;

	@Inject
	private Communicator comm;

	@Inject
	private MessageBroker broker;

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

		StartUpMessage msg = StartUpMessage.builder()
			.linkIds(Id.getAllIds(Link.class))
			.nodeIds(Id.getAllIds(Node.class))
			.personIds(Id.getAllIds(Person.class))
			.build();

		log.info("Sending startup messages...");

		// Check if Ids are consistent
		log.debug("#" + comm.getRank() + " notifyStartup before allGather Ids.");
		List<StartUpMessage> all = comm.allGather(msg, 10, serializer);
		log.debug("#" + comm.getRank() + " notifyStartup after allGather Ids.");
		for (StartUpMessage m : all) {
			Id.check(Link.class, m.getLinkIds());
			Id.check(Node.class, m.getNodeIds());
			Id.check(Person.class, m.getPersonIds());
		}

	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {

		// TODO: need to check if multiple syncs are required

		// Event handler have already been registered at this point
		if (manager instanceof DistributedEventsManager d) {
			d.syncEventRegistry(comm);
		}

	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {

		injector.getInstance(LPExecutor.class).shutdown();

		// Wait for all nodes to finish
		comm.allGather(new ShutDownMessage(), Integer.MAX_VALUE, serializer);

		log.info("Simulation finished");

	}
}
