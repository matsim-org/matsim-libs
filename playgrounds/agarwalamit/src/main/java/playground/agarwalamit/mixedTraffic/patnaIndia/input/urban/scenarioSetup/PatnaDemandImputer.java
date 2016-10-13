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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.input.urban.scenarioSetup.PatnaCalibrationUtils.PatnaDemandLabels;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.MapUtils;
import playground.agarwalamit.utils.RandomNumberUtils;

/**
 * @author amit
 */

public class PatnaDemandImputer {

	public static final Logger LOG = Logger.getLogger(PatnaCalibrationUtils.class);

	final Map<String,Integer> mode2counter = new HashMap<>();
	final Map<String,Integer> inc2counter = new HashMap<>();
	final Map<String,Integer> dailyExp2counter = new HashMap<>();

	//to remove the top element, linkedlist is used.
	List<String> randomModes = new LinkedList<>();
	List<String> dailyCostInterval = new LinkedList<>();
	List<String> monthlyIncInterval = new LinkedList<>();

	void processForUnknownData(SortedMap<String, Double> modeDistriCMP, SortedMap<String, Double> costDataCMP, SortedMap<String, Double> incomeDataCMP){
		randomModes = getRandomModesFromDistributions(modeDistriCMP, mode2counter, "plans");
		dailyCostInterval = getRandomModesFromDistributions(costDataCMP,dailyExp2counter, "interval daily cost plans");
		monthlyIncInterval = getRandomModesFromDistributions(incomeDataCMP, inc2counter, "interval income plans");
	}

	private static LinkedList<String> getRandomModesFromDistributions(SortedMap<String,Double> groupNumbers, Map<String,Integer> obj2counter, String identifier){
		int unknownDataCounter = obj2counter.get("NA");
		if (unknownDataCounter == 0) return new LinkedList<>();

		// share of each category
		groupNumbers = MapUtils.getDoublePercentShare(groupNumbers);

		// exclude if some count is already same/above desired number 
		SortedMap<String,Double> category2share = new TreeMap<>(groupNumbers);

		int totalLegs = MapUtils.intValueSum( obj2counter );
		for(String str : groupNumbers.keySet()){
			double desireNumberOfLegs = Math.round( groupNumbers.get(str) * totalLegs / 100. ) ;
			double existingNumberOfLegs = obj2counter.get(str) == null ?  0. : obj2counter.get(str);
			double requiredNumber = desireNumberOfLegs-existingNumberOfLegs;
			if (requiredNumber < 0){
				requiredNumber = 0;
				LOG.warn("The trip diary had "+existingNumberOfLegs + " " +str+" "+identifier+" and desired "+str+" "+identifier+" are "+desireNumberOfLegs
						+"; making this difference to zero and distributing it to other categories.");
				category2share.remove(str);
			}
		}
		
		// desired number of legs
		return new LinkedList<>( RandomNumberUtils.getRandomStringsFromDiscreteDistribution(category2share, unknownDataCounter) );
	}

	void readFileToFillCounter(final String inputFile){
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

					//--
					if(PatnaUtils.ALL_MODES.contains(mode)) ;// one of non-slum file have modes instead of codes.
					else if(Integer.valueOf(mode) <=9) mode = PatnaCalibrationUtils.getTravelModeFromCode(mode);
					else mode = "NA";

					if ( mode2counter.containsKey(mode) ) mode2counter.put(mode, mode2counter.get(mode)+1 );
					else mode2counter.put(mode, 1);
					//--

					//--
					if( ! monthlyIncome.equals("a") && !monthlyIncome.equals("99999") ) monthlyIncome = String.valueOf(PatnaCalibrationUtils.getIncomeInterval(monthlyIncome));
					else monthlyIncome = "NA";

					if ( inc2counter.containsKey(monthlyIncome) ) inc2counter.put(monthlyIncome, inc2counter.get(monthlyIncome)+1 );
					else inc2counter.put(monthlyIncome, 1);
					//--

					//--
					if (  ! dailyExpenditure.equals("a") &&  ! dailyExpenditure.equals("") && ! dailyExpenditure.equals("9999") ){
						dailyExpenditure = String.valueOf(PatnaCalibrationUtils.getDailyExpenditureInterval(dailyExpenditure));
					} else dailyExpenditure = "NA";

					if ( dailyExp2counter.containsKey(dailyExpenditure) ) dailyExp2counter.put(dailyExpenditure, dailyExp2counter.get(dailyExpenditure)+1 );
					else dailyExp2counter.put(dailyExpenditure, 1);
					//--
				}
				line = reader.readLine();
			}
		} catch (IOException e) {
			throw new RuntimeException(". Reason "+e);
		}
	}
}