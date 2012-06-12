package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.Time;

/**
 * @author Ihab
 *
 */
public class TextFileWriter {
	private final static Logger log = Logger.getLogger(TextFileWriter.class);

	public void write(String directoryExtItParam, SortedMap<Integer, ExtItInformation> extIt2information) {
		File file = new File(directoryExtItParam+"/extItData.txt");
		   
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "ITERATION ; NumberOfBuses ; headway (hh:mm:ss) ; fare (AUD) ; capacity (pers/veh) ; OperatorCosts (AUD) ; OperatorRevenue (AUD); OperatorProfit (AUD) ; UsersLogSum (AUD) ; Welfare (AUD) ; CarLegs ; PtLegs ; WalkLegs ; SumOfWaitingTimes (sec)";
	    bw.write(zeile1);
	    bw.newLine();
	
	    for (Integer iteration : extIt2information.keySet()){
	    	double numberOfBuses = extIt2information.get(iteration).getNumberOfBuses();
	    	String headway = Time.writeTime(extIt2information.get(iteration).getHeadway(), Time.TIMEFORMAT_HHMMSS);
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
	    	double waitTime = extIt2information.get(iteration).getSumOfWaitingTimes();
	    	
	    	String zeile = iteration+ " ; "+numberOfBuses+" ; "+headway+" ; "+fare+" ; "+capacity+" ; "+costs+ " ; "+revenue+" ; "+operatorProfit+" ; "+userScoreSum+" ; "+totalScore+" ; "+carLegs+" ; "+ptLegs+" ; "+walkLegs+" ; "+waitTime;
	
	    	bw.write(zeile);
	        bw.newLine();
	    }
	
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}		
	}

	public void writeMatrices(String directoryExtItParam, SortedMap<Integer, ExtItInformation> it2information) {
		
		SortedMap<Integer, SortedMap<Double, Double>> busNumber2fare2welfare = new TreeMap<Integer, SortedMap<Double, Double>>();
		SortedMap<Integer, SortedMap<Double, Double>> busNumber2fare2profit = new TreeMap<Integer, SortedMap<Double, Double>>();

		// Sort data
		for (Integer it1 : it2information.keySet()){
			double numberOfBuses = it2information.get(it1).getNumberOfBuses();
			SortedMap<Double, Double> fare2welfare = new TreeMap<Double, Double>();
			SortedMap<Double, Double> fare2profit = new TreeMap<Double, Double>();
			for (Integer it2 : it2information.keySet()){
				if (it2information.get(it2).getNumberOfBuses() == numberOfBuses){
					fare2welfare.put(it2information.get(it2).getFare()*(-1), it2information.get(it2).getWelfare());
					fare2profit.put(it2information.get(it2).getFare()*(-1), it2information.get(it2).getOperatorProfit());
				}
			busNumber2fare2welfare.put((int) numberOfBuses, fare2welfare);
			busNumber2fare2profit.put((int) numberOfBuses, fare2profit);
			}
	    }
		
		// Write sorted data to text file
		writeMatrix(busNumber2fare2welfare, directoryExtItParam, "matrix_welfare.txt", "Welfare (AUD)");
		writeMatrix(busNumber2fare2profit, directoryExtItParam, "matrix_profit.txt", "Profit (AUD)");
	}
	
	private void writeMatrix(SortedMap<Integer, SortedMap<Double, Double>> busNumber2fare2value, String directoryExtItParam, String filename, String title) {
		
		File file = new File(directoryExtItParam+"/"+filename);
		
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = title;
	    bw.write(zeile1);
	    bw.newLine();
	    
		String zeile = "BusNumber / Fare (AUD) ";
		for (Double fare : busNumber2fare2value.get(busNumber2fare2value.firstKey()).keySet()){
		   zeile = zeile + " ; " + fare.toString();
		}
		bw.write(zeile);
		bw.newLine();
		        
		for (Integer busNumber : busNumber2fare2value.keySet()){
	        String zeileX = busNumber.toString();
	        for (Double fare : busNumber2fare2value.get(busNumber).keySet()){
	        	zeileX = zeileX + " ; " + busNumber2fare2value.get(busNumber).get(fare).toString();
	        }
	    bw.write(zeileX);
		bw.newLine();
		}
		
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}		
	}
}
