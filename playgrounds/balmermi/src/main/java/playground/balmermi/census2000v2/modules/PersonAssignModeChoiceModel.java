///* *********************************************************************** *
// * project: org.matsim.*
// * PersonMobilityToolModel.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2007 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.balmermi.census2000v2.modules;
//
//import java.io.BufferedWriter;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.ArrayList;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.TransportMode;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.population.Plan;
//import org.matsim.core.api.experimental.facilities.ActivityFacilities;
//import org.matsim.core.config.groups.PlanomatConfigGroup;
//import org.matsim.core.facilities.ActivityFacilityImpl;
//import org.matsim.core.gbl.Gbl;
//import org.matsim.core.gbl.MatsimRandom;
//import org.matsim.core.population.ActivityImpl;
//import org.matsim.core.population.LegImpl;
//import org.matsim.core.population.PersonImpl;
//import org.matsim.core.utils.geometry.CoordUtils;
//import org.matsim.knowledges.Knowledges;
//import org.matsim.population.algorithms.AbstractPersonAlgorithm;
//import org.matsim.population.algorithms.PlanAlgorithm;
//import org.matsim.population.algorithms.PlanAnalyzeSubtours;
//import org.matsim.world.Zone;
//
//import playground.balmermi.census2000.data.Municipalities;
//
//public class PersonAssignModeChoiceModel extends AbstractPersonAlgorithm implements PlanAlgorithm {
//
//	//////////////////////////////////////////////////////////////////////
//	// member variables
//	//////////////////////////////////////////////////////////////////////
//
//	private final static Logger log = Logger.getLogger(PersonAssignModeChoiceModel.class);
//
//	private static final String YES = "yes";
//	private static final String ALWAYS = "always";
//
//	private static final String H = "h";
//	private static final String E = "e";
//	private static final String W = "w";
//	private static final String S = "s";
//
//	private FileWriter fw = null;
//	private BufferedWriter out = null;
//
//	private final PlanAnalyzeSubtours past;
//	private final Municipalities municipalities;
//
//	private final Knowledges knowledges;
//	private final ActivityFacilities facilities;
//
//	//////////////////////////////////////////////////////////////////////
//	// constructors
//	//////////////////////////////////////////////////////////////////////
//
//	public PersonAssignModeChoiceModel(final Municipalities municipalities, String outfile, Knowledges knowledges, final ActivityFacilities facilities, final PlanomatConfigGroup planomatConfig) {
//		log.info("    init " + this.getClass().getName() + " module...");
//		this.past = new PlanAnalyzeSubtours();
//		this.past.setTripStructureAnalysisLayer(planomatConfig.getTripStructureAnalysisLayer());
//		this.municipalities = municipalities;
//		this.knowledges = knowledges;
//		this.facilities = facilities;
//		try {
//			fw = new FileWriter(outfile);
//			out = new BufferedWriter(fw);
//			out.write("pid\tsex\tage\tlicense\tcar_avail\temployed\ttickets\thomex\thomey\t");
//			out.write("subtour_id\tsubtour_purpose\tprev_subtour_mode\tsubtour_mode\t");
//			out.write("subtour_startx\tsubtour_starty\tsubtour_startudeg\tsubtour_distance\tsubtour_trips\t");
//			out.write("subtour_starttime\tsubtour_endtime\tsubtour_zoneid\n");
//			out.flush();
//		} catch (IOException e) {
//			e.printStackTrace();
//			System.exit(-1);
//		}
//		log.info("    done.");
//	}
//
//	//////////////////////////////////////////////////////////////////////
//	// private methods
//	//////////////////////////////////////////////////////////////////////
//
//	private final boolean isRidePossible(PersonImpl person) {
//		double r = MatsimRandom.getRandom().nextDouble();
//		if (!person.hasLicense()) {
//			if (r < 0.54) { return true; }
//			else { return false; }
//		}
//		else { return false; }
//	}
//
//	//////////////////////////////////////////////////////////////////////
//
//	private final boolean isPtPossible(PersonImpl person) {
//		double r = MatsimRandom.getRandom().nextDouble();
//		if (person.getCarAvail().equals(ALWAYS)) {
//			if (r < 0.40) { return true; }
//			else  { return false; }
//		}
//		else if (r < 0.90) { return true; }
//		else { return false; }
//	}
//
//	private final int getPrevMode(int s_act_idx, Plan p) {
//		// prev_mode; // 0= car; 1= Pt; 2= Car passenger; 3= Bike; 4= Walk; -1: subtour is starting from home;
//		ActivityImpl act = (ActivityImpl)p.getPlanElements().get(s_act_idx);
//		if (act.getType().startsWith(H)) { return -1; }
//		LegImpl leg = (LegImpl)p.getPlanElements().get(s_act_idx-1);
//		if (leg.getMode().equals(TransportMode.car)) { return 0; }
//		else if (leg.getMode().equals(TransportMode.pt)) { return 1; }
//		else if (leg.getMode().equals(TransportMode.ride)) { return 2; }
//		else if (leg.getMode().equals(TransportMode.bike)) { return 3; }
//		else if (leg.getMode().equals(TransportMode.walk)) { return 4; }
//		else { Gbl.errorMsg("pid="+p.getPerson().getId()+": leg_mode="+leg.getMode()+" not known!"); return -2; }
//	}
//
//	//////////////////////////////////////////////////////////////////////
//
//	private final int getUrbanDegree(ArrayList<Integer> act_indices, Plan p) {
//		ActivityImpl act = (ActivityImpl)p.getPlanElements().get(act_indices.get(0));
//		Zone zone = (Zone)((ActivityFacilityImpl) this.facilities.getFacilities().get(act.getFacilityId())).getUpMapping().values().iterator().next();
//		return this.municipalities.getMunicipality(zone.getId()).getRegType();
//	}
//
//	//////////////////////////////////////////////////////////////////////
//
//	private final double calcTourDistance(ArrayList<Integer> act_indices, Plan p) {
//		double dist = 0.0;
//		for (int j=1; j<act_indices.size(); j++) {
//			ActivityImpl from_act = (ActivityImpl)p.getPlanElements().get(act_indices.get(j-1));
//			ActivityImpl to_act = (ActivityImpl)p.getPlanElements().get(act_indices.get(j));
//			dist += CoordUtils.calcDistance(this.facilities.getFacilities().get(to_act.getFacilityId()).getCoord(), this.facilities.getFacilities().get(from_act.getFacilityId()).getCoord());
//		}
//		return dist/1000.0;
//	}
//
//	//////////////////////////////////////////////////////////////////////
//
////	private final ModelModeChoice createModel(int mainpurpose, Plan p) {
////		ModelModeChoice m = null;
////		if (((PersonImpl) p.getPerson()).getAge() >= 18) {
////			if (mainpurpose == 0) { m = new ModelModeChoiceWork18Plus(); }
////			else if (mainpurpose == 1) { m = new ModelModeChoiceEducation18Plus(); }
////			else if (mainpurpose == 2) { m = new ModelModeChoiceShop18Plus(); }
////			else if (mainpurpose == 3) { m = new ModelModeChoiceLeisure18Plus(); }
////			else { Gbl.errorMsg("This should never happen!"); }
////		}
////		else {
////			if (mainpurpose == 1) { m = new ModelModeChoiceEducation18Minus (); }
////			else { m = new ModelModeChoiceOther18Minus (); }
////		}
////		return m;
////	}
//
//	//////////////////////////////////////////////////////////////////////
//
//	private final int getMainPurpose(ArrayList<Integer> act_indices, Plan p) {
//		//   GET the mainpurpose of the subtour
//		int mainpurpose = 3; // 0 := work; 1 := edu; 2 := shop 3:=leisure
//		for (int j=1; j<act_indices.size()-1; j++) {
//			ActivityImpl act = (ActivityImpl)p.getPlanElements().get(act_indices.get(j));
//			String type = act.getType().substring(0,1); // h,w,e,s,l
//			if (mainpurpose == 3) {
//				if (type.equals(H)) { mainpurpose = 0; }
//				else if (type.equals(W)) { mainpurpose = 0; }
//				else if (type.equals(E)) { mainpurpose = 1; }
//				else if (type.equals(S)) { mainpurpose = 2; }
//			}
//			else if (mainpurpose == 2) {
//				if (type.equals(H)) { mainpurpose = 0; }
//				else if (type.equals(W)) { mainpurpose = 0; }
//				else if (type.equals(E)) { mainpurpose = 1; }
//			}
//			else if (mainpurpose == 1) {
//				if (type.equals(H)) { mainpurpose = 0; }
//				else if (type.equals(W)) { mainpurpose = 0; }
//			}
//		}
//		return mainpurpose;
//	}
//
//	//////////////////////////////////////////////////////////////////////
//	// run methods
//	//////////////////////////////////////////////////////////////////////
//
//	@Override
//	public void run(Person pp) {
//		PersonImpl person = (PersonImpl) pp;
//		// GET the subtours
//		Plan p = person.getSelectedPlan();
//		past.run(p);
//		int[] subtour_leg_indices = past.getSubtourIndexation();
//
//		// GET the maximal index
//		int max_subtour_leg_indices = -1;
//		for (int i=0; i<subtour_leg_indices.length; i++) {
//			if (subtour_leg_indices[i] > max_subtour_leg_indices) { max_subtour_leg_indices = subtour_leg_indices[i]; }
//		}
//
//		// FOR each subtour index
//		for (int i=max_subtour_leg_indices; i >= 0; i--) {
//
//			// GET the indices of the legs of the current subtour in the given plan
//			ArrayList<Integer> leg_indices = new ArrayList<Integer>();
//			for (int j=0; j<subtour_leg_indices.length; j++) {
//				if (subtour_leg_indices[j] == i) { leg_indices.add(2*j+1); }
//			}
//
//			// GET the indices of the acts of the current subtour in the given plan
//			ArrayList<Integer> act_indices = new ArrayList<Integer>();
//			act_indices.add(leg_indices.get(0)-1);
//			for (int j=0; j<leg_indices.size(); j++) {
//				act_indices.add(leg_indices.get(j)+1);
//			}
//
//			// CREATE the model
//			int mainpurpose = this.getMainPurpose(act_indices,p);
//			throw new RuntimeException("ModeChoiceModel deactivated, see Source Code.");
//			/* I (mrieser) had to disable the inclusion of the mode choice model in this place
//			 * because of a circular dependency between the playgrounds of balmermi and ciarif.
//			 */
//			/* ***** disable mode choice -- begin ****
//			ModelModeChoice model = this.createModel(mainpurpose,p);
//
//			// SET variables
//			// age; // 0-[unlimited]
//			model.setAge(person.getAge());
//			// udeg; // degree of urbanization [2-5] (1=urbanized=reference)
//			model.setUrbanDegree(this.getUrbanDegree(act_indices,p));
//			// license; // yes = 1; no = 0;
//			if (person.getLicense().equals(YES)) { model.setLicenseOwnership(true); } else { model.setLicenseOwnership(false); }
//			// dist_subtour; // distance of the sub-tour (in kilometers)
//			model.setDistanceTour(this.calcTourDistance(act_indices,p));
//			// dist_h_w; // distance between home and work or education facility (in km)
//			Coord h_coord = this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_HOME).get(0).getFacility().getCoord();
//			ArrayList<ActivityOptionImpl> prim_acts = new ArrayList<ActivityOptionImpl>();
//			prim_acts.addAll(this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_W2));
//			prim_acts.addAll(this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_W3));
//			prim_acts.addAll(this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_EKIGA));
//			prim_acts.addAll(this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_EPRIM));
//			prim_acts.addAll(this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_ESECO));
//			prim_acts.addAll(this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_EHIGH));
//			prim_acts.addAll(this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_EOTHR));
//			if (prim_acts.isEmpty()) { model.setDistanceHome2Work(0.0); }
//			else {
//				Coord p_coord = prim_acts.get(MatsimRandom.getRandom().nextInt(prim_acts.size())).getFacility().getCoord();
//				model.setDistanceHome2Work(CoordUtils.calcDistance(h_coord, p_coord)/1000.0);
//			}
//			// tickets; // holds some kind of season tickets
//			model.setTickets(person.getTravelcards());
//			// purpose; // main purpose of the tour (Work = 0, Education = 1, Shop=2, leis=3)
//			model.setMainPurpose(mainpurpose);
//			// car; // availability of car (Always, Sometimes, Never)
//			model.setCar(person.getCarAvail());
//			// male; // 0-[unlimited]
//			model.setMale(person.getSex());
//			// bike; // bike ownership
//			if (MatsimRandom.getRandom().nextDouble() < 0.54) { model.setBike(true); } else { model.setBike(false); }
//			// prev_mode; // 0= car; 1= Pt; 2= Car passenger; 3= Bike; 4= Walk; -1: subtour is starting from home;
//			int prev_mode = this.getPrevMode(act_indices.get(0),p);
//			model.setPrevMode(prev_mode);
//			// home_coord; //Coordinates of the home facility of the agent
//			model.setHomeCoord(h_coord);
//			// ride; // states if a car lift is possible, to avoid too much ride instead of pt, to check the reason it works like this
//			model.setRide(this.isRidePossible(person));
//			// pt; // pt possible
//			model.setPt(this.isPtPossible(person));
//			// END SET
//
//			// CALC mode
//			int modechoice = model.calcModeChoice();
//			TransportMode mode = null;
//			if (modechoice == 0) { mode = TransportMode.car; }
//			else if (modechoice == 1) { mode = TransportMode.pt; }
//			else if (modechoice == 2) { mode = TransportMode.ride; }
//			else if (modechoice == 3) { mode = TransportMode.bike; }
//			else if (modechoice == 4) { mode = TransportMode.walk; }
//			else { Gbl.errorMsg("pid="+person.getId()+": modechoice="+modechoice+" knot known!"); }
//			// SET the mode for the legs of the subtour
//			for (int j=0; j<leg_indices.size(); j++) {
//				LegImpl l = (LegImpl)p.getPlanElements().get(leg_indices.get(j));
//				l.setMode(mode);
//			}
//
//			// write a line
//			try {
//				int pid = Integer.parseInt(person.getId().toString());
//				out.write(pid+"\t");
//				out.write(person.getSex()+"\t");
//				out.write(person.getAge()+"\t");
//				out.write(person.getLicense()+"\t");
//				out.write(person.getCarAvail()+"\t");
//				out.write(person.getEmployed()+"\t");
//				if (person.getTravelcards() != null) {  out.write("yes\t"); }
//				else {  out.write("no\t"); }
//				out.write(h_coord.getX()+"\t");
//				out.write(h_coord.getY()+"\t");
//				int subtourid = pid*100+i;
//				out.write(subtourid+"\t");
//				out.write(mainpurpose+"\t");
//				if (prev_mode == 0) { out.write(TransportMode.car.toString() + "\t"); }
//				else if (prev_mode == 1) { out.write(TransportMode.pt.toString() + "\t"); }
//				else if (prev_mode == 2) { out.write(TransportMode.ride.toString() + "\t"); }
//				else if (prev_mode == 3) { out.write(TransportMode.bike.toString() + "\t"); }
//				else if (prev_mode == 4) { out.write(TransportMode.walk.toString() + "\t"); }
//				else if (prev_mode == -1) { out.write(TransportMode.undefined.toString() + "\t"); }
//				else { Gbl.errorMsg("pid="+person.getId()+": prev_mode="+prev_mode+" knot known!"); }
//				out.write(mode.toString()+"\t");
//				ActivityImpl st_startact = (ActivityImpl)person.getSelectedPlan().getPlanElements().get(act_indices.get(0));
//				ActivityImpl st_endact = (ActivityImpl)person.getSelectedPlan().getPlanElements().get(act_indices.get(act_indices.size()-1));
//				Coord start_coord = st_startact.getFacility().getCoord();
//				Zone zone = (Zone)((ActivityFacilityImpl) st_startact.getFacility()).getUpMapping().values().iterator().next();
//				out.write(start_coord.getX()+"\t");
//				out.write(start_coord.getY()+"\t");
//				out.write(this.getUrbanDegree(act_indices,p)+"\t");
//				out.write(this.calcTourDistance(act_indices,p)+"\t");
//				out.write(leg_indices.size()+"\t");
//				out.write(st_startact.getEndTime()+"\t");
//				out.write(st_endact.getStartTime()+"\t");
//				out.write(zone.getId()+"\n");
//			} catch (Exception e) {
//				e.printStackTrace();
//				System.exit(-1);
//			}
//		 ********** disable mode choice model -- end *********** */
//
//		}
//	}
//
//	public void run(Plan plan) {
//	}
//
//	//////////////////////////////////////////////////////////////////////
//	// close method
//	//////////////////////////////////////////////////////////////////////
//
//	public final void close() {
//		try {
//			out.flush();
//			out.close();
//			fw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//			System.exit(-1);
//		}
//	}
//}
