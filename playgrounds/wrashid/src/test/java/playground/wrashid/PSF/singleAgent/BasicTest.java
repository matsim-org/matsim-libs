package playground.wrashid.PSF.singleAgent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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
import playground.wrashid.parkingChoice.infrastructure.api.PParking;

import java.util.HashMap;
import java.util.LinkedList;

public class BasicTest extends MatsimTestCase {

	Controler controler;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Config config = loadConfig("test/input/playground/wrashid/PSF/singleAgent/config.xml");

		config.plansCalcRoute().setInsertingAccessEgressWalk(false);
		// too many things don't work with access/egress walk true. kai, jun'16

		ConfigUtils.addOrGetModule(config, ParametersPSF.PSF_MODULE, ParametersPSF.class);

		controler = new Controler(config);
		controler.getConfig().controler().setCreateGraphs(false);

		controler.addControlerListener(new AddEnergyScoreListener());
	}

	/*
	 * The agent drives to work and arrives still before the start of peak hour
	 * energy. Therefore the agent charges fully immediately. When arriving at
	 * home, it is still peak hour, therefore the agent waits, until the peak
	 * hour tariff is over and then starts charging.
	 */

	public void testOptimizedCharger() {
		LogEnergyConsumption logEnergyConsumption = new LogEnergyConsumption(controler);
		LogParkingTimes logParkingTimes = new LogParkingTimes(controler);
		SimulationStartupListener simulationStartupListener = new SimulationStartupListener(controler);
		controler.addControlerListener(simulationStartupListener);

		simulationStartupListener.addEventHandler(logEnergyConsumption);
		simulationStartupListener.addEventHandler(logParkingTimes);

		controler.run();

		OptimizedCharger optimizedCharger = new OptimizedCharger(logEnergyConsumption.getEnergyConsumption(), logParkingTimes
				.getParkingTimes(), Double.parseDouble(controler.getConfig().findParam("PSF", "default.maxBatteryCapacity")));
		HashMap<Id<Person>, ChargingTimes> chargingTimes = optimizedCharger.getChargingTimes();

		ChargingTimes chargingTimesOfAgentOne = chargingTimes.get(Id.create("1", Person.class));
		ChargeLog chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(0);

		// the first charging opportunity at work is used (it has low tariff)
		assertEquals(22989, chargeLogOfAgentOne.getStartChargingTime(),  1);
		// yy for some unknown reason, this assertion
		// * works on the build server
		// * works when only this test or only this test class or only this test package is run locally on my machine
		// * fails when I run all wrashid tests locally on my machine.  It is far off then: 2700 instead of 22989. 
		// kai, may'15


		assertEquals(23104, chargeLogOfAgentOne.getEndChargingTime(),  1);

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(1);

		// when the vehicle arrives at home, there is high tariff, therefore the
		// vehicle doesn't start charging
		// immediately, but waits until low tariff starts)

		assertEquals(72000, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(72085, chargeLogOfAgentOne.getEndChargingTime(),  1);


		// after charging the battery of the agent is full (allow for small rounding error)
		assertEquals(ParametersPSF.getDefaultMaxBatteryCapacity(), chargeLogOfAgentOne.getEndSOC(),  0.1);


		// the agent should charge twice.
		assertEquals(2, chargingTimesOfAgentOne.getChargingTimes().size());

		// check, if charging events are written out
		// the charging log output is not performed anymore during the tests.
		//File outputChargingLog= new File(ParametersPSF.getMainChargingTimesOutputFilePath());
		//assertTrue("output charging log does not exist. expected at " + outputChargingLog.getPath(), outputChargingLog.exists());
	}



	public void testLogParkingTime() {
		LogParkingTimes logParkingTimes = new LogParkingTimes(controler);
		SimulationStartupListener simulationStartupListener = new SimulationStartupListener(controler);
		controler.addControlerListener(simulationStartupListener);

		simulationStartupListener.addEventHandler(logParkingTimes);

		controler.run();

		ParkingTimes parkingTimes = logParkingTimes.getParkingTimes().get(Id.create("1", Person.class));

		// allow small delta of one second (because the output time in the log
		// file is truncated
		assertEquals(21610, parkingTimes.getFirstParkingDepartTime(),  1);
		assertEquals(61449, parkingTimes.getLastParkingArrivalTime(),  1);

		ParkLog parkLog = parkingTimes.getParkingTimes().get(0);
		assertEquals(22989, parkLog.getStartParkingTime(),  1);
		assertEquals(59349, parkLog.getEndParkingTime(),  1);
	}

	public void testLogEnergyConsumption() {
		LogEnergyConsumption logEnergyConsumption = new LogEnergyConsumption(controler);
		SimulationStartupListener simulationStartupListener = new SimulationStartupListener(controler);
		controler.addControlerListener(simulationStartupListener);

		simulationStartupListener.addEventHandler(logEnergyConsumption);

		controler.run();

		EnergyConsumption energyConsumption = logEnergyConsumption.getEnergyConsumption().get(Id.create("1", Person.class));

		// allow small delta of one second (because the output time in the log
		// file is truncated
		// check the time of the entrance of the last link
		assertEquals(61389, energyConsumption.getTempEnteranceTimeOfLastLink(),  1);
		LinkEnergyConsumptionLog energyLog = energyConsumption.getLinkEnergyConsumption().get(0);
		assertEquals(21670, energyLog.getEnterTime(),  1);
		assertEquals(22029,  energyLog.getLeaveTime(),1);
	}

}
