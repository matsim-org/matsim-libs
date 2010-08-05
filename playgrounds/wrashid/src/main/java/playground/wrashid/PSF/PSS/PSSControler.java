package playground.wrashid.PSF.PSS;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.ParametersPSFMutator;
import playground.wrashid.PSF.energy.AddEnergyScoreListener;
import playground.wrashid.PSF.energy.AfterSimulationListener;
import playground.wrashid.PSF.energy.SimulationStartupListener;
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
	private static int iterationNumber = 0;
	private static final int numberOfTimeBins = 96;
	private double[][] minimumPriceSignal;

	public static int getIterationNumber() {
		return iterationNumber;
	}

	public PSSControler(String configFilePath, ParametersPSFMutator parameterPSFMutator) {
		this.configFilePath = configFilePath;
		this.parameterPSFMutator = parameterPSFMutator;
	}

	public static void main(String[] args) {
		PSSControler pssControler = new PSSControler("a:\\data\\matsim\\input\\runRW1002\\config.xml", null);

		// just run PPSS
		pssControler.runPSS();

		// run something to debug...
		// pssControler.runMATSimIterations();
		// pssControler.prepareMATSimInput();
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
		File resultsDirectory = new File(resultDirectory);
		if (resultsDirectory.list().length > 0) {
			throw new Error("The result directory is not empty.");
		}

		if (!new File(outputPSSPath + "\\hubPriceInfo.txt").exists()) {
			throw new Error("The initial price file is not in " + outputPSSPath);
		}

		// copy the initial prices to the result directory.
		GeneralLib.copyFile(outputPSSPath + "\\hubPriceInfo.txt", resultDirectory + "initialHubPriceInfo.txt");

		for (iterationNumber = 0; iterationNumber < numberOfIterations; iterationNumber++) {
			runMATSimIterations();
			saveMATSimResults();

			preparePSSInput();
			runPSS();

			savePSSResults();
			prepareMATSimInput();
		}
	}

	private void prepareMATSimInput() {

		// not needed: configure in the config, that the input is read from the
		// output folder of PSS.

		if (minimumPriceSignal == null) {
			minimumPriceSignal = new double[numberOfTimeBins][ParametersPSF.getNumberOfHubs()];
		}

		

		// from iteration 1 onwards: maintain price levels.
		if (iterationNumber >= 1) {
			double[][] newPriceSignalMatrix = GeneralLib.readMatrix(numberOfTimeBins, ParametersPSF.getNumberOfHubs(), false,
					outputPSSPath + "\\hubPriceInfo.txt");

			double[][] oldPriceSignalMatrix = ParametersPSF.getHubPriceInfo().getPriceMatrix();

			for (int i = 0; i < numberOfTimeBins; i++) {
				for (int j = 0; j < ParametersPSF.getNumberOfHubs(); j++) {
					// get new values for price level and minimum price signal level 
					double[] priceSignalProcessing=FirstPriceSignalMaintainingAlgorithm.processPriceSignal(newPriceSignalMatrix[i][j], oldPriceSignalMatrix[i][j], minimumPriceSignal[i][j]);
					
					newPriceSignalMatrix[i][j]=priceSignalProcessing[0];
					minimumPriceSignal[i][j]=priceSignalProcessing[1];
					
				}
			}

			// remove old price
			File tempFile = new File(outputPSSPath + "\\hubPriceInfo.txt");
			if (tempFile.exists()) {
				tempFile.delete();
			}

			// write out adapted price
			GeneralLib.writeMatrix(newPriceSignalMatrix, outputPSSPath + "\\hubPriceInfo.txt", null);

			GeneralLib.copyFile(outputPSSPath + "\\hubPriceInfo.txt", getIterationResultDirectory() + "\\"
					+ "hubPriceInfo-internal" + (iterationNumber + 1) + ".txt");

		}

	}



	private void savePSSResults() {
		String hubPriceInfoFileName = getIterationResultDirectory() + "\\" + "hubPriceInfo" + (iterationNumber + 1);
		String hubPricePeaksFileName = getIterationResultDirectory() + "\\" + "hubPricePeaks" + (iterationNumber + 1);

		GeneralLib.copyFile(outputPSSPath + "\\hubPriceInfo.txt", hubPriceInfoFileName + ".txt");
		GeneralLib.copyFile(outputPSSPath + "\\hubPricePeaks.txt", hubPricePeaksFileName + ".txt");

		double[][] hubPriceInfo = GeneralLib.readMatrix(numberOfTimeBins, ParametersPSF.getNumberOfHubs(), false,
				hubPriceInfoFileName + ".txt");
		double[][] hubPricePeaks = GeneralLib.readMatrix(numberOfTimeBins, ParametersPSF.getNumberOfHubs(), false,
				hubPricePeaksFileName + ".txt");

		GeneralLib.writeGraphic(hubPriceInfoFileName + ".png", hubPriceInfo, "Hub Energy Prices", "Time of Day [s]", "Price [CHF]");
		GeneralLib.writeGraphic(hubPricePeaksFileName + ".png", hubPricePeaks, "Hub Energy Prices (only Peak)", "Time of Day [s]",
				"Price [CHF]");
	}

	private void preparePSSInput() {
		// remove chargingLog file, if it exists already
		File tempChargingLogFile = new File(inputPSSPath + "\\chargingLog.txt");
		if (tempChargingLogFile.exists()) {
			tempChargingLogFile.delete();
		}

		// copy charging log to input directory of PSS
		GeneralLib.copyFile(ParametersPSF.getMainChargingTimesOutputFilePath(), inputPSSPath + "\\chargingLog.txt");
	}

	private void saveMATSimResults() {
		// copy all data from the matsim output directory to the results
		// directory
		String matsimOutputFolderName = controler.getControlerIO().getOutputFilename("");

		GeneralLib.copyDirectory(matsimOutputFolderName, getIterationResultDirectory());

	}

	public void runMATSimIterations() {
		
		Gbl.reset();
		
		
		
		// use the right Controler (read parameter 
		Config config = new Config();
		MatsimConfigReader reader = new MatsimConfigReader(config);
		reader.readFile(configFilePath);
		String tempStringValue = config.findParam(ParametersPSF.getPSFModule(), "main.inputEventsForSimulationPath");
		if (tempStringValue != null) {
			// ATTENTION, this does not work at the moment, because the read link from the 
			// event file is null and this causes some probelems in my handlers...
			controler = new EventReadControler(configFilePath,tempStringValue);
		} else {
			controler = new Controler(configFilePath);
		}
		
		controler.addControlerListener(new AddEnergyScoreListener());
		controler.setOverwriteFiles(true);

		LogEnergyConsumption logEnergyConsumption = new LogEnergyConsumption(controler);
		LogParkingTimes logParkingTimes = new LogParkingTimes(controler);
		SimulationStartupListener simulationStartupListener = new SimulationStartupListener(controler);
		controler.addControlerListener(simulationStartupListener);

		simulationStartupListener.addEventHandler(logEnergyConsumption);
		simulationStartupListener.addEventHandler(logParkingTimes);
		simulationStartupListener.addParameterPSFMutator(parameterPSFMutator);

		AfterSimulationListener afterSimulationListener = new AfterSimulationListener(logEnergyConsumption, logParkingTimes);
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

			String matsimFolderPath = "C:\\Program Files (x86)\\MATLAB\\R2008b\\bin\\";
			// String scriptPath = "a:\\";
			String scriptName;
			if (iterationNumber == 0) {
				scriptName = "IVT_main_base";
			} else {
				scriptName = "IVT_main";
			}

			out.println("cd " + matsimFolderPath);
			out.println("c:");
			out.println("matlab -r " + scriptName);
			out.flush();
			System.out.println("Starting PSS...");

			// check, if MATLab finished
			tempFile = new File(outputPSSPath + "\\fertig.txt");
			while (!tempFile.exists()) {
				// TODO: do counting, if this not possible

				for (int i = 0; i < 1000000000; i++) {

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

	private String getIterationResultDirectory() {
		return resultDirectory + "iteration" + iterationNumber;
	}

}
