package playground.wrashid.PSF.singleAgent;

import java.util.HashMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.ParametersPSFMutator;
import playground.wrashid.PSF.energy.AddEnergyScoreListener;
import playground.wrashid.PSF.energy.SimulationStartupListener;
import playground.wrashid.PSF.energy.charging.ChargeLog;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF.energy.charging.optimizedCharging.OptimizedCharger;
import playground.wrashid.PSF.energy.consumption.LogEnergyConsumption;
import playground.wrashid.PSF.parking.LogParkingTimes;

public class AdvancedTest2 extends MatsimTestCase implements ParametersPSFMutator {

	/*
	 *  The agent arrives at work after starting of peak hour tariff, therefore the car does not charge at work.
	 *  The agent starts charging at home after peak hour.
	 */

	public void mutateParameters() {
		ParametersPSF.setTestingPeakPriceStartTime(20000);
	}
	
	public void testOptimizedCharger() {
		Controler controler=new Controler("test/input/playground/wrashid/PSF/singleAgent/" + "config.xml");
		controler.addControlerListener(new AddEnergyScoreListener());
		controler.setOverwriteFiles(true);
		
		LogEnergyConsumption logEnergyConsumption=new LogEnergyConsumption(controler);
		LogParkingTimes logParkingTimes=new LogParkingTimes(controler);
		SimulationStartupListener simulationStartupListener=new SimulationStartupListener(controler);
		controler.addControlerListener(simulationStartupListener);
		
		simulationStartupListener.addEventHandler(logEnergyConsumption);
		simulationStartupListener.addEventHandler(logParkingTimes);
		simulationStartupListener.addParameterPSFMutator(this);
		
		controler.run();
		
		OptimizedCharger optimizedCharger= new OptimizedCharger(logEnergyConsumption.getEnergyConsumption(),logParkingTimes.getParkingTimes());
		HashMap<Id, ChargingTimes> chargingTimes=optimizedCharger.getChargingTimes();
		
		ChargingTimes chargingTimesOfAgentOne=chargingTimes.get(new IdImpl("1"));
		ChargeLog chargeLogOfAgentOne=chargingTimesOfAgentOne.getChargingTimes().get(0);
		
		// the first charging opportunity at work is used (it has low tariff)
		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 72000,1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 72200,1);
		
		// the agent should charge only using one slot
		assertEquals(1, chargingTimesOfAgentOne.getChargingTimes().size());
	}

	
	
	
	
}
