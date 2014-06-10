package org.matsim.contrib.parking.PC2;

import org.matsim.contrib.parking.PC2.scoring.ParkingCostModel;
import org.matsim.core.controler.Controler;

public class ParkingModule {

	private Controler controler;
	private ParkingCostModel parkingCostModel; // TODO: don't overwrite parking cost model from config, if already set. 

	public ParkingModule(Controler controler){
		this.controler = controler;
	}
	
	public void setParkingCostModel(ParkingCostModel parkingCostModel){
		this.parkingCostModel = parkingCostModel;
	}

}
