/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.ikaddoura.busCorridor.finalDyn;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author Ihab
 *
 */

public class ExternalControler {
	
	private final static Logger log = Logger.getLogger(ExternalControler.class);
	
	static String networkFile = "../../shared-svn/studies/ihab/busCorridor/input_test/network.xml";
	static String configFile = "../../shared-svn/studies/ihab/busCorridor/input_test/config_busline.xml";
	static String populationFile = "../../shared-svn/studies/ihab/busCorridor/input_test/population.xml"; // for first iteration only
	static String outputExternalIterationDirPath = "../../shared-svn/studies/ihab/busCorridor/output_test";
	static int numberOfExternalIterations = 0;
	static int lastInternalIteration = 0; // for ChangeTransitLegMode: ModuleDisableAfterIteration = 28
	
	// settings for first iteration or if values not changed for all iterations
	TimePeriod p1 = new TimePeriod(1, "SVZ_1", 2, 8*3600, 9*3600); // orderId, id, numberOfBuses, fromTime, toTime
	TimePeriod p2 = new TimePeriod(2, "HVZ_1", 3, 9*3600, 10*3600);
	TimePeriod p3 = new TimePeriod(3, "NVZ", 2, 9*3600, 14*3600);
	TimePeriod p4 = new TimePeriod(4, "HVZ_2", 5, 14*3600, 17*3600);
	TimePeriod p5 = new TimePeriod(5, "SVZ_2", 1, 17*3600, 23*3600);

	private double fare = -2.5; // negative!
	private int capacity = 50; // standing room + seats (realistic values between 19 and 101!)

	private int extItNr;
	private String directoryExtIt;
	private int maxNumberOfBuses;
	
	private Map<Integer, TimePeriod> day = new HashMap<Integer, TimePeriod>();

	private Map<Integer, Double> iteration2operatorProfit = new HashMap<Integer, Double>();
	private Map<Integer, Double> iteration2operatorCosts = new HashMap<Integer, Double>();
	private Map<Integer, Double> iteration2operatorEarnings = new HashMap<Integer, Double>();
	private Map<Integer, Double> iteration2numberOfBuses = new HashMap<Integer, Double>();
	private Map<Integer, String> iteration2day = new HashMap<Integer, String>();
	private Map<Integer, Double> iteration2userScore = new HashMap<Integer,Double>();
	private Map<Integer, Double> iteration2userScoreSum = new HashMap<Integer,Double>();
	private Map<Integer, Double> iteration2totalScore = new HashMap<Integer,Double>();
	private Map<Integer, Integer> iteration2numberOfCarLegs = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> iteration2numberOfPtLegs = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> iteration2numberOfWalkLegs = new HashMap<Integer, Integer>();
	private Map<Integer, Double> iteration2fare = new HashMap<Integer, Double>();
	private Map<Integer, Double> iteration2capacity = new HashMap<Integer, Double>();

	public static void main(final String[] args) throws IOException {
		ExternalControler simulation = new ExternalControler();
		simulation.externalIteration();
	}
	
	private void externalIteration() throws IOException {
		
		day.put(p1.getOrderId(), p1);
		day.put(p2.getOrderId(), p2);
//		day.put(p3.getOrderId(), p3);
//		day.put(p4.getOrderId(), p4);
//		day.put(p5.getOrderId(), p5);
		
		for (int extIt = 0; extIt <= numberOfExternalIterations ; extIt++){
			log.info("************* EXTERNAL ITERATION "+extIt+" BEGINS *************");
			this.setExtItNr(extIt);
			this.setDirectoryExtIt(outputExternalIterationDirPath +"/extITERS/extIt."+extIt);
			File directory = new File(this.getDirectoryExtIt());
			directory.mkdirs();
			
			this.setMaxNumberOfBuses(day);

			VehicleScheduleWriter transitWriter = new VehicleScheduleWriter(this.day, this.getCapacity(), networkFile, this.getDirectoryExtIt());
			transitWriter.writeTransit();

			InternalControler internalControler = new InternalControler(configFile, this.extItNr, this.getDirectoryExtIt(), lastInternalIteration, populationFile, outputExternalIterationDirPath, this.getMaxNumberOfBuses(), networkFile, fare);
			internalControler.run();

			Operator operator = new Operator(this.getExtItNr(), this.getMaxNumberOfBuses(), this.getCapacity());
			operator.calculateScore(this.getDirectoryExtIt(), lastInternalIteration, networkFile);
//			operator.analyzeScores();
			
			Users users = new Users();
			users.analyzeScores(this.getDirectoryExtIt(), networkFile);
			users.analyzeLegModes(this.getDirectoryExtIt(), lastInternalIteration);

			this.iteration2operatorProfit.put(this.getExtItNr(), operator.getProfit());
			this.iteration2operatorCosts.put(this.getExtItNr(), operator.getCosts());
			this.iteration2operatorEarnings.put(this.getExtItNr(), operator.getEarnings());
			this.iteration2numberOfBuses.put(this.getExtItNr(), (double) this.getMaxNumberOfBuses());
			this.iteration2day.put(this.getExtItNr(), this.day.toString());
			this.iteration2userScoreSum.put(this.getExtItNr(), users.getScoreSum());
			this.iteration2userScore.put(this.getExtItNr(), users.getAvgExecScore());
			this.iteration2totalScore.put(this.getExtItNr(), (users.getScoreSum()+operator.getProfit()));
			this.iteration2numberOfCarLegs.put(this.getExtItNr(), users.getNumberOfCarLegs());
			this.iteration2numberOfPtLegs.put(this.getExtItNr(), users.getNumberOfPtLegs());
			this.iteration2numberOfWalkLegs.put(this.getExtItNr(), users.getNumberOfWalkLegs());
			this.iteration2fare.put(this.getExtItNr(), this.getFare());
			this.iteration2capacity.put(this.getExtItNr(),(double) this.getCapacity());
			
			// settings for next external iteration	
			if (this.getExtItNr() < numberOfExternalIterations){
				
//				this.setDay(increaseNumberOfBusesAllTimePeriods(1));
				
//				this.setDay(increaseBuses("HVZ_1", 1)); // id, number of buses
//				this.setDay(increaseBuses("HVZ_2", 2)); // id, number of buses
//
//				this.setDay(extend("HVZ_1", 30 * 60));
//				this.setDay(extend("HVZ_2", 30 * 60));
				
//				this.setFare(operator.increaseFare(this.getFare(), -0.5)); // absolute value
//				this.setCapacity(operator.increaseCapacity(2)); // absolute value
			}
			
			log.info("************* EXTERNAL ITERATION "+extIt+" ENDS *************");
		}

		TextFileWriter stats = new TextFileWriter();
		stats.writeFile(outputExternalIterationDirPath, this.iteration2numberOfBuses, this.iteration2day, this.iteration2fare, this.iteration2capacity, this.iteration2operatorCosts, this.iteration2operatorEarnings, this.iteration2operatorProfit, this.iteration2userScore, this.iteration2userScoreSum, this.iteration2totalScore, this.iteration2numberOfCarLegs, this.iteration2numberOfPtLegs, this.iteration2numberOfWalkLegs);

		ChartFileWriter chartWriter = new ChartFileWriter();
		
		chartWriter.writeChart_Parameters(outputExternalIterationDirPath, this.iteration2numberOfBuses, "Number of buses per iteration", "NumberOfBuses");
		chartWriter.writeChart_Parameters(outputExternalIterationDirPath, this.iteration2capacity, "Vehicle capacity per iteration", "Capacity");
		chartWriter.writeChart_Parameters(outputExternalIterationDirPath, this.iteration2fare, "Bus fare per iteration", "Fare");

		chartWriter.writeChart_LegModes(outputExternalIterationDirPath, this.iteration2numberOfCarLegs, this.iteration2numberOfPtLegs);
		chartWriter.writeChart_UserScores(outputExternalIterationDirPath, this.iteration2userScore);
		chartWriter.writeChart_UserScoresSum(outputExternalIterationDirPath, this.iteration2userScoreSum);
		chartWriter.writeChart_TotalScore(outputExternalIterationDirPath, this.iteration2totalScore);
		chartWriter.writeChart_OperatorScores(outputExternalIterationDirPath, this.iteration2operatorProfit, this.iteration2operatorCosts, this.iteration2operatorEarnings);
	}

	/**
	 * @param i
	 * @param j
	 * @return
	 */
	private Map<Integer, TimePeriod> increaseBuses(String periodId, int increase) {
		Map<Integer, TimePeriod> dayMod = this.getDay();	
		int period = 0;
		for (TimePeriod tt : dayMod.values()){
			if (tt.getId().equals(periodId)){
				period = tt.getOrderId();
			}
		}
		if (dayMod.containsKey(period)){
			dayMod.get(period).increaseNumberOfBuses(increase);
		}
		return dayMod;
	}

	/**
	 * @param i
	 * @param j
	 * @return
	 */
	private Map<Integer, TimePeriod> extend(String periodId, double time) {
		Map<Integer, TimePeriod> dayNextExtIt = this.getDay();	
		int period = 0;
		for (TimePeriod timePeriod : dayNextExtIt.values()){
			if (timePeriod.getId().equals(periodId)){
				period = timePeriod.getOrderId();
			}
		}
		
		if (dayNextExtIt.containsKey(period)){
			
			dayNextExtIt.get(period).changeFromTime(-time/2);
			dayNextExtIt.get(period).changeToTime(time/2);
			if (dayNextExtIt.containsKey(period-1)){
				dayNextExtIt.get(period-1).changeToTime(-time/2);
			}
			if (dayNextExtIt.containsKey(period+1)){
				dayNextExtIt.get(period+1).changeFromTime(time/2);
			}
			
			int removeNr = 0;
			if (dayNextExtIt.containsKey(period-1)){
				System.out.println("A");
				if(dayNextExtIt.get(period-1).getFromTime() >= dayNextExtIt.get(period-1).getToTime()){
					removeNr = period-1;
				}	
			}
			
			if (removeNr > 0){
				dayNextExtIt.remove(removeNr);
			}
			
			removeNr = 0;
			if (dayNextExtIt.containsKey(period+1)){
				System.out.println("B");
				if(dayNextExtIt.get(period+1).getFromTime() >= dayNextExtIt.get(period+1).getToTime()){
					removeNr = period+1;
				}	
			}
			
			if (removeNr > 0){
				dayNextExtIt.remove(removeNr);
			}
			
			removeNr = 0;
			if(dayNextExtIt.get(period).getFromTime() >= dayNextExtIt.get(period).getToTime()){
				removeNr = period;
			}
			
			if (removeNr > 0){
				dayNextExtIt.remove(removeNr);
			}
		}
		
		int nr = 1;
		Map<Integer, TimePeriod> dayNextExtItOutput = new HashMap<Integer, TimePeriod>();	
		for (TimePeriod t : dayNextExtIt.values()){
			t.setOrderId(nr);
			dayNextExtItOutput.put(nr, t);
			nr++;
		}
		
		
		log.info("Time periods for next external Iteration: "+dayNextExtItOutput.toString());
		return dayNextExtItOutput;
	}

	/**
	 * @param i
	 * @return
	 */
	private Map<Integer, TimePeriod> increaseNumberOfBusesAllTimePeriods(int i) {
		Map<Integer, TimePeriod> dayNextExtIt = new HashMap<Integer, TimePeriod>();
		for (TimePeriod t : this.getDay().values()){
			TimePeriod t2 = t;
			t2.increaseNumberOfBuses(1);
			dayNextExtIt.put(t.getOrderId(), t2);
		}
		return dayNextExtIt;
	}

	/**
	 * @return the numberOfBuses
	 */
	public int getMaxNumberOfBuses() {
		return maxNumberOfBuses;
	}

	/**
	 * @param numberOfBuses the numberOfBuses to set
	 */
	public void setMaxNumberOfBuses(Map<Integer, TimePeriod> day) {
		int maxBusNumber = 0;
		for (TimePeriod t : day.values()){
			if (t.getNumberOfBuses() > maxBusNumber){
				maxBusNumber = t.getNumberOfBuses();
			}
		}
		log.info("Total number of Vehicles: "+maxBusNumber);
		this.maxNumberOfBuses = maxBusNumber;
	}

	/**
	 * @return the extItNr
	 */
	public int getExtItNr() {
		return extItNr;
	}

	/**
	 * @param extItNr the extItNr to set
	 */
	public void setExtItNr(int extItNr) {
		this.extItNr = extItNr;
	}

	/**
	 * @return the directoryExtIt
	 */
	public String getDirectoryExtIt() {
		return directoryExtIt;
	}

	/**
	 * @param directoryExtIt the directoryExtIt to set
	 */
	public void setDirectoryExtIt(String directoryExtIt) {
		this.directoryExtIt = directoryExtIt;
	}

	/**
	 * @return the fare
	 */
	public double getFare() {
		return fare;
	}

	/**
	 * @param fare the fare to set
	 */
	public void setFare(double fare) {
		this.fare = fare;
	}

	/**
	 * @return the capacity
	 */
	public int getCapacity() {
		return capacity;
	}

	/**
	 * @param capacity the capacity to set
	 */
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	/**
	 * @return the day
	 */
	public Map<Integer, TimePeriod> getDay() {
		return day;
	}

	/**
	 * @param day the day to set
	 */
	public void setDay(Map<Integer, TimePeriod> day) {
		this.day = day;
	}
	
	
	
}
