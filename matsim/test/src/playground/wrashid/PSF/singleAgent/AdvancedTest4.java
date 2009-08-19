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

public class AdvancedTest4 extends MatsimTestCase implements ParametersPSFMutator {

	/*
	 *  The vehicle must charge during work, because else he will not reach home.
	 *  Off peak charging can only be performed at home.
	 */

	public void mutateParameters() {
		ParametersPSF.setTestingPeakPriceStartTime(20000);
		ParametersPSF.setTestingPeakPriceEndTime(72000);
		
		ParametersPSF.setTestingEnergyConsumptionPerLink(8000000);
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
		
		// the vehicle should start charging immediately at work 20M Joules, which are needed
		// at least to reach home again. It requires 5714.28 seconds to charge this amount of
		// energy (at 3500W)
		
		// the first charging duration should be till 23400 (because of 900 second bins)
		// 
		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 22989, 1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 23400, 1);

		
		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(1);

		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 72000, 1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 111111, 1);

		// the agent should charge twice.
		assertEquals(2, chargingTimesOfAgentOne.getChargingTimes().size());
	}

	
	
	
	
}
