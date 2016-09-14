package playground.sebhoerl.avtaxi.framework;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.QSim;

public class AVPrebooking implements MobsimInitializedListener {
	final private PassengerEngine passengerEngine;
	
	public AVPrebooking(PassengerEngine passengerEngine) {
		this.passengerEngine = passengerEngine;
	}
	
	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		QSim qsim = (QSim) e.getQueueSimulation();
		
        /*passengerEngine.prebookTrip(
        		0.0, 
        		(MobsimPassengerAgent) qsim.getAgentMap().get(Id.createPersonId("PersonA")), 
        		Id.createLinkId("AB"), 
        		Id.createLinkId("CD"), 
        		8.0 * 3600.0 - 120.0);
        
        passengerEngine.prebookTrip(
        		0.0, 
        		(MobsimPassengerAgent) qsim.getAgentMap().get(Id.createPersonId("PersonB")), 
        		Id.createLinkId("AB"), 
        		Id.createLinkId("CD"), 
        		8.0 * 3600.0 - 115.0);
        
        passengerEngine.prebookTrip(
        		0.0, 
        		(MobsimPassengerAgent) qsim.getAgentMap().get(Id.createPersonId("PersonA")), 
        		Id.createLinkId("CD"), 
        		Id.createLinkId("AB"), 
        		16.5 * 3600.0 - 120.0);
        
        passengerEngine.prebookTrip(
        		0.0, 
        		(MobsimPassengerAgent) qsim.getAgentMap().get(Id.createPersonId("PersonB")), 
        		Id.createLinkId("CD"), 
        		Id.createLinkId("AB"), 
        		16.5 * 3600.0 - 115.0);*/
	}
}
