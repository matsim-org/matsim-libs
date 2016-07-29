/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.input.urban.scenarioSetup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.input.urban.scenarioSetup.PatnaCalibrationUtils.PatnaDemandLabels;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.RandomNumberUtils;

/**
 * Reading the raw input data, cleaning it, getting data randomly if not available and then writing it back.
 * 
 * @author amit
 */

public class PatnaSlumDemandCleaner {

	private BufferedWriter writer  ;
	private final String outFile ;

	public PatnaSlumDemandCleaner(String outFile){
		this.outFile = outFile;
	}

	public static void main(String[] args) {
		String inputFile = PatnaUtils.INPUT_FILES_DIR+"/plans/tripDiaryDataIncome/raw_uncleanData/slum_allZones_uncleanedData.txt"; 
		String outFile = PatnaUtils.INPUT_FILES_DIR+"/plans/tripDiaryDataIncome/slum_allZones_cleanedData.txt";

		PatnaSlumDemandCleaner pdfc = new PatnaSlumDemandCleaner(outFile);
		pdfc.run(inputFile);
	}

	public void run (String inputFile){
		this.writer = IOUtils.getBufferedWriter(this.outFile);

		try {
			for (PatnaDemandLabels pdLabels : PatnaDemandLabels.values()) {
				writer.write(pdLabels.toString()+"\t");
			}
			writer.newLine();
		} catch (IOException e) {
			throw new RuntimeException(". Reason "+e);
		}

		storeRandomDataForUnknownFields();

		readZonesFileAndWriteData(inputFile);

		try{
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to the file. Reason :"+e);
		}
	}

	private List<String> randomModes;
	private List<String> dailyCostInterval;
	private List<String> monthlyIncInterval;

	private void storeRandomDataForUnknownFields(){
		{ // travel modes
			// update the mode instead of 9999 --- 480 such plans 
			// using aggregate distribution; see Table 5-13
			SortedMap<String, Double> groupNumbers = new TreeMap<>();
			groupNumbers.put("motorbike", 7.);
			groupNumbers.put("bike", 35.); //groupNumbers.put("bike", 39.);
			groupNumbers.put("pt", 19.); // groupNumbers.put("pt", 15.);
			groupNumbers.put("walk", 38.);

			// to remove the top element, linkedlist is used.
			this.randomModes = new LinkedList<>( RandomNumberUtils.getRandomStringsFromDiscreteDistribution(groupNumbers, 480) );
		}
		{ // daily expenditure 1215 such plans
			SortedMap<String, Double> groupNumbers = new TreeMap<>();
			groupNumbers.put("1", 0.32);
			groupNumbers.put("2", 0.64);
			groupNumbers.put("3", 0.03);
			groupNumbers.put("4", 0.01);
			dailyCostInterval = new LinkedList<>(  RandomNumberUtils.getRandomStringsFromDiscreteDistribution(groupNumbers, 1215) );
		}

		{ // monthly income 1244 such plans 
			SortedMap<String, Double> groupNumbers = new TreeMap<>();
			groupNumbers.put("1", 0.01);
			groupNumbers.put("2", 0.10);
			groupNumbers.put("3", 0.53);
			groupNumbers.put("4", 0.23);
			groupNumbers.put("5", 0.11);
			groupNumbers.put("6", 0.01);
			groupNumbers.put("7", 0.01);
			monthlyIncInterval = new LinkedList<>(  RandomNumberUtils.getRandomStringsFromDiscreteDistribution(groupNumbers, 1244) );
		}
	}

	public void readZonesFileAndWriteData(final String inputFile) {

		BufferedReader reader = IOUtils.getBufferedReader(inputFile);

		String line;
		try {
			line = reader.readLine();
		} catch (IOException e1) {
			throw new RuntimeException(". Reason "+e1);
		}

		List<String> labels = new ArrayList<>();

		while(line!=null) {
			String row [] = line.split("\t");
			List<String> strs = Arrays.asList(row);

			if( row[0].substring(0, 1).matches("[A-Za-z]") // labels 
					&& !row[0].startsWith("NA") // "NA" could also be inside the data 
					) {
				for (String s : strs){ 
					labels.add(s); 
				}
			} else { // main data

				String ward = strs.get( labels.indexOf( PatnaDemandLabels.ward.toString() ));
				String member = strs.get( labels.indexOf( PatnaDemandLabels.member.toString() ));
				String sex = strs.get( labels.indexOf( PatnaDemandLabels.sex.toString() ));
				String age = strs.get( labels.indexOf( PatnaDemandLabels.age.toString() ));
				String occupation = strs.get( labels.indexOf( PatnaDemandLabels.occupation.toString() ));

				String originWard = strs.get( labels.indexOf( PatnaDemandLabels.originZone.toString() ));
				String destinationWard = strs.get( labels.indexOf( PatnaDemandLabels.destinationZone.toString() ));
				String tripPurpose = strs.get( labels.indexOf( PatnaDemandLabels.tripPurpose.toString() ));
				String mode = strs.get( labels.indexOf( PatnaDemandLabels.mode.toString() ));
				String monthlyIncome = strs.get( labels.indexOf( PatnaDemandLabels.monthlyIncome.toString() )); 
				String dailyExpenditure = strs.get( labels.indexOf( PatnaDemandLabels.dailyTransportCost.toString() ));

				String tripFreq = strs.get( labels.indexOf( PatnaDemandLabels.tripFrequency.toString() ));

				tripPurpose = PatnaCalibrationUtils.getTripPurpose(tripPurpose);

				if (mode.equals("9999")) {
					mode = randomModes.remove(0); // always take what is on top.
				} else if (Integer.valueOf(mode) <= 9 ) {
					mode = PatnaCalibrationUtils.getTravelModeFromCode(mode);
				}

				if(monthlyIncome.equals("a") ) {
					monthlyIncome = monthlyIncInterval.remove(0);
				}

				if(dailyExpenditure.equals("a") || dailyExpenditure.equals("") || dailyExpenditure.equals("9999")) {
					dailyExpenditure = dailyCostInterval.remove(0);
				}

				monthlyIncome = String.valueOf(PatnaCalibrationUtils.getIncomeInterval(monthlyIncome));
				dailyExpenditure = String.valueOf(PatnaCalibrationUtils.getDailyExpenditureInterval(dailyExpenditure));

				try {
					writer.write(ward+"\t"+
							member+"\t"+
							sex+"\t"+
							age+"\t"+
							occupation+"\t"+
							monthlyIncome+"\t"+
							dailyExpenditure+"\t"+
							originWard+"\t"+
							destinationWard+"\t"+
							tripPurpose+"\t"+// all of entries in this file is HBW trips
							mode+"\t"+
							tripFreq+"\n"
							);
				} catch (IOException e) {
					throw new RuntimeException(". Reason "+e);
				}
			}
			try {
				line = reader.readLine();
			} catch (IOException e) {
				throw new RuntimeException(". Reason "+e);
			}
		}
	}
}