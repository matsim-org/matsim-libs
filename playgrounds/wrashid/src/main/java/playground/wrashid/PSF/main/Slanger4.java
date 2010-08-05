package playground.wrashid.PSF.main;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.ParametersPSFMutator;
import playground.wrashid.PSF.PSS.PSSControler;
import playground.wrashid.PSF.energy.AddEnergyScoreListener;
import playground.wrashid.PSF.energy.SimulationStartupListener;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF.energy.charging.optimizedCharging.OptimizedCharger;
import playground.wrashid.PSF.energy.consumption.LogEnergyConsumption;
import playground.wrashid.PSF.parking.LogParkingTimes;

public class Slanger4 implements ParametersPSFMutator {

	public static void main(String[] args) {
		// for running on "slanger4"
		
		PSSControler pssControler=new PSSControler("a:\\data\\matsim\\input\\runRW1003\\config.xml", null);
		
		pssControler.runMATSimPSSIterations(2);
		
	} 

	public void mutateParameters() {
		// TODO Auto-generated method stub
		
	}
	
}
