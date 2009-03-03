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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.ActivityOption;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;
import org.matsim.world.Zone;

import playground.balmermi.census2000.data.Municipalities;
import playground.balmermi.census2000v2.data.CAtts;
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

	private static final String YES = "yes";
	private static final String ALWAYS = "always";

	private static final String H = "h";
	private static final String E = "e";
	private static final String W = "w";
	private static final String S = "s";

	private FileWriter fw = null;
	private BufferedWriter out = null;

	private final PlanAnalyzeSubtours past = new PlanAnalyzeSubtours();
	private final Municipalities municipalities;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonAssignModeChoiceModel(final Municipalities municipalities, String outfile) {
		log.info("    init " + this.getClass().getName() + " module...");
		this.municipalities = municipalities;
		try {
			fw = new FileWriter(outfile);
			out = new BufferedWriter(fw);
			out.write("pid\tsex\tage\tlicense\tcar_avail\temployed\ttickets\thomex\thomey\t");
			out.write("subtour_id\tsubtour_purpose\tprev_subtour_mode\tsubtour_mode\t");
			out.write("subtour_startx\tsubtour_starty\tsubtour_startudeg\tsubtour_distance\tsubtour_trips\t");
			out.write("subtour_starttime\tsubtour_endtime\tsubtour_zoneid\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
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
		if (leg.getMode().equals(BasicLeg.Mode.car)) { return 0; }
		else if (leg.getMode().equals(BasicLeg.Mode.pt)) { return 1; }
		else if (leg.getMode().equals(BasicLeg.Mode.ride)) { return 2; }
		else if (leg.getMode().equals(BasicLeg.Mode.bike)) { return 3; }
		else if (leg.getMode().equals(BasicLeg.Mode.walk)) { return 4; }
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
			ArrayList<ActivityOption> prim_acts = new ArrayList<ActivityOption>();
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
			int prev_mode = this.getPrevMode(act_indices.get(0),p);
			model.setPrevMode(prev_mode);
			// home_coord; //Coordinates of the home facility of the agent
			model.setHomeCoord(h_coord);
			// ride; // states if a car lift is possible, to avoid too much ride instead of pt, to check the reason it works like this
			model.setRide(this.isRidePossible(person));
			// pt; // pt possible
			model.setPt(this.isPtPossible(person));
			// END SET

			// CALC mode
			int modechoice = model.calcModeChoice();
			BasicLeg.Mode mode = null;
			if (modechoice == 0) { mode = BasicLeg.Mode.car; }
			else if (modechoice == 1) { mode = BasicLeg.Mode.pt; }
			else if (modechoice == 2) { mode = BasicLeg.Mode.ride; }
			else if (modechoice == 3) { mode = BasicLeg.Mode.bike; }
			else if (modechoice == 4) { mode = BasicLeg.Mode.walk; }
			else { Gbl.errorMsg("pid="+person.getId()+": modechoice="+modechoice+" knot known!"); }

			// SET the mode for the legs of the subtour
			for (int j=0; j<leg_indices.size(); j++) {
				Leg l = (Leg)p.getActsLegs().get(leg_indices.get(j));
				l.setMode(mode);
			}
			
			// write a line
			try {
				int pid = Integer.parseInt(person.getId().toString());
				out.write(pid+"\t");
				out.write(person.getSex()+"\t");
				out.write(person.getAge()+"\t");
				out.write(person.getLicense()+"\t");
				out.write(person.getCarAvail()+"\t");
				out.write(person.getEmployed()+"\t");
				if (person.getTravelcards() != null) {  out.write("yes\t"); }
				else {  out.write("no\t"); }
				out.write(h_coord.getX()+"\t");
				out.write(h_coord.getY()+"\t");
				int subtourid = pid*100+i;
				out.write(subtourid+"\t");
				out.write(mainpurpose+"\t");
				if (prev_mode == 0) { out.write(BasicLeg.Mode.car.toString() + "\t"); }
				else if (prev_mode == 1) { out.write(BasicLeg.Mode.pt.toString() + "\t"); }
				else if (prev_mode == 2) { out.write(BasicLeg.Mode.ride.toString() + "\t"); }
				else if (prev_mode == 3) { out.write(BasicLeg.Mode.bike.toString() + "\t"); }
				else if (prev_mode == 4) { out.write(BasicLeg.Mode.walk.toString() + "\t"); }
				else if (prev_mode == -1) { out.write(BasicLeg.Mode.undefined.toString() + "\t"); }
				else { Gbl.errorMsg("pid="+person.getId()+": prev_mode="+prev_mode+" knot known!"); }
				out.write(mode.toString()+"\t");
				Act st_startact = (Act)person.getSelectedPlan().getActsLegs().get(act_indices.get(0));
				Act st_endact = (Act)person.getSelectedPlan().getActsLegs().get(act_indices.get(act_indices.size()-1));
				Coord start_coord = st_startact.getFacility().getCenter();
				Zone zone = (Zone)st_startact.getFacility().getUpMapping().values().iterator().next();
				out.write(start_coord.getX()+"\t");
				out.write(start_coord.getY()+"\t");
				out.write(this.getUrbanDegree(act_indices,p)+"\t");
				out.write(this.calcTourDistance(act_indices,p)+"\t");
				out.write(leg_indices.size()+"\t");
				out.write(st_startact.getEndTime()+"\t");
				out.write(st_endact.getStartTime()+"\t");
				out.write(zone.getId()+"\n");
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
	
	public void run(Plan plan) {
	}
	
	//////////////////////////////////////////////////////////////////////
	// close method
	//////////////////////////////////////////////////////////////////////

	public final void close() {
		try {
			out.flush();
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
