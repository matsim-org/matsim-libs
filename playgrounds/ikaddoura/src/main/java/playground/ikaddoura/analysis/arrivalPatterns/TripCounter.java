/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.ikaddoura.analysis.arrivalPatterns;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;

/**
 * @author lkroeger, ikaddoura
 *
 */
public class TripCounter implements AgentWaitingForPtEventHandler, PersonEntersVehicleEventHandler, TransitDriverStartsEventHandler {
	
	private Map<Id, Double> personId2startWaitingForPt = new HashMap<Id, Double>();
	private Map<Double, Integer> waitingTimeAllocation = new TreeMap<Double, Integer>();
	private List<Double> differenceList = new ArrayList<Double>();
	private Map<Id, Double> personId2WaitingTime = new HashMap<Id, Double>();

	private List<Id> worker = new ArrayList<Id>();
	private int counterTotal = 0;
	private double waitingTimeTotalSum = 0.0;
	private double waitingTimeTotalAvg = 0.0;
	private double waitingTimeTmp = 0.0;
		
	double median = 0.0;
	double lowerQuartile = 0.0;
	double upperQuartile = 0.0;
	double maxValue = 0.0;
	double minValue = 0.0;
	private List<Id> ptDriver = new ArrayList<Id>();
	
	double[] waitingTimePerHour = new double[20];
	int[] waitingCounterPerHour = new int[20];
	double[] waitingTimePerHourAvg = new double[20];
	
	private Map<Id, Double> waitingTimesPerStopSum = new TreeMap<Id, Double>();
	private Map<Id, Id> waitingAgentsPerStop = new TreeMap<Id, Id>();
	private Map<Id, Integer> waitingCounterPerStop = new TreeMap<Id, Integer>();
	private Map<Id, Double> waitingTimesPerStopAvg = new TreeMap<Id, Double>();

	double waitingTimetAvgWork = 0.0;
	double waitingTimeAvgOther = 0.0;

	double waitingTimeWork = 0.0;
	double waitingTimeOther = 0.0;
	
	int waitingCounterWork = 0;
	int waitingCounterOther = 0;

	@Override
	public void reset(int iteration) {	
	}
	
	@Override
	public void handleEvent(AgentWaitingForPtEvent event) {
		
		this.personId2startWaitingForPt.put(event.getPersonId(), event.getTime());
		counterTotal++;
		
		if (event.getPersonId().toString().contains("Work")){
			worker.add(event.getPersonId());
		
		} else {
			//
		}
		
		if (waitingCounterPerStop.get(event.getWaitingAtStopId()) == null){
			waitingCounterPerStop.put(event.getWaitingAtStopId(), 1);
			
		} else {
			waitingCounterPerStop.put(event.getWaitingAtStopId(), (waitingCounterPerStop.get(event.getWaitingAtStopId())+1));
		}
		
		waitingAgentsPerStop.put(event.getPersonId(), event.getWaitingAtStopId());
		
	}
	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		
		ptDriver.add(event.getDriverId());
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		
		if (personId2startWaitingForPt.containsKey(event.getPersonId())){
			waitingTimeTmp = event.getTime() - personId2startWaitingForPt.get(event.getPersonId());
			waitingTimeTotalSum = waitingTimeTotalSum + waitingTimeTmp;
			differenceList.add(waitingTimeTmp);
			personId2WaitingTime.put(event.getPersonId(), waitingTimeTmp);
			
			java.util.Collections.sort(differenceList);
			
			if (waitingTimeAllocation.get(waitingTimeTmp) == null) {
				waitingTimeAllocation.put(waitingTimeTmp, 1);
			
			} else {
				int wert = waitingTimeAllocation.get(waitingTimeTmp);
				waitingTimeAllocation.remove(waitingTimeTmp);
				waitingTimeAllocation.put(waitingTimeTmp, wert + 1);
			}
			
			//average waiting times per stop:
			if(waitingTimesPerStopSum.get(waitingAgentsPerStop.get(event.getPersonId())) == null){
				waitingTimesPerStopSum.put(waitingAgentsPerStop.get(event.getPersonId()), waitingTimeTmp);
			
			} else{
				waitingTimesPerStopSum.put(waitingAgentsPerStop.get(event.getPersonId()), (waitingTimesPerStopSum.get(waitingAgentsPerStop.get(event.getPersonId()))+waitingTimeTmp));
			}
			
			if(waitingTimesPerStopAvg.get(waitingAgentsPerStop.get(event.getPersonId())) == null){
				waitingTimesPerStopAvg.put(waitingAgentsPerStop.get(event.getPersonId()), waitingTimeTmp);
			
			}else{
				waitingTimesPerStopAvg.put(waitingAgentsPerStop.get(event.getPersonId()), waitingTimesPerStopSum.get(waitingAgentsPerStop.get(event.getPersonId()))/waitingCounterPerStop.get(waitingAgentsPerStop.get(event.getPersonId())));
			}
			
			waitingAgentsPerStop.remove(event.getPersonId());	
				
			//analysis Work vs. Other:
			//at first draw up list worker (important for back rides), using Events ActStart
			//then computation
			if (worker.contains(event.getPersonId())) {
				waitingTimeWork = waitingTimeWork + waitingTimeTmp;
				waitingCounterWork++;
			
			} else {
				waitingTimeOther = waitingTimeOther + waitingTimeTmp;
				waitingCounterOther++;
			}
			
			//analysis for separate hours:
			for(int i = 0 ; i<20 ; i++) {
				if(event.getTime() > (14400+3600*i) && event.getTime() <= (14400+3600*(i+1))){
					waitingTimePerHour[i] = waitingTimePerHour[i] + waitingTimeTmp;
					waitingCounterPerHour[i]++;
				}
			}
		}
	}

	public void printCounts(String outputFolder) {
		
		File folder = new File(outputFolder);
		folder.mkdirs();
		
//		try {//raw data difference List
//			BufferedWriter br  = new BufferedWriter(new FileWriter(new File(outputFolder+"rawDataDifferenceList.csv")));
//			
//			br.write("waiting time");
//			
//			for(int x = 0; x < differenceList.size(); x++){
//			br.write("\n"+ differenceList.get(x));
//			}
//			
//			br.close();
//			
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		try {
			BufferedWriter br  = new BufferedWriter(new FileWriter(new File(outputFolder+"personId2startWaitingForPt.csv")));
			
			br.write("person Id;start waiting time");
			br.newLine();
			for (Id id : this.personId2startWaitingForPt.keySet()){
				br.write(id+";"+this.personId2startWaitingForPt.get(id));
				br.newLine();
			}
			
			br.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			BufferedWriter br  = new BufferedWriter(new FileWriter(new File(outputFolder+"personId2waitingTime.csv")));
			
			br.write("person Id;start waiting time");
			br.newLine();
			for (Id id : this.personId2WaitingTime.keySet()){
				br.write(id + ";" + this.personId2WaitingTime.get(id));
				br.newLine();
			}
			
			br.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		try {
//			BufferedWriter br  = new BufferedWriter(new FileWriter(new File(outputFolder+"waitingTime_Frequency.csv")));
//			
//			br.write("Waiting time; frequency");
//			
//			for(double key : waitingTimeAllocation.keySet()){
//			br.write("\n"+key+"; "+ waitingTimeAllocation.get(key));
//			}
//			
//			br.close();
//			
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//		System.out.println();
//		System.out.println();
//		System.out.println("### Beginn Auswertung ###");
//		System.out.println();
//		
//		waitingTimeTotalAvg = waitingTimeTotalSum/counterTotal;
//		
//		minValue = differenceList.get(0);
//		System.out.println("Der minimale Wert der Wartezeit beträgt: "+minValue+" Sekunden.");
//		maxValue = differenceList.get(differenceList.size()-1);
//		System.out.println("Der maximale Wert der Wartezeit beträgt: "+maxValue+" Sekunden.");
//		
//		median = differenceList.get((differenceList.size()-1)/2);
//		System.out.println("Der Median der Wartezeit in Sekunden: " + median);
//		lowerQuartile = differenceList.get((differenceList.size()-1)*1/4);
//		System.out.println("Das untere Quartil der Wartezeit in Sekunden: " + lowerQuartile);
//		upperQuartile = differenceList.get((differenceList.size()-1)*3/4);
//		System.out.println("Das obere Quartil der Wartezeit in Sekunden: " + upperQuartile);
//		
//		System.out.println("Durchschnittliche Wartezeit in Sekunden: " + waitingTimeTotalAvg);
//		
//		System.out.println();
//		
//		for(int i = 0 ; i<20 ; i++){
//			waitingTimePerHourAvg[i] = waitingTimePerHour[i]/waitingCounterPerHour[i];
//		}
//		
//		waitingTimetAvgWork = waitingTimeWork/waitingCounterWork;
//		waitingTimeAvgOther = waitingTimeOther/waitingCounterOther;
//		
//		for(int i = 0 ; i<20 ; i++){
//			System.out.println("Durchschnittliche Wartezeit zwischen "+(i+4)+" und "+(i+5)+" Uhr: " + waitingTimePerHourAvg[i]+", Anzahl Fahrten: "+ waitingCounterPerHour[i]);
//		}
//
//		System.out.println("Durchschnittliche Wartezeit 'Work': " + waitingTimetAvgWork);
//		System.out.println("Durchschnittliche Wartezeit 'Other': " + waitingTimeAvgOther);
//		System.out.println();
//		
//		int numberColumns;
//
//		numberColumns = 10;
//
////		int widthColumns = 30;
////		numberColumns = (int) maxValue/widthColumns;
//				
//		Map<String, Integer> frequencyColumns = new HashMap<String, Integer>();
//		int tmpNumberPerColumn = 0;
//		
//		//Spaltenweise darstellen:Balkendarstellung
//		for(int d=0;d<numberColumns;d++){
//			for(int p=0; p<differenceList.size(); p++ ) {
//				if ((differenceList.get(p)>=(d*((maxValue-minValue+1)/numberColumns))) && (differenceList.get(p)<((d+1)*((maxValue-minValue+1)/numberColumns)))){
//					if(frequencyColumns.get((int) (d*((maxValue-minValue+1)/numberColumns)+0.5001)+" bis "+(((int) ((d+1)*((maxValue-minValue+1)/numberColumns)-0.0001)))) == null){
//					frequencyColumns.put((int) (d* ((maxValue-minValue+1)/numberColumns)+0.5001)+" bis "+(((int) ((d+1)*((maxValue-minValue+1)/numberColumns)-0.0001))), 1);
//					} else {
//						tmpNumberPerColumn = frequencyColumns.get((int) (d*((maxValue-minValue+1)/numberColumns)+0.5001)+" bis "+(((int) ((d+1)*((maxValue-minValue+1)/numberColumns)-0.0001))));
//						frequencyColumns.put((int) (d*((maxValue-minValue+1)/numberColumns)+0.5001)+" bis "+(((int) ((d+1)*((maxValue-minValue+1)/numberColumns)-0.0001))), tmpNumberPerColumn + 1);
//					}
//				}
//		
//			}
//		}
//		
//		try { //Column Diagram
//			BufferedWriter br  = new BufferedWriter(new FileWriter(new File(outputFolder+"columns.csv")));
//			
//			br.write("Waiting time; frequency");
//			
//			for(String key : frequencyColumns.keySet()){
//			br.write("\n"+key+"; "+ frequencyColumns.get(key));
//			}
//			
//			br.close();
//			
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		
	}

}
	
	
	
	
	



