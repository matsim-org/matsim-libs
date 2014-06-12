package org.matsim.contrib.parking.PC2;

import java.util.Map;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.PC2.scoring.ParkingBetas;
import org.matsim.contrib.parking.PC2.scoring.ParkingCostModel;
import org.matsim.contrib.parking.PC2.scoring.ParkingScoreManager;
import org.matsim.contrib.parking.PC2.scoring.ParkingScoringFunctionFactory;
import org.matsim.contrib.parking.PC2.simulation.ParkingInfrastructureManager;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PersonImpl;

public class GeneralParkingModule implements StartupListener, IterationStartsListener {

	private Controler controler;
	private ParkingCostModel parkingCostModel; // TODO: don't overwrite parking cost model from config, if already set.
	private ParkingScoreManager parkingScoreManager;
	public ParkingScoreManager getParkingScoreManager() {
		return parkingScoreManager;
	}

	public void setParkingScoreManager(ParkingScoreManager parkingScoreManager) {
		this.parkingScoreManager = parkingScoreManager;
	}

	protected ParkingInfrastructureManager parkingInfrastructureManager;

	public GeneralParkingModule(Controler controler){
		this.setControler(controler);
		
		controler.addControlerListener(this);
	}
	
	public void setParkingCostModel(ParkingCostModel parkingCostModel){
		this.parkingCostModel= parkingCostModel;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		parkingScoreManager.prepareForNewIteration();
	}
	
	public ParkingInfrastructureManager getParkingInfrastructure() {
		return parkingInfrastructureManager;
	}
	
	public void setParkingInfrastructurManager(ParkingInfrastructureManager parkingInfrastructureManager) {
		this.parkingInfrastructureManager = parkingInfrastructureManager;
	}

	public Controler getControler() {
		return controler;
	}

	public void setControler(Controler controler) {
		this.controler = controler;
	}
}
