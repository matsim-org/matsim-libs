package playground.smetzler.bike;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import com.google.inject.Provider;

public class BikeQSimFactory implements Provider<Mobsim> {
	
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
		QNetsimEngine netsimEngine = new QNetsimEngine(qSim) ;
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
		bike.setMaximumVelocity(20.0/3.6);
		bike.setPcuEquivalents(0.25);
		modeVehicleTypes.put("bike", bike);

//		VehicleType bicycles = VehicleUtils.getFactory().createVehicleType(Id.create("bicycle", VehicleType.class));
//		bicycles.setMaximumVelocity(15.0/3.6);
//		bicycles.setPcuEquivalents(0.05);
//		modeVehicleTypes.put("bicycle", bicycles);
//
//		VehicleType walks = VehicleUtils.getFactory().createVehicleType(Id.create("walk", VehicleType.class));
//		walks.setMaximumVelocity(1.5);
//		walks.setPcuEquivalents(0.10);  			// assumed pcu for walks is 0.1
//		modeVehicleTypes.put("walk", walks);

		agentSource.setModeVehicleTypes(modeVehicleTypes);

		qSim.addAgentSource(agentSource);

		return qSim ;
	}

}
