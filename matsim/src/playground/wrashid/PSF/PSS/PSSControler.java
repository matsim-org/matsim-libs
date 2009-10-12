package playground.wrashid.PSF.PSS;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.controler.Controler;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.ParametersPSFMutator;
import playground.wrashid.PSF.energy.AddEnergyScoreListener;
import playground.wrashid.PSF.energy.SimulationStartupListener;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF.energy.charging.optimizedCharging.OptimizedCharger;
import playground.wrashid.PSF.energy.consumption.LogEnergyConsumption;
import playground.wrashid.PSF.parking.LogParkingTimes;
import playground.wrashid.lib.GeneralLib;

public class PSSControler {

	Controler controler;
	String resultDirectory = "A:\\data\\results\\";
	String inputPSSPath = "A:\\data\\PSS\\input";
	String outputPSSPath = "A:\\data\\PSS\\output";
	String configFilePath;
	ParametersPSFMutator parameterPSFMutator;
	private int iterationNumber=0;

	public PSSControler(String configFilePath,ParametersPSFMutator parameterPSFMutator) {
		this.configFilePath=configFilePath;
		this.parameterPSFMutator=parameterPSFMutator;
	}
	
	
	/**
	 * number of iterations, in which both MATSim and PSS (Power System
	 * Simulation) is run. The number of iterations within matsim is specified
	 * in the config file.
	 * 
	 * @param numberOfIterations
	 */
	public void runMATSimPSSIterations(int numberOfIterations) {
		
		for (int i=0;i<numberOfIterations;i++){
			runMATSimIterations();
			saveMATSimResults();
			
			preparePSSInput();
			runPSS();
			
			savePSSResults();
			prepareMATSimInput();
			
		}
	}
	
	

	private void prepareMATSimInput() {
		
		// not needed: configure in the config, that the input is read from the output folder of PSS.
		
	}


	private void savePSSResults() {
		// TODO Auto-generated method stub
		GeneralLib.copyFile(outputPSSPath + "hubPriceInfo.txt", getIterationResultDirectory() + "\\" + "hubPriceInfo.txt");
	}


	private void preparePSSInput() {
		// copy charging log to input directory of PSS
		GeneralLib.copyFile(ParametersPSF.getMainChargingTimesOutputFilePath(), inputPSSPath + "chargingLog.txt");
	}


	private void saveMATSimResults() {
		// copy all data from the matsim output directory to the results directory
		String matsimOutputFolderName= 	controler.getOutputFilename("");
		
		GeneralLib.copyDirectory(matsimOutputFolderName, getIterationResultDirectory());
		
		
	}


	private void runMATSimIterations() {
		controler = new Controler(configFilePath);
		controler.addControlerListener(new AddEnergyScoreListener());
		controler.setOverwriteFiles(true);

		LogEnergyConsumption logEnergyConsumption = new LogEnergyConsumption(controler);
		LogParkingTimes logParkingTimes = new LogParkingTimes(controler);
		SimulationStartupListener simulationStartupListener = new SimulationStartupListener(controler);
		controler.addControlerListener(simulationStartupListener);

		simulationStartupListener.addEventHandler(logEnergyConsumption);
		simulationStartupListener.addEventHandler(logParkingTimes);
		simulationStartupListener.addParameterPSFMutator(parameterPSFMutator);

		controler.run();

		OptimizedCharger optimizedCharger = new OptimizedCharger(logEnergyConsumption.getEnergyConsumption(), logParkingTimes
				.getParkingTimes());
		HashMap<Id, ChargingTimes> chargingTimes = optimizedCharger.getChargingTimes();

		ChargingTimes.printEnergyUsageStatistics(chargingTimes, ParametersPSF.getHubLinkMapping());
	}

	private void runPSS() {


		// clean output directory
		File tempFile = new File(outputPSSPath + "hubPriceInfo.txt");
		if (tempFile.exists()) {
			tempFile.delete();
		}

		tempFile = new File(outputPSSPath + "fertig.txt");
		if (tempFile.exists()) {
			tempFile.delete();
		}

		// run matlab via DOS console
		try {
			Process proc = Runtime.getRuntime().exec("cmd.exe");
			BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(proc.getOutputStream())), true);

			String scriptPath = "a:\\";
			String scriptName = "IVT_main";
			out.println("a:");
			out.println("cd " + scriptPath);
			out.println("matlab -nodisplay -nojvm -r " + scriptName);
			out.flush();
		
		
		// check, if MATLab finished
		tempFile = new File(outputPSSPath + "fertig.txt");
		while (!tempFile.exists()){
			// TODO: do counting, if this not possible
			Thread.sleep(5000);
		}
		
		// stop/kill matlab 
		Runtime.getRuntime().exec("taskkill /im matlab.exe /f");
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String getIterationResultDirectory(){
		return resultDirectory + "\\iteration" + iterationNumber;
	}

}
