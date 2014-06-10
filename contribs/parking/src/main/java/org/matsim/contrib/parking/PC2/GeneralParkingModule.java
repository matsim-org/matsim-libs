package org.matsim.contrib.parking.PC2;

import java.util.Map;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.PC2.scoring.ParkingBetas;
import org.matsim.contrib.parking.PC2.scoring.ParkingCostModel;
import org.matsim.contrib.parking.PC2.scoring.ParkingScoreManager;
import org.matsim.contrib.parking.PC2.scoring.ParkingScoringFunctionFactory;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PersonImpl;

public class GeneralParkingModule implements StartupListener, IterationStartsListener {

	protected Controler controler;
	private ParkingCostModel parkingCostModel; // TODO: don't overwrite parking cost model from config, if already set.
	private ParkingScoreManager parkingScoreManager;

	public GeneralParkingModule(Controler controler){
		this.controler = controler;
		
		parkingScoreManager=new ParkingScoreManager();
		controler.setScoringFunctionFactory(new ParkingScoringFunctionFactory (controler.getScoringFunctionFactory(),parkingScoreManager));
		controler.addControlerListener(this);
	}
	
	public void setParkingCostModel(ParkingCostModel parkingCostModel){
		this.parkingCostModel = parkingCostModel;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		parkingScoreManager.init(controler);
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		parkingScoreManager.prepareForNewIteration();
	}
}
