package playground.ikaddoura.optimization.io;

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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Time;

import playground.ikaddoura.optimization.IterationInfo;
import playground.ikaddoura.optimization.users.FareData;

/**
 * @author Ihab
 *
 */
public class TextFileWriter {
	private final static Logger log = Logger.getLogger(TextFileWriter.class);

	public void writeExtItData(String directoryExtItParam, SortedMap<Integer, IterationInfo> extIt2information) {
		File file = new File(directoryExtItParam+"/extItData.csv");
		   
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile0 = directoryExtItParam;
	    bw.write(zeile0);
	    bw.newLine();
	    String zeile1 = "ITERATION;Headway (sec);NumberOfBuses;Headway (hh:mm:ss);Fare (AUD);AvgFarePerAgent (AUD);Capacity (Pers/Veh);TotalDemand;OperatorCosts (AUD);OperatorRevenue (AUD);OperatorProfit (AUD);UsersLogSum (AUD);Welfare (AUD);CarLegs;PtLegs;WalkLegs;AvgWaitingTimeAll (sec);AvgWaitingTimeNotMissing (sec);AvgWaitingTimeMissing (sec);NumberOfMissedBusTrips;NumberOfNotMissedBusTrips;MissedBusses;boardingDeniedEvents;t0MinusTActSum (sec);avgT0MinusTActPerPerson (sec);avgT0MinusTActDivT0perTrip;maxDepartureDelay (sec);maxArrivalDelay (sec);NumberOfAgentsNoValidPlan";
	    bw.write(zeile1);
	    bw.newLine();
	
	    for (Integer iteration : extIt2information.keySet()){
	    	double numberOfBuses = extIt2information.get(iteration).getNumberOfBuses();
	    	double headway_sec = extIt2information.get(iteration).getHeadway();
	    	String headway = Time.writeTime(extIt2information.get(iteration).getHeadway(), Time.TIMEFORMAT_HHMMSS);
	    	double totalDemand = extIt2information.get(iteration).getTotalDemand();
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
	    	double boardingDeniedEvents = extIt2information.get(iteration).getNumberOfBoardingDeniedEvents();
	    	double t0MinustActSum = extIt2information.get(iteration).getT0MinusTActSum();
	    	double avgT0MinustActPerPerson = extIt2information.get(iteration).getAvgT0MinusTActPerPerson();
	    	double avgT0MinusTActDivT0perTrip = extIt2information.get(iteration).getAvgT0MinusTActDivT0PerTrip();
	    	double numberOfAgentsNoValidPlan = extIt2information.get(iteration).getNoValidPlanScore();
	    	double avgFarePerAgent = extIt2information.get(iteration).getAverageFarePerAgent();
	    	double maxDepartureDelay = extIt2information.get(iteration).getMaxDepartureDelay();
	    	double maxArrivalDelay = extIt2information.get(iteration).getMaxArrivalDelay();
	    	
	    	String zeile = iteration+ ";"+headway_sec+";"+numberOfBuses+";"+headway+";"+fare+";"+avgFarePerAgent+";"+capacity+";"+totalDemand+";"+costs+ ";"+revenue+";"+operatorProfit+";"+userScoreSum+";"+totalScore+";"+carLegs+";"+ptLegs+";"+walkLegs+";"+avgWaitTimeAll+";" +avgWaitTimeNotMissing+";"+avgWaitTimeMissing+";"+waitingTimeMoreThanHeadway+";"+waitingTimeLessThanHeadway+";"+missedBusses+ ";"+boardingDeniedEvents+";"+t0MinustActSum+ ";" +avgT0MinustActPerPerson+ ";" +avgT0MinusTActDivT0perTrip+ ";"+maxDepartureDelay+";"+maxArrivalDelay+";"+numberOfAgentsNoValidPlan;
	
	    	bw.write(zeile);
	        bw.newLine();
	    }
	
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}		
	}
	
	public void writeMatrices(String directoryExtItParam, SortedMap<Integer, IterationInfo> it2information) {
		
		SortedMap<Integer, SortedMap<Double, Double>> busNumber2fare2welfare = new TreeMap<Integer, SortedMap<Double, Double>>();
		SortedMap<Integer, SortedMap<Double, Double>> busNumber2fare2profit = new TreeMap<Integer, SortedMap<Double, Double>>();
		SortedMap<Integer, SortedMap<Double, Double>> busNumber2fare2userLogsum = new TreeMap<Integer, SortedMap<Double, Double>>();
		SortedMap<Integer, SortedMap<Double, Double>> busNumber2fare2carLegs = new TreeMap<Integer, SortedMap<Double, Double>>();
		SortedMap<Integer, SortedMap<Double, Double>> busNumber2fare2ptLegs = new TreeMap<Integer, SortedMap<Double, Double>>();
		SortedMap<Integer, SortedMap<Double, Double>> busNumber2fare2avgT0MinusTAktPerPerson = new TreeMap<Integer, SortedMap<Double, Double>>();
		SortedMap<Integer, SortedMap<Double, Double>> busNumber2fare2avgT0MinusTActDivT0PerTrip = new TreeMap<Integer, SortedMap<Double, Double>>();
		SortedMap<Integer, SortedMap<Double, Double>> busNumber2fare2numberOfMissedBusTrips = new TreeMap<Integer, SortedMap<Double, Double>>();
		SortedMap<Integer, SortedMap<Double, Double>> busNumber2fare2avgWaitingTimeMissing = new TreeMap<Integer, SortedMap<Double, Double>>();
		SortedMap<Integer, SortedMap<Double, Double>> busNumber2fare2avgWaitingTimeNotMissing = new TreeMap<Integer, SortedMap<Double, Double>>();

		// ...

		// Sort data
		for (Integer it1 : it2information.keySet()){
			double numberOfBuses = it2information.get(it1).getNumberOfBuses();
			SortedMap<Double, Double> fare2welfare = new TreeMap<Double, Double>();
			SortedMap<Double, Double> fare2profit = new TreeMap<Double, Double>();
			SortedMap<Double, Double> fare2userLogsum = new TreeMap<Double, Double>();
			SortedMap<Double, Double> fare2carLegs = new TreeMap<Double, Double>();
			SortedMap<Double, Double> fare2ptLegs = new TreeMap<Double, Double>();
			SortedMap<Double, Double> fare2avgT0MinusTAktPerPerson = new TreeMap<Double, Double>();
			SortedMap<Double, Double> fare2avgT0MinusTActDivT0PerTrip = new TreeMap<Double, Double>();
			SortedMap<Double, Double> fare2numberOfMissedBusTrips = new TreeMap<Double, Double>();
			SortedMap<Double, Double> fare2avgWaitingTimeMissing = new TreeMap<Double, Double>();
			SortedMap<Double, Double> fare2avgWaitingTimeNotMissing = new TreeMap<Double, Double>();

			// ...
			
			for (Integer it2 : it2information.keySet()){
				if (it2information.get(it2).getNumberOfBuses() == numberOfBuses){
					fare2welfare.put(it2information.get(it2).getFare()*(-1), it2information.get(it2).getWelfare());
					fare2profit.put(it2information.get(it2).getFare()*(-1), it2information.get(it2).getOperatorProfit());
					fare2userLogsum.put(it2information.get(it2).getFare()*(-1), it2information.get(it2).getUsersLogSum());
					fare2carLegs.put(it2information.get(it2).getFare()*(-1), it2information.get(it2).getNumberOfCarLegs());
					fare2ptLegs.put(it2information.get(it2).getFare()*(-1), it2information.get(it2).getNumberOfPtLegs());
					fare2avgT0MinusTAktPerPerson.put(it2information.get(it2).getFare()*(-1), it2information.get(it2).getAvgT0MinusTActPerPerson());
					fare2avgT0MinusTActDivT0PerTrip.put(it2information.get(it2).getFare()*(-1), it2information.get(it2).getAvgT0MinusTActDivT0PerTrip());
					fare2numberOfMissedBusTrips.put(it2information.get(it2).getFare()*(-1), (double) it2information.get(it2).getMissedBusTrips());
					fare2avgWaitingTimeMissing.put(it2information.get(it2).getFare()*(-1), it2information.get(it2).getAvgWaitingTimeMissingBus());
					fare2avgWaitingTimeNotMissing.put(it2information.get(it2).getFare()*(-1), it2information.get(it2).getAvgWaitingTimeNotMissingBus());

					// ...

				}
				
			busNumber2fare2welfare.put((int) numberOfBuses, fare2welfare);
			busNumber2fare2profit.put((int) numberOfBuses, fare2profit);
			busNumber2fare2userLogsum.put((int) numberOfBuses, fare2userLogsum);
			busNumber2fare2carLegs.put((int) numberOfBuses, fare2carLegs);
			busNumber2fare2ptLegs.put((int) numberOfBuses, fare2ptLegs);
			busNumber2fare2avgT0MinusTAktPerPerson.put((int) numberOfBuses, fare2avgT0MinusTAktPerPerson);
			busNumber2fare2avgT0MinusTActDivT0PerTrip.put((int) numberOfBuses, fare2avgT0MinusTActDivT0PerTrip);
			busNumber2fare2numberOfMissedBusTrips.put((int) numberOfBuses, fare2numberOfMissedBusTrips);
			busNumber2fare2avgWaitingTimeMissing.put((int) numberOfBuses, fare2avgWaitingTimeMissing);
			busNumber2fare2avgWaitingTimeNotMissing.put((int) numberOfBuses, fare2avgWaitingTimeNotMissing);
			
			// ...
			
			}
	    }
		
		// Write sorted data to text file
		writeMatrix(busNumber2fare2welfare, directoryExtItParam, "matrix_welfare.csv", "Welfare (AUD)");
		writeMatrix(busNumber2fare2profit, directoryExtItParam, "matrix_profit.csv", "Profit (AUD)");
		writeMatrix(busNumber2fare2userLogsum, directoryExtItParam, "matrix_userLogsum.csv", "UserLogSum (AUD)");
		writeMatrix(busNumber2fare2carLegs, directoryExtItParam, "matrix_carLegs.csv", "Number of Car Legs");
		writeMatrix(busNumber2fare2ptLegs, directoryExtItParam, "matrix_ptLegs.csv", "Number of Pt Legs");
		writeMatrix(busNumber2fare2avgT0MinusTAktPerPerson, directoryExtItParam, "matrix_car_avgT0MinusTAktPerPerson.csv", "avg t0-tAkt per car-user (sec)");
		writeMatrix(busNumber2fare2avgT0MinusTActDivT0PerTrip, directoryExtItParam, "matrix_car_avgT0MinusTActDivT0PerTrip.csv", "avg (t0-tAct)/t0 per car-trip");
		writeMatrix(busNumber2fare2numberOfMissedBusTrips, directoryExtItParam, "matrix_pt_numberOfMissedBusTrips.csv", "Number of Trips when a bus was missed");
		writeMatrix(busNumber2fare2avgWaitingTimeMissing, directoryExtItParam, "matrix_pt_avgWaitingTimeMissing.csv", "avg waiting time per pt-user when a bus was missed (sec)");
		writeMatrix(busNumber2fare2avgWaitingTimeNotMissing, directoryExtItParam, "matrix_pt_avgWaitingTimeNotMissing.csv", "avg waiting time per pt-user when person got the first arriving bus (sec)");
		
		// ...
	}
	
	private void writeMatrix(SortedMap<Integer, SortedMap<Double, Double>> busNumber2fare2value, String directoryExtItParam, String filename, String title) {
		
		File file = new File(directoryExtItParam+"/"+filename);
		
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = title;
	    bw.write(zeile1);
	    bw.newLine();
	    
//		String zeile = "BusNumber / Fare (AUD) ";
	    String zeile = "";
		for (Double fare : busNumber2fare2value.get(busNumber2fare2value.firstKey()).keySet()){
		   zeile = zeile + ";" + fare.toString();
		}
		bw.write(zeile);
		bw.newLine();
		        
		for (Integer busNumber : busNumber2fare2value.keySet()){
	        String zeileX = busNumber.toString();
	        for (Double fare : busNumber2fare2value.get(busNumber).keySet()){
	        	zeileX = zeileX + ";" + busNumber2fare2value.get(busNumber).get(fare).toString();
	        }
	    bw.write(zeileX);
		bw.newLine();
		}
		
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}		
	}

	public void writeWaitDataPerPerson(String directoryExtItParam2Param1, SortedMap<Integer, IterationInfo> extIt2information, int extItParam1) {
		
		String path = directoryExtItParam2Param1 + "/Data/";
		File directory = new File(path);
		directory.mkdirs();

		File file = new File(path + "/personWaitData.csv");
			
		try {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		    
		String zeile0 = "PersonWaitData of external iteration " + extItParam1 + ":";
		bw.write(zeile0);
		bw.newLine();
		    
		String zeile1 = "PersonId;WaitingTimes [sec]";
		bw.write(zeile1);
		bw.newLine();
		
		for (Id<Person> personId : extIt2information.get(extItParam1).getPersonId2waitingTimes().keySet()){
		    
			List<Double> waitingTimes = extIt2information.get(extItParam1).getPersonId2waitingTimes().get(personId);

			String zeile = personId.toString();
	    	
	    	for (Double time : waitingTimes){
	    		zeile = zeile + ";" + time.toString();
	    	}
	 
	    	bw.write(zeile);
	        bw.newLine();
		 }
		
		 bw.flush();
		 bw.close();
		 log.info("Textfile written to "+file.toString());
	    
		 } catch (IOException e) {}	
		
	}

	public void writeFareData(String directoryExtItParam, List<FareData> list) {
		File file = new File(directoryExtItParam + "/time2fareData.csv");
		   
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile0 = directoryExtItParam;
	    bw.write(zeile0);
	    bw.newLine();
	    String zeile1 = "Time;Amount(AUD);PersonID";
	    bw.write(zeile1);
	    bw.newLine();
	
	    for (FareData fareData : list){
	    	
	    	String zeile = fareData.getTime() + ";" + fareData.getAmount() + ";" + fareData.getPersonId();
	
	    	bw.write(zeile);
	        bw.newLine();
	    }
	
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}
	}

	public void wrtieFarePerTime(String outputPath, Map<Double, Double> detailFareAnalysis) {
		File file = new File(outputPath + "/tripDepartureTimePeriod2avgFare.csv");
		   
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile0 = outputPath;
	    bw.write(zeile0);
	    bw.newLine();
	    String zeile1 = "Trip departure time period (bis unter);average fare per trip (AUD)";
	    bw.write(zeile1);
	    bw.newLine();
	
	    for (Double time : detailFareAnalysis.keySet()){
	    	
	    	String zeile = time + ";" + detailFareAnalysis.get(time);
	
	    	bw.write(zeile);
	        bw.newLine();
	    }
	
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}
		
	}

	public void writeTripFarePerId(String outputPath, Map<Id<Person>, Double> firstTripFares, Map<Id<Person>, Double> secondTripFares) {
	
		File file = new File(outputPath + "/personId2tripFares.csv");
		   
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile0 = outputPath;
	    bw.write(zeile0);
	    bw.newLine();
	    String zeile1 = "PersonID;fare first trip (AUD);fare second trip (AUD)";
	    bw.write(zeile1);
	    bw.newLine();
	
	    for (Id<Person> id : firstTripFares.keySet()){
	    	
	    	String zeile = id + ";" + firstTripFares.get(id) + ";" + secondTripFares.get(id);
	
	    	bw.write(zeile);
	        bw.newLine();
	    }
	
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}
		
	}
	
}
