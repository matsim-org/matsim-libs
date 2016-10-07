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
 *   pdc program is free software; you can redistribute it and/or modify  *
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

/**
 * Reading the raw input data, cleaning it, getting data randomly if not available and then writing it back.
 * 
 * @author amit
 */

public class PatnaSlumDemandCleaner  {

	private BufferedWriter writer  ;
	private final String outFile ;

	private final PatnaDemandImputer pdc = new PatnaDemandImputer();

	public PatnaSlumDemandCleaner(String outFile){
		this.outFile = outFile;
	}

	public static void main(String[] args) {
		String inputFile = PatnaUtils.INPUT_FILES_DIR+"/raw/plans/tripDiaryDataIncome/raw_uncleanData/slum_allZones_uncleanedData.txt"; 
		String outFile = PatnaUtils.INPUT_FILES_DIR+"/raw/plans/tripDiaryDataIncome/slum_allZones_cleanedData.txt";

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

		// store data 
		pdc.readFileToFillCounter(inputFile);

		// -- desired distributions
		//  update the mode instead of NA --- 480 such plans see Table 5-13
		SortedMap<String, Double> modeDistriCMP = new TreeMap<>();
		modeDistriCMP.put("motorbike", 7.);
		modeDistriCMP.put("bike", 35.); 
		modeDistriCMP.put("pt", 19.); 
		modeDistriCMP.put("walk", 38.);	


		// daily expenditure 1215 plans with no data; table 5-8
		SortedMap<String, Double> costDataCMP = new TreeMap<>();
		costDataCMP.put("1", 0.32);
		costDataCMP.put("2", 0.64);
		costDataCMP.put("3", 0.03);
		costDataCMP.put("4", 0.01);

		// monthly income -- 1244 unknown data fields ; table 5-7
		SortedMap<String, Double> incomeDataCMP = new TreeMap<>();
		incomeDataCMP.put("1", 0.01);
		incomeDataCMP.put("2", 0.10);
		incomeDataCMP.put("3", 0.53);
		incomeDataCMP.put("4", 0.23);
		incomeDataCMP.put("5", 0.11);
		incomeDataCMP.put("6", 0.01);
		incomeDataCMP.put("7", 0.01);

		// get the distribution
		pdc.processForUnknownData(modeDistriCMP, costDataCMP, incomeDataCMP);

		// now write everything back to desired format
		readZonesFileToWriteData(inputFile);

		try{
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to the file. Reason :"+e);
		}
	}

	public void readZonesFileToWriteData(final String inputFile) {

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
					mode = pdc.randomModes.remove(0); // always take what is on top.
				} else if (Integer.valueOf(mode) <= 9 ) {
					mode = PatnaCalibrationUtils.getTravelModeFromCode(mode);
				}

				if(monthlyIncome.equals("a") ) {
					monthlyIncome = pdc.monthlyIncInterval.remove(0);
					monthlyIncome = String.valueOf( PatnaUtils.getAverageIncome(monthlyIncome) );
				}

				if(dailyExpenditure.equals("a") || dailyExpenditure.equals("") || dailyExpenditure.equals("9999")) {
					dailyExpenditure = pdc.dailyCostInterval.remove(0);
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
}