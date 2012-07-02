/* *********************************************************************** *
 * project: org.matsim.*
 * SfAirScheduleBuilder
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

package air.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class SfFlightDelayAnalysis {

	/**
	 * @author fuerbas
	 * When using SfAirController flight delays can be analyzed using this class. Range of delays may be determined by
	 * MIN_MAX_DELAY, any delays larger (or smaller for negative values) will be accumulated. DELAY_OUTPUT_INTERVAL
	 * can be used to define an interval within which delay will be accumulated.
	 */
	
	private Map<String, Double> actualArrival;
	private Map<String, Double> scheduledArrival;
	private Map<Integer, Integer> delay;
	private Map<Integer, Integer> delayAcc;
	
	private static final int MIN_MAX_DELAY = 60;	//set desired min./max. delay
	private static final int DELAY_OUTPUT_INTERVAL = 5;		//set interval for delay accumulation
	
	private static String actualTimes = "Z:\\WinHome\\munich_output\\ITERS\\it.0\\0.statistic.csv";
	private static String scheduledTimes= "Z:\\WinHome\\shared-svn\\studies\\countries\\de\\flight\\sf_oag_flight_model\\munich\\flight_model_muc_all_flights\\oag_flights.txt";
	private static String delayOutput = "Z:\\WinHome\\munich_output\\delay.csv";
	private static String delayOutputAcc = "Z:\\WinHome\\munich_output\\delay_acc.csv";
	
	public SfFlightDelayAnalysis() {
		this.actualArrival = new HashMap<String, Double>(); 
		this.scheduledArrival = new HashMap<String, Double>();
		this.delay = new TreeMap<Integer, Integer>();
		this.delayAcc = new TreeMap<Integer, Integer>();
	}
	
	public static void main(String[] args) {

		SfFlightDelayAnalysis ana = new SfFlightDelayAnalysis();	
		try {
			ana.analyzeDelays();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void analyzeDelays() throws Exception {
		
		BufferedReader brActual = new BufferedReader(new FileReader(new File(actualTimes)));
		BufferedReader brScheduled = new BufferedReader(new FileReader(new File(scheduledTimes)));
		BufferedWriter bwDelay = new BufferedWriter(new FileWriter(new File(delayOutput)));
		BufferedWriter bwDelayAcc = new BufferedWriter(new FileWriter(new File(delayOutputAcc)));
		BufferedWriter bwDelaySingleFlights = new BufferedWriter(new FileWriter(new File("Z:\\WinHome\\munich_output\\delayByFlight.csv")));
		
		for (int kk=0; kk<=(MIN_MAX_DELAY+1); kk++) {
			this.delay.put(kk, 0);
			this.delay.put(-kk, 0);
		}
		
		for (int kk=0; kk<=(MIN_MAX_DELAY+1); kk++) {
			if (kk%DELAY_OUTPUT_INTERVAL == 0) {
				this.delayAcc.put(kk, 0);
				this.delayAcc.put(-kk, 0);			
			}
			else if (kk == (MIN_MAX_DELAY+1)) {
				this.delayAcc.put(kk, 0);
				this.delayAcc.put(-kk, 0);
			}
		}
		
		int lines = 0;
			
		while (brActual.ready()) {
			
				String line = brActual.readLine();
				String[] entries = line.split("\t");
				String flightNumber = entries[0];
				if (lines>0) {
				Double arrival = Double.parseDouble(entries[1])/60;
				this.actualArrival.put(flightNumber, arrival);
			}
			lines++;
		}
		
		brActual.close();
			
		while (brScheduled.ready()) {
			String line = brScheduled.readLine();
			String[] entries = line.split("\t");
			String flightNumber = entries[2];
			if (this.actualArrival.containsKey(flightNumber)) {
			Double arrival = Double.parseDouble(entries[3])+Double.parseDouble(entries[4]);
			this.scheduledArrival.put(flightNumber, arrival/60);
			long flightDelay = Math.round(this.actualArrival.get(flightNumber)-this.scheduledArrival.get(flightNumber));
			
			bwDelaySingleFlights.write(flightNumber+"\t"+this.actualArrival.get(flightNumber)+"\t"+flightDelay);
			bwDelaySingleFlights.newLine();
			
				if (flightDelay==0) {
					int soFar = this.delay.get(0);
					soFar++;
					this.delay.put(0, soFar);
					this.delayAcc.put(0, soFar);
				}
			
				for (int ii=1; ii<=MIN_MAX_DELAY; ii++) {
					if (flightDelay == ii) {
						int soFar = this.delay.get(ii);
						soFar++;
						this.delay.put(ii, soFar);
						int delay;
						if (ii%DELAY_OUTPUT_INTERVAL == 0) delay = ii;
						else delay = ii - (ii%DELAY_OUTPUT_INTERVAL) + DELAY_OUTPUT_INTERVAL;
						int soFarAcc = this.delayAcc.get(delay);
						soFarAcc++;
						this.delayAcc.put(delay, soFarAcc);
					}
					else if (flightDelay>MIN_MAX_DELAY) {
						int soFar = this.delay.get(MIN_MAX_DELAY+1);
						soFar++;
						this.delay.put(MIN_MAX_DELAY+1, soFar);
						this.delayAcc.put(MIN_MAX_DELAY+1, soFar);
						break;
					}
				}
				
				for (int jj=1; jj<=MIN_MAX_DELAY; jj++) {
					if (flightDelay == (-jj)) {
						int soFar = this.delay.get(-jj);
						soFar++;
						this.delay.put(-jj, soFar);
						int delay;
						if (jj%DELAY_OUTPUT_INTERVAL == 0) delay = -jj;
						else delay = -(jj - (jj%DELAY_OUTPUT_INTERVAL) + DELAY_OUTPUT_INTERVAL);
						int soFarAcc = this.delayAcc.get(delay);
						soFarAcc++;
						this.delayAcc.put(delay, soFarAcc);
					}
					else if (flightDelay<-MIN_MAX_DELAY) {
						int soFar = this.delay.get(-(MIN_MAX_DELAY+1));
						soFar++;
						this.delay.put(-(MIN_MAX_DELAY+1), soFar);
						this.delayAcc.put(-(MIN_MAX_DELAY+1), soFar);
						break;
					}
				}
			}
		}
		brScheduled.close();
		bwDelaySingleFlights.close();
		bwDelay.write("Delay in minutes \t Number of Delays");
		bwDelay.newLine();
		bwDelayAcc.write("Delay in minutes \t Number of Delays");
		bwDelayAcc.newLine();
		
		Iterator it = this.delay.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        bwDelay.write(pairs.getKey().toString()+"\t"+pairs.getValue());
	        bwDelay.newLine();
	    }
	    
		Iterator it2 = this.delayAcc.entrySet().iterator();
	    while (it2.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it2.next();
	        bwDelayAcc.write(pairs.getKey().toString()+"\t"+pairs.getValue());
	        bwDelayAcc.newLine();
	    }
	    bwDelay.flush();
	    bwDelay.close();
	    bwDelayAcc.flush();
	    bwDelayAcc.close();
	}

}
