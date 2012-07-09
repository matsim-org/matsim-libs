package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Time;

/**
 * @author Ihab
 *
 */
public class TextFileWriter {
	private final static Logger log = Logger.getLogger(TextFileWriter.class);

	public void writeExtItData(String directoryExtItParam, SortedMap<Integer, ExtItInformation> extIt2information) {
		File file = new File(directoryExtItParam+"/extItData.txt");
		   
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "ITERATION ; NumberOfBuses ; Headway (hh:mm:ss) ; Fare (AUD) ; Capacity (Pers/Veh) ; OperatorCosts (AUD) ; OperatorRevenue (AUD); OperatorProfit (AUD) ; UsersLogSum (AUD) ; Welfare (AUD) ; CarLegs ; PtLegs ; WalkLegs ; AvgWaitingTimeAll (sec) ; AvgWaitingTimeNotMissing (sec) ; AvgWaitingTimeMissing (sec) ; NumberOfMissedBusTrips ; NumberOfNotMissedBusTrips ; MissedBusses ; NumberOfAgentsNoValidPlan";
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
	    	double avgWaitTimeAll = extIt2information.get(iteration).getAvgWaitingTimeAll();
	    	double avgWaitTimeMissing = extIt2information.get(iteration).getAvgWaitingTimeMissingBus();
	    	double avgWaitTimeNotMissing = extIt2information.get(iteration).getAvgWaitingTimeNotMissingBus();
	    	double waitingTimeMoreThanHeadway = extIt2information.get(iteration).getMissedBusTrips();
	    	double waitingTimeLessThanHeadway = extIt2information.get(iteration).getNotMissedBusTrips();
	    	double missedBusses = extIt2information.get(iteration).getNumberOfMissedVehicles();
	    	double numberOfAgentsNoValidPlan = extIt2information.get(iteration).getNoValidPlanScore();
	    	
	    	String zeile = iteration+ " ; "+numberOfBuses+" ; "+headway+" ; "+fare+" ; "+capacity+" ; "+costs+ " ; "+revenue+" ; "+operatorProfit+" ; "+userScoreSum+" ; "+totalScore+" ; "+carLegs+" ; "+ptLegs+" ; "+walkLegs+" ; "+avgWaitTimeAll+" ; " +avgWaitTimeNotMissing+" ; "+avgWaitTimeMissing+" ; "+waitingTimeMoreThanHeadway+" ; "+waitingTimeLessThanHeadway+" ; "+missedBusses+ " ; "+numberOfAgentsNoValidPlan;
	
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

	public void writeDataTransitStops(String directoryExtItParam2Param1, SortedMap<Integer, ExtItInformation> extIt2information, int extItParam1) {
		String path = directoryExtItParam2Param1 + "/Data/";
		File directory = new File(path);
		directory.mkdirs();

		File file = new File(path + "/transitStopWaitData.txt");
			
		try {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		    
		String zeile0 = "TransitStopsWaitData of external iteration " + extItParam1 + ":";
		bw.write(zeile0);
		bw.newLine();
		    
		String zeile1 = "TransitStopId ; SumOfWaitingTime (sec) ; AverageWaitingTime (sec) ; WaitingTime > Headway (trips) ; MissedVehicles";
		bw.write(zeile1);
		bw.newLine();
		
		for (FacilityWaitTimeInfo facilityInfo : extIt2information.get(extItParam1).getId2facilityWaitInfo().values()){
		    	
			Id facilityId = facilityInfo.getFacilityId();
	    	Double avgWaitTime = facilityInfo.getAvgWaitingTime();
	    	Double sumWaitTime = facilityInfo.getSumOfWaitingTimes();
	    	int waitTimeMoreThanHeadway = facilityInfo.getNumberOfWaitingTimesMoreThanHeadway();
	    	int missedVehicles = facilityInfo.getNumberOfMissedVehicles();
	    	
	    	String zeile = facilityId + " ; " + sumWaitTime + " ; " + avgWaitTime + " ; " + waitTimeMoreThanHeadway + " ; " + missedVehicles;
	
	    	bw.write(zeile);
	        bw.newLine();
		 }
		
		 bw.flush();
		 bw.close();
		 log.info("Textfile written to "+file.toString());
	    
		 } catch (IOException e) {}		
	}

	public void writeDataEachTransitStop(String directoryExtItParam2Param1, SortedMap<Integer, ExtItInformation> extIt2information, int extItParam1) {
		String path = directoryExtItParam2Param1 + "/Data/";
		File directory = new File(path);
		directory.mkdirs();
		
		for (FacilityWaitTimeInfo facilityInfo : extIt2information.get(extItParam1).getId2facilityWaitInfo().values()) {
			
			File file = new File(path + "/transitStopWaitData_" + facilityInfo.getFacilityId().toString() + ".txt");
			
			try {
			    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			    
			    String zeile0 = "TransitStopWaitData of TransitStopFacility " + facilityInfo.getFacilityId().toString() + " (External iteration " + extItParam1 + "):";
			    bw.write(zeile0);
			    bw.newLine();
			    
			    String zeile1 = "WaitingTimeId ; Daytime (sec) (when entering a vehicle) ; PersonId ; WaitingTime (sec)";
			    bw.write(zeile1);
			    bw.newLine();
			   			   	
			    for (Id waitingTimeId : facilityInfo.getWaitingEvent2DayTime().keySet()){
			    				    	
			    	Double dayTime = facilityInfo.getWaitingEvent2DayTime().get(waitingTimeId);
			    	Double waitingTime = facilityInfo.getWaitingEvent2WaitingTime().get(waitingTimeId);
			    	Id personId = facilityInfo.getWaitingEvent2PersonId().get(waitingTimeId);
			    	
			    	String zeile = waitingTimeId + " ; " + dayTime + " ; " + personId + " ; "+ waitingTime;
			
			    	bw.write(zeile);
			        bw.newLine();
			    }
			
			    bw.flush();
			    bw.close();
			    log.info("Textfile written to "+file.toString());
		    
			    } catch (IOException e) {}		
			
			
			
		}
		
	}

	public void writeLoadData1(String directoryExtItParam2Param1, SortedMap<Integer, ExtItInformation> extIt2information, int extItParam1) {
		String path = directoryExtItParam2Param1 + "/Data/";
		File directory = new File(path);
		directory.mkdirs();

		File file = new File(path + "/LoadDataTransitStops.txt");
			
		try {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		    
		String zeile0 = "LoadDataTransitStops " + directoryExtItParam2Param1 + ":";
		bw.write(zeile0);
		bw.newLine();
		
		ExtItInformation info = extIt2information.get(extItParam1);

		for (AnalysisPeriod anaPeriod : info.getAnalysisPeriods().values()){
				
			for (Id routeId : anaPeriod.getRouteId2RouteInfo().keySet()){
				bw.newLine();
				bw.newLine();
				
				String zeile1 = "TransitRoute: " + routeId.toString();
				bw.write(zeile1);
				bw.newLine();
				
				bw.newLine();
				String zeilePeriod = "Period (hour): " + anaPeriod.getStart()/3600. + " - " + anaPeriod.getEnd()/3600.;
				bw.write(zeilePeriod);
				bw.newLine();
	
				String zeile2 = "TransitStopId ; EnteringAgents ; LeavingAgents ; PassengersWhenLeavingTransitStop";
				bw.write(zeile2);
				bw.newLine();
				
				Map<Id, FacilityLoadInfo> stopId2FacilityLoadInfo = anaPeriod.getRouteId2RouteInfo().get(routeId).getTransitStopId2FacilityLoadInfo();
				List<Id> stopIDs = anaPeriod.getRouteId2RouteInfo().get(routeId).getStopIDs();
				
				for (Id stopId : stopIDs){
					String zeile3 = stopId.toString() + " ; " + stopId2FacilityLoadInfo.get(stopId).getPersonEntering() + " ; " + stopId2FacilityLoadInfo.get(stopId).getPersonLeaving() + " ; " + stopId2FacilityLoadInfo.get(stopId).getPassengersWhenLeavingFacility();
					bw.write(zeile3);
					bw.newLine();
				}		
			}
		}
		
		 bw.flush();
		 bw.close();
		 log.info("Textfile written to "+file.toString());
	    
		 } catch (IOException e) {}		
	}
	
	public void writeLoadData2(String directoryExtItParam2Param1, SortedMap<Integer, ExtItInformation> extIt2information, int extItParam1) {
		String path = directoryExtItParam2Param1 + "/Data/";
		File directory = new File(path);
		directory.mkdirs();

		File file = new File(path + "/LoadData.txt");
			
		try {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		    
		String zeile0 = "LoadData " + directoryExtItParam2Param1 + ":";
		bw.write(zeile0);
		bw.newLine();
		
		ExtItInformation info = extIt2information.get(extItParam1);

		String zeile1 = "Period (from-to) ; EnteringAgents ; LeavingAgents";
		bw.write(zeile1);
		bw.newLine();
		
		for (AnalysisPeriod anaPeriod : info.getAnalysisPeriods().values()){
			String zeile = anaPeriod.getStart()/3600. + " - " + anaPeriod.getEnd()/3600. + " ; " + anaPeriod.getEntering() + " ; " + anaPeriod.getLeaving();
			bw.write(zeile);
			bw.newLine();
		}
		
		 bw.flush();
		 bw.close();
		 log.info("Textfile written to "+file.toString());
	    
		 } catch (IOException e) {}		
	}
	
	
}
