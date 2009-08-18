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

public class AdvancedTest1 extends MatsimTestCase implements ParametersPSFMutator {

	// now the car arrives earlier at home, than the peak time starts
	// this means, the car should start charging immediately at home upon arrival
	public void mutateParameters() {
		ParametersPSF.setTestingPeakPriceEndTime(60000);
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
		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 22989,1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 23104,1);
		
		chargeLogOfAgentOne=chargingTimesOfAgentOne.getChargingTimes().get(1);
		
		// when the vehicle arrives at home, there is high tariff, therefore the vehicle doesn't start charging
		// immediately, but waits until low tariff starts)
		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 61449,1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 61535,1);
		
		// the agent should charge twice.
		assertEquals(2, chargingTimesOfAgentOne.getChargingTimes().size());
	}

	
	
	
	
}
