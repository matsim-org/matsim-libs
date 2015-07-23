package org.matsim.contrib.parking.PC2;

import org.matsim.contrib.parking.PC2.scoring.ParkingScoreManager;
import org.matsim.contrib.parking.PC2.simulation.ParkingChoiceSimulation;
import org.matsim.contrib.parking.PC2.simulation.ParkingInfrastructureManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;

public class GeneralParkingModule implements StartupListener, IterationStartsListener,BeforeMobsimListener, IterationEndsListener {

	private Controler controler;
	private ParkingScoreManager parkingScoreManager;
	public ParkingScoreManager getParkingScoreManager() {
		return parkingScoreManager;
	}

	public void setParkingScoreManager(ParkingScoreManager parkingScoreManager) {
		this.parkingScoreManager = parkingScoreManager;
	}

	protected ParkingInfrastructureManager parkingInfrastructureManager;
	private ParkingChoiceSimulation parkingSimulation;

	public GeneralParkingModule(Controler controler){
		this.controler = controler ;
		
		controler.addControlerListener(this);
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		parkingSimulation = new ParkingChoiceSimulation(controler, parkingInfrastructureManager);
		controler.getEvents().addHandler(parkingSimulation);
//		controler.addControlerListener(parkingSimulation);
		// was not doing anything there. kai, jul'15
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		
	}
	
	public ParkingInfrastructureManager getParkingInfrastructure() {
		return parkingInfrastructureManager;
	}
	
	public void setParkingInfrastructurManager(ParkingInfrastructureManager parkingInfrastructureManager) {
		this.parkingInfrastructureManager = parkingInfrastructureManager;
	}

	@Deprecated
	// lower level objects may keep back pointers to higher level objects if they have to, but we prefer that they do not provide them
	// as a service. kai, apr'15
	public Controler getControler() {
		return controler;
	}

//	public void setControler(Controler controler) {
//		this.controler = controler;
//	}
	// lower level objects may keep back pointers to higher level objects if they have to, but we prefer that they do not provide them
	// as a service. kai, apr'15

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		parkingScoreManager.prepareForNewIteration();
		parkingInfrastructureManager.reset();
		parkingSimulation.prepareForNewIteration();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
	}
}
