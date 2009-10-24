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
	private static final int numberOfTimeBins = 96;
	private double[][] minimumPriceSignal;

	public static int getIterationNumber() {
		return iterationNumber;
	}


	public PSSControler(String configFilePath,ParametersPSFMutator parameterPSFMutator) {
		this.configFilePath=configFilePath;
		this.parameterPSFMutator=parameterPSFMutator;
	}
	
	
	public static void main(String[] args) {
		PSSControler pssControler=new PSSControler("a:\\data\\matsim\\input\\runRW1002\\config.xml", null);
		
		//pssControler.runPSS();
		
		pssControler.runMATSimIterations();
		pssControler.prepareMATSimInput();
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
		
		
		
		if (minimumPriceSignal==null){
			minimumPriceSignal=new double[numberOfTimeBins][ParametersPSF.getNumberOfHubs()];
		}
		
		
		
		
		
		
		// from iteration 1 onwards: if prices have been high in the previous iteration and they are
		// still above 10 Rappen, then take the max(current iteration,previous iteration) and add 20% to the price.
		// This is the price for the next iteration.
		if (iterationNumber>=1){
			double [][] newPriceSignalMatrix=GeneralLib.readMatrix(numberOfTimeBins, ParametersPSF.getNumberOfHubs(), false, outputPSSPath + "\\hubPriceInfo.txt");
		
			double [][] oldPriceSignalMatrix=ParametersPSF.getHubPriceInfo().getPriceMatrix();
			
			
			for (int i=0;i<numberOfTimeBins;i++){
				for (int j=0;j<ParametersPSF.getNumberOfHubs();j++){
					// if the price was previously higher than 10 and still higher than 10, then increase the price level
					// by 30%, because this means the price is still too low
					
					
					// as the prices are determined by e.g. a squar root probabilistic algortihm, a
					// value of 30% results in much less reduction of vehicle energy conumption during 
					// the given slot than by 30%
					if (newPriceSignalMatrix[i][j]>10.0 && oldPriceSignalMatrix[i][j]>10.0){
						newPriceSignalMatrix[i][j]=1.3*oldPriceSignalMatrix[i][j];
						
						// we are sure, that the maximum price level for this slot is the current value
						
						minimumPriceSignal[i][j]=Math.max(newPriceSignalMatrix[i][j],minimumPriceSignal[i][j]);
					}
					
					// if this is the first time, the price has been above 10.0, we need to find out the minimumPriceSignal value
					// therefore we decrease the value of the price signal slowly
					
					// => as soon, as the current price has doped enough, there will be a rise in the price, leading to the 
					// mimumPriceSignal beeing set.
					if (newPriceSignalMatrix[i][j]<10.0 && oldPriceSignalMatrix[i][j]>10.0 && minimumPriceSignal[i][j]==0.0){
						// decrease price by 5 %
						newPriceSignalMatrix[i][j]=oldPriceSignalMatrix[i][j]*0.95;
					}
					
					
					// if the new price is smaller than the minimum Price signal, it should be set to the mimimumPriceSignal
					// => needed for stabilization of the system (because when the right price is reached, PPSS will
					// drop the price lower than 10 => we need to keep the price high. (correct it).
					if (newPriceSignalMatrix[i][j]< minimumPriceSignal[i][j]){
						newPriceSignalMatrix[i][j]= minimumPriceSignal[i][j];
					}
					
									
					
					// if new price is now higher than 10 and previously it was lower than 10 => then leave it as it is
					
					// if new price is lower then and previously it was also lower than 10 => leave it as it is
					
					// => this means: decrease price slower than increasing the price.
				}
			}
			
			// remove old price
			File tempFile = new File(outputPSSPath + "\\hubPriceInfo.txt");
			if (tempFile.exists()) {
				tempFile.delete();
			}
			
			// write out adapted price
			GeneralLib.writeMatrix(newPriceSignalMatrix, outputPSSPath + "\\hubPriceInfo.txt", null);
			
			GeneralLib.copyFile(outputPSSPath + "\\hubPriceInfo.txt", getIterationResultDirectory() + "\\" + "hubPriceInfo-internal" + (iterationNumber+1) +".txt");
			
		}
		
		
	}


	private void savePSSResults() {
		String hubPriceInfoFileName=getIterationResultDirectory() + "\\" + "hubPriceInfo" + (iterationNumber+1);
		String hubPricePeaksFileName=getIterationResultDirectory() + "\\" + "hubPricePeaks" + (iterationNumber+1);
		
		GeneralLib.copyFile(outputPSSPath + "\\hubPriceInfo.txt", hubPriceInfoFileName + ".txt");
		GeneralLib.copyFile(outputPSSPath + "\\hubPricePeaks.txt", hubPricePeaksFileName + ".txt");
		
		double[][] hubPriceInfo=GeneralLib.readMatrix(numberOfTimeBins, ParametersPSF.getNumberOfHubs(), false, hubPriceInfoFileName + ".txt");
		double[][] hubPricePeaks=GeneralLib.readMatrix(numberOfTimeBins, ParametersPSF.getNumberOfHubs(), false, hubPricePeaksFileName + ".txt");
		
		GeneralLib.writeGraphic(hubPriceInfoFileName + ".png", hubPriceInfo, "Hub Energy Prices", "Time of Day [s]", "Price [CHF]");
		GeneralLib.writeGraphic(hubPricePeaksFileName + ".png", hubPricePeaks, "Hub Energy Prices (only Peak)", "Time of Day [s]", "Price [CHF]");
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

		tempFile = new File(outputPSSPath + "\\hubPricePeaks.txt");
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
