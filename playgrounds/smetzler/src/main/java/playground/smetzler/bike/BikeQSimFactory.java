package playground.smetzler.bike;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.multimodal.simengine.MultiModalQSimModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.DefaultLinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import com.google.inject.Provider;

public class BikeQSimFactory implements Provider<Mobsim> {
	
	@Inject Map<String, TravelTime> multiModalTravelTimes;

	
	@Inject Scenario scenario;
	@Inject EventsManager eventsManager;
	@Override
	public Mobsim get() {

		QSimConfigGroup conf = scenario.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		// construct the QSim:
		QSim qSim = new QSim(scenario, eventsManager);

		// add the activity engine:
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);

		// add the netsim engine:

		// possible variant without Dobler approach, where link speed calculator is used to influence bicycle speeds:
//		ConfigurableQNetworkFactory qNetworkFactory = new ConfigurableQNetworkFactory(eventsManager, scenario) ;
//		qNetworkFactory.setLinkSpeedCalculator(new LinkSpeedCalculator(){
//			LinkSpeedCalculator delegate = new DefaultLinkSpeedCalculator() ;
//			@Override public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
//				if ( vehicle.getVehicle().getType().equals( "bike" ) ) {
//					return 0.1 ; // compute bicycle speed instead
//				} else {
//					return delegate.getMaximumVelocity(vehicle, link, time) ;
//				}
//			}
//		});
//		QNetsimEngine netsimEngine = new QNetsimEngine(qSim, qNetworkFactory ) ;

		QNetsimEngine netsimEngine = new QNetsimEngine(qSim ) ;
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

		TeleportationEngine teleportationEngine = new TeleportationEngine(scenario, eventsManager);
		qSim.addMobsimEngine(teleportationEngine);

		AgentFactory agentFactory = new DefaultAgentFactory(qSim);

		PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
		Map<String, VehicleType> modeVehicleTypes = new HashMap<>();

		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car, VehicleType.class));
		car.setMaximumVelocity(60.0/3.6);
		car.setPcuEquivalents(1.0);
		modeVehicleTypes.put("car", car);

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create("bike", VehicleType.class));
		bike.setMaximumVelocity(30.0/3.6);
		bike.setPcuEquivalents(0.0);
		modeVehicleTypes.put("bike", bike);

		new MultiModalQSimModule(scenario.getConfig(), this.multiModalTravelTimes).configure(qSim);
		
		agentSource.setModeVehicleTypes(modeVehicleTypes);

		qSim.addAgentSource(agentSource);

		return qSim ;
	}

}
