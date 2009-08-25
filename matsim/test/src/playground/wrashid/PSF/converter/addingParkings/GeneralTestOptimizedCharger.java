package playground.wrashid.PSF.converter.addingParkings;

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

public class GeneralTestOptimizedCharger extends MatsimTestCase implements
		ParametersPSFMutator {

	String configFile;
	
	/*
	 * Just testing, that the scenario runs without errors.
	 */

	public void mutateParameters() {
		
	}

	
	// make this class general, so that we can test with it, if a configuration runs...
	// just make a new test, which runs the method testOptimizedCharger of the object
	//"test/input/playground/wrashid/PSF/converter/addParkings/"	+ "config3.xml"
	
	public GeneralTestOptimizedCharger(String configFile){
		this.configFile=configFile;
	}
	
	public void optimizedChargerTest() {
		Controler controler = new Controler(
				this.configFile);
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
				"66805"));
 
		// the agent charges once at home in the evening (during off peak time), because the energy consumption
		// per link is set very low in the scenario and there is no need to charge during peak time.
		assertEquals(1, chargingTimesOfAgentOne.getChargingTimes().size());
	}

}
