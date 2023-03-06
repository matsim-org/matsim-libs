/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.mobsim.qsim.pt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNode;
import org.matsim.core.mobsim.qsim.pt.AbstractTransitDriverAgent;
import org.matsim.core.mobsim.qsim.pt.DefaultTransitDriverAgentFactory;
import org.matsim.core.mobsim.qsim.pt.SimpleTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.pt.TransitQVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufImpl;
import org.matsim.pt.UmlaufStueck;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import ch.sbb.matsim.config.SBBTransitConfigGroup;

/**
 * @author cdobler
 */
public class ParallelSBBTransitQSimEngine extends TransitQSimEngine /*implements DepartureHandler, MobsimEngine, AgentSource*/ {

    private static final Logger log = LogManager.getLogger(ParallelSBBTransitQSimEngine.class);

    private final SBBTransitConfigGroup config;
    private final TransitConfigGroup ptConfig;
    private final QSim qSim;
    private TransitDriverAgentFactory deterministicDriverFactory;
    private TransitDriverAgentFactory networkDriverFactory;
    private TransitStopHandlerFactory stopHandlerFactory = new SimpleTransitStopHandlerFactory();

	private final List<SBBTransitEngineRunner> runners = new ArrayList<>();
	private final int numOfThreads;
	private final ExecutorService pool;
	private final byte[] assignedEngine;
    
    @Inject
    public ParallelSBBTransitQSimEngine(QSim qSim, ReplanningContext context) {
        super(qSim, new SimpleTransitStopHandlerFactory(), new ReconstructingUmlaufBuilder(qSim.getScenario()));
        this.qSim = qSim;
        this.config = ConfigUtils.addOrGetModule(qSim.getScenario().getConfig(), SBBTransitConfigGroup.GROUP_NAME, SBBTransitConfigGroup.class);
        this.ptConfig = qSim.getScenario().getConfig().transit();

        final Map<TransitRoute, List<Link[]>> linksCache;
        if (this.config.getCreateLinkEventsInterval() > 0) {
            linksCache = new ConcurrentHashMap<>();
        } else {
            linksCache = null;
        }

        checkSettings();

		this.numOfThreads = this.qSim.getScenario().getConfig().qsim().getNumberOfThreads();
		this.pool = Executors.newFixedThreadPool(this.numOfThreads);
		
		for (int i = 0; i < this.numOfThreads; i++) this.runners.add(new SBBTransitEngineRunner(qSim, context, this.agentTracker, linksCache));
		
		this.assignedEngine = new byte[Id.getNumberOfIds(Link.class)];
    }

    private void checkSettings() {
        if (this.config.getDeterministicServiceModes().isEmpty()) {
            log.warn("There are no modes registered for the deterministic transit simulation, so no transit vehicle will be handled by this engine.");
        }
    }

    @Override
    @Inject
    public void setTransitStopHandlerFactory(final TransitStopHandlerFactory stopHandlerFactory) {
        this.stopHandlerFactory = stopHandlerFactory;
    }

    @Override
    public void setInternalInterface(InternalInterface internalInterface) {
    	for (SBBTransitEngineRunner runner : this.runners) runner.setInternalInterface(internalInterface);

        this.deterministicDriverFactory = new SBBTransitDriverAgentFactory(internalInterface, this.agentTracker, this.config.getDeterministicServiceModes());
        this.networkDriverFactory = new DefaultTransitDriverAgentFactory(internalInterface, this.agentTracker);
    }

    @Override
    public void insertAgentsIntoMobsim() {
        createVehiclesAndDrivers();
    }

    @Override
    public boolean handleDeparture(double time, MobsimAgent agent, Id<Link> linkId) {
        return this.runners.get(this.assignedEngine[agent.getCurrentLinkId().index()]).handleDeparture(time, agent, linkId);
    }

    @Override
    public void onPrepareSim() {
    	for (SBBTransitEngineRunner runner : this.runners) runner.onPrepareSim();
		
		try {
			/*
			 * Use the same logic to assign the engines as the QNetsimEngine.
			 * TODO: solve this in a cleaner way.
			 */
			int roundRobin = 0;
			for (NetsimNode node : this.qSim.getNetsimNetwork().getNetsimNodes().values()) {
				int i = roundRobin % this.numOfThreads;
				for (Id<Link> linkId : node.getNode().getOutLinks().keySet()) this.assignedEngine[linkId.index()] = (byte) i;
				roundRobin++;
			}
		} catch (NullPointerException e) { // Should only happen in one of the unit tests.
			log.warn("Caught NullPointerException while preparing ParallelSBBTransitQSimEngine: " + e.getMessage(), e);
		}
    }

    @Override
    public void doSimStep(double time) {
		List<Callable<Boolean>> list = this.runners.stream().map(runner -> new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				runner.doSimStep(time);
				return true;
			}
		}).collect(Collectors.toList());
		
		try {
			for (Future<Boolean> future : pool.invokeAll(list)) {
				future.get();
			}
		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			log.error(e.getMessage(), e);
			if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
			else throw new RuntimeException(e);
		}
    }

    @Override
    public void afterSim() {
		List<Callable<Boolean>> list = this.runners.stream().map(runner -> new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				runner.afterSim();
				return true;
			}
		}).collect(Collectors.toList());
		
		try {
			for (Future<Boolean> future : pool.invokeAll(list)) {
				future.get();
			}
		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			log.error(e.getMessage(), e);
			if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
			else throw new RuntimeException(e);
		}
    }

    private void createVehiclesAndDrivers() {
        Scenario scenario = this.qSim.getScenario();
        TransitSchedule schedule = scenario.getTransitSchedule();
        Vehicles vehicles = scenario.getTransitVehicles();
        Set<String> deterministicModes = this.config.getDeterministicServiceModes();
        Set<String> passengerModes = this.ptConfig.getTransitModes();
        Set<String> commonModes = new HashSet<>(deterministicModes);
        commonModes.retainAll(passengerModes);
        if (!commonModes.isEmpty()) {
            throw new RuntimeException(
                    "There are modes configured to be pt passenger modes as well as deterministic service modes. This will not work! common modes = " + CollectionUtils.setToString(commonModes));
        }
        Set<String> mainModes = new HashSet<>(this.qSim.getScenario().getConfig().qsim().getMainModes());
        mainModes.retainAll(deterministicModes);
        if (!mainModes.isEmpty()) {
            throw new RuntimeException(
                    "There are modes configured to be deterministic service modes as well as qsim main modes. This will not work! common modes = " + CollectionUtils.setToString(mainModes));
        }

        for (TransitLine line : schedule.getTransitLines().values()) {
            for (TransitRoute route : line.getRoutes().values()) {
                String mode = route.getTransportMode();
                boolean isDeterministic = deterministicModes.contains(mode);
                for (Departure dep : route.getDepartures().values()) {
                    Vehicle veh = vehicles.getVehicles().get(dep.getVehicleId());
                    Umlauf umlauf = createUmlauf(line, route, dep);
                    createAndScheduleDriver(veh, umlauf, isDeterministic);
                }
            }
        }
    }

    private void createAndScheduleDriver(Vehicle veh, Umlauf umlauf, boolean isDeterministic) {
        AbstractTransitDriverAgent driver;
        if (isDeterministic) {
            driver = this.deterministicDriverFactory.createTransitDriver(umlauf);
        } else {
            driver = this.networkDriverFactory.createTransitDriver(umlauf);
        }
        TransitQVehicle qVeh = new TransitQVehicle(veh);
        qVeh.setDriver(driver);
        qVeh.setStopHandler(this.stopHandlerFactory.createTransitStopHandler(veh));
        driver.setVehicle(qVeh);

        Leg firstLeg = (Leg) driver.getNextPlanElement();
        if (!isDeterministic) {
            Id<Link> startLinkId = firstLeg.getRoute().getStartLinkId();
            this.qSim.addParkedVehicle(qVeh, startLinkId);
        }
        this.qSim.insertAgentIntoMobsim(driver);
    }

    private Umlauf createUmlauf(TransitLine line, TransitRoute route, Departure departure) {
        Id<Umlauf> id = Id.create(line.getId().toString() + "_" + route.getId().toString() + "_" + departure.getId().toString(), Umlauf.class);
        UmlaufImpl umlauf = new UmlaufImpl(id);
        UmlaufStueck part = new UmlaufStueck(line, route, departure);
        umlauf.getUmlaufStuecke().add(part);
        return umlauf;
    }
}
