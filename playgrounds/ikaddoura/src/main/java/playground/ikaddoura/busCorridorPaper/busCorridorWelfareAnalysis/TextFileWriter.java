package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.Time;

/**
 * @author Ihab
 *
 */
public class TextFileWriter {
	private final static Logger log = Logger.getLogger(TextFileWriter.class);

	public void write(String directoryExtItParam2, SortedMap<Integer, ExtItInformation> extIt2information) {
		File file = new File(directoryExtItParam2+"/extItData.txt");
		   
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "ITERATION ; NumberOfBuses ; fare (AUD) ; capacity (pers/veh) ; OperatorCosts (AUD) ; OperatorRevenue (AUD); OperatorProfit (AUD) ; UsersLogSum (AUD) ; Welfare (AUD) ; CarLegs ; PtLegs ; WalkLegs ; SumOfWaitingTimes";
	    bw.write(zeile1);
	    bw.newLine();
	
	    for (Integer iteration : extIt2information.keySet()){
	    	double numberOfBuses = extIt2information.get(iteration).getNumberOfBuses();
	    	double costs = extIt2information.get(iteration).getOperatorCosts();
	    	double revenue = extIt2information.get(iteration).getOperatorRevenue();
	    	double operatorProfit = extIt2information.get(iteration).getOperatorProfit();
	    	double userScoreSum = extIt2information.get(iteration).getUsersLogSum();
	    	double totalScore = extIt2information.get(iteration).getWelfare();
	    	double carLegs = extIt2information.get(iteration).getNumberOfCarLegs();
	    	double ptLegs = extIt2information.get(iteration).getNumberOfPtLegs();
	    	double walkLegs = extIt2information.get(iteration).getNumberOfWalkLegs();
	    	double fare = extIt2information.get(iteration).getFare();
	    	double capacity = extIt2information.get(iteration).getCapacity();
	    	String waitTime = Time.writeTime(extIt2information.get(iteration).getSumOfWaitingTimes(), Time.TIMEFORMAT_HHMMSS);
	    	
	    	String zeile = iteration+ " ; "+numberOfBuses+" ; "+fare+" ; "+capacity+" ; "+costs+ " ; "+revenue+" ; "+operatorProfit+" ; "+userScoreSum+" ; "+totalScore+" ; "+carLegs+" ; "+ptLegs+" ; "+walkLegs+" ; "+waitTime;
	
	    	bw.write(zeile);
	        bw.newLine();
	    }
	
	    bw.flush();
	    bw.close();
	    log.info("Analysis Textfile written to "+file.toString());
    
	    } catch (IOException e) {}		
	}
}
