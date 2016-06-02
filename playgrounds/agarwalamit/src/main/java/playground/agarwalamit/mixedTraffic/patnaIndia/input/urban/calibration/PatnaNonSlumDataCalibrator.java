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

package playground.agarwalamit.mixedTraffic.patnaIndia.input.urban.calibration;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils.PatnaUrbanActivityTypes;
import playground.agarwalamit.utils.AARandomNumberGenerator;

/**
 * A class to impute the data for nonSlum (between 27-42 wards) based on the HBW data of these zones.
 * All the tables referred in the comments are taken from PatnaCMP.
 * 
 * Importantly, I think, the two process
 * <li> 
 * generating an unknown number/mode randomly one by one based on the distribution during usage and,
 * </li>
 * <li>
 * first generate all random numbers/mode based on the distribution, pick one from the group before using;
 * </li>
 * will give two different outcomes. Thus, using the later because it will be more close to given distribution.
 * 
 * @author amit
 */

public class PatnaNonSlumDataCalibrator {

	private final String HBE = PatnaUrbanActivityTypes.educational.toString();
	private final String HBS = PatnaUrbanActivityTypes.social.toString();
	private final String HBO = PatnaUrbanActivityTypes.other.toString();

	private final int TOTAL_PLANS_REQUIRED;

	private final double REQUIRED_HBE_PLANS_SHARE ;
	private final double REQUIRED_HBS_PLANS_SHARE ;
	private final double REQUIRED_HBO_PLANS_SHARE ;

	private final int LOWER_BOUND ;
	private final int UPPER_BOUND ;

	PatnaNonSlumDataCalibrator () {

		// for zones 27-42 only HBW trips are available (812 plans), PatnaCMP gives share of HBW, HBE, HBS and HBO -- 45%, 34%, 4% and 17%
		// 812 plans are 45% of x; thus x = 1804; requiredPlans = 992

		TOTAL_PLANS_REQUIRED = 992;

		//out of 1804, HBE = 613; HBS = 72, HBO = 307 ==> HBE =0.62, HBS = 0.07, HBO = 0.31

		REQUIRED_HBE_PLANS_SHARE = 0.62;  
		REQUIRED_HBS_PLANS_SHARE = 0.07;
		REQUIRED_HBO_PLANS_SHARE = 0.31;

		// origin zone range
		LOWER_BOUND = 27;
		UPPER_BOUND = 42;
	}

	public static void main(String[] args) {
		String outputFile = "../../../../repos/shared-svn/projects/patnaIndia/inputs/tripDiaryDataIncome/nonSlum_27-42_imputed.txt";
		new PatnaNonSlumDataCalibrator().run(outputFile);
	}

	public void run(String outputFile) {

		SortedMap<String, List<Integer> > originZones = new TreeMap<>();
		{ // origin zones == between 27 to 42
			SortedMap<String, Double> groupNumbers = new TreeMap<>();
			groupNumbers.put(HBE, REQUIRED_HBE_PLANS_SHARE);
			groupNumbers.put(HBS, REQUIRED_HBS_PLANS_SHARE);
			groupNumbers.put(HBO, REQUIRED_HBO_PLANS_SHARE);
			AARandomNumberGenerator aarng = new AARandomNumberGenerator(groupNumbers);
			originZones = aarng.getRandomNumbers(LOWER_BOUND, UPPER_BOUND, TOTAL_PLANS_REQUIRED);
		}

		List<Integer> destinationZones = new ArrayList<>();
		{//destination zones == can be anything between 1 to 72.
			SortedMap<String, Double> groupNumbers = new TreeMap<>();
			groupNumbers.put("DESTINATION", 1.0);
			AARandomNumberGenerator aarng = new AARandomNumberGenerator(groupNumbers);
			destinationZones = aarng.getRandomNumbers(1, 72, TOTAL_PLANS_REQUIRED).get("DESTINATION");
		}

		// modes == can be anything from urban_all_modes, however, it is complicated if we use beeline distance distribution
		// or let simulation do the beeline correction ...

		//see table 5-14 in PatnaCMP for following
		List<String> modes = new ArrayList<>();

		{// HBE --> car 1, motorbike 9, bike 30, pt 37 and walk 22
			SortedMap<String, Double> groupNumbers = new TreeMap<>();
			groupNumbers.put("car", 1.);
			groupNumbers.put("motorbike", 9.);
			groupNumbers.put("bike", 30.);
			groupNumbers.put("pt", 37.);
			groupNumbers.put("walk", 22.);
			AARandomNumberGenerator aarng = new AARandomNumberGenerator(groupNumbers);
			modes.addAll( aarng.getRandomDistribution( (int) Math.round(TOTAL_PLANS_REQUIRED * REQUIRED_HBE_PLANS_SHARE) ) );
		}
		{// HBS --> car 10, motorbike 28, bike 34, pt 8 and walk 20
			SortedMap<String, Double> groupNumbers = new TreeMap<>();
			groupNumbers.put("car", 10.);
			groupNumbers.put("motorbike", 28.);
			groupNumbers.put("bike", 34.);
			groupNumbers.put("pt", 8.);
			groupNumbers.put("walk", 20.);
			AARandomNumberGenerator aarng = new AARandomNumberGenerator(groupNumbers);
			modes.addAll( aarng.getRandomDistribution( (int) Math.round(TOTAL_PLANS_REQUIRED * REQUIRED_HBS_PLANS_SHARE) ) );
		}
		{// HBO --> car 3, motorbike 12, bike 24, pt 34 and walk 27
			SortedMap<String, Double> groupNumbers = new TreeMap<>();
			groupNumbers.put("car", 3.);
			groupNumbers.put("motorbike", 12.);
			groupNumbers.put("bike", 24.);
			groupNumbers.put("pt", 34.);
			groupNumbers.put("walk", 27.);
			AARandomNumberGenerator aarng = new AARandomNumberGenerator(groupNumbers);
			modes.addAll( aarng.getRandomDistribution( (int) Math.round(TOTAL_PLANS_REQUIRED * REQUIRED_HBO_PLANS_SHARE) ) );
		}

		// daily cost == can be anything between 1 to 5; see Table 5-8
		// nonSlum; (interval -share) 1-22, 2-52, 3-14, 4-8, 5-3
		List<String> dailyCostInterval = new ArrayList<>();
		{
			SortedMap<String, Double> groupNumbers = new TreeMap<>();
			groupNumbers.put("1", 0.22);
			groupNumbers.put("2", 0.52);
			groupNumbers.put("3", 0.14);
			groupNumbers.put("4", 0.08);
			groupNumbers.put("5", 0.03);
			AARandomNumberGenerator aarng = new AARandomNumberGenerator(groupNumbers);
			dailyCostInterval = aarng.getRandomDistribution(TOTAL_PLANS_REQUIRED);
		}

		// incomeInterval == can be anything between 1 to 7; see Table 5-7
		// nonSlum; (interval - cummulative) 1 - 1, 2- 2, 3 -23, 4-50, 5-68, 6-81, 7-100
		List<String> incomeInterval = new ArrayList<>();
		{
			SortedMap<String, Double> groupNumbers = new TreeMap<>();
			groupNumbers.put("1", 0.01);
			groupNumbers.put("2", 0.02);
			groupNumbers.put("3", 0.23);
			groupNumbers.put("4", 0.50);
			groupNumbers.put("5", 0.68);
			groupNumbers.put("6", 0.81);
			groupNumbers.put("7", 1.0);
			AARandomNumberGenerator aarng = new AARandomNumberGenerator(groupNumbers);
			incomeInterval = aarng.getRandomDistribution(TOTAL_PLANS_REQUIRED);
		}

		// now write the data
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			writer.write("ward\tmember\tsex\tage\toccupation\tmonthlyInc\tdailyCost\toriginWard\tdesiward\tpurpose\tmode\tfreq\n");
			int index = 0;
			for(String tripPurpose : originZones.keySet() ) {
				for (int ii : originZones.get(tripPurpose) ) {
					writer.write("NA\tNA\tNA\tNA\tNA\t");//all NA's
					writer.write(incomeInterval.get(index)+"\t");
					writer.write(dailyCostInterval.get(index)+"\t");
					writer.write(ii+"\t");
					writer.write(destinationZones.get(index)+"\t");
					writer.write(tripPurpose+"\t");
					writer.write(modes.get(index)+"\t");
					writer.write("NA\n");	
					index++;
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not writtern. Reason "+ e);
		}
	}
}