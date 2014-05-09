package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;

/**
 * Requirements: some scripts need write access to r folder to install new packages -> put r folder outside of programms folder and remove read only access
 * 
 * @author wrashid
 *
 */
public class RIntegration {

//	public String pathToRScriptExe = "C:\\Program Files\\R\\R-3.1.0\\bin\\RScript.exe";
	public String pathToRScriptExe = "C:\\soft\\R-3.1.0\\bin\\RScript.exe";
	public String scriptFolder = "C:\\data\\Dropbox\\ETH\\static data\\r-script-lib\\";
	public String tempFolder = "c:/tmp/";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputPath = "C:/data/Dropbox/ETH/Projekte/STRC2014/experiments/keepGroups vs not/scoreDiffs.png";
		// generateBoxPlot("C:/data/Dropbox/ETH/Projekte/STRC2014/experiments/keepGroups vs not/scoreDiffs.txt",outputPath,"title","xAxis","yAxis",
		// "c:\\tmp\\boxPlotBatchOutput.batch");
		new RIntegration()
				.generateBoxPlot(
						"C:/data/Dropbox/ETH/Projekte/STRC2014/experiments/keepGroups vs not/scoreDiffs.txt",
						outputPath, "title", "xAxis", "yAxis", null);
	}

	/**
	 * 
	 * @param inputData
	 * @param outputPlotPath
	 * @param title
	 * @param xAxis
	 * @param yAxis
	 * @param outputLogPath
	 */
	public void generateBoxPlot(String inputData, String outputPlotPath,
			String title, String xAxis, String yAxis, String outputLogPath) {
		String scriptPath = scriptFolder + "generalBoxPlot.R";
		String[] args = new String[5];
		args[0] = inputData;
		args[1] = outputPlotPath;
		args[2] = title;
		args[3] = xAxis;
		args[4] = yAxis;
		callRScript(scriptPath, args, outputLogPath);
	}

	public void generateHistogram(String inputData, String outputPlotPath,
			String title, String xAxis, String yAxis, String outputLogPath) {
		String scriptPath = scriptFolder + "generalHistogram.R";
		String[] args = new String[5];
		args[0] = inputData;
		args[1] = outputPlotPath;
		args[2] = title;
		args[3] = xAxis;
		args[4] = yAxis;
		callRScript(scriptPath, args, outputLogPath);
	}
	
	
	/**
	 * 
	 * @param inputData
	 * @param outputPlotPath
	 * @param title
	 * @param xAxis
	 * @param yAxis
	 * @param outputLogPath
	 * @param cutPct => until which percentage graph should be drawn, e.g. 0.95
	 */
	public void generateCumulativeFrequencyGraph(String inputData, String outputPlotPath,
			String title, String xAxis, String yAxis, String outputLogPath, double cutPct) {
		String scriptPath = scriptFolder + "generalCumulativeFrquencyGraph.R";
		String[] args = new String[6];
		args[0] = inputData;
		args[1] = outputPlotPath;
		args[2] = title;
		args[3] = xAxis;
		args[4] = yAxis;
		args[5] = Double.toString(cutPct);
		callRScript(scriptPath, args, outputLogPath);
	}

	// TODO: start script in background, so that cmd window does not open up
	// TODO: update code, such that writing batch file is not necessary

	/**
	 * 
	 * @param scriptPath
	 * @param args
	 * @param scriptOutputLogPath
	 *            => null means, not interested
	 */
	public void callRScript(String scriptPath, String[] args,
			String scriptOutputLogPath) {
		Matrix file = new Matrix();
		String batPath = getPathToTempFile("bat");

		String batStringFirstLine = "set R_Script=\"" + pathToRScriptExe + "\"";
		String batStringSecondLine = "%R_Script% \"" + scriptPath + "\" ";

		for (int i = 0; i < args.length; i++) {
			batStringSecondLine += "\"" + args[i] + "\" ";
		}

		if (scriptOutputLogPath != null) {
			batStringSecondLine += " > \"" + scriptOutputLogPath + "\" 2>&1";
		}

		file.putString(0, 0, batStringFirstLine);
		file.putString(1, 0, batStringSecondLine);
		file.putString(2, 0, "exit");

		file.writeMatrix(batPath);

		try {
			Process p = Runtime.getRuntime().exec(
					"cmd /c start /wait " + batPath);
			p.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
		//new File(batPath).deleteOnExit();
	}

	public String getPathToTempFile(String extension) {
		Random rand = new Random();
		long nextLong = rand.nextLong();

		String batPath = tempFolder + nextLong + "." + extension;
		while (new File(batPath).exists()) {
			nextLong = rand.nextLong();
			batPath = tempFolder + nextLong + "." + extension;
		}
		
		return batPath;
	}
	
}
