/* *********************************************************************** *
 * project: org.matsim.*
 * CtSuveyDataExtractor.java
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

/**
 * 
 */
package playground.jjoubert.projects.wb;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.population.capeTownTravelSurvey.HouseholdEnums;
import playground.southafrica.population.capeTownTravelSurvey.PersonEnums;
import playground.southafrica.population.capeTownTravelSurvey.SurveyParser;
import playground.southafrica.population.utilities.ComprehensivePopulationReader;
import playground.southafrica.utilities.Header;

/**
 * Class to extract data, usable in R, from the City of Cape Town survey
 * population, which has already been converted from the raw survey data into
 * a MATSim population using {@link SurveyParser}.
 * 
 * @author jwjoubert
 */
public class CtSurveyDataExtractor {
	final private static Logger LOG = Logger.getLogger(CtSurveyDataExtractor.class);
	final private static String[] PRIMARIES = {"w", "e1", "e3"};

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(CtSurveyDataExtractor.class.toString(), args);
		
		String folder = args[0];
		folder += folder.endsWith("/") ? "" : "/";
		
		ComprehensivePopulationReader cpr = new ComprehensivePopulationReader();
		cpr.parse(folder);
		
		/* Check how many people are travelling. */
		int peopleTravelling = 0;
		for(Person person : cpr.getScenario().getPopulation().getPersons().values()){
			Plan plan = person.getSelectedPlan();
			if(WbUtils.isTravelling(plan)){
				peopleTravelling++;
			}
		}
		LOG.info(String.format("Number of people travelling: %d (%.2f%%)", peopleTravelling, 
				( ((double)peopleTravelling) / ((double)cpr.getScenario().getPopulation().getPersons().size()) )*100.0));
		
		/* Parsing the data for each individual. */
		BufferedWriter bw = IOUtils.getBufferedWriter(folder + "rData.csv.gz");
		try{
			String header = "id,isTravelling,age,ageGroup,education,gender,cLic,cAccess,cOwn,mcLic,mcAccess,mcOwn,hhSize,hhAsset1,hhAsset2,modePrimary,distPrimary,modeMaxLegDist,modeMaxDist,modeMaxLegTime,modeMaxTime";
			bw.write(header);
			bw.newLine();

			/* Process each individual. */ 
			for(Person person : cpr.getScenario().getPopulation().getPersons().values()){
				bw.write(parseDataForPerson(cpr.getScenario(), person));
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + bw.toString());
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + bw.toString());
			}
		}
		
		Header.printFooter();
	}

	private static String parseDataForPerson(Scenario sc, Person person){
		/* Individual attributes. */
		String id = person.getId().toString();
		String isTravelling = String.valueOf( WbUtils.isTravelling(person.getSelectedPlan()) );
		String age = String.valueOf( PersonEnums.AgeGroup.parseAgeFromBirthYear(sc.getPopulation().getPersonAttributes().getAttribute(id, "yearOfBirth").toString()) );
		String ageGroup = PersonEnums.AgeGroup.parseFromBirthYear(sc.getPopulation().getPersonAttributes().getAttribute(id, "yearOfBirth").toString()).getDescription();
		String education = PersonEnums.Education.parseFromDescription(sc.getPopulation().getPersonAttributes().getAttribute(id, "education").toString()).getDescription().replace(",", ""); // Remove commas.
		String gender = sc.getPopulation().getPersonAttributes().getAttribute(id, "gender").toString();
		String carLicense = sc.getPopulation().getPersonAttributes().getAttribute(id, "license_car").toString();
		String mcLicense = sc.getPopulation().getPersonAttributes().getAttribute(id, "license_motorcycle").toString();

		/* Household attributes. */
		String hhId = id.split("_")[0];
		//
		String asset1 = HouseholdEnums.AssetClass1.parseFromDescription(sc.getHouseholds().getHouseholdAttributes().getAttribute(hhId, "assetClassMethod1").toString()).toString();
		asset1 = convertStringToFirstLetterUppercase(asset1);
		String asset2 = HouseholdEnums.AssetClass2.parseFromDescription(sc.getHouseholds().getHouseholdAttributes().getAttribute(hhId, "assetClassMethod2").toString()).toString();
		asset2 = convertStringToFirstLetterUppercase(asset2);
		//
		String hhSize = sc.getHouseholds().getHouseholdAttributes().getAttribute(hhId, "householdSize").toString();
		String carAccess = sc.getHouseholds().getHouseholdAttributes().getAttribute(hhId, "numberOfHouseholdCarsAccessTo").toString();
		String carOwn = sc.getHouseholds().getHouseholdAttributes().getAttribute(hhId, "numberOfHouseholdCarsOwned").toString();
		String mcAccess = sc.getHouseholds().getHouseholdAttributes().getAttribute(hhId, "numberOfHouseholdMotorcyclesAccessTo").toString();
		String mcOwn = sc.getHouseholds().getHouseholdAttributes().getAttribute(hhId, "numberOfHouseholdMotorcyclesOwned").toString();
		
		/* Plan attributes. */
		String primary = parseClosestPrimary(person.getSelectedPlan());
		String modeMaxLegDistance = getMainModeByLegDistance(person.getSelectedPlan());
		String modeMaxDistance = getMainModeByDistance(person.getSelectedPlan());
		String modeMaxLegDuration = getMainModeByLegDuration(person.getSelectedPlan());
		String modeMaxDuration = getMainModeByDuration(person.getSelectedPlan());
		
		String s = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n", 
				id,
				isTravelling,
				age,
				ageGroup,
				education,
				gender,
				carLicense,
				carAccess,
				carOwn,
				mcLicense,
				mcAccess,
				mcOwn,
				hhSize,
				asset1,
				asset2,
				primary,
				modeMaxLegDistance,
				modeMaxDistance,
				modeMaxLegDuration,
				modeMaxDuration);
			
		return s;
	}
	
	private static String convertStringToFirstLetterUppercase(String s){
		s = s.toLowerCase();
		
		String newS = s.substring(0, 1).toUpperCase();
		newS += s.substring(1, s.length());
		
		return newS;
	}
	
	
	private static String parseClosestPrimary(Plan plan){
		String s = null;
		
		double dPrimary = Double.POSITIVE_INFINITY;
		String mPrimary = null;
		Activity primary = null;
		
		/* Find the home location. */
		Coord hCoord = null;
		Iterator<PlanElement> iterator = plan.getPlanElements().iterator();
		while(hCoord == null && iterator.hasNext()){
			PlanElement pe = iterator.next();
			if(pe instanceof Activity){
				Activity act = (Activity)pe;
				if(act.getType().startsWith("h")){
					hCoord = act.getCoord();
				}
			}
		}
		
		/* Find the closest Primary. */
		iterator = plan.getPlanElements().iterator();
		Leg lastLeg = null;
		while(iterator.hasNext()){
			PlanElement pe = iterator.next();
			if(pe instanceof Activity){
				Activity act = (Activity)pe;
				if(Arrays.asList(PRIMARIES).contains(act.getType())){
					double distFromHome = CoordUtils.calcEuclideanDistance(hCoord, act.getCoord());
					if(distFromHome < dPrimary){
						if(lastLeg == null){
							LOG.warn("Unknown mode to primary... ignoring primary.");
						} else{
							primary = act;
							dPrimary = distFromHome;
							mPrimary = lastLeg.getMode();
						}
					}
				}
			} else{
				Leg leg = (Leg)pe;
				lastLeg = leg;
			}
		}
		
		if(primary == null){
			s = "NA,NA";
		} else{
			s = String.format("%s,%.0f", mPrimary, dPrimary);
		}
		
		return s;
	}
	
	
	private static String getMainModeByLegDistance(Plan plan){
		String s = null;
		
		double maxDistance = Double.NEGATIVE_INFINITY;
		String maxMode = null;
		
		/* Assuming all plans are alternating Activity-Leg-Activity-... */
		for(int i = 1; i < plan.getPlanElements().size(); i+=2){
			PlanElement pe = plan.getPlanElements().get(i);
			if(pe instanceof Leg){
				Leg leg = (Leg)pe;
				
				Coord cBefore = ((Activity)plan.getPlanElements().get(i-1)).getCoord();
				Coord cAfter = ((Activity)plan.getPlanElements().get(i+1)).getCoord();
				double dist = CoordUtils.calcEuclideanDistance(cBefore, cAfter);
				
				if(dist > maxDistance){
					maxDistance = dist;
					maxMode = leg.getMode();
				}
			} else{
				LOG.error("Plan seems to be non-alternating Activity-Leg-...");
			}
		}
		
		if(maxMode == null){
			s = "NA";
		} else{
			s = maxMode;
		}
		
		return s;
	}
	
	
	private static String getMainModeByDistance(Plan plan){
		String s = null;
		Map<String, Double> map = new HashMap<>();
		
		/* Assuming all plans are alternating Activity-Leg-Activity-... */
		for(int i = 1; i < plan.getPlanElements().size(); i+=2){
			PlanElement pe = plan.getPlanElements().get(i);
			if(pe instanceof Leg){
				Leg leg = (Leg)pe;
				
				Coord cBefore = ((Activity)plan.getPlanElements().get(i-1)).getCoord();
				Coord cAfter = ((Activity)plan.getPlanElements().get(i+1)).getCoord();
				double dist = CoordUtils.calcEuclideanDistance(cBefore, cAfter);
				
				if(map.containsKey(leg.getMode())){
					double old = map.get(leg.getMode());
					map.put(leg.getMode(), old + dist);
				} else{
					map.put(leg.getMode(), dist);
				}
				
			} else{
				LOG.error("Plan seems to be non-alternating Activity-Leg-...");
			}
		}
		
		/* Now get the maximum distance. */
		double maxDistance = Double.NEGATIVE_INFINITY;
		String maxMode = null;
		for(String mode : map.keySet()){
			double dist = map.get(mode);
			if(dist > maxDistance){
				maxDistance = dist;
				maxMode = mode;
			}
		}
		
		if(maxMode == null){
			s = "NA";
		} else{
			s = maxMode;
		}
		
		return s;
	}
	
	
	private static String getMainModeByLegDuration(Plan plan){
		String s = null;
		
		double maxTime = Double.NEGATIVE_INFINITY;
		String maxMode = null;
		
		/* Assuming all plans are alternating Activity-Leg-Activity-... */
		for(int i = 1; i < plan.getPlanElements().size(); i+=2){
			PlanElement pe = plan.getPlanElements().get(i);
			if(pe instanceof Leg){
				Leg leg = (Leg)pe;
				
				double legTime = leg.getTravelTime() / 60.0; // Travel time in seconds.
				
				if(legTime > maxTime){
					maxTime = legTime;
					maxMode = leg.getMode();
				}
			} else{
				LOG.error("Plan seems to be non-alternating Activity-Leg-...");
			}
		}
		
		if(maxMode == null){
			s = "NA";
		} else{
			s = maxMode;
		}
		
		return s;
	}
	
	
	private static String getMainModeByDuration(Plan plan){
		String s = null;
		Map<String, Double> map = new HashMap<>();
		
		/* Assuming all plans are alternating Activity-Leg-Activity-... */
		for(int i = 1; i < plan.getPlanElements().size(); i+=2){
			PlanElement pe = plan.getPlanElements().get(i);
			if(pe instanceof Leg){
				Leg leg = (Leg)pe;
				
				double legTime = leg.getTravelTime() / 60.0; // Travel time in seconds.
				
				if(map.containsKey(leg.getMode())){
					double old = map.get(leg.getMode());
					map.put(leg.getMode(), old + legTime);
				} else{
					map.put(leg.getMode(), legTime);
				}
			} else{
				LOG.error("Plan seems to be non-alternating Activity-Leg-...");
			}
		}
		
		/* Now get the maximum duration. */
		double maxTime = Double.NEGATIVE_INFINITY;
		String maxMode = null;
		for(String mode : map.keySet()){
			double legTime = map.get(mode);
			if(legTime > maxTime){
				maxTime = legTime;
				maxMode = mode;
			}
		}
		
		if(maxMode == null){
			s = "NA";
		} else{
			s = maxMode;
		}
		
		return s;
	}
	
	
}





