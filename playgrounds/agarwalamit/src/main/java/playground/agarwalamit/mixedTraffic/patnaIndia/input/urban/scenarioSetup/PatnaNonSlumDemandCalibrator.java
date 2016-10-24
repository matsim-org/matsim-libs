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

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.input.urban.scenarioSetup.PatnaCalibrationUtils.PatnaDemandLabels;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils.PatnaUrbanActivityTypes;
import playground.agarwalamit.utils.RandomNumberUtils;

/**
 * A class to get the data for nonSlum (between 27-42 wards) based on the other data (HBW data for these zones is available). 
 * All the tables referred in the comments are taken from PatnaCMP.
 * 
 * <li> Total plans for HBE, HBS, HBO are calculated from number of HBW trip diaries and share of each type in total population for urban </li>
 * 
 * Importantly, I think, the two process
 * <li> generating an unknown number/mode randomly one by one based on the distribution during usage and, </li>
 * <li> first generate all random numbers/mode, shuffle them randomly, pick one from the group before using; </li>
 * will give two different outcomes. Thus, using the later because it will be more consistent to given distribution.
 * 
 * @author amit
 */

public class PatnaNonSlumDemandCalibrator {

	private final String HBE = PatnaUrbanActivityTypes.educational.toString();
	private final String HBS = PatnaUrbanActivityTypes.social.toString();
	private final String HBO = PatnaUrbanActivityTypes.other.toString();

	private final int TOTAL_PLANS_REQUIRED;

	private final int REQUIRED_HBE_PLANS ;
	private final int REQUIRED_HBS_PLANS ;
	private final int REQUIRED_HBO_PLANS ;

	private final int LOWER_BOUND ;
	private final int UPPER_BOUND ;


	public PatnaNonSlumDemandCalibrator () {

		// for zones 27-42 only HBW trips are available (812 plans), PatnaCMP gives share of HBW, HBE, HBS and HBO -- 45%, 34%, 4% and 17%
		// 812 plans are 45% of x; thus x = 1804; requiredPlans = 992

		TOTAL_PLANS_REQUIRED = 992;

		//out of 1804, HBE = 613; HBS = 72, HBO = 307 

		REQUIRED_HBE_PLANS = 613;  
		REQUIRED_HBS_PLANS = 72;
		REQUIRED_HBO_PLANS = 307;

		// origin zone range
		LOWER_BOUND = 27;
		UPPER_BOUND = 42;
	}

	public static void main(String[] args) {
		String outputFile = PatnaUtils.INPUT_FILES_DIR+"/raw/plans/tripDiaryDataIncome/nonSlum_27-42_imputed.txt";
		new PatnaNonSlumDemandCalibrator().processForZone27To42(outputFile);
	}

	/**
	 * Since no data is available between 27 to 42 zones for non slum. We get it from the available stats.
	 */
	public void processForZone27To42(String outputFile) {

		List<Integer> originZones = new ArrayList<>(); // order is maintained and we know the numbers
		{ // origin zones == between 27 to 42

			for (int idx = 0; idx < TOTAL_PLANS_REQUIRED; idx++) {
				originZones.add(RandomNumberUtils.getUniformlyRandomNumber(LOWER_BOUND, UPPER_BOUND));
			}
		}

		List<Integer> destinationZones_HBE = new ArrayList<>(); 
		{ // for education .. // zones are OuterCordonUtils.getAreaType2ZoneIds().get("Educational");
			for (int idx = 0; idx < REQUIRED_HBE_PLANS; idx++) {
				destinationZones_HBE.add(RandomNumberUtils.getUniformlyRandomNumber(37, 42));
			}
		}
		
		List<Integer> destinationZones_HBS_HBO = new ArrayList<>();
		{//destination zones == can be anything between 1 to 72.
			for (int idx = 0; idx < REQUIRED_HBS_PLANS+REQUIRED_HBO_PLANS; idx++) {
				destinationZones_HBS_HBO.add(RandomNumberUtils.getUniformlyRandomNumber(1, 72));
			}
		}

		// modes == can be anything from urban_all_modes, however, it is complicated if we use beeline distance distribution
		// or let simulation do the beeline correction ...

		//see table 5-14 in PatnaCMP for following
		List<String> modes = new ArrayList<>();

		{// HBE --> car 1, motorbike 9, bike 30, pt 37 and walk 22
			SortedMap<String, Double> groupNumbers = new TreeMap<>();
			groupNumbers.put("car", 1.);
			groupNumbers.put("motorbike", 9.);
			groupNumbers.put("bike", 23.); //groupNumbers.put("bike", 30.);
			groupNumbers.put("pt", 44.); //groupNumbers.put("pt", 37.);
			groupNumbers.put("walk", 22.);
			modes.addAll( RandomNumberUtils.getRandomStringsFromDiscreteDistribution(groupNumbers, REQUIRED_HBE_PLANS ) );
		}
		{// HBS --> car 10, motorbike 28, bike 34, pt 8 and walk 20
			SortedMap<String, Double> groupNumbers = new TreeMap<>();
			groupNumbers.put("car", 10.);
			groupNumbers.put("motorbike", 28.);
			groupNumbers.put("bike", 33.); //groupNumbers.put("bike", 34.);
			groupNumbers.put("pt", 9.); //groupNumbers.put("pt", 8.);
			groupNumbers.put("walk", 20.);
			modes.addAll( RandomNumberUtils.getRandomStringsFromDiscreteDistribution(groupNumbers, REQUIRED_HBS_PLANS ) );
		}
		{// HBO --> car 3, motorbike 12, bike 24, pt 34 and walk 27
			SortedMap<String, Double> groupNumbers = new TreeMap<>();
			groupNumbers.put("car", 3.);
			groupNumbers.put("motorbike", 12.);
			groupNumbers.put("bike", 20.); //groupNumbers.put("bike", 24.);
			groupNumbers.put("pt", 38.); // groupNumbers.put("pt", 34.);
			groupNumbers.put("walk", 27.);
			modes.addAll( RandomNumberUtils.getRandomStringsFromDiscreteDistribution(groupNumbers, REQUIRED_HBO_PLANS ) );
		}

		// now write the data
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			for(PatnaDemandLabels label : PatnaDemandLabels.values()) {
				writer.write(label.toString()+"\t");
			}
			writer.newLine();

			for( int index = 0; index< originZones.size(); index++ ) {
				writer.write( "NA\tNA\tNA\tNA\tNA\t" ); //all NA's

				String tripPurpose ;
				String destinationZone ;
				if( index < REQUIRED_HBE_PLANS ) {
					tripPurpose = HBE;
					destinationZone = String.valueOf( destinationZones_HBE.get(index) );
				} else if( index < REQUIRED_HBE_PLANS + REQUIRED_HBS_PLANS ) {
					tripPurpose = HBS;
					destinationZone = String.valueOf( destinationZones_HBS_HBO.get(index-REQUIRED_HBE_PLANS) );
				} else {
					tripPurpose = HBO;
					destinationZone = String.valueOf( destinationZones_HBS_HBO.get(index-REQUIRED_HBE_PLANS-REQUIRED_HBS_PLANS) );
				}

				writer.write( "a" + "\t" );
				writer.write( "a" + "\t" );
				writer.write( originZones.get( index ) + "\t" );
				writer.write( destinationZone + "\t" );
				
				writer.write( tripPurpose + "\t");
				writer.write( modes.get(index) + "\t");
				writer.write( "NA\n" );	
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not writtern. Reason "+ e);
		}
	}
}