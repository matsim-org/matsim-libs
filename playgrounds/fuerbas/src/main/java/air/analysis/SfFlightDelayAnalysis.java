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
	 */
	
	private Map<String, Double> actualArrival;
	private Map<String, Double> scheduledArrival;
	private Map<Integer, Integer> delay;	
	
	private static String actualTimes = "Z:\\WinHome\\shared-svn\\studies\\countries\\world\\flight\\sf_oag_flight_model\\output\\ITERS\\it.0\\0.statistic.csv";
	private static String scheduledTimes= "Z:\\WinHome\\shared-svn\\studies\\countries\\world\\flight\\sf_oag_flight_model\\oag_flights.txt";
	private static String delayOutput = "Z:\\WinHome\\shared-svn\\studies\\countries\\world\\flight\\sf_oag_flight_model\\output\\delay.csv";
	
	public SfFlightDelayAnalysis() {
		this.actualArrival = new HashMap<String, Double>(); 
		this.scheduledArrival = new HashMap<String, Double>();
		this.delay = new TreeMap<Integer, Integer>();
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
		BufferedWriter bwDelaySingleFlights = new BufferedWriter(new FileWriter(new File("Z:\\WinHome\\shared-svn\\studies\\countries\\world\\flight\\sf_oag_flight_model\\delayByFlight.csv")));
		
		this.delay.put(0, 0);
		this.delay.put(1, 0);
		this.delay.put(2, 0);
		this.delay.put(3, 0);
		this.delay.put(4, 0);
		this.delay.put(5, 0);
		this.delay.put(10, 0);
		this.delay.put(15, 0);
		this.delay.put(20, 0);
		this.delay.put(25, 0);
		this.delay.put(30, 0);
		this.delay.put(31, 0);
		
		this.delay.put(-1, 0);
		this.delay.put(-2, 0);
		this.delay.put(-3, 0);
		this.delay.put(-4, 0);
		this.delay.put(-5, 0);
		this.delay.put(-10, 0);
		this.delay.put(-15, 0);
		this.delay.put(-20, 0);
		this.delay.put(-25, 0);
		this.delay.put(-30, 0);
		this.delay.put(-31, 0);
		
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
			Double arrival = Double.parseDouble(entries[3])+Double.parseDouble(entries[4]);
			this.scheduledArrival.put(flightNumber, arrival/60);
			Double flightDelay = this.actualArrival.get(flightNumber)-this.scheduledArrival.get(flightNumber);
			if (flightDelay >=60.) System.out.println(flightNumber+"..."+flightDelay+"STA"+this.scheduledArrival.get(flightNumber)+"ATA"+this.actualArrival.get(flightNumber));
			bwDelaySingleFlights.write(this.actualArrival.get(flightNumber)+"\t"+flightDelay);
			bwDelaySingleFlights.newLine();
			
				if (flightDelay == 0.) {
					Integer soFar = this.delay.get(0);
					soFar++;
					this.delay.put(0, soFar);
				}
				
				if (flightDelay > 0. && flightDelay < 1.) {
					Integer soFar = this.delay.get(1);
					soFar++;
					this.delay.put(1, soFar);
				}
				
				if (flightDelay >=1. && flightDelay < 2.) {
					Integer soFar = this.delay.get(2);
					soFar++;
					this.delay.put(2, soFar);
				}
				
				if (flightDelay >=2. && flightDelay < 3.) {
					Integer soFar = this.delay.get(3);
					soFar++;
					this.delay.put(3, soFar);
				}
				
				if (flightDelay >=3. && flightDelay < 4.) {
					Integer soFar = this.delay.get(4);
					soFar++;
					this.delay.put(4, soFar);
				}
				
				if (flightDelay >=4. && flightDelay < 5.) {
					Integer soFar = this.delay.get(5);
					soFar++;
					this.delay.put(5, soFar);
				}
				
				if (flightDelay >= 5. && flightDelay < 10.) {
					Integer soFar = this.delay.get(10);
					soFar++;
					this.delay.put(10, soFar);
				}
				
				if (flightDelay >= 10. && flightDelay < 15.) {
					Integer soFar = this.delay.get(15);
					soFar++;
					this.delay.put(15, soFar);
				}
				
				if (flightDelay >= 15. && flightDelay < 20.) {
					Integer soFar = this.delay.get(20);
					soFar++;
					this.delay.put(20, soFar);
				}
				
				if (flightDelay >= 20. && flightDelay < 25.) {
					Integer soFar = this.delay.get(25);
					soFar++;
					this.delay.put(25, soFar);
				}
				
				if (flightDelay >= 25. && flightDelay < 30.) {
					Integer soFar = this.delay.get(30);
					soFar++;
					this.delay.put(30, soFar);
				}
				
				if (flightDelay >= 30.) {
					Integer soFar = this.delay.get(31);
					soFar++;
					this.delay.put(31, soFar);
				}
				
				if (flightDelay < 0. && flightDelay >-1.) {
					Integer soFar = this.delay.get(-1);
					soFar++;
					this.delay.put(-1, soFar);
				}
				
				if (flightDelay <=-1. && flightDelay >-2.) {
					Integer soFar = this.delay.get(-2);
					soFar++;
					this.delay.put(-2, soFar);
				}
				
				if (flightDelay <=-2. && flightDelay >-3.) {
					Integer soFar = this.delay.get(-3);
					soFar++;
					this.delay.put(-3, soFar);
				}
				
				if (flightDelay <= -3. && flightDelay >-4.) {
					Integer soFar = this.delay.get(-4);
					soFar++;
					this.delay.put(-4, soFar);
				}
				
				if (flightDelay <= -4. && flightDelay >-5.) {
					Integer soFar = this.delay.get(-5);
					soFar++;
					this.delay.put(-5, soFar);
				}
				
				if (flightDelay <= -5. && flightDelay >-10.) {
					Integer soFar = this.delay.get(-10);
					soFar++;
					this.delay.put(-10, soFar);
				}
				
				if (flightDelay <= -10. && flightDelay >-15.) {
					Integer soFar = this.delay.get(-15);
					soFar++;
					this.delay.put(-15, soFar);
				}
				
				if (flightDelay <= -15. && flightDelay >-20.) {
					Integer soFar = this.delay.get(-20);
					soFar++;
					this.delay.put(-20, soFar);
				}
				
				if (flightDelay <= -20. && flightDelay >-25.) {
					Integer soFar = this.delay.get(-25);
					soFar++;
					this.delay.put(-25, soFar);
				}
				
				if (flightDelay <= -25. && flightDelay >-30.) {
					Integer soFar = this.delay.get(-30);
					soFar++;
					this.delay.put(-30, soFar);
				}
				
				if (flightDelay <= -30.) {
					Integer soFar = this.delay.get(-31);
					soFar++;
					this.delay.put(-31, soFar);
				}				
				
		}
		
		brScheduled.close();
		bwDelaySingleFlights.close();
		
		bwDelay.write("Delay in minutes \t Number of Delays");
		bwDelay.newLine();
		
		Iterator it = this.delay.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        bwDelay.write(pairs.getKey().toString()+"\t"+pairs.getValue());
	        bwDelay.newLine();
	    }
	    
	    bwDelay.flush();
	    bwDelay.close();
		
	}

}
