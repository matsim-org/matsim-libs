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
	private ParkingConfig parkingConfig;

	public GeneralParkingModule(Controler controler){
		this.controler = controler;
		this.parkingConfig=new ParkingConfig();
		
		parkingScoreManager=new ParkingScoreManager();
		
		controler.addControlerListener(this);
	}
	
	public void factoryIsSetExternally(){
		parkingConfig.factoryIsSetExternally=true;
	}
	
	public void setParkingCostModel(ParkingCostModel parkingCostModel){
		this.parkingCostModel= parkingCostModel;
		parkingConfig.costModelSetExternally=true;
	}
	
	public void setupComplete(){
		if (!parkingConfig.setupComplete){
			if (!parkingConfig.factoryIsSetExternally){
				controler.setScoringFunctionFactory(new ParkingScoringFunctionFactory (controler.getScoringFunctionFactory(),parkingScoreManager));
			}
			
			parkingConfig.setupComplete=true;
		} else {
			DebugLib.stopSystemAndReportInconsistency();
		}
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		parkingConfig.consistencyCheck_setupCompleteInvoked();
		
		if (!parkingConfig.costModelSetExternally){
			if (controler.getConfig().getParam("parkingChoice", "parkingCostModel")!=null){
				// TODO: try to read model from config file
			} else {
				//TODO: assign zero cost model, here 
			}
		}
		
		parkingScoreManager.init(controler);
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		parkingScoreManager.prepareForNewIteration();
	}
}
