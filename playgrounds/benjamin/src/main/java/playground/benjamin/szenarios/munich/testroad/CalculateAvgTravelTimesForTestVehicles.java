/* *********************************************************************** *
 * project: org.matsim.*
 * CalculateAvgTravelTimesForTestVehicles.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.szenarios.munich.testroad;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.benjamin.dataprepare.CheckingTabularFileHandler;

/**
 * @author benjamin
 *
 */
public class CalculateAvgTravelTimesForTestVehicles {

	static String testVehicleDataPath = "../../detailedEval/teststrecke/testVehicle/";
	static String fileName = "_travelTimes.csv";

	private static SortedMap<Integer, SortedMap<Integer, Integer>> data = new TreeMap<Integer, SortedMap<Integer, Integer>>();

	static Integer [] days = {
		// dont use this since they changed counts logfile format during the day! 20060125,
		20060127,
		20060131,
		20090317,
		20090318,
		20090319,
		20090707,
		20090708,
		20090709,
		20091201,
		20091202,
		20091203
	};


	public static void main(String[] args) {

		calculateAvgTravelTimesFromData(testVehicleDataPath, fileName, days, data);
		calculateAvgTravelTimesFromSim();
	}

	private static void calculateAvgTravelTimesFromData(String testVehicleDataPath2, String fileName, Integer[] days,	SortedMap<Integer, SortedMap<Integer, Integer>> data) {

		for(int day : days){
			SortedMap<Integer, Integer> inflowTimes2TravelTimes = getInflowTimes2TravelTimes(testVehicleDataPath + day + fileName);
			data.put(day, inflowTimes2TravelTimes);
		}

		SortedMap<Integer, Double> hours2AvgTravelTimes = calculateAvgTravelTimesPerHour(data);
		writeAvgTravelTimesPerHour(hours2AvgTravelTimes);
	}

	private static void calculateAvgTravelTimesFromSim() {
		// TODO Auto-generated method stub
		
	}

	private static SortedMap<Integer, Integer> getInflowTimes2TravelTimes(String inputFile) {
		final SortedMap<Integer, Integer> inflowTimes2TravelTimes = new TreeMap<Integer, Integer>();

		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(inputFile);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});

		try {
			new TabularFileParser().parse(tabFileParserConfig, new CheckingTabularFileHandler() {

				private static final int INFLOWTIME = 0;
				private static final int TRAVELTIME = 1;

				public void startRow(String[] row) {
					first = false;
					numColumns = row.length;
					check(row);
					addDepartureTime(row);
				}

				private void addDepartureTime(String[] row) {
					Integer inflowTime = new Integer(row[INFLOWTIME]);
					Integer travelTime = new Integer(row[TRAVELTIME]);

					inflowTimes2TravelTimes.put(inflowTime, travelTime);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		System.out.println(inflowTimes2TravelTimes);
		System.out.println("=====");
		return inflowTimes2TravelTimes;

	}

	private static SortedMap<Integer, Double> calculateAvgTravelTimesPerHour(SortedMap<Integer, SortedMap<Integer, Integer>> data) {
		SortedMap<Integer, Double> hours2TravelTimes = new TreeMap<Integer, Double>();

		for(int i = 6; i < 20; i++){
			List<Integer> travelTimes = new ArrayList<Integer>();
			Integer lowerBound = i * 3600;
			Integer upperBound = (i+1) * 3600;

			for(Entry <Integer, SortedMap<Integer, Integer>> entry : data.entrySet()){
				SortedMap<Integer, Integer> inflowTimes2TravelTimes = entry.getValue();

				for(Entry <Integer, Integer> entroy: inflowTimes2TravelTimes.entrySet()){
					Integer inflowTime = entroy.getKey();
					Integer travelTime = entroy.getValue();

					if(inflowTime > lowerBound && inflowTime <= upperBound){
						travelTimes.add(travelTime);
					}
					else{
						// do nothing
					}
				}
			}
			System.out.println(travelTimes);
			Integer sum = 0;
					
			for(int iterator : travelTimes){
				sum = sum + iterator;
			}
			
			Integer mediumHour = (lowerBound + upperBound) / 2;
			Double avgTravelTime = (double) sum / (double) travelTimes.size();

			hours2TravelTimes.put(mediumHour, avgTravelTime);
		}
		return hours2TravelTimes;
	}

	private static void writeAvgTravelTimesPerHour(SortedMap<Integer, Double> hours2AvgTravelTimes) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(testVehicleDataPath + "averageTravelTimes.txt")));
			//header
			bw.write("hour" + "\t" + "avgTravelTime");
			bw.newLine();

			//fill with values
			for(Entry<Integer, Double> entry: hours2AvgTravelTimes.entrySet()){
				Integer hour = entry.getKey();
				Double avgTravelTime = entry.getValue();

				bw.write(hour.toString());
				bw.write("\t");
				bw.write(avgTravelTime.toString());
				bw.newLine();
			}
			bw.close();
			System.out.println("Wrote average travel times to " + testVehicleDataPath);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
