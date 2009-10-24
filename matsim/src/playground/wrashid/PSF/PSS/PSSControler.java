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
import org.matsim.core.gbl.Gbl;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.ParametersPSFMutator;
import playground.wrashid.PSF.energy.AddEnergyScoreListener;
import playground.wrashid.PSF.energy.AfterSimulationListener;
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
	private static int iterationNumber=0;

	public static int getIterationNumber() {
		return iterationNumber;
	}


	public PSSControler(String configFilePath,ParametersPSFMutator parameterPSFMutator) {
		this.configFilePath=configFilePath;
		this.parameterPSFMutator=parameterPSFMutator;
	}
	
	
	public static void main(String[] args) {
		PSSControler pssControler=new PSSControler("a:\\data\\matsim\\input\\runRW1002\\config.xml", null);
		
		pssControler.runPSS();
	}
	
	/**
	 * number of iterations, in which both MATSim and PSS (Power System
	 * Simulation) is run. The number of iterations within matsim is specified
	 * in the config file.
	 * 
	 * @param numberOfIterations
	 */
	public void runMATSimPSSIterations(int numberOfIterations) {
		// if result folder not empty, stop the execution.
		File resultsDirectory=new File(resultDirectory);
		if (resultsDirectory.list().length>0){
			System.out.println("The result directory is not empty."); 
			System.exit(0);
		}
		
		if (!new File(outputPSSPath + "\\hubPriceInfo.txt").exists()){
			System.out.println("The initial price file is not in " + outputPSSPath); 
			System.exit(0);
		}
		
		// copy the initial prices to the result directory.
		GeneralLib.copyFile(outputPSSPath + "\\hubPriceInfo.txt", resultDirectory + "initialHubPriceInfo.txt");
		
		for (iterationNumber=0;iterationNumber<numberOfIterations;iterationNumber++){
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
		GeneralLib.copyFile(outputPSSPath + "\\hubPriceInfo.txt", getIterationResultDirectory() + "\\" + "hubPriceInfo" + (iterationNumber+1)  + ".txt");
		GeneralLib.copyFile(outputPSSPath + "\\hubPricePeaks.txt", getIterationResultDirectory() + "\\" + "hubPricePeaks" + (iterationNumber+1) +  ".txt");
	}


	private void preparePSSInput() {
		// remove chargingLog file, if it exists already
		File tempChargingLogFile=new File(inputPSSPath + "\\chargingLog.txt");
		if (tempChargingLogFile.exists()){
			tempChargingLogFile.delete();
		}
		
		// copy charging log to input directory of PSS
		GeneralLib.copyFile(ParametersPSF.getMainChargingTimesOutputFilePath(), inputPSSPath + "\\chargingLog.txt");
	}


	private void saveMATSimResults() {
		// copy all data from the matsim output directory to the results directory
		String matsimOutputFolderName= 	controler.getOutputFilename("");
		
		GeneralLib.copyDirectory(matsimOutputFolderName, getIterationResultDirectory());
		
		
	}


	public void runMATSimIterations() {
		Gbl.reset();
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

		
		AfterSimulationListener afterSimulationListener=new AfterSimulationListener(logEnergyConsumption, logParkingTimes);
		controler.addControlerListener(afterSimulationListener);
		
		controler.run();

		
	}

	private void runPSS() {


		// clean output directory
		File tempFile = new File(outputPSSPath + "\\hubPriceInfo.txt");
		if (tempFile.exists()) {
			tempFile.delete();
		}

		tempFile = new File(outputPSSPath + "\\fertig.txt");
		if (tempFile.exists()) {
			tempFile.delete();
		}

		// run matlab via DOS console
		try {
			Process proc = Runtime.getRuntime().exec("cmd.exe");
			BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(proc.getOutputStream())), true);

			String matsimFolderPath="C:\\Program Files (x86)\\MATLAB\\R2008b\\bin\\";
			//String scriptPath = "a:\\";
			String scriptName;
			if (iterationNumber==0){
				scriptName="IVT_main_base";
			} else {
				scriptName="IVT_main";
			}
			
			out.println("cd " + matsimFolderPath);
			out.println("c:");
			out.println("matlab -r " + scriptName);
			out.flush();
			System.out.println("Starting PSS...");
		
		
		// check, if MATLab finished
		tempFile = new File(outputPSSPath + "\\fertig.txt");
		while (!tempFile.exists()){
			// TODO: do counting, if this not possible
			
			for (int i=0;i<1000000000;i++){
				
			}
			System.out.print(".");
		}
		System.out.println("");
		System.out.println("Killing matlab...");
		
		// stop/kill matlab 
		Runtime.getRuntime().exec("taskkill /im matlab.exe /f");
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String getIterationResultDirectory(){
		return resultDirectory + "iteration" + iterationNumber;
	}

}
