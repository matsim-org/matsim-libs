package org.matsim.dsim;

import com.google.inject.Injector;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.messages.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.communication.Communicator;
import org.matsim.core.config.Config;
import org.matsim.core.controler.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

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
    private final double oversubscribe;

    public DistributedController(Communicator comm, Config config, int threads, double oversubscribe) {
        this.comm = comm;
        this.config = config;
        this.threads = threads;
        this.oversubscribe = oversubscribe;
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

        log.warn("Adding freight and ride as modes to all car links. As we need this in some scenarios. Ideally, this would be encoded in the network already.");

        scenario.getNetwork().getLinks().values().parallelStream()
                .filter(l -> l.getAllowedModes().contains(TransportMode.car))
                .forEach(l -> l.setAllowedModes(Stream.concat(l.getAllowedModes().stream(), Stream.of("freight")).collect(Collectors.toSet())));


        DistributedSimulationModule simulationModule = new DistributedSimulationModule(comm, threads, oversubscribe);

        Controler defaultController = new Controler(scenario);
        defaultController.addOverridingModule(simulationModule);

        Injector injector = defaultController.getInjector();

        Node node = injector.getInstance(Node.class);

        ControlerListenerManagerImpl listenerManager = (ControlerListenerManagerImpl) injector.getInstance(ControlerListenerManager.class);

        // Added manually, somehow not picked up by the injector
        DSimControllerListener listener = new DSimControllerListener();
        injector.injectMembers(listener);
        listenerManager.addControlerListener(listener);

        listenerManager.fireControlerStartupEvent();

        PrepareForSim prepareForSim = injector.getInstance(PrepareForSim.class);
        prepareForSim.run();

        listenerManager.fireControlerIterationStartsEvent(0, false);

        listenerManager.fireControlerBeforeMobsimEvent(0, false);

        // Run the mobsim
        DSim dsim = injector.getInstance(DSim.class);
        dsim.run();

        listenerManager.fireControlerAfterMobsimEvent(0, false);
        listenerManager.fireControlerScoringEvent(0, false);
        listenerManager.fireControlerIterationEndsEvent(0, false);

        listenerManager.fireControlerShutdownEvent(false, 0);

        OutputDirectoryHierarchy out = injector.getInstance(OutputDirectoryHierarchy.class);

        if (node.getRank() == 0) {
            // TODO: hard-coded to write network output for easier testing
            NetworkUtils.writeNetwork(scenario.getNetwork(), out.getOutputFilename(Controler.DefaultFiles.network));

            String prefix = out.getOutputFilename("events_node");
            IOHandler.mergeEvents(prefix, out.getOutputFilename("output_events.xml"));

        }
    }
}
