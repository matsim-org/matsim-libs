package playground.wrashid.PSF.energy;

import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import playground.wrashid.PSF.parking.LogParkingTimes;

public class SimulationStartupListener implements StartupListener {

	LogParkingTimes logParkingTimes;
	
	public SimulationStartupListener(LogParkingTimes logParkingTimes) {
		super();
		this.logParkingTimes = logParkingTimes;
	}

	public void notifyStartup(StartupEvent event) {
		event.getControler().getEvents().addHandler(logParkingTimes);		
	}

}
