package playground.wrashid.PSF.singleAgent;

import java.util.HashMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.energy.AddEnergyScoreListener;
import playground.wrashid.PSF.energy.SimulationStartupListener;
import playground.wrashid.PSF.energy.charging.ChargeLog;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF.energy.charging.optimizedCharging.OptimizedCharger;
import playground.wrashid.PSF.energy.consumption.EnergyConsumption;
import playground.wrashid.PSF.energy.consumption.LinkEnergyConsumptionLog;
import playground.wrashid.PSF.energy.consumption.LogEnergyConsumption;
import playground.wrashid.PSF.parking.LogParkingTimes;
import playground.wrashid.PSF.parking.ParkLog;
import playground.wrashid.PSF.parking.ParkingTimes;

public class BasicTests extends MatsimTestCase {
	
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
		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 72000,1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 72085,1);
		
		// the agent should charge twice.
		assertEquals(2, chargingTimesOfAgentOne.getChargingTimes().size());
	}
	
	public void testLogParkingTime() {
		Controler controler=new Controler("test/input/playground/wrashid/PSF/singleAgent/" + "config.xml");
		controler.addControlerListener(new AddEnergyScoreListener());
		controler.setOverwriteFiles(true);
		
		LogParkingTimes logParkingTimes=new LogParkingTimes(controler);
		SimulationStartupListener simulationStartupListener=new SimulationStartupListener(controler);
		controler.addControlerListener(simulationStartupListener);
		
		simulationStartupListener.addEventHandler(logParkingTimes);
		
		
		controler.run();
		
		
		ParkingTimes parkingTimes=logParkingTimes.getParkingTimes().get(new IdImpl("1"));
		
		// allow small delta of one second (because the output time in the log file is truncated
		assertEquals(parkingTimes.getFirstParkingDepartTime(),21610,1);
		assertEquals(parkingTimes.getLastParkingArrivalTime(),61449,1);
		
		
		
		
		ParkLog parkLog=parkingTimes.getParkingTimes().get(0);
		assertEquals(parkLog.getStartParkingTime(),22989,1);
		assertEquals(parkLog.getEndParkingTime(),59349,1);
	}
	
	public void testLogEnergyConsumption() {
		Controler controler=new Controler("test/input/playground/wrashid/PSF/singleAgent/" + "config.xml");
		controler.addControlerListener(new AddEnergyScoreListener());
		controler.setOverwriteFiles(true);
		
		LogEnergyConsumption logEnergyConsumption=new LogEnergyConsumption(controler);
		SimulationStartupListener simulationStartupListener=new SimulationStartupListener(controler);
		controler.addControlerListener(simulationStartupListener);
		
		simulationStartupListener.addEventHandler(logEnergyConsumption);
		
		
		controler.run();
		
		
		EnergyConsumption energyConsumption=logEnergyConsumption.getEnergyConsumption().get(new IdImpl("1"));
		
		// allow small delta of one second (because the output time in the log file is truncated
		// check the time of the entrance of the last link
		assertEquals(energyConsumption.getTempEnteranceTimeOfLastLink(),60129,1);
		LinkEnergyConsumptionLog energyLog=energyConsumption.getLinkEnergyConsumption().get(0);
		assertEquals(energyLog.getEnterTime(),21670,1);
		assertEquals(energyLog.getLeaveTime(),22029,1);
	}
	
	
}
