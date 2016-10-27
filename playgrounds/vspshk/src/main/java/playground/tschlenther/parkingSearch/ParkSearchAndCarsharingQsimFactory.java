/*package playground.tschlenther.parkingSearch;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.carsharing.manager.CarSharingVehiclesNew;
import org.matsim.contrib.carsharing.qsim.ParkCSVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.Map;

*//**
 * only difference to the CarSharingQsimFactory is, that we need to bind our own agentfactory (schlenther 04/16)
 *//*

public class ParkSearchAndCarsharingQsimFactory implements Provider<Netsim>{


	@Inject private  Scenario sc;
	@Inject private  Provider<TripRouter> tripRouterProvider;	
	@Inject private  EventsManager eventsManager;
	
	@Inject private LeastCostPathCalculatorFactory pathCalculatorFactory ;
	@Inject private Map<String,TravelDisutilityFactory> travelDisutilityFactories ;
	@Inject private Map<String,TravelTime> travelTimes ;

	@Override
	public Netsim get() {
		
		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		QSim qSim = new QSim(sc, eventsManager);
		
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);


        QNetsimEngineModule.configure(qSim);
		
		TeleportationEngine teleportationEngine = new TeleportationEngine(sc, eventsManager);
		qSim.addMobsimEngine(teleportationEngine);
				
		AgentFactory agentFactory = null;			
			
		CarSharingVehiclesNew carSharingVehicles = new CarSharingVehiclesNew(sc);
		//added part
		//a simple way to place vehicles at the original location at the start of each simulation
		carSharingVehicles.readVehicleLocations();
					
		TravelTime travelTime = travelTimes.get( TransportMode.car ) ;
		
		TravelDisutilityFactory travelDisutilityFactory = travelDisutilityFactories.get( TransportMode.car ) ;
		TravelDisutility travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime) ;
		
		LeastCostPathCalculator pathCalculator = pathCalculatorFactory.createPathCalculator(sc.getNetwork(), travelDisutility, travelTime ) ;
		
		agentFactory = new ParkSearchAndCarsharingAgentFactory(qSim, sc, tripRouterProvider, carSharingVehicles, pathCalculator);
		
		
		if (sc.getConfig().network().isTimeVariantNetwork()) {
				qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		
		//we need to park carsharing vehicles on the network
		ParkCSVehicles parkSource = new ParkCSVehicles(sc.getPopulation(), agentFactory, qSim,
					carSharingVehicles);
		qSim.addAgentSource(agentSource);
		qSim.addAgentSource(parkSource);
		
		return qSim;
	}
		
}
*/