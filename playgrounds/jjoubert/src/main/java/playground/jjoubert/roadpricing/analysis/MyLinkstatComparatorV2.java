/* *********************************************************************** *
 * project: org.matsim.*
 * RunMultipleComparators.java
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

package playground.jjoubert.roadpricing.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import nl.knaw.dans.common.dbflib.CorruptedTableException;
import nl.knaw.dans.common.dbflib.DbfLibException;
import nl.knaw.dans.common.dbflib.Field;
import nl.knaw.dans.common.dbflib.IfNonExistent;
import nl.knaw.dans.common.dbflib.InvalidFieldLengthException;
import nl.knaw.dans.common.dbflib.InvalidFieldTypeException;
import nl.knaw.dans.common.dbflib.NumberValue;
import nl.knaw.dans.common.dbflib.Record;
import nl.knaw.dans.common.dbflib.Table;
import nl.knaw.dans.common.dbflib.Type;
import nl.knaw.dans.common.dbflib.Value;
import nl.knaw.dans.common.dbflib.Version;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class MyLinkstatComparatorV2 {
	private final static Logger log = Logger.getLogger(MyLinkstatComparatorV2.class);
	public String baselineFilename;
	public String comparisonFilename;
	public Double scaleFactor;
	public Integer statistic;
	private List<String> fieldList;
	private Map<String, Integer> fieldMap;
	private Map<Id, Map<String, Double>> linkMap;
	private Network network;
	private double minimumDifference;
	private double maximumDifference;
	private int linkscompared;
	private int valuesAdded;

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String baselineFilename = null;
		String comparisonFilename = null;
		Double scaleFactor = null;
		Integer statistic = null;
		Double threshold = null;
		String networkFile = null;
		Integer minimumLanes = null;
		String outputFile = null;
		
		if(args.length != 8){
			throw new IllegalArgumentException("Wrong number of arguments");
		} else{
			baselineFilename = args[0];
			comparisonFilename = args[1];
			scaleFactor = Double.parseDouble(args[2]);
			statistic = Integer.parseInt(args[3]);
			threshold = Double.parseDouble(args[4]);
			networkFile = args[5];
			minimumLanes = Integer.parseInt(args[6]);
			outputFile = args[7];
		}
		
		/* Find the column index for each provided field. */
		MyLinkstatComparatorV2 mlc = new MyLinkstatComparatorV2(baselineFilename, comparisonFilename, scaleFactor, statistic);
		mlc.getFields();
		
		/* Check if outputfile exist, and delete if it does. */
		File output = new File(outputFile);
		if(output.exists()){
			log.warn("The output file " + outputFile + " exists and will be deleted!");
			output.delete();
		}
				
		// Read the network file if the number of lanes are specified.
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(sc);
		nr.parse(networkFile);
		
		mlc.network = sc.getNetwork();
		mlc.compare(minimumLanes, threshold);
		mlc.writeComparison(outputFile);
		
		log.info(String.format("Minimum difference: %5.4f", mlc.minimumDifference));
		log.info(String.format("Maximum difference: %5.4f", mlc.maximumDifference));
		log.info("Of the " + sc.getNetwork().getLinks().size() + " links, " +
				mlc.linkMap.size() + " were considered (had at least " + 
				minimumLanes + " lane(s)) and exceeded the statistic threshold of " + threshold
		);

		
		log.info("-----------------------");
		log.info("       Completed");
		log.info("=======================");
	}
	
	public MyLinkstatComparatorV2(String baselineFilename, String comparisonFilename, double scaleFactor, int statistic) {
		this.baselineFilename = baselineFilename;
		this.comparisonFilename = comparisonFilename;
		this.scaleFactor = scaleFactor;
		this.statistic = statistic;
		fieldList = new ArrayList<String>();
		fieldMap = new TreeMap<String, Integer>();
		linkMap = new TreeMap<Id, Map<String,Double>>();
		
		minimumDifference = Double.POSITIVE_INFINITY;
		maximumDifference = Double.NEGATIVE_INFINITY;
		
		linkscompared = 0;
	}
	
	
	public void compare(Integer minimumLanes, double threshold){
		int counter = 0;
		int multiplier = 1;
		
		try {
			BufferedReader br1 = IOUtils.getBufferedReader(this.baselineFilename);
			BufferedReader br2 = IOUtils.getBufferedReader(this.comparisonFilename);
			try{
				String line1 = br1.readLine();
				String line2 = br2.readLine();
				while((line1 = br1.readLine()) != null && 
						  (line2 = br2.readLine()) != null){
					String[] sa1 = line1.split("\t");
					String[] sa2 = line2.split("\t");
					if(!sa1[0].equalsIgnoreCase(sa2[0])){
						log.error("Two line entries do not have the same link Id.");
					} else{
						if(this.network.getLinks().get(new IdImpl(sa1[0])).getNumberOfLanes() >= minimumLanes){
							linkscompared++;
							
							boolean exceedsThreshold = false;
							
							/* Compare each of the fields. */
							Map<String, Double> entry = new TreeMap<String, Double>();
							for(String field : fieldMap.keySet()){
								
								double difference = getStatistic(sa1, sa2,	field);
								if(Math.abs(difference)
										>= threshold){
									exceedsThreshold = true;
								}
								entry.put(field, difference);
								
								minimumDifference = Math.min(minimumDifference, difference);
								maximumDifference = Math.max(maximumDifference, difference);							
							}
							if(exceedsThreshold){
								linkMap.put(new IdImpl(sa1[0]), entry);	
								valuesAdded++;
							}
						}
					}
					
					/* Report progress. */
					if(++counter == multiplier){
						log.info("   lines compared: " + counter);
						multiplier *= 2;
					}
				}
			} finally{
				br1.close();
				br2.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("   lines compared: " + counter + " (Done)");
		log.info("Values added:      " + valuesAdded);
	}

	
	public void writeComparison(String filename){
		String extention = filename.substring(filename.lastIndexOf(".")+1); 
		if(extention.equalsIgnoreCase("dbf")){
			writeComparisonToDbf(filename);
		} else if(extention.equalsIgnoreCase("txt") || extention.equalsIgnoreCase("csv")){
			writeComparisonToFile(filename);
		}else{
			log.warn("Could not write to filetype " + extention);
		}
	}
	
	private void writeComparisonToFile(String filename){
		log.info("Writing comparison to " + filename);
		
		int counter = 0;
		int multiplier = 1;

		try {
			BufferedWriter bw = IOUtils.getBufferedWriter(filename);
			try{
				/* Write header. */
				bw.write("LinkId,");
				for(String s : fieldList){
					bw.write(s);
					bw.write(",");
				}
				bw.write("LaneKm");
				bw.newLine();

				/* Write entries from linkMap. */
				for(Id linkId : linkMap.keySet()){
					bw.write(linkId.toString());
					bw.write(",");
					for(int i = 0; i < fieldList.size(); i++){ 
						String field = fieldList.get(i);
						Double difference;
						if(!linkMap.get(linkId).containsKey(field)){
							difference = 0.0;
							log.warn("could not find LinkId " + linkId + "; Field " + field);
						} else{
							difference = linkMap.get(linkId).get(field);
						}
						bw.write(String.format("%2.4f", difference));
						bw.write(",");
					}
					double laneKm = this.network.getLinks().get(linkId).getLength() * this.network.getLinks().get(linkId).getNumberOfLanes() / 1000;
					bw.write(String.valueOf(laneKm));
					bw.newLine();
					
										
					/* Report progress. */
					if(++counter == multiplier){
						log.info("   lines written: " + counter);
						multiplier *= 2;
					}
				}
				log.info("   lines written: " + counter + " (Done)");
			} finally{
				bw.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private void writeComparisonToDbf(String filename){
		log.info("Writing comparison to " + filename);
		
		int counter = 0;
		int multiplier = 1;
		
		Field idField = new Field("LinkId", Type.NUMBER, 7);
		List<Field> listOfFields = new ArrayList<Field>();
		listOfFields.add(idField);
		for(String s : fieldList){
			Field f = new Field(s, Type.NUMBER, 10, 4);
			listOfFields.add(f);
		}
		
		/* Add daily MIN and MAX fields. */
		Field min = new Field("dailyMin", Type.NUMBER, 10, 4);
		listOfFields.add(min);
		Field max = new Field("dailyMax", Type.NUMBER, 10, 4);
		listOfFields.add(max);
		
		log.info("   Fields in table:");
		for(Field f : listOfFields){
			log.info("      " + f.getName() + "; Type (" + f.getType() + "); Decimal count (" + f.getDecimalCount() + ")" );
		}

		Map<String, Value> map;
		Table t;
		try {
			t = new Table(new File(filename), Version.DBASE_4, listOfFields);
			t.open(IfNonExistent.CREATE);
			try{
				for(Id linkId : linkMap.keySet()){
					double dailyMin = Double.MAX_VALUE;
					double dailyMax = Double.MIN_VALUE;
					map = new HashMap<String, Value>();
					map.put(idField.getName(), new NumberValue(Integer.parseInt(linkId.toString())));
					for(String field : fieldList){
						Double difference;
						if(!linkMap.get(linkId).containsKey(field)){
							difference = 0.0;
							log.warn("could not find LinkId " + linkId + "; Field " + field);
						} else{
							difference = linkMap.get(linkId).get(field);
						}
						Value diffValue = new NumberValue(difference);
						map.put(field, diffValue);

						dailyMin = Math.min(dailyMin, difference);
						dailyMax = Math.max(dailyMax, difference);
					}
					map.put("dailyMin", new NumberValue(dailyMin));
					map.put("dailyMax", new NumberValue(dailyMax));
					
					t.addRecord(new Record(map));
					
					/* Report progress. */
					if(++counter == multiplier){
						log.info("   lines written: " + counter);
						multiplier *= 2;
					}
				}
			} catch (DbfLibException e) {
				e.printStackTrace();
			} finally{
				t.close();
			}
		} catch (InvalidFieldTypeException e) {
			e.printStackTrace();
		} catch (InvalidFieldLengthException e) {
			e.printStackTrace();
		} catch (CorruptedTableException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("   lines written: " + counter + " (Done)");		
	}
	
	private void getFields(){
		log.info("Find fields in linkstats files...");
		
		List<String> fieldArray = null;
		switch (statistic) {
		case 1:
			/* Calculate the actual difference in the traffic volumes. */
			fieldArray = getAllVolumeFields();
			break;
		case 2:
			/* Calculate the difference in volume-to-capacity ratio. */
			fieldArray = getAllVolumeFields();
			break;
		case 3:
			/* Calculate the difference in Average Annual Daily Traffic (AADT) 
			 * volume-to-capacity ratio. */
			fieldArray = getAvgTotalVolumeField();
			break;
		case 4:
			/* Calculate the difference in Average Annual Daily Traffic (AADT) 
			 * volume-to-capacity ratio. */
			fieldArray = getAvgTotalVolumeField();
			break;
		default:
			break;
		}

		
		try {
			BufferedReader br1 = IOUtils.getBufferedReader(this.baselineFilename);
			BufferedReader br2 = IOUtils.getBufferedReader(this.comparisonFilename);
			try{
				String[] h1 = br1.readLine().split("\t");
				String[] h2 = br2.readLine().split("\t");
				for(String field : fieldArray){
					int index = 0;
					boolean found = false;
					while(!found && index < h1.length){
						if(h1[index].equalsIgnoreCase(field) &&
								h1[index].equalsIgnoreCase(h2[index])){
							found = true;
						} else{
							index++;
						}
					}
					if(!found){
						log.warn("   Could not find " + field + " in both linkstats files. Field will be ignored.");
					} else{
						log.info("   Found field " + field + " in column " + index + " in both files.");
						fieldMap.put(field, index);
						fieldList.add(field);
					}
				}
								
			} finally{
				br1.close();
				br2.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private double getStatistic(String[] sa1, String[] sa2, String field){
		Double statistic = null;
		Double baseVolume = null;
		Double compareVolume = null;
		Double capacity = null;
		switch (this.statistic) {
		case 1:
			/* Calculate the difference in the traffic volumes - correcting for 
			 * populations smaller than 100%. */
			baseVolume = Double.parseDouble(sa1[fieldMap.get(field)]) / this.scaleFactor;
			compareVolume = Double.parseDouble(sa2[fieldMap.get(field)]) / this.scaleFactor;
			statistic = compareVolume - baseVolume;
			break;
		case 2:
			/* Calculate the difference in volume-to-capacity ratio. */
			capacity = this.network.getLinks().get(new IdImpl(sa1[0])).getCapacity();
			baseVolume = Double.parseDouble(sa1[fieldMap.get(field)]) / this.scaleFactor;
			compareVolume = Double.parseDouble(sa2[fieldMap.get(field)]) / this.scaleFactor;
			statistic = (compareVolume - baseVolume) / capacity;
			break;
		case 3:
			/* Calculate the difference in Average Annual Daily Traffic (AADT) 
			 * volume-to-capacity ratio. The statistic categorizes the change
			 * into specified transition classes. */
			capacity = this.network.getLinks().get(new IdImpl(sa1[0])).getCapacity();
			baseVolume = Double.parseDouble(sa1[fieldMap.get(field)]) / this.scaleFactor;
			compareVolume = Double.parseDouble(sa2[fieldMap.get(field)]) / this.scaleFactor;
			double vtc1 = 0.10*baseVolume / capacity;
			double vtc2 = 0.10*compareVolume / capacity;
			if(vtc1 <= 0.4 && vtc2 > 0.4 && vtc2 <= 0.62){
				statistic = 1.0;
			} else if(vtc1 <= 0.4 && vtc2 > 0.62){
				statistic = 2.0;
			} else if(vtc1 > 0.4 && vtc1 <= 0.62 && vtc2 <= 0.4){
				statistic = 3.0;
			} else if(vtc1 > 0.4 && vtc1 <= 0.62 && vtc2 > 0.62){
				statistic = 4.0;
			} else if(vtc1 > 0.62 && vtc2 > 0.4 && vtc2 <= 0.62){
				statistic = 5.0;
			} else if(vtc1 > 0.62 && vtc2 <= 0.4){
				statistic = 6.0;
			} else{
				statistic = 0.0;
			}
			break;	
		case 4:
			/* Calculate the state-of-congestion, AADT volume-to-capacity ratio,
			 * for each link. The statistic is similar to `3' above, but allows
			 * one to identify in exactly which state-of-congestion class a 
			 * link was before and after toll.
			 */
			capacity = this.network.getLinks().get(new IdImpl(sa1[0])).getCapacity();
			baseVolume = Double.parseDouble(sa1[fieldMap.get(field)]) / this.scaleFactor;
			compareVolume = Double.parseDouble(sa2[fieldMap.get(field)]) / this.scaleFactor;
			double vtc11 = 0.10*baseVolume / capacity;
			double vtc22 = 0.10*compareVolume / capacity;
			if(vtc11 <= 0.4 && vtc22 <= 0.4){
				statistic = 1.0;
			} else if(vtc11 <= 0.4 && vtc22 > 0.4 && vtc22 <= 0.62){
				statistic = 2.0;
			} else if(vtc11 <= 0.4 && vtc22 > 0.62){
				statistic = 3.0;
			} else if(vtc11 > 0.4 && vtc11 <= 0.62 && vtc22 <= 0.4){
				statistic = 4.0;
			} else if(vtc11 > 0.4 && vtc11 <= 0.62 && vtc22 > 0.4 && vtc22 <= 0.62){
				statistic = 5.0;
			} else if(vtc11 > 0.4 && vtc11 <= 0.62 && vtc22 > 0.62){
				statistic = 6.0;
			} else if(vtc11 > 0.62 && vtc22 <= 0.4){
				statistic = 7.0;
			} else if(vtc11 > 0.62 && vtc22 > 0.4 && vtc22 <= 0.62){
				statistic = 8.0;
			} else if(vtc11 > 0.62 && vtc22 > 0.62){
				statistic = 9.0;
			} else{
				statistic = 0.0;
			}
		default:
			break;
		}
		
		return statistic;
	}
	
	
	
	
	private List<String> getAllVolumeFields(){
		List<String> fieldList = new ArrayList<String>();
		fieldList.add("HRS0-1avg"); 
		fieldList.add("HRS1-2avg"); 
		fieldList.add("HRS2-3avg"); 
		fieldList.add("HRS3-4avg"); 
		fieldList.add("HRS4-5avg"); 
		fieldList.add("HRS5-6avg");
		fieldList.add("HRS6-7avg"); 
		fieldList.add("HRS7-8avg");
		fieldList.add("HRS8-9avg"); 
		fieldList.add("HRS9-10avg"); 
		fieldList.add("HRS10-11avg");
		fieldList.add("HRS11-12avg");
		fieldList.add("HRS12-13avg");
		fieldList.add("HRS13-14avg");
		fieldList.add("HRS14-15avg");
		fieldList.add("HRS15-16avg");
		fieldList.add("HRS16-17avg");
		fieldList.add("HRS17-18avg");
		fieldList.add("HRS18-19avg");
		fieldList.add("HRS19-20avg");
		fieldList.add("HRS20-21avg");
		fieldList.add("HRS21-22avg");
		fieldList.add("HRS22-23avg");
		fieldList.add("HRS23-24avg");
		return fieldList;
	}
	
	private List<String> getAvgTotalVolumeField(){
		List<String> fieldList = new ArrayList<String>();
		fieldList.add("HRS0-24avg"); 
		return fieldList;
	}


}
