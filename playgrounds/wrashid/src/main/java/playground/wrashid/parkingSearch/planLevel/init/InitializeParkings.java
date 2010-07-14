package playground.wrashid.parkingSearch.planLevel.init;

import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkLayer;

import playground.wrashid.lib.GlobalRegistry;

public class InitializeParkings implements StartupListener {

	public void notifyStartup(StartupEvent event) {
		ParkingRoot.init((ActivityFacilitiesImpl) event.getControler().getFacilities(), (NetworkLayer) event.getControler().getNetwork(), event.getControler());
		
		
		
		GlobalRegistry.controler=event.getControler();
	}

}
