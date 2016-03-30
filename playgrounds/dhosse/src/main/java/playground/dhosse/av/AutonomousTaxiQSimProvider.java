package playground.dhosse.av;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.router.DistanceAsTravelDisutility;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.contrib.taxi.TaxiActionCreator;
import org.matsim.contrib.taxi.TaxiRequestCreator;
import org.matsim.contrib.taxi.TaxiUtils;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams.TravelTimeSource;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.taxi.scheduler.TaxiSchedulerParams;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.jbischoff.taxi.usability.TaxiConfigGroup;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class AutonomousTaxiQSimProvider implements Provider<QSim> {

	
	private TaxiConfigGroup tcg;
	private MatsimVrpContextImpl context;
	private RuleBasedTaxiOptimizer optimizer;
	private EventsManager events;
	private TravelTime travelTime;
	    
	@Inject
	AutonomousTaxiQSimProvider(Config config, MatsimVrpContext context, EventsManager events,
	        Map<String, TravelTime> travelTimes)
	{
	    this.tcg = (TaxiConfigGroup)config.getModule("taxiConfig");
	    this.context = (MatsimVrpContextImpl)context;
	    this.events = events;
	    this.travelTime = travelTimes.get(TransportMode.car);

	}
	
	@Override
	public QSim get() {
		return createMobsim(context.getScenario(), this.events);
	}

	private QSim createMobsim(Scenario sc, EventsManager eventsManager)
    {
        initiate();
        QSim qSim = DynAgentLauncherUtils.initQSim(sc, eventsManager);
        qSim.addQueueSimulationListeners(optimizer);
        context.setMobsimTimer(qSim.getSimTimer());
        PassengerEngine passengerEngine = VrpLauncherUtils.initPassengerEngine(TaxiUtils.TAXI_MODE,
                new TaxiRequestCreator(), optimizer, context, qSim);
        LegCreator legCreator = VrpLegs.createLegWithOfflineTrackerCreator(qSim.getSimTimer());
        TaxiActionCreator actionCreator = new TaxiActionCreator(passengerEngine, legCreator,
                tcg.getPickupDuration());
        
//        VrpLauncherUtils.initAgentSources(qSim, context, optimizer, actionCreator);
        //instead of the common agent sources, add a population and an autonomous taxi agent source
        qSim.addAgentSource(new AutonomousTaxiAgentSource(actionCreator, context, optimizer, qSim));
        qSim.addAgentSource(new PopulationAgentSource(context.getScenario().getPopulation(),
                new DefaultAgentFactory(qSim), qSim));
        
        return qSim;
    
    }


    void initiate()
    {
        //this initiation takes place upon creating qsim for each iteration
        TravelDisutility travelDisutility = new DistanceAsTravelDisutility();

        TaxiSchedulerParams params = new TaxiSchedulerParams(tcg.isDestinationKnown(),
                tcg.isVehicleDiversion(), tcg.getPickupDuration(), tcg.getDropoffDuration(), 1.);

        resetSchedules(context.getVrpData().getVehicles().values());

        TaxiScheduler scheduler = new TaxiScheduler(context, params, travelTime, travelDisutility);

        RuleBasedTaxiOptimizerParams optimParams = new RuleBasedTaxiOptimizerParams(createConfig());

        TaxiOptimizerContext optimContext = new TaxiOptimizerContext(context, travelTime,
                travelDisutility, optimParams, scheduler);
        optimizer = new RuleBasedTaxiOptimizer(optimContext);

    }


    private void resetSchedules(Iterable<Vehicle> vehicles)
    {

        for (Vehicle v : vehicles) {
            VehicleImpl vi = (VehicleImpl)v;
            vi.resetSchedule();

        }
    }
    
    private Configuration createConfig()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("netFile", this.context.getScenario().getConfig().network().getInputFile());

        //demand: 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0
        map.put("plansFile", this.context.getScenario().getConfig().plans().getInputFile());
        //supply: 25, 50
        map.put("taxisFile", this.tcg.getVehiclesFile());

        map.put("onlineVehicleTracker", Boolean.FALSE);
        map.put("otfvis", "true");

        String sPrefix = "";
        map.put(sPrefix + "destinationKnown", this.tcg.isDestinationKnown());
        map.put(sPrefix + "vehicleDiversion", this.tcg.isVehicleDiversion());
        map.put(sPrefix + "pickupDuration", this.tcg.getPickupDuration());
        map.put(sPrefix + "dropoffDuration", this.tcg.getDropoffDuration());

        String oPrefix = "";
        map.put(oPrefix + AbstractTaxiOptimizerParams.PARAMS_CLASS,
                RuleBasedTaxiOptimizerParams.class.getName());
        map.put(oPrefix + AbstractTaxiOptimizerParams.ID, "AT");
        map.put(oPrefix + AbstractTaxiOptimizerParams.TRAVEL_TIME_SOURCE,
                TravelTimeSource.FREE_FLOW_SPEED.name());
        map.put(oPrefix + RuleBasedTaxiOptimizerParams.GOAL, RuleBasedTaxiOptimizer.Goal.MIN_PICKUP_TIME.name());
        map.put(oPrefix + RuleBasedTaxiOptimizerParams.NEAREST_REQUESTS_LIMIT, this.tcg.getNearestRequestsLimit());
        map.put(oPrefix + RuleBasedTaxiOptimizerParams.NEAREST_VEHICLES_LIMIT, this.tcg.getNearestVehiclesLimit());
        map.put(oPrefix + RuleBasedTaxiOptimizerParams.CELL_SIZE, 1000);

        return new MapConfiguration(map);
    }
	
}
