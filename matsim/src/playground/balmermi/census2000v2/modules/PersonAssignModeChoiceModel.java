/* *********************************************************************** *
 * project: org.matsim.*
 * PersonMobilityToolModel.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.balmermi.census2000v2.modules;

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.facilities.Activity;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;
import org.matsim.utils.geometry.Coord;
import org.matsim.world.Zone;

import playground.balmermi.census2000.data.Municipalities;
import playground.balmermi.census2000v2.data.CAtts;
import playground.balmermi.census2000v2.data.Household;
import playground.ciarif.models.subtours.ModelModeChoice;
import playground.ciarif.models.subtours.ModelModeChoiceEducation18Minus;
import playground.ciarif.models.subtours.ModelModeChoiceEducation18Plus;
import playground.ciarif.models.subtours.ModelModeChoiceLeisure18Plus;
import playground.ciarif.models.subtours.ModelModeChoiceOther18Minus;
import playground.ciarif.models.subtours.ModelModeChoiceShop18Plus;
import playground.ciarif.models.subtours.ModelModeChoiceWork18Plus;

public class PersonAssignModeChoiceModel extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PersonAssignModeChoiceModel.class);

	private static final Integer MAXNUMP = 14;
	private static final String YES = "yes";
	private static final String ALWAYS = "always";

	private static final String H = "h";
	private static final String E = "e";
	private static final String W = "w";
	private static final String S = "s";

	private static final String RIDE = "ride";
	private static final String PT = "pt";
	private static final String CAR = "car";
	private static final String BIKE = "bike";
	private static final String WALK = "walk";

	private final PlanAnalyzeSubtours past = new PlanAnalyzeSubtours();
	private final Municipalities municipalities;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonAssignModeChoiceModel(final Municipalities municipalities) {
		log.info("    init " + this.getClass().getName() + " module...");
		this.municipalities = municipalities;
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final boolean isRidePossible(Person person) {
		double r = MatsimRandom.random.nextDouble();
		if (!person.hasLicense()) {
			if (r < 0.54) { return true; }
			else { return false; }
		}
		else { return false; }
	}
	
	//////////////////////////////////////////////////////////////////////

	private final boolean isPtPossible(Person person) {
		double r = MatsimRandom.random.nextDouble();
		if (person.getCarAvail().equals(ALWAYS)) {
			if (r < 0.40) { return true; }
			else  { return false; }
		}
		else if (r < 0.90) { return true; }
		else { return false; }
	}
	
	private final int getPrevMode(int s_act_idx, Plan p) {
		// prev_mode; // 0= car; 1= Pt; 2= Car passenger; 3= Bike; 4= Walk; -1: subtour is starting from home;
		Act act = (Act)p.getActsLegs().get(s_act_idx);
		if (act.getType().startsWith(H)) { return -1; }
		Leg leg = (Leg)p.getActsLegs().get(s_act_idx-1);
		if (leg.getMode().equals(CAR)) { return 0; }
		else if (leg.getMode().equals(PT)) { return 1; }
		else if (leg.getMode().equals(RIDE)) { return 2; }
		else if (leg.getMode().equals(BIKE)) { return 3; }
		else if (leg.getMode().equals(WALK)) { return 4; }
		else { Gbl.errorMsg("pid="+p.getPerson().getId()+": leg_mode="+leg.getMode()+" not known!"); return -2; }
	}
	
	//////////////////////////////////////////////////////////////////////

	private final int getUrbanDegree(ArrayList<Integer> act_indices, Plan p) {
		Act act = (Act)p.getActsLegs().get(act_indices.get(0));
		Zone zone = (Zone)act.getFacility().getUpMapping().values().iterator().next();
		return this.municipalities.getMunicipality(zone.getId()).getRegType();
	}
	
	//////////////////////////////////////////////////////////////////////

	private final double calcTourDistance(ArrayList<Integer> act_indices, Plan p) {
		double dist = 0.0;
		for (int j=1; j<act_indices.size(); j++) {
			Act from_act = (Act)p.getActsLegs().get(act_indices.get(j-1));
			Act to_act = (Act)p.getActsLegs().get(act_indices.get(j));
			dist += to_act.getFacility().getCenter().calcDistance(from_act.getFacility().getCenter());
		}
		return dist/1000.0;
	}

	//////////////////////////////////////////////////////////////////////

	private final ModelModeChoice createModel(int mainpurpose, Plan p) {
		ModelModeChoice m = null;
		if (p.getPerson().getAge() >= 18) {
			if (mainpurpose == 0) { m = new ModelModeChoiceWork18Plus(); }
			else if (mainpurpose == 1) { m = new ModelModeChoiceEducation18Plus(); }
			else if (mainpurpose == 2) { m = new ModelModeChoiceShop18Plus(); }
			else if (mainpurpose == 3) { m = new ModelModeChoiceLeisure18Plus(); }
			else { Gbl.errorMsg("This should never happen!"); }
		}
		else {
			if (mainpurpose == 1) { m = new ModelModeChoiceEducation18Minus (); }
			else { m = new ModelModeChoiceOther18Minus (); }
		}
		return m;
	}
	
	//////////////////////////////////////////////////////////////////////

	private final int getMainPurpose(ArrayList<Integer> act_indices, Plan p) {
		//   GET the mainpurpose of the subtour
		int mainpurpose = 3; // 0 := work; 1 := edu; 2 := shop 3:=leisure
		for (int j=1; j<act_indices.size()-1; j++) {
			Act act = (Act)p.getActsLegs().get(act_indices.get(j));
			String type = act.getType().substring(0,1); // h,w,e,s,l
			if (mainpurpose == 3) {
				if (type.equals(H)) { mainpurpose = 0; }
				else if (type.equals(W)) { mainpurpose = 0; }
				else if (type.equals(E)) { mainpurpose = 1; }
				else if (type.equals(S)) { mainpurpose = 2; }
			}
			else if (mainpurpose == 2) {
				if (type.equals(H)) { mainpurpose = 0; }
				else if (type.equals(W)) { mainpurpose = 0; }
				else if (type.equals(E)) { mainpurpose = 1; }
			}
			else if (mainpurpose == 1) {
				if (type.equals(H)) { mainpurpose = 0; }
				else if (type.equals(W)) { mainpurpose = 0; }
			}
		}
		return mainpurpose;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {

		// GET the subtours
		Plan p = person.getSelectedPlan();
		past.run(p);
		int[] subtour_leg_indices = past.getSubtourIndexation();
		
		// GET the maximal index
		int max_subtour_leg_indices = -1;
		for (int i=0; i<subtour_leg_indices.length; i++) {
			if (subtour_leg_indices[i] > max_subtour_leg_indices) { max_subtour_leg_indices = subtour_leg_indices[i]; }
		}

		// FOR each subtour index
		for (int i=max_subtour_leg_indices; i >= 0; i--) {

			// GET the indices of the legs of the current subtour in the given plan
			ArrayList<Integer> leg_indices = new ArrayList<Integer>();
			for (int j=0; j<subtour_leg_indices.length; j++) {
				if (subtour_leg_indices[j] == i) { leg_indices.add(2*j+1); }
			}

			// GET the indices of the acts of the current subtour in the given plan
			ArrayList<Integer> act_indices = new ArrayList<Integer>();
			act_indices.add(leg_indices.get(0)-1);
			for (int j=0; j<leg_indices.size(); j++) {
				act_indices.add(leg_indices.get(j)+1);
			}
			
			// CREATE the model
			int mainpurpose = this.getMainPurpose(act_indices,p);
			ModelModeChoice model = this.createModel(mainpurpose,p);
			
			// SET variables
			// age; // 0-[unlimited]
			model.setAge(person.getAge());
			// udeg; // degree of urbanization [2-5] (1=urbanized=reference)
			model.setUrbanDegree(this.getUrbanDegree(act_indices,p));
			// license; // yes = 1; no = 0;
			if (person.getLicense().equals(YES)) { model.setLicenseOwnership(true); } else { model.setLicenseOwnership(false); }
			// dist_subtour; // distance of the sub-tour (in kilometers)
			model.setDistanceTour(this.calcTourDistance(act_indices,p));
			// dist_h_w; // distance between home and work or education facility (in km)
			Coord h_coord = person.getKnowledge().getActivities(CAtts.ACT_HOME).get(0).getFacility().getCenter();
			ArrayList<Activity> prim_acts = new ArrayList<Activity>();
			prim_acts.addAll(person.getKnowledge().getActivities(CAtts.ACT_W2));
			prim_acts.addAll(person.getKnowledge().getActivities(CAtts.ACT_W3));
			prim_acts.addAll(person.getKnowledge().getActivities(CAtts.ACT_EKIGA));
			prim_acts.addAll(person.getKnowledge().getActivities(CAtts.ACT_EPRIM));
			prim_acts.addAll(person.getKnowledge().getActivities(CAtts.ACT_ESECO));
			prim_acts.addAll(person.getKnowledge().getActivities(CAtts.ACT_EHIGH));
			prim_acts.addAll(person.getKnowledge().getActivities(CAtts.ACT_EOTHR));
			if (prim_acts.isEmpty()) { model.setDistanceHome2Work(0.0); }
			else {
				Coord p_coord = prim_acts.get(MatsimRandom.random.nextInt(prim_acts.size())).getFacility().getCenter();
				model.setDistanceHome2Work(h_coord.calcDistance(p_coord)/1000.0);
			}
			// tickets; // holds some kind of season tickets 
			model.setTickets(person.getTravelcards());
			// purpose; // main purpose of the tour (Work = 0, Education = 1, Shop=2, leis=3)
			model.setMainPurpose(mainpurpose);
			// car; // availability of car (Always, Sometimes, Never)
			model.setCar(person.getCarAvail());
			// male; // 0-[unlimited]
			model.setMale(person.getSex());
			// bike; // bike ownership
			if (MatsimRandom.random.nextDouble() < 0.54) { model.setBike(true); } else { model.setBike(false); }
			// prev_mode; // 0= car; 1= Pt; 2= Car passenger; 3= Bike; 4= Walk; -1: subtour is starting from home;
			model.setPrevMode(this.getPrevMode(act_indices.get(0),p));
			// home_coord; //Coordinates of the home facility of the agent
			model.setHomeCoord(h_coord);
			// ride; // states if a car lift is possible, to avoid too much ride instead of pt, to check the reason it works like this
			model.setRide(this.isRidePossible(person));
			// pt; // pt possible
			model.setPt(this.isPtPossible(person));
			// END SET

			// CALC mode
			int modechoice = model.calcModeChoice();
			String mode = null;
			if (modechoice == 0) { mode = CAR; }
			else if (modechoice == 1) { mode = PT; }
			else if (modechoice == 2) { mode = RIDE; }
			else if (modechoice == 3) { mode = BIKE; }
			else if (modechoice == 4) { mode = WALK; }
			else { Gbl.errorMsg("pid="+person.getId()+": modechoice="+modechoice+" knot known!"); }

			// SET the mode for the legs of the subtour
			for (int j=0; j<leg_indices.size(); j++) {
				Leg l = (Leg)p.getActsLegs().get(leg_indices.get(j));
				l.setMode(mode);
			}
		}
	}
	
	public void run(Plan plan) {
	}
}
