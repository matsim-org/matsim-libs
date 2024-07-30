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

	private final Controler controler;
	private ParkingScore parkingScoreManager;
	private ParkingInfrastructure parkingInfrastructureManager;
	private ParkingChoiceSimulation parkingChoiceSimulation;

	public GeneralParkingModule(Controler controler){
		this.controler = controler ;
		controler.addControlerListener(this);
	}

	@Override public void notifyStartup(StartupEvent event) {
		parkingChoiceSimulation = new ParkingChoiceSimulation(controler.getScenario(), parkingInfrastructureManager);
		controler.getEvents().addHandler( parkingChoiceSimulation );
	}

	@Override public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		parkingScoreManager.notifyBeforeMobsim();
		parkingInfrastructureManager.notifyBeforeMobsim();
		parkingChoiceSimulation.notifyBeforeMobsim();
	}

	public final void setParkingScoreManager(ParkingScore parkingScoreManager) {
		this.parkingScoreManager = parkingScoreManager;
	}

	public final void setParkingInfrastructurManager(ParkingInfrastructure parkingInfrastructureManager) {
		this.parkingInfrastructureManager = parkingInfrastructureManager;
	}

}
