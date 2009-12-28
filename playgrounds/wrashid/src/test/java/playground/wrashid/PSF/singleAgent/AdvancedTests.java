package playground.wrashid.PSF.singleAgent;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.ParametersPSFMutator;
import playground.wrashid.PSF.V2G.BatteryStatistics;
import playground.wrashid.PSF.energy.AddEnergyScoreListener;
import playground.wrashid.PSF.energy.SimulationStartupListener;
import playground.wrashid.PSF.energy.charging.ChargeLog;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF.energy.charging.optimizedCharging.OptimizedCharger;
import playground.wrashid.PSF.energy.consumption.LogEnergyConsumption;
import playground.wrashid.PSF.parking.LogParkingTimes;

/**
 * Note: Cannot simply inherit from class TestCase, but must inherit from
 * MatsimTestCase, because else there are some errors.
 * 
 * @author rashid_waraich
 * 
 */
public class AdvancedTests extends MatsimTestCase {

	Controler controler;
	SimulationStartupListener simulationStartupListener;
	LogEnergyConsumption logEnergyConsumption;
	LogParkingTimes logParkingTimes;
	HashMap<Id, ChargingTimes> chargingTimes;

	private void initTest(String configFile) {
		Config config = loadConfig(configFile);
		controler = new Controler(config);

		controler.addControlerListener(new AddEnergyScoreListener());
		controler.setCreateGraphs(false);

		logEnergyConsumption = new LogEnergyConsumption(controler);
		logParkingTimes = new LogParkingTimes(controler);
		simulationStartupListener = new SimulationStartupListener(controler);
		controler.addControlerListener(simulationStartupListener);

		simulationStartupListener.addEventHandler(logEnergyConsumption);
		simulationStartupListener.addEventHandler(logParkingTimes);
	}

	// now the car arrives earlier at home, than the off peak time starts
	// this means, the car should start charging immediately at home upon
	// arrival

	public void test1OptimizedCharger() {
		initTest("test/input/playground/wrashid/PSF/singleAgent/config.xml");

		simulationStartupListener.addParameterPSFMutator(new ParametersPSFMutator() {
			public void mutateParameters() {
				ParametersPSF.setTestingPeakPriceEndTime(60000);
			}
		});

		controler.run();

		ChargingTimes chargingTimesOfAgentOne = getChargingTimesOfAgent("1");
		ChargeLog chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(0);

		// after the first charge, the battery should be full
		assertEquals(ParametersPSF.getDefaultMaxBatteryCapacity(), chargeLogOfAgentOne.getEndSOC() );

		assertEquals(22989,chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(23104,chargeLogOfAgentOne.getEndChargingTime(),  1);

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(1);

		assertEquals(61449,chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(61535,chargeLogOfAgentOne.getEndChargingTime(),  1);

		// after charging, the battery of the agent is full (allow for small
		// rounding error)
		assertEquals(ParametersPSF.getDefaultMaxBatteryCapacity(),chargeLogOfAgentOne.getEndSOC(),  0.1);

		// the agent should charge twice.
		assertEquals(chargingTimesOfAgentOne.getChargingTimes().size(),2 );
	}

	/*
	 * The agent arrives at work after starting of peak hour tariff, therefore
	 * the car does not charge at work. The agent starts charging at home after
	 * peak hour.
	 */
	public void test2OptimizedCharger() {
		initTest("test/input/playground/wrashid/PSF/singleAgent/config.xml");

		simulationStartupListener.addParameterPSFMutator(new ParametersPSFMutator() {
			public void mutateParameters() {
				ParametersPSF.setTestingPeakPriceStartTime(20000);
			}
		});

		controler.run();

		OptimizedCharger optimizedCharger = new OptimizedCharger(logEnergyConsumption.getEnergyConsumption(), logParkingTimes
				.getParkingTimes());
		HashMap<Id, ChargingTimes> chargingTimes = optimizedCharger.getChargingTimes();

		ChargingTimes chargingTimesOfAgentOne = chargingTimes.get(new IdImpl("1"));
		ChargeLog chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(0);

		assertEquals(72000,chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(72200,chargeLogOfAgentOne.getEndChargingTime(),  1);

		// after charging the battery of the agent is full (allow for small
		// rounding error)
		assertEquals(ParametersPSF.getDefaultMaxBatteryCapacity(), chargeLogOfAgentOne.getEndSOC(),  0.1);

		// the agent should charge only using one slot
		assertEquals(1, chargingTimesOfAgentOne.getChargingTimes().size());
	}

	/*
	 * There is always peak hour tariff. Therefore the agent should charge
	 * immediately when arriving at work and at home.
	 */
	public void test3OptimizedCharger() {
		initTest("test/input/playground/wrashid/PSF/singleAgent/" + "config.xml");

		simulationStartupListener.addParameterPSFMutator(new ParametersPSFMutator() {
			public void mutateParameters() {
				ParametersPSF.setTestingPeakPriceStartTime(0);
				ParametersPSF.setTestingPeakPriceEndTime(86400);
			}
		});

		controler.run();

		ChargingTimes chargingTimesOfAgentOne = getChargingTimesOfAgent("1");
		ChargeLog chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(0);

		// after the first charge, the battery should be full
		assertEquals(ParametersPSF.getDefaultMaxBatteryCapacity(),chargeLogOfAgentOne.getEndSOC());

		assertEquals(22989,chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(23104,chargeLogOfAgentOne.getEndChargingTime(),  1);

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(1);

		assertEquals(61449,chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(61535, chargeLogOfAgentOne.getEndChargingTime(),  1);

		// after charging the battery of the agent is full (allow for small
		// rounding error)
		assertEquals(ParametersPSF.getDefaultMaxBatteryCapacity(),chargeLogOfAgentOne.getEndSOC(),  0.1);

		// the agent should charge twice.
		assertEquals(2, chargingTimesOfAgentOne.getChargingTimes().size());
	}

	/*
	 * The vehicle must charge during work, because else he will not reach home.
	 * Off peak charging can only be performed at home.
	 */
	public void test4OptimizedCharger() {
		initTest("test/input/playground/wrashid/PSF/singleAgent/config.xml");

		simulationStartupListener.addParameterPSFMutator(new ParametersPSFMutator() {
			public void mutateParameters() {
				ParametersPSF.setTestingPeakPriceStartTime(20000);
				ParametersPSF.setTestingPeakPriceEndTime(72000);

				ParametersPSF.setTestingEnergyConsumptionPerLink(8000000);
			}
		});

		controler.run();

		ChargingTimes chargingTimesOfAgentOne = getChargingTimesOfAgent("1");
		ChargeLog chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(0);

		// the vehicle should start charging immediately at work 20M Joules,
		// which are needed
		// at least to reach home again. It requires 5714.28 seconds to charge
		// this amount of
		// energy (at 3500W)

		// the first charging duration should be till 23400 (because of 900
		// second bins)
		// 
		assertEquals(22989, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(23400, chargeLogOfAgentOne.getEndChargingTime(),  1);

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(1);
		assertEquals(23400, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(24300, chargeLogOfAgentOne.getEndChargingTime(),  1);

		// skipping some chargings
		// chargingTimesOfAgentOne.getChargingTimes().get(2).getEndChargingTime()
		// should be 25200
		// get(3) should be: 26100
		// get(4) should be: 27000
		// get(5) should be: 27900

		// the agent just charges as much at work as he needs to charge (for
		// driving home)
		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(6);
		assertEquals(27900, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals( 28704, chargeLogOfAgentOne.getEndChargingTime(), 1);

		// the agent continues charging at home as soon as the off peak tariff
		// starts
		// actually, when the agent arrives at home, his battery is empty and
		// 10285.714 seconds
		// are needed at 3500W to recharge it.
		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(7);
		assertEquals(72000, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(72900, chargeLogOfAgentOne.getEndChargingTime(),  1);

		// skipping 10 some checks

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(17);
		assertEquals(81000, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(81900, chargeLogOfAgentOne.getEndChargingTime(),  1);

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(18);
		assertEquals(81900, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(82285, chargeLogOfAgentOne.getEndChargingTime(),  1);

		// after charging the battery of the agent is full (allow for small
		// rounding error)
		assertEquals(ParametersPSF.getDefaultMaxBatteryCapacity(), chargeLogOfAgentOne.getEndSOC(),  0.1);

		// make sure, this is the last charging of the agent
		assertEquals(19, chargingTimesOfAgentOne.getChargingTimes().size());
	}

	/*
	 * The agent has now three activities (two times work and once time home).
	 * 
	 * - The agent will not reach the second parking if he does not charge at
	 * the first parking.
	 * 
	 * 
	 * - The vehicle should charge at the first facility enough energy, that he
	 * can reach home again (e.g.) . This means, he will charge 1.2M Joule. The
	 * vehicle will not charge at the second facility, because the charging
	 * price is the same as at the first facility (but earlier).
	 */
	public void test5OptimizedCharger() {
		initTest("test/input/playground/wrashid/PSF/singleAgent/config2.xml");

		simulationStartupListener.addParameterPSFMutator(new ParametersPSFMutator() {
			public void mutateParameters() {
				ParametersPSF.setTestingPeakPriceStartTime(20000);
				ParametersPSF.setTestingPeakPriceEndTime(72000);

				ParametersPSF.setTestingEnergyConsumptionPerLink(8000000);
			}
		});

		controler.run();

		// TODO: has problem with update of jdeqsim/ empty car routes -> why, unknown at the moment
		//assertionsTest5();

	}

	public void assertionsTest5() {
		ChargingTimes chargingTimesOfAgentOne = getChargingTimesOfAgent("1");
		ChargeLog chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(0);

		// the vehicle should start charging immediately at work 12M Joules,
		// which are needed
		// at least to reach the second parking. It requires 3428.57 seconds to
		// charge
		// this amount of
		// energy (at 3500W)

		// the first charging duration should be till 23400 (because of 900
		// second bins)
		// 
		assertEquals(22989, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(23400, chargeLogOfAgentOne.getEndChargingTime(),  1);

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(1);
		assertEquals(23400, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(24300, chargeLogOfAgentOne.getEndChargingTime(),  1);

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(2);
		assertEquals(24300, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(25200, chargeLogOfAgentOne.getEndChargingTime(),  1);

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(3);
		assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 25200, 1);
		assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 26100, 1);

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(4);
		assertEquals(26100, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(26418, chargeLogOfAgentOne.getEndChargingTime(),  1);

		// at this point, the agent has charged 12M Joules, so that he can reach
		// the second parking. Now taking the second parking also into account,
		// the agent tries to reach home (additional 8M Joules are needed).
		// Time required for charging this much energy:
		// 2285.7142857142857142857142857143

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(5);
		assertEquals(26418, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(27000, chargeLogOfAgentOne.getEndChargingTime(),  1);

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(6);
		assertEquals(27000, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(27900, chargeLogOfAgentOne.getEndChargingTime(),  1);

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(7);
		assertEquals(27900, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(28704, chargeLogOfAgentOne.getEndChargingTime(),  1);

		// the agent continues charging at home as soon as the off peak tariff
		// starts
		// actually, when the agent arrives at home, his battery is empty and
		// 10285.714 seconds
		// are needed at 3500W to recharge it.
		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(8);
		assertEquals(72000, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(72900, chargeLogOfAgentOne.getEndChargingTime(),  1);

		// skipping 10 some checks

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(18);
		assertEquals(81000, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(81900, chargeLogOfAgentOne.getEndChargingTime(),  1);

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(19);
		assertEquals(81900, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(82285, chargeLogOfAgentOne.getEndChargingTime(),  1);

		// after charging the battery of the agent is full (allow for small
		// rounding error)
		assertEquals(ParametersPSF.getDefaultMaxBatteryCapacity(), chargeLogOfAgentOne.getEndSOC(),  0.1);

		// make sure, this is the last charging of the agent
		assertEquals(20, chargingTimesOfAgentOne.getChargingTimes().size());
	}

	public ChargingTimes getChargingTimesOfAgent(String agentId) {
		OptimizedCharger optimizedCharger = new OptimizedCharger(logEnergyConsumption.getEnergyConsumption(), logParkingTimes
				.getParkingTimes());
		chargingTimes = optimizedCharger.getChargingTimes();

		ChargingTimes chargingTimesOfAgent = chargingTimes.get(new IdImpl(agentId));
		return chargingTimesOfAgent;
	}

	/*
	 * testingModeOn=false => test mode turned off in config file and therefore
	 * using explicit hub link mappings and hub energy prices.
	 */
	public void test6OptimizedCharger() {
		initTest("test/input/playground/wrashid/PSF/singleAgent/config3.xml");

		simulationStartupListener.addParameterPSFMutator(new ParametersPSFMutator() {
			public void mutateParameters() {

			}
		});

		controler.run();

		ChargingTimes chargingTimesOfAgentOne = getChargingTimesOfAgent("1");

		ChargeLog chargeLogOfAgentOne;

		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(0);
		assertEquals(22989.8, chargeLogOfAgentOne.getStartChargingTime(),  1);
		assertEquals(23400.0, chargeLogOfAgentOne.getEndChargingTime(),  1);

		// TODO: has problem with update of jdeqsim/ empty car routes -> why, unknown at the moment
		//chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(9);
		//assertEquals(chargeLogOfAgentOne.getStartChargingTime(), 29700.0, 1);
		//assertEquals(chargeLogOfAgentOne.getEndChargingTime(), 30492.901121572733, 1);

		// chargingTimesOfAgentOne.print();
		
		// check the statistics about the battery for usage of the V2G concept.
		double[][] gridConnectedEnergy = BatteryStatistics.getGridConnectedEnergy(chargingTimes,logParkingTimes.getParkingTimes());
		double[][] getGridConnectedPower=BatteryStatistics.getGridConnectedPower(logParkingTimes.getParkingTimes(), ParametersPSF.getDefaultChargingPowerAtParking());
		assertEquals(3500.0, getGridConnectedPower[(int) Math.round(Math.floor(22989/900))][0]);
		assertEquals(0.0, getGridConnectedPower[(int) Math.round(Math.floor((22989-1000)/900))][0]);
		
		assertEquals(36000000.0, gridConnectedEnergy[0][0], 1);
		
		assertEquals(36000000.0, gridConnectedEnergy[(int) Math.round(Math.floor((22989-1000)/900))][0],1);
		assertEquals(1.117484607449543E7, gridConnectedEnergy[(int) Math.round(Math.floor(22989/900))][0],1);
	}

	/*
	 * This is a special boundary case, because the owner of the car leaves his
	 * car at his working place and walks home. This case actually occurs in the
	 * berlin 1% scenario (agentId="193077"). But his does not cause the error. The error is caused,
	 * because the car arrives at work very fast (within the first charging bin, available at work).
	 * => This caused a negative energy value. => bug resolved now in FacilityChargingPrice.getEndTimeOfCharge(...)
	 * 
	 * As the peak is during the night, the agent should start charging immediately upon arriving.
	 */
	public void test7OptimizedCharger() {
		initTest("test/input/playground/wrashid/PSF/singleAgent/config6.xml");

		simulationStartupListener.addParameterPSFMutator(new ParametersPSFMutator() {
			public void mutateParameters() {
				ParametersPSF.setTestingPeakPriceStartTime(600);
				ParametersPSF.setTestingPeakPriceEndTime(1200);
			}
		});

		controler.run();

		ChargingTimes chargingTimesOfAgentOne = getChargingTimesOfAgent("1");

		ChargeLog chargeLogOfAgentOne;

		// the agent starts charging in the night and the link, where he charges, is not the home link, but the working place link
		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(0);
		assertEquals(21721.0, chargeLogOfAgentOne.getStartChargingTime(),  1.0);
		assertEquals("20", chargeLogOfAgentOne.getLinkId().toString());
		
		// after charging, the battery of the agent is full (allow for small
		// rounding error)
		assertEquals(chargingTimesOfAgentOne.getChargingTimes().getLast().getEndSOC(), ParametersPSF.getDefaultMaxBatteryCapacity(), 0.1);
	}  
  
	/**
	 * This covers a fixed bug, which occurs during charging in the night.
	 * 
	 * This also tests the method ChargingTimes.getEnergyUsageStatistics 
	 */
	public void test8OptimizedCharger() {
		initTest("test/input/playground/wrashid/PSF/singleAgent/config8.xml"); 

		simulationStartupListener.addParameterPSFMutator(new ParametersPSFMutator() {
			public void mutateParameters() {
				ParametersPSF.setTestingPeakPriceStartTime(20000);
				ParametersPSF.setTestingPeakPriceEndTime(86000);

				ParametersPSF.setTestingEnergyConsumptionPerLink(8000000);
			}
		});

		controler.run();

		ChargingTimes chargingTimesOfAgentOne = getChargingTimesOfAgent("1");
		ChargeLog chargeLogOfAgentOne;
		
		// fully charged in the end
		chargeLogOfAgentOne = chargingTimesOfAgentOne.getChargingTimes().get(11);
		assertEquals(ParametersPSF.getDefaultMaxBatteryCapacity(), chargeLogOfAgentOne.getEndSOC(), 1.0);
		
		// also test energy Usage Statistics
		
		double[][] energyUsageStatistics = ChargingTimes.getEnergyUsageStatistics(chargingTimes,ParametersPSF.getHubLinkMapping()); 
		
		// the vehicle charges between 
		
		//energy charged between 150 and 165 min:  3150000
		//energy charged between 165 and 180 min:  1350000
		assertEquals(3150000.0, energyUsageStatistics[10][0], 1.0);
		assertEquals(1350000.0, energyUsageStatistics[11][0], 1.0);
	}
} 
