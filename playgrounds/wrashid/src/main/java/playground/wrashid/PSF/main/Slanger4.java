package playground.wrashid.PSF.main;

import playground.wrashid.PSF.ParametersPSFMutator;
import playground.wrashid.PSF.PSS.PSSControler;

public class Slanger4 implements ParametersPSFMutator {

	public static void main(String[] args) {
		// for running on "slanger4"
		
		PSSControler pssControler=new PSSControler("a:\\data\\matsim\\input\\runRW1003\\config.xml", null);
		
		pssControler.runMATSimPSSIterations(2);
		/*
		Controler services = new Controler("a:\\data\\matsim\\input\\runRW1002\\config.xml");
		services.addControlerListener(new AddEnergyScoreListener());
		services.setOverwriteFiles(true);

		LogEnergyConsumption logEnergyConsumption = new LogEnergyConsumption(
				services);
		LogParkingTimes logParkingTimes = new LogParkingTimes(services);
		SimulationStartupListener simulationStartupListener = new SimulationStartupListener(
				services);
		services.addControlerListener(simulationStartupListener);

		simulationStartupListener.addEventHandler(logEnergyConsumption);
		simulationStartupListener.addEventHandler(logParkingTimes);
		simulationStartupListener.addParameterPSFMutator(new Slanger4());

		services.run();
		
		OptimizedCharger optimizedCharger= new OptimizedCharger(logEnergyConsumption.getEnergyConsumption(),logParkingTimes.getParkingTimes(),ParametersPSF.getDefaultMaxBatteryCapacity());
		HashMap<Id, ChargingTimes> chargingTimes=optimizedCharger.getChargingTimes();
		
		ChargingTimes.printEnergyUsageStatistics(chargingTimes, ParametersPSF.getHubLinkMapping());
		*/
	} 

	public void mutateParameters() {
		// TODO Auto-generated method stub
		
	}
	
}
