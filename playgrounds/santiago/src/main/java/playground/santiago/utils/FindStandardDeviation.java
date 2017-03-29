package playground.santiago.utils;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;


import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;
import org.apache.commons.lang.math.IntRange;
import org.apache.log4j.Logger;

public class FindStandardDeviation {
	

	private String tripsFolder = "../../../shared-svn/projects/santiago/scenario/trips/" ;
	private String transantiagoTrips = tripsFolder + "toCompare/tripsTransantiago.txt" ;
	private String populationTrips = tripsFolder + "toCompare/tripsMATSim.txt";
	
	
	private int numberOfExperiments = 10; //number of experiments to estimate the "expected value" of the error measure.
	private int maxStandardDeviation = 60; //max. value for standard deviation to be tested (in minutes).
	private int intervalLength = 15; //size of bins to build the histograms (in minutes). Try pair values or 5 - multiples values.
	
	
	private final static Logger log = Logger.getLogger(FindStandardDeviation.class);

	 public static void main (String[]arg){
		 FindStandardDeviation fsd = new FindStandardDeviation();
		 fsd.run();
	 }
	 
	 private void run () {


		 ArrayList<Double> transantiagoTimes = readData(transantiagoTrips);
		 ArrayList<Double> populationTimes = readData (populationTrips);
		 
		 
		 TreeMap<Integer,Double> tsHistogram = createHistogram (transantiagoTimes);
		 
	//	 write(tsHistogram, "histogramTS.txt"); //Histogram with bins' size equal to 15 minutes - not necessary
		 
		 TreeMap<Integer,Double> summary = new TreeMap<>();
		 
		 
		 
		 for (int sd = 1; sd<=maxStandardDeviation ; ++sd){
			 
			 double error = 0;
			 double expectedError = 0;
			 ArrayList<Double> randomizedTimes = new ArrayList<>();
			 TreeMap<Integer,Double> populationHistogram = new TreeMap<>(); 

			 
			 for (int rep = 0; rep<numberOfExperiments; ++rep){
				 
				 randomizedTimes.clear();
				 populationHistogram.clear();
				 
				 randomizedTimes = randomizeEndTimes(sd, populationTimes);
				 populationHistogram = createHistogram (randomizedTimes);
				 error = error + getMAPE(populationHistogram,tsHistogram);


			 }
			 
			 expectedError = error / numberOfExperiments;
			 summary.put(sd, expectedError);
			 write(populationHistogram, "histogramWith" + sd + "StandardDeviation.txt");
			 write(randomizedTimes, "tripsMATSim-"+sd+"SD.txt");
		 }
		 

		 write(summary, "summary.txt");
		 
	 }
	 
	 
	 //returns a list of trip-start-times given a .txt file - OK
	 private ArrayList<Double> readData(String tripsFile){

		 ArrayList <Double> times = new ArrayList<>();

		 try {

			 BufferedReader br = IOUtils.getBufferedReader(tripsFile);
			 String line;

			 while ((line = br.readLine()) != null) {
				 String entries[] = line.split(";");
				 if ((entries[2].equalsIgnoreCase("bus")||entries[2].equalsIgnoreCase("metro")||entries[2].equalsIgnoreCase("ZP"))&&(!entries[1].equals("-Infinity"))){
					 times.add(Double.parseDouble(entries[1]));
				 }

			 }



		 } catch (IOException e) {
			 log.error(new Exception(e));
		 }
		 
		 return times;
		 
		 }
	 
	 private double createRandomEndTime(Random random, int standardDeviation){
		 //draw two random numbers [0;1] from uniform distribution
		 double r1 = random.nextDouble();
		 double r2 = random.nextDouble();

		 //Box-Muller-Method in order to get a normally distributed variable
		 double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		 double endTime = standardDeviation*60*normal;

		 return endTime;
		}
	 
	 //returns a list of randomized time according to a normal distribution - OK
	 private ArrayList<Double> randomizeEndTimes(int standardDeviation, ArrayList<Double> times){
		 log.info("Randomizing activity end times...");
			ArrayList <Double> randomizedTimes = new ArrayList<>();
		 
			Random random = MatsimRandom.getRandom();
			
			for(double time: times){				
				
				double delta = 0;
				
				while(delta == 0){
					
					delta = createRandomEndTime(random, standardDeviation);
					
					if(time + delta < 0){
						delta = 0;
					}
					if(time + delta  > 24 * 3600){
						delta = 0;
					}
					
				}				
			
					randomizedTimes.add(time+delta);
								
					}
			
			log.info("...Done");
			
			
			return randomizedTimes;
			

		}
	 
	 private TreeMap<Integer,Double> createHistogram (ArrayList<Double> times){
		 
		 Collections.sort(times);		 
		 TreeMap<Integer,Double> histogram = new TreeMap<>();		 
		 double totalTrips = times.size();
		 
		 int totalBins = (24 * 60)/ intervalLength;
		 

		 
		 //go through the bins.
		 for (int i=1;i<=totalBins;++i){
			 double numberOfTrips = 0; //trips that should be INSIDE each bin.

			 int lowerLimit = 60*intervalLength*(i-1);
			 int upperLimit = (60*intervalLength*i)-1;
			 IntRange range = new IntRange(lowerLimit, upperLimit);
			 
			 for (int j=0;j<times.size();++j){
				 if(range.containsDouble(times.get(j))){

					 ++numberOfTrips;

				 }

		 }
			 double proportionOfTrips = (numberOfTrips/totalTrips)*100;
			 histogram.put(i, proportionOfTrips); 
		 }
		 return histogram;
	 }
	 	 
	 private double getMAPE(TreeMap<Integer,Double> popHist, TreeMap<Integer,Double> tsHist){
		 double error = 0;
		 
		 int n = popHist.size(); //could be tsHist.size(), they should have the same number of bins.
		 
		 for (int i = 1; i<=n; ++i){
			 

			 double actual = tsHist.get(i);
			 double forecast = popHist.get(i);
			 //TODO: Add a condition to check when actual == 0
			 error = error + Math.abs(actual-forecast)/Math.abs(actual);
			 
			 
		 }
		 
		 double MAPE =(error/n)*100;
		 
		 return MAPE;
	 }
	 
	 private double getChiSquare(TreeMap<Integer,Double> popHist, TreeMap<Integer,Double> tsHist){
		 double distance = 0;
		 
		 int n = popHist.size(); //could be tsHist.size(), they should have the same number of bins.
		 
		 for (int i = 1; i<=n; ++i){
			 

			 double actual = tsHist.get(i);
			 double forecast = popHist.get(i);
			 //TODO: Add a condition to check when actual == 0
			 distance = distance + (Math.pow((actual-forecast), 2)/(actual+forecast));
			 
			 
		 }
		 
		 double chiSquare =(distance)*0.5;
		 
		 return chiSquare;
	 }
	 	 
	 private void write(TreeMap<Integer,Double> histogram, String fileName){
		 try {
				
				PrintWriter pw1 = new PrintWriter (new FileWriter ( tripsFolder + "toCompare/" + fileName ));			
				for(Map.Entry<Integer,Double> entry : histogram.entrySet()) {
					  int key = entry.getKey();
					  double value = entry.getValue();		
					  pw1.println(key + " - " + value);
					
				}	
	
				pw1.close();


			} catch (IOException e) {
				log.error(new Exception(e));
			}
	 
	 }
	 
	 private void write(ArrayList<Double> map, String fileName){
		 try {
				
				PrintWriter pw1 = new PrintWriter (new FileWriter ( tripsFolder + "toCompare/" + fileName ));

				
				for(double entry : map) {
		
					  pw1.println(entry);
					
				}	
	
				pw1.close();


			} catch (IOException e) {
				log.error(new Exception(e));
			}
	 
	 }
	 
	 
	 
}
