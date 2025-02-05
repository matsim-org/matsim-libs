package org.matsim.dsim;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.communication.Communicator;
import org.matsim.core.config.Config;
import org.matsim.core.controler.*;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple implementation of a controller. Main entry point for testing, but only a single iteration and not the full matsim loop.
 */
@Log4j2
public class DistributedController implements ControlerI {


	private final Communicator comm;
	private final Config config;
	private final int threads;

	public DistributedController(Communicator comm, Config config, int threads) {
		this.comm = comm;
		this.config = config;
		this.threads = threads;
	}

	@Override
	@SneakyThrows
	public void run() {

		// TODO: always loads whole scenario
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// index modes and activity types as ids, as we are using those later in the simulation and in the wire types
		scenario.getPopulation().getPersons().values().parallelStream()
			.flatMap(person -> person.getPlans().stream())
			.flatMap(plan -> plan.getPlanElements().stream())
			.forEach(e -> {
				if (e instanceof Activity a) {
					Id.create(a.getType(), String.class);
				} else if (e instanceof Leg l) {
					if (l.getMode() != null)
						Id.create(l.getMode(), String.class);
					if (l.getRoutingMode() != null)
						Id.create(l.getRoutingMode(), String.class);
				}
			});

		// Automatically set the network mode which is now required
		for (VehicleType value : scenario.getVehicles().getVehicleTypes().values()) {
			try {
				value.getNetworkMode();
			} catch (NullPointerException e) {
				value.setNetworkMode(value.getId().toString());
			}
		}

		log.warn("Adding freight and ride as modes to all car links. As we need this in some scenarios. Ideally, this would be encoded in the network already.");

		scenario.getNetwork().getLinks().values().parallelStream()
			.filter(l -> l.getAllowedModes().contains(TransportMode.car))
			.forEach(l -> l.setAllowedModes(Stream.concat(l.getAllowedModes().stream(), Stream.of("freight")).collect(Collectors.toSet())));

		config.dsim().setThreads(threads);

		DistributedContext ctx = DistributedContext.create(comm, config);
		Controler defaultController = new Controler(scenario, ctx);

		Injector injector = defaultController.getInjector();

		ComputeNode computeNode = injector.getInstance(ComputeNode.class);
		OutputDirectoryHierarchy io = injector.getInstance(OutputDirectoryHierarchy.class);

		ControlerListenerManagerImpl listenerManager = (ControlerListenerManagerImpl) injector.getInstance(ControlerListenerManager.class);

		addCoreControllers(listenerManager, injector);

		listenerManager.fireControlerStartupEvent();

		PrepareForSim prepareForSim = injector.getInstance(PrepareForSim.class);
		prepareForSim.run();

		injector.getInstance(IterationStopWatch.class).beginIteration(0);
		io.createIterationDirectory(0);

		listenerManager.fireControlerIterationStartsEvent(0, false);

		listenerManager.fireControlerBeforeMobsimEvent(0, false);

		// Run the mobsim
		DSim dsim = injector.getInstance(DSim.class);
		dsim.run();

		listenerManager.fireControlerAfterMobsimEvent(0, false);
		listenerManager.fireControlerScoringEvent(0, false);
		listenerManager.fireControlerIterationEndsEvent(0, false);

		listenerManager.fireControlerShutdownEvent(false, 0);

		if (computeNode.getRank() == 0) {
			// TODO: hard-coded to write network output for easier testing
			NetworkUtils.writeNetwork(scenario.getNetwork(), io.getOutputFilename(Controler.DefaultFiles.network));

//            String prefix = out.getOutputFilename("events_node");
//            IOHandler.mergeEvents(prefix, out.getOutputFilename("output_events.xml"));
		}
	}

	/**
	 * Add some default controllers to replicate behaviour of {@link NewControler}.
	 */
	private void addCoreControllers(ControlerListenerManager listenerManager, Injector injector) {

		listenerManager.addControlerListener(injector.getInstance(DumpDataAtEnd.class));
		listenerManager.addControlerListener(injector.getInstance(PlansScoring.class));
		listenerManager.addControlerListener(injector.getInstance(PlansDumping.class));
		listenerManager.addControlerListener(injector.getInstance(EventsHandling.class));

		Set<ControlerListener> listeners = injector.getInstance(Key.get(new TypeLiteral<>() {
		}));
		for (ControlerListener l : listeners) {
			listenerManager.addControlerListener(l);
		}
	}
}
