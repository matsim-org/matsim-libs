package playground.wrashid.PSF.main;

import playground.wrashid.PSF.ParametersPSFMutator;
import playground.wrashid.PSF.PSS.PSSControler;

public class LocalHost implements ParametersPSFMutator {

	public static void main(String[] args) {		
		// for starting one the local computer
		PSSControler pssControler=new PSSControler("c:\\data\\matsim\\input\\runRW1003\\config.xml", null);
		pssControler.runMATSimIterations();
		
		/*
		Controler controler = new Controler("a:\\data\\matsim\\input\\runRW1002\\config.xml");
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
		simulationStartupListener.addParameterPSFMutator(new Berlin());

		controler.run();
		
		OptimizedCharger optimizedCharger= new OptimizedCharger(logEnergyConsumption.getEnergyConsumption(),logParkingTimes.getParkingTimes());
		HashMap<Id, ChargingTimes> chargingTimes=optimizedCharger.getChargingTimes();
		
		ChargingTimes.printEnergyUsageStatistics(chargingTimes, ParametersPSF.getHubLinkMapping());
		*/
	} 

	public void mutateParameters() {
		// TODO Auto-generated method stub
		
	}
	
}
