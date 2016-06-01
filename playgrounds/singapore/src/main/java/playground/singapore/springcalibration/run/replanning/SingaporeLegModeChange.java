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
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.PermissibleModesCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.singapore.springcalibration.run.TaxiUtils;

/**
 * Changes the transportation mode of one leg in a plan to a randomly chosen
 * mode, given a list of possible modes. Insures that the newly chosen mode
 * is different from the existing mode (if possible).
 *
 * @author anhorni
 */
public class SingaporeLegModeChange implements PlanAlgorithm {

	private final String[] possibleModes;
	private boolean ignoreCarAvailability = false;
	private double walkThreshold = 2000.0;
	private static final Logger log = Logger.getLogger(SingaporeLegModeChange.class);
	private TaxiUtils taxiUtils;
	private SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup;
	private PermissibleModesCalculator permissibleModesCalculator;
		
	private final Random rng;

	/**
	 * @param possibleModes
	 * @param rng The random number generator used to draw random numbers to select another mode.
	 * @see TransportMode
	 * @see MatsimRandom
	 */
	public SingaporeLegModeChange(final String[] possibleModes, final Random rng, Population population, 
			TaxiUtils taxiUtils, SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup) {
		
		this.possibleModes = possibleModes.clone(); // here get the modes from change leg NOT subtour mode choice!
		this.rng = rng;
		this.taxiUtils = taxiUtils;
		this.subtourModeChoiceConfigGroup = subtourModeChoiceConfigGroup;
		log.info("Replanning for population of size: " + population.getPersons().size());
		
		this.permissibleModesCalculator =
				new SingaporePermissibleModesCalculator(
						population,
						possibleModes);
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
				
		Collection<String> permissibleModes = this.permissibleModesCalculator.getPermissibleModes(plan);
		
		if (!this.ignoreCarAvailability) {
			forbidCar = !(permissibleModes.contains(TransportMode.car));
			forbidPassenger = !(permissibleModes.contains("passenger"));
		}
		
		forbidOther = !(permissibleModes.contains(TransportMode.other));
		forbidSchoolbus = !(permissibleModes.contains("schoolbus"));

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
		
		String oldMode = leg.getMode();
		leg.setMode(newMode);
		
		if (!oldMode.equals("taxi") && newMode.equals("taxi")) {
			this.handleTaxi(plan, leg, 1);
		}
		if (oldMode.equals("taxi") && !newMode.equals("taxi")) {
			this.handleTaxi(plan, leg, -1);
		}
		
	}

	private String chooseModeOtherThan(final String currentMode, final boolean forbidCar, final boolean forbidPassenger, final boolean forbidWalk, final boolean forbidOther, final boolean forbidSchoolbus) {
		String newMode;
		int cnt = 0;
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
			if (this.allConstraintsRespected(newMode, forbidCar, forbidPassenger, forbidWalk, forbidOther, forbidSchoolbus)) {
				break;
			} 
			cnt++;
			if (this.possibleModes.length == 2 || cnt >= this.possibleModes.length) {
				newMode = currentMode; // there is no other mode available
				break;
			}		
		}
		return newMode;
	}
	
	private boolean allConstraintsRespected(String newMode, final boolean forbidCar, final boolean forbidPassenger, final boolean forbidWalk, final boolean forbidOther, final boolean forbidSchoolbus) {
		if (forbidCar && TransportMode.car.equals(newMode)) return false;
		if (forbidPassenger && "passenger".equals(newMode)) return false;
		if (forbidWalk && TransportMode.walk.equals(newMode)) return false;
		if (forbidOther && TransportMode.other.equals(newMode)) return false;
		if (forbidSchoolbus && "schoolbus".equals(newMode)) return false;
		return true;
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
				tmpPlan.addActivity(currentActivity);
				
				if (nextLeg == leg) {	
					Coord coord = currentActivity.getCoord();
					int hour = (int)(currentActivity.getEndTime() / 3600.0);
					double taxiWaitTime = this.taxiUtils.getWaitingTime(coord, hour);
					double taxiWaitEndTime = currentActivity.getEndTime() + taxiWaitTime;
					
					Leg taxiWalkLeg = new LegImpl(TaxiUtils.taxi_walk);
					taxiWalkLeg.setDepartureTime(currentActivity.getEndTime());
					taxiWalkLeg.setTravelTime(0.0);
					tmpPlan.addLeg(taxiWalkLeg);
										
					Activity taxiWaitAct = new ActivityImpl(TaxiUtils.wait4Taxi, currentActivity.getCoord(), currentActivity.getLinkId());					
					taxiWaitAct.setEndTime(taxiWaitEndTime);	
					tmpPlan.addActivity(taxiWaitAct);
				}
				
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
