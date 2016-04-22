/* *********************************************************************** *
 * project: org.matsim.*
 * SingaporeChooseRandomSingleLegMode.java
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

package playground.singapore.springcalibration.run.replanning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.PermissibleModesCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.PtConstants;

import playground.singapore.springcalibration.run.TaxiUtils;

/**
 * Changes the transportation mode of one leg in a plan to a randomly chosen
 * mode, given a list of possible modes. Insures that the newly chosen mode
 * is different from the existing mode (if possible).
 *
 * @author anhorni
 */
public class SingaporeTripOrSubtourModeChange implements PlanAlgorithm {

	private final String[] possibleModes;
	private boolean ignoreCarAvailability = false;
	private double walkThreshold = 5000.0;
	private Population population;
	private static final Logger log = Logger.getLogger(SingaporeTripOrSubtourModeChange.class);
	private TaxiUtils taxiUtils;
	private SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup;
		
	private final Random rng;

	/**
	 * @param possibleModes
	 * @param rng The random number generator used to draw random numbers to select another mode.
	 * @see TransportMode
	 * @see MatsimRandom
	 */
	public SingaporeTripOrSubtourModeChange(final String[] possibleModes, final Random rng, Population population, TaxiUtils taxiUtils, SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup) {
		this.possibleModes = possibleModes.clone();
		this.rng = rng;
		this.population = population;
		this.taxiUtils = taxiUtils;
		this.subtourModeChoiceConfigGroup = subtourModeChoiceConfigGroup;
		log.info("Replanning for population of size: " + population.getPersons().size());
	}

	public void setIgnoreCarAvailability(final boolean ignoreCarAvailability) {
		this.ignoreCarAvailability = ignoreCarAvailability;
	}

	@Override
	public void run(final Plan plan) {
		boolean forbidCar = false;
		boolean forbidPassenger = false;
		boolean forbidOther = false;
		boolean forbidSchoolbus = false;
		
		if (!this.ignoreCarAvailability) {
			String carAvail = (String) population.getPersonAttributes().getAttribute(plan.getPerson().getId().toString(), "car");
			String license = (String) population.getPersonAttributes().getAttribute(plan.getPerson().getId().toString(), "license");						
			// as defined only people with license and car are allowed to use car
			if ("never".equals(carAvail) || "no".equals(license)) {
				forbidCar = true;
			}
			if ("never".equals(carAvail)) {
				forbidPassenger = true;
			}
		}
		
		String ageStr = (String) population.getPersonAttributes().getAttribute(plan.getPerson().getId().toString(), "age");
		// if there is no age given, e.g., for freight agents
		int age = 25;	
		String cleanedAge = ageStr.replace("age", "");
		cleanedAge = cleanedAge.replace("up", "");
		if (ageStr != null) age = Integer.parseInt(cleanedAge);
		if (age < 20) forbidOther = true;
		if (age > 20) forbidSchoolbus = true;

		ArrayList<Leg> legs = new ArrayList<Leg>();
		int cnt = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				legs.add((Leg) pe);
				cnt++;
			}
		}
		if (cnt == 0) {
			return;
		}
		int rndIdx = this.rng.nextInt(cnt);
		
		Leg chosenLeg = legs.get(rndIdx);
		
		 
		/*	
		 If leg is in a subtour where car (or any other chain-based mode) is used, then break
		  
		  Legs can only be in a car subtour if they are themselves car legs, 
		  thus we do not have to extract subtours and check for car and pipapo 
		  but instead we can only check for leg ?= car and return if yes.
		  In this way we are sure not to break the subtour constraints regarding mass conservation.
		  */
		String[] chainBasedModes = this.subtourModeChoiceConfigGroup.getChainBasedModes();
		if (Arrays.asList(chainBasedModes).contains(chosenLeg.getMode())) return;
		
		// If freight mode also return. Probably this is ensured already by the subpop handling, TODO: check
		if (chosenLeg.getMode().equals("freight")) return;
							

		// just to speed up relaxation
		boolean forbidWalk= false;
		Activity nextAct = PlanUtils.getNextActivity(plan, chosenLeg);
		Activity previousAct = PlanUtils.getPreviousActivity(plan, chosenLeg);
		double distance = CoordUtils.calcEuclideanDistance(previousAct.getCoord(), nextAct.getCoord());		
		if (distance > walkThreshold) forbidWalk = true; 
			
		setRandomLegMode(plan, chosenLeg, forbidCar, forbidPassenger, forbidWalk, forbidOther, forbidSchoolbus);
	}
	
	private void setRandomLegMode(Plan plan, final Leg leg, final boolean forbidCar, final boolean forbidPassenger, final boolean forbidWalk, final boolean forbidOther, boolean forbidSchoolbus) {
		String previousActivity = PlanUtils.getPreviousActivity(plan, leg).getType();
		String nextActivity = PlanUtils.getNextActivity(plan, leg).getType();
		
		if (!(previousActivity.equals("home") && nextActivity.contains("school") ||
				(previousActivity.contains("school") && nextActivity.equals("home")))) {
				forbidSchoolbus = true;	
		}
		
		String newMode = chooseModeOtherThan(leg.getMode(), 
				forbidCar, 
				forbidPassenger, 
				forbidWalk, 
				forbidOther, 
				forbidSchoolbus);
		
		if (!leg.getMode().equals("taxi") && newMode.equals("taxi")) {
			this.handleTaxi(plan, leg, 1);
		}
		if (leg.getMode().equals("taxi") && !newMode.equals("taxi")) {
			this.handleTaxi(plan, leg, -1);
		}
		leg.setMode(newMode);
	}

	private String chooseModeOtherThan(final String currentMode, final boolean forbidCar, final boolean forbidPassenger, final boolean forbidWalk, final boolean forbidOther, final boolean forbidSchoolbus) {
		String newMode;
		while (true) {
			int newModeIdx = this.rng.nextInt(this.possibleModes.length - 1);
			for (int i = 0; i <= newModeIdx; i++) {
				if (this.possibleModes[i].equals(currentMode)) {
					/* if the new Mode is after the currentMode in the list of possible
					 * modes, go one further, as we have to ignore the current mode in
					 * the list of possible modes. */
					newModeIdx++;
					break;
				}
			}
			newMode = this.possibleModes[newModeIdx];
			if (!(forbidCar && TransportMode.car.equals(newMode))) {
				break;
			} else {
				if (this.possibleModes.length == 2) {
					newMode = currentMode; // there is no other mode available
					break;
				}
			}
			
			if (!(forbidPassenger && "passenger".equals(newMode))) {
				break;
			} else {
				if (this.possibleModes.length == 2) {
					newMode = currentMode; // there is no other mode available
					break;
				}
			}
			
			if (!(forbidWalk && TransportMode.walk.equals(newMode))) {
				break;
			} else {
				if (this.possibleModes.length == 2) {
					newMode = currentMode; // there is no other mode available
					break;
				}
			}
			
			if (!(forbidOther && TransportMode.other.equals(newMode))) {
				break;
			} else {
				if (this.possibleModes.length == 2) {
					newMode = currentMode; // there is no other mode available
					break;
				}
			}
			
			if (!(forbidSchoolbus && "schoolbus".equals(newMode))) {
				break;
			} else {
				if (this.possibleModes.length == 2) {
					newMode = currentMode; // there is no other mode available
					break;
				}
			}
		}
		return newMode;
	}
	
	private void handleTaxi(Plan plan, Leg leg, int addOrRemove) {
		// add or remove taxi waiting time act and leg
		if (addOrRemove == 1) {
			this.newPlanWithTaxiStages(plan, leg);
			
		} else if (addOrRemove == -1) {
			Activity wait4taxiActivity = PlanUtils.getPreviousActivity(plan, leg);
			Leg walk2taxiLeg = PlanUtils.getPreviousLeg(plan, wait4taxiActivity);
			
			// see also TransitActsRemover
			//int act2RemoveIndex = plan.getPlanElements().indexOf(wait4taxiActivity);
			// this one is not necessary as it will be removed with the leg!
			//((PlanImpl) plan).removeActivity(act2RemoveIndex);
			
			int leg2RemoveIndex = plan.getPlanElements().indexOf(walk2taxiLeg);
			((PlanImpl) plan).removeLeg(leg2RemoveIndex);
		}
	}
	
	/*
	 * Is this really the most efficient way of adding an activiyt at a certain plan loc?
	 */
	private void newPlanWithTaxiStages(Plan plan, Leg leg) {
		Plan tmpPlan = new PlanImpl();
		
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity currentActivity = (Activity)pe;
				Leg nextLeg = PlanUtils.getNextLeg(plan, currentActivity);
				if (nextLeg.getMode().equals("taxi")&& !currentActivity.equals(TaxiUtils.wait4Taxi)) {
					Coord coord = currentActivity.getCoord();
					int hour = (int)(currentActivity.getEndTime() / 3600.0);
					double taxiWaitTime = this.taxiUtils.getWaitingTime(coord, hour);
					double taxiWaitEndTime = currentActivity.getEndTime() + taxiWaitTime;
					
					Leg taxiWalkLeg = new LegImpl(TaxiUtils.taxi_walk);
					taxiWalkLeg.setDepartureTime(currentActivity.getEndTime());
					taxiWalkLeg.setTravelTime(0.0);
										
					Activity taxiWaitAct = new ActivityImpl(TaxiUtils.wait4Taxi, currentActivity.getCoord(), currentActivity.getLinkId());					
					taxiWaitAct.setEndTime(taxiWaitEndTime);
					tmpPlan.addLeg(taxiWalkLeg);
					tmpPlan.addActivity(taxiWaitAct);
				}
				tmpPlan.addActivity(currentActivity);
			}
			if (pe instanceof Leg) {
				Leg currentLeg = (Leg)pe;
				tmpPlan.addLeg(currentLeg);
			}
			
		}
		plan.getPlanElements().clear();
		PlanUtils.copyFrom(tmpPlan, plan);	
	}
}
