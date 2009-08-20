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

public class AdvancedTest5 extends MatsimTestCase implements
		ParametersPSFMutator {

	/*
	 * The agent has now three activities (two times work and once time home).
	 * 
	 * - The agent will not reach the second parking if he does not charge at
	 * the first parking.
	 * 
	 * 
	 * - The vehicle should charge at the first facility enough energy, that he can reach home again (e.g.) . This means, he will charge 1.2M Joule.
	 * The vehicle will not charge at the second facility, because the charging price is the
	 * same as at the first facility (but earlier). 
	 */

	public void mutateParameters() {
		ParametersPSF.setTestingPeakPriceStartTime(20000);
		ParametersPSF.setTestingPeakPriceEndTime(72000);

		ParametersPSF.setTestingEnergyConsumptionPerLink(8000000);
	}

	public void testOptimizedCharger() {
		Controler controler = new Controler(
				"test/input/playground/wrashid/PSF/singleAgent/"
						+ "config2.xml");
		controler.addControlerListener(new AddEnergyScoreListener());
		controler.setOverwriteFiles(true);

		LogEnergyConsumption logEnergyConsumption = new LogEnergyConsumption(
				controler);
		LogParkingTimes logParkingTimes = new LogParkingTimes(controler);
		SimulationStartupListener simulationStartupListener = new SimulationStartupListener(
				controler);
		controler.addControlerListener(simulationStartupListener);

		simulationStartupListener.addEventHandler(logEnergyConsumption);
		simulationStartupListener.addEventHandler(logParkingTimes);
		simulationStartupListener.addParameterPSFMutator(this);

		controler.run();

		OptimizedCharger optimizedCharger = new OptimizedCharger(
				logEnergyConsumption.getEnergyConsumption(), logParkingTimes
						.getParkingTimes());
		HashMap<Id, ChargingTimes> chargingTimes = optimizedCharger
				.getChargingTimes();

		ChargingTimes chargingTimesOfAgentOne = chargingTimes.get(new IdImpl(
				"1"));
		ChargeLog chargeLogOfAgentOne = chargingTimesOfAgentOne
				.getChargingTimes().get(0);

		// the vehicle should start charging immediately at work 12M Joules,
		// which are needed
		// at least to reach the second parking. It requires 3428.57 seconds to charge
		// this amount of
		// energy (at 3500W)

		// the first charging duration should be till 23400 (because of 900
		// second bins)
		// 
		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 22989, 1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 23400, 1);

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(1);
		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 23400, 1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 24300, 1);

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(2);
		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 24300, 1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 25200, 1);
		
		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(3);
		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 25200, 1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 26100, 1);
		
		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(4);
		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 26100, 1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 26418, 1);
		
		// at this point, the agent has charged 12M Joules, so that he can reach 
		// the second parking. Now taking the second parking also into account,
		// the agent tries to reach home (additional 8M Joules are needed).
		// Time required for charging this much energy: 2285.7142857142857142857142857143
		
		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(5);
		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 26418, 1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 27000, 1);
		
		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(6);
		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 27000, 1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 27900, 1);
		
		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(7);
		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 27900, 1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 28704, 1);

		// the agent continues charging at home as soon as the off peak tariff
		// starts
		// actually, when the agent arrives at home, his battery is empty and
		// 10285.714 seconds
		// are needed at 3500W to recharge it.
		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(8);
		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 72000, 1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 72900, 1);

		// skipping 10 some checks

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes()
				.get(18);
		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 81000, 1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 81900, 1);

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes()
				.get(19);
		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 81900, 1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 82285, 1);

		// make sure, this is the last charging of the agent
		assertEquals(20, chargingTimesOfAgentOne.getChargingTimes().size());
	}

}
