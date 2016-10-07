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
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.input.urban.scenarioSetup.PatnaCalibrationUtils.PatnaDemandLabels;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils.PatnaUrbanActivityTypes;

/**
 * Reading the raw input data, cleaning it, getting data randomly if not available and then writing it back.
 * 
 * @author amit
 */

public class PatnaNonSlumDemandCleaner {

	private BufferedWriter writer  ;
	private final String outFile ;

	private final PatnaDemandImputer pdc = new PatnaDemandImputer();

	public PatnaNonSlumDemandCleaner(String outFile){
		this.outFile = outFile;
	}

	public static void main(String[] args) {
		String inputFile1 = PatnaUtils.INPUT_FILES_DIR+"/raw/plans/tripDiaryDataIncome/raw_uncleanData/nonSlum_27-42_uncleanedData.txt"; 
		String inputFile2 = PatnaUtils.INPUT_FILES_DIR+"/raw/plans/tripDiaryDataIncome/raw_uncleanData/nonSlum_restZones_uncleanedData.txt";
		String inputFile3 = PatnaUtils.INPUT_FILES_DIR+"/raw/plans/tripDiaryDataIncome/nonSlum_27-42_imputed.txt";

		String outFile = PatnaUtils.INPUT_FILES_DIR+"/raw/plans/tripDiaryDataIncome/nonSlum_allZones_cleanedData.txt";

		PatnaNonSlumDemandCleaner pdfc = new PatnaNonSlumDemandCleaner(outFile);
		pdfc.run(inputFile1, inputFile2, inputFile3);
	}

	public void run (String inputFile1, String inputFile2, String inputFile3){
		this.writer = IOUtils.getBufferedWriter(this.outFile);

		try {
			for (PatnaDemandLabels pdLabels : PatnaDemandLabels.values()) {
				writer.write(pdLabels.toString()+"\t");
			}
			writer.newLine();
		} catch (IOException e) {
			throw new RuntimeException(". Reason "+e);
		}

		//store data
		readFileToFillCounter(inputFile1,true);
		readFileToFillCounter(inputFile2,false);
		readFileToFillCounter(inputFile3,false); // this is cleaned file. However, good to just add it to the same file.

		// -- desired distributions
		// update the mode instead of 9999 --- 71 such plans only in 27-42 zones file
		// Table 5-14, HBW , car 8, motorbike 38, bike 37, pt 11, walk 5
		SortedMap<String, Double> modeDistriCMP = new TreeMap<>();
		modeDistriCMP.put("car", 8.);
		modeDistriCMP.put("motorbike", 38.);
		modeDistriCMP.put("bike", 37.); //groupNumbers.put("bike", 37.);
		modeDistriCMP.put("pt", 11.); // groupNumbers.put("pt", 11.);
		modeDistriCMP.put("walk", 5.);

		// daily expenditure 4138 such plans in restZonesFile only. 
		SortedMap<String, Double> costDataCMP = new TreeMap<>(); // Table 5-8
		costDataCMP.put("1", 0.22);
		costDataCMP.put("2", 0.52);
		costDataCMP.put("3", 0.14);
		costDataCMP.put("4", 0.08);
		costDataCMP.put("5", 0.03);

		// monthly income 4065 such plans in restZonesFile only.
		SortedMap<String, Double> incomeDataCMP = new TreeMap<>();
		incomeDataCMP.put("1", 0.01);
		incomeDataCMP.put("2", 0.01);
		incomeDataCMP.put("3", 0.21);
		incomeDataCMP.put("4", 0.27);
		incomeDataCMP.put("5", 0.18);
		incomeDataCMP.put("6", 0.13);
		incomeDataCMP.put("7", 0.19);

		// get the distribution
		pdc.processForUnknownData(modeDistriCMP,costDataCMP,incomeDataCMP);

		// now write everthing back to desired format
		readZonesFileAndWriteData(inputFile1, true);
		readZonesFileAndWriteData(inputFile2, false);
		readZonesFileAndWriteData(inputFile3, false); 

		try{
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to the file. Reason :"+e);
		}
	}

	public void readZonesFileAndWriteData(final String inputFile, 
			final boolean isZoneBW27To42 // only HBW trips are available for 27 to 42; rest is imputed.
			) {

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
				String monthlyIncome = strs.get( labels.indexOf( PatnaDemandLabels.monthlyIncome.toString() ));  // Rs or na
				String dailyExpenditure = strs.get( labels.indexOf( PatnaDemandLabels.dailyTransportCost.toString() )); // Rs or na

				String tripFreq = strs.get( labels.indexOf( PatnaDemandLabels.tripFrequency.toString() ));

				if(tripPurpose.equals("9999") && isZoneBW27To42 ) {
					tripPurpose = PatnaUrbanActivityTypes.work.toString();
				} else tripPurpose = PatnaCalibrationUtils.getTripPurpose(tripPurpose);

				if (mode.equals("9999")) {
					mode = pdc.randomModes.remove(0); // always take what is on top.
				} else if (PatnaUtils.ALL_MODES.contains(mode)) {
					// nothing to do.
				} else if (Integer.valueOf(mode) <= 9 ) {
					mode = PatnaCalibrationUtils.getTravelModeFromCode(mode);
				}

				if(monthlyIncome.equals("a") || monthlyIncome.equals("99999")) {
					monthlyIncome = pdc.monthlyIncInterval.remove(0); // interval and NOT actual income
					monthlyIncome = String.valueOf( PatnaUtils.getAverageIncome(monthlyIncome) );
				}

				if(dailyExpenditure.equals("a")) {
					dailyExpenditure = pdc.dailyCostInterval.remove(0);// interval and NOT actual cost
					dailyExpenditure = String.valueOf( PatnaUtils.getAverageDailyTranportCost(dailyExpenditure) );
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

	private void readFileToFillCounter(final String inputFile, final boolean isZoneBW27To42){
		try (BufferedReader reader = IOUtils.getBufferedReader(inputFile)) {
			String line = reader.readLine();
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

					String mode = strs.get( labels.indexOf( PatnaDemandLabels.mode.toString() ));
					String monthlyIncome = strs.get( labels.indexOf( PatnaDemandLabels.monthlyIncome.toString() )); 
					String dailyExpenditure = strs.get( labels.indexOf( PatnaDemandLabels.dailyTransportCost.toString() ));
					
					String tripPurpose = strs.get( labels.indexOf( PatnaDemandLabels.tripPurpose.toString() ));
					
					if(tripPurpose.equals("9999") && isZoneBW27To42 ) {
						tripPurpose = PatnaUrbanActivityTypes.work.toString();
					} else tripPurpose = PatnaCalibrationUtils.getTripPurpose(tripPurpose);

					//--
					if(PatnaUtils.ALL_MODES.contains(mode)) ;// one of non-slum file have modes instead of codes.
					else if(Integer.valueOf(mode) <=9) mode = PatnaCalibrationUtils.getTravelModeFromCode(mode);
					else mode = "NA";

					if(tripPurpose.equalsIgnoreCase(PatnaUrbanActivityTypes.work.toString()) ){  // only work trips; distri is also taken for work trips
						if ( pdc.mode2counter.containsKey(mode) ) pdc.mode2counter.put(mode, pdc.mode2counter.get(mode)+1 );
						else pdc.mode2counter.put(mode, 1);
					}
					//--

					//--
					if( ! monthlyIncome.equals("a") && !monthlyIncome.equals("99999") ) monthlyIncome = String.valueOf(PatnaCalibrationUtils.getIncomeInterval(monthlyIncome));
					else monthlyIncome = "NA";

					if ( pdc.inc2counter.containsKey(monthlyIncome) ) pdc.inc2counter.put(monthlyIncome, pdc.inc2counter.get(monthlyIncome)+1 );
					else pdc.inc2counter.put(monthlyIncome, 1);
					//--

					//--
					if (  ! dailyExpenditure.equals("a") &&  ! dailyExpenditure.equals("") && ! dailyExpenditure.equals("9999") ){
						dailyExpenditure = String.valueOf(PatnaCalibrationUtils.getDailyExpenditureInterval(dailyExpenditure));
					} else dailyExpenditure = "NA";

					if ( pdc.dailyExp2counter.containsKey(dailyExpenditure) ) pdc.dailyExp2counter.put(dailyExpenditure, pdc.dailyExp2counter.get(dailyExpenditure)+1 );
					else pdc.dailyExp2counter.put(dailyExpenditure, 1);
					//--
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			throw new RuntimeException(". Reason "+e);
		}
	}
}