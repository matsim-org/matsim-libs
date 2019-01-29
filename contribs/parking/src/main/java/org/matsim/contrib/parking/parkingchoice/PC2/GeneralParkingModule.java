package org.matsim.contrib.parking.parkingchoice.PC2;

import org.matsim.contrib.parking.parkingchoice.PC2.scoring.ParkingScore;
import org.matsim.contrib.parking.parkingchoice.PC2.simulation.ParkingChoiceSimulation;
import org.matsim.contrib.parking.parkingchoice.PC2.simulation.ParkingInfrastructure;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;

public class GeneralParkingModule implements StartupListener, BeforeMobsimListener {

	private Controler controler;
	private ParkingScore parkingScoreManager;
	public final ParkingScore getParkingScoreManager() {
		return parkingScoreManager;
	}

	public final void setParkingScoreManager(ParkingScore parkingScoreManager) {
		this.parkingScoreManager = parkingScoreManager;
	}

	private ParkingInfrastructure parkingInfrastructureManager;
	private ParkingChoiceSimulation parkingSimulation;

	public GeneralParkingModule(Controler controler){
		this.controler = controler ;
		
		controler.addControlerListener(this);
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		parkingSimulation = new ParkingChoiceSimulation(controler.getScenario(), parkingInfrastructureManager);
		controler.getEvents().addHandler(parkingSimulation);
//		controler.addControlerListener(parkingSimulation);
		// was not doing anything there. kai, jul'15
	}

	public final ParkingInfrastructure getParkingInfrastructure() {
		return parkingInfrastructureManager;
	}
	
	public final void setParkingInfrastructurManager(ParkingInfrastructure parkingInfrastructureManager) {
		this.parkingInfrastructureManager = parkingInfrastructureManager;
	}

	@Deprecated
	// lower level objects may keep back pointers to higher level objects if they have to, but we prefer that they do not provide them
	// as a service. kai, apr'15
	public final Controler getControler() {
		return controler;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		parkingScoreManager.prepareForNewIteration();
		parkingInfrastructureManager.reset();
		parkingSimulation.prepareForNewIteration();
	}

	protected final ParkingInfrastructure getParkingInfrastructureManager() {
		return parkingInfrastructureManager;
	}
	
	protected final ParkingChoiceSimulation getParkingSimulation() {
		return parkingSimulation;
	}	

}
