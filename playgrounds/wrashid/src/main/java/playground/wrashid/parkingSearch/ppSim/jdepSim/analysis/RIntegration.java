package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.StringMatrix;

public class RIntegration {

	public String pathToRScriptExe="C:\\Program Files\\R\\R-3.1.0\\bin\\RScript.exe";
	public String scriptFolder="C:\\data\\Dropbox\\ETH\\static data\\r-script-lib\\";
	public String tempFolder="c:/tmp/";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputPath = "C:/data/Dropbox/ETH/Projekte/STRC2014/experiments/keepGroups vs not/scoreDiffs.png";
//		generateBoxPlot("C:/data/Dropbox/ETH/Projekte/STRC2014/experiments/keepGroups vs not/scoreDiffs.txt",outputPath,"title","xAxis","yAxis", "c:\\tmp\\boxPlotBatchOutput.batch");
		new RIntegration().generateBoxPlot("C:/data/Dropbox/ETH/Projekte/STRC2014/experiments/keepGroups vs not/scoreDiffs.txt",outputPath,"title","xAxis","yAxis", null);
	}
	
	
	
	
	/**
	 * 
	 * @param inputData
	 * @param outputPlotPath
	 * @param title
	 * @param xAxis
	 * @param yAxis
	 * @param outputLogPath => null means, not interested
	 */
	public void generateBoxPlot(String inputData, String outputPlotPath, String title, String xAxis, String yAxis, String outputLogPath){
			String scriptPath= scriptFolder + "generalBoxPlot.R";
			String[] args=new String[2];
			args[0]=inputData;
			args[1]=outputPlotPath;
			callRScript(scriptPath,args,outputLogPath);
	}
	
	// TODO: start script in background, so that cmd window does not open up
	// TODO: update code, such that writing batch file is not necessary
	
	public void callRScript(String scriptPath, String[] args, String scriptOutputLogPath){
		StringMatrix file=new StringMatrix();
		String batPath = getPathToTempBat();
		
		String batStringFirstLine="set R_Script=\""+ pathToRScriptExe + "\"";
		String batStringSecondLine="%R_Script% \"" + scriptPath + "\" ";
		
		for (int i=0;i<args.length;i++){
			batStringSecondLine+="\""+ args[i] +"\" " ;
		}
		
		if (scriptOutputLogPath!=null){
			batStringSecondLine+=" > \""+ scriptOutputLogPath +"\" 2>&1";
		}
		
		
		file.putString(0, 0, batStringFirstLine);
		file.putString(1, 0, batStringSecondLine);
		file.putString(2, 0, "exit");
		
		file.writeMatrix(batPath);
		
		try {
			Process p = Runtime.getRuntime().exec("cmd /c start /wait " + batPath);
		    p.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}


	private String getPathToTempBat() {
		Random rand=new Random();
		long nextLong = rand.nextLong();
		
		String batPath=tempFolder+nextLong + ".bat";
		while (new File(batPath).exists()){
			nextLong = rand.nextLong();
			batPath=tempFolder+nextLong + ".bat";
		}
		
		return batPath;
	}

}
