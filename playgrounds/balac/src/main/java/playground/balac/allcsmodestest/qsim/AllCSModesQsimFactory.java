package playground.balac.allcsmodestest.qsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import playground.balac.freefloating.qsim.FreeFloatingVehiclesLocation;
import playground.balac.onewaycarsharingredisgned.qsimparking.OneWayCarsharingRDWithParkingVehicleLocation;
import playground.balac.twowaycarsharingredisigned.qsim.TwoWayCSVehicleLocation;

import java.io.IOException;

/*
 *
 *
 *
 */

public class AllCSModesQsimFactory implements MobsimFactory{


	private final Scenario scenario;
	private final Controler controler;	
	
	private CarSharingVehicles carSharingVehicles;
	
	public AllCSModesQsimFactory(final Scenario scenario, final Controler controler) {
		
		this.scenario = scenario;
		this.controler = controler;
		carSharingVehicles = null;		
	}
	
		
	@Override
	public Netsim createMobsim(Scenario sc, EventsManager eventsManager) {

		carSharingVehicles = new CarSharingVehicles(scenario);

		FreeFloatingVehiclesLocation ffvehiclesLocationqt = null;
		OneWayCarsharingRDWithParkingVehicleLocation owvehiclesLocationqt = null;
		TwoWayCSVehicleLocation twvehiclesLocationqt = null;		
		
		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		QSim qSim = new QSim(sc, eventsManager);
		
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);


        QNetsimEngineModule.configure(qSim);
		
		TeleportationEngine teleportationEngine = new TeleportationEngine(scenario, eventsManager);
		qSim.addMobsimEngine(teleportationEngine);
				
		AgentFactory agentFactory = null;			
			
		try {
			//adde part
			//a simple way to place vehicles at the original location at the start of each simulation
			this.carSharingVehicles.readVehicleLocations();
			ffvehiclesLocationqt = new FreeFloatingVehiclesLocation
					(this.controler, this.carSharingVehicles.getFreeFLoatingVehicles());
			owvehiclesLocationqt = new OneWayCarsharingRDWithParkingVehicleLocation
					(this.controler, this.carSharingVehicles.getOneWayVehicles());
			twvehiclesLocationqt = new TwoWayCSVehicleLocation
					(sc, this.carSharingVehicles.getRoundTripVehicles());
		
		agentFactory = new AllCSModesAgentFactory(qSim, scenario, controler, ffvehiclesLocationqt, owvehiclesLocationqt, twvehiclesLocationqt);
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		
		//we need to park carsharing vehicles on the network
		ParkCSVehicles parkSource = new ParkCSVehicles(sc.getPopulation(), agentFactory, qSim, ffvehiclesLocationqt, owvehiclesLocationqt, twvehiclesLocationqt);
		qSim.addAgentSource(agentSource);
		qSim.addAgentSource(parkSource);
		return qSim;
	}
		
}
