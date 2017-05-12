package playground.santiago.utils;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

public class CouplingScoreStatistics {
	
	private HashMap <String,String> simSteps = new HashMap<>();
	private String pct = "10pct";
	private String outputFolder = "../../../baseCaseAnalysis/" + pct + "/2_testingSteps/";
	private final Logger log = Logger.getLogger(CouplingScoreStatistics.class);
	
	
	public static void main(String[] args) {		
		CouplingScoreStatistics css = new CouplingScoreStatistics ();
		css.run();
	}
	
	private void run(){
//		simSteps.put("0", "0-100");
//		simSteps.put("A1_0","100-300");
//		simSteps.put("SP1_0","100-1100");
//		simSteps.put("P1_0", "100-900");
//		simSteps.put("F_0","100-1100");
//		simSteps.put("1d", "100-200");
//		simSteps.put("2d_0", "200-1200");
//		simSteps.put("H_0", "300-500");
//		simSteps.put("G_0", "300-500");
//		simSteps.put("I_0", "300-500");
//		simSteps.put("J_0", "300-500");
//		simSteps.put("0_24", "0-300");
//		simSteps.put("0_24C0", "300-600");
//		simSteps.put("0_24T0", "300-600");
//		simSteps.put("0_24T2", "600-800");
//		simSteps.put("0_24T1", "600-800");
//		simSteps.put("0_1", "0-300");
//		simSteps.put("0_P", "0-100");
//		simSteps.put("0.A", "0-500");
//		simSteps.put("1", "100-600");		
//		simSteps.put("1.A", "600-800");		
//		simSteps.put("1.B", "600-800");
//		simSteps.put("0x", "0-100");
//		simSteps.put("1", "100-400");		
//		simSteps.put("1x", "300-600");
//		simSteps.put("1xA", "600-800");
		simSteps.put("1xB", "600-800");
		
		
		try{
		PrintWriter pw = new PrintWriter (new FileWriter ( outputFolder + "stepsScoreStatistics.txt" ));
		pw.println("ITERATION" + "\t" + "avg. EXECUTED" + "\t" + "avg. WORST" + "\t" + "avg. AVG" + "\t" + "avg. BEST" + "\t" + "Step" + "\t" + "REAL ITERATION");
		
		for(HashMap.Entry<String, String> entry :simSteps.entrySet()){
			
			String stepOutputFolder;
			stepOutputFolder = "../../../runs-svn/santiago/baseCase" + pct + "/outputOfStep" + entry.getKey() + "/";
				

			
				try{
					
					String [] firstLastIt = entry.getValue().split("-");
					int firstIt = Integer.parseInt(firstLastIt[0]);					
					String scoreStats = stepOutputFolder + "scorestats.txt";
					BufferedReader br = IOUtils.getBufferedReader(scoreStats);
					String line=br.readLine(); //skipping the headers.					
					while ((line = br.readLine()) != null){
						String[] entries = line.split("\t");
						int preIt = Integer.parseInt(entries[0]);
						int realIt = firstIt + preIt;
						pw.println(line + "\t" + entry.getKey() + "\t" + realIt);
					}			

				}catch(IOException e){
					log.error(new Exception(e));
				}



			
			
		}
		
		
		pw.close();
		
		}catch(IOException e){
			log.error(new Exception(e));
		}

		
	}


	
	
}
