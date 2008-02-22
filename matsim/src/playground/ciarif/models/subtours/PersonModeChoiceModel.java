/* *********************************************************************** *
 * project: org.matsim.*
 * PersonModeChoiceModel.java
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

package playground.ciarif.models.subtours;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.vecmath.Vector2d;

import org.matsim.basic.v01.BasicAct;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;

import playground.balmermi.census2000.data.Persons;
import sun.misc.Cleaner;


public class PersonModeChoiceModel extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String RIDE = "ride";
	private static final String PT = "pt";
	private static final String CAR = "car";
	private static final String BIKE = "bike";
	private static final String WALK = "walk";
	private static final String E = "education";
	private static final String W = "work";
	private static final String S = "shop";
	private static final String H = "home";
	private static final Coord ZERO = new Coord(0.0,0.0);
	private final Persons persons;
	private ModelModeChoice model;
	 
		
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonModeChoiceModel(final Persons persons) {
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.persons = persons;
		System.out.println("    done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////
	
	private final void handleSubTours(final Plan plan, final TreeMap<Integer, ArrayList<Integer>> subtours, int subtour_idx) {
		
		// setting subtour parameters
		if (plan == null) { Gbl.errorMsg("Person id=" + plan.getPerson().getId() + "does not have a selected plan."); }
		Iterator<BasicAct> act_it = plan.getIteratorAct();
		CoordI home_coord = null;
		CoordI work_coord = null;
		//act_it.hasNext();
		double dist_h_w = 0.0;
		while (act_it.hasNext()) {
			Act act = (Act)act_it.next();
			if (H.equals(act.getType())) { home_coord = act.getCoord();}
			else if (W.equals(act.getType())) { work_coord = act.getCoord(); }
		}
		if ((home_coord == null) || (home_coord.equals(ZERO))) { Gbl.errorMsg("No home coord defined!"); }
		if ((work_coord != null) && (work_coord.equals(ZERO))) { Gbl.errorMsg("Weird work coord defined!!!"); }
		if (work_coord != null) { dist_h_w = work_coord.calcDistance(home_coord); }
		TreeMap<Integer, Integer> modeSubTours = new TreeMap<Integer, Integer>();
		
		for (int i=subtour_idx-1; i>=0; i=i-1) {
			ArrayList<Integer> subtour = subtours.get(i);
			int mainpurpose = 3; //mainpurpose:  0 := work; 1 := edu; 2 := shop 3:=leisure
			double d = 0.0;
			CoordI start = ((Act)plan.getActsLegs().get(subtour.get(0))).getCoord();
			CoordI prev = start;
			String type = null;
			for (int k=1; k<subtour.size()-1; k=k+1) { 
				type = ((Act)plan.getActsLegs().get(subtour.get(k))).getType();
				if (mainpurpose == 1){
					if (type == W) { mainpurpose = 0; break; }
				}
				else if (mainpurpose == 2) {
					if (type == W) { mainpurpose = 0; break; }
					else if (type == E) { mainpurpose = 1; }
				}
				else if (mainpurpose == 3) {
					if (type == W) {mainpurpose = 0; break; }
					else if (type == E) {mainpurpose = 1; break;}
					else if (type == S) {mainpurpose = 2;}
				} 
				CoordI curr = ((Act)plan.getActsLegs().get(subtour.get(k))).getCoord();
				d = d + curr.calcDistance(prev);
				prev = curr;
			}
			d = d/1000.0;
			
			// Defining previous mode
			int prev_mode = 0;
			if (subtour.get(0) != 0) {
				for (int j=subtours.size()-1; j>=0; j=j-1) {
					if (subtours.get(j).contains(subtour.get(0))) {
						prev_mode = modeSubTours.get(j); break;
					}
				}	
			}
			
			// choose mode choice model based on main purpose
			if (plan.getPerson().getAge() >=18) {
				if (mainpurpose == 0) {model = new ModelModeChoiceWork18Plus();}
				else if (mainpurpose == 1) {model = new ModelModeChoiceEducation18Plus();}
				else if (mainpurpose == 2) {model = new ModelModeChoiceShop18Plus();}
				else if (mainpurpose == 3) {model = new ModelModeChoiceLeisure18Plus();}
				else { Gbl.errorMsg("This should never happen!"); }
			}
			else {
				if (mainpurpose == 1) {model = new ModelModeChoiceEducation18Minus ();}
				else {model = new ModelModeChoiceOther18Minus ();}
			}
					
			// generating a random bike ownership (see STRC2007 paper Ciari for more details)
			boolean has_bike = true;
			if (Gbl.random.nextDouble() < 0.44) { has_bike = false; }	
			
			
			// setting person parameters
			System.out.println(model);
			model.setDistanceHome2Work(dist_h_w);
			model.setAge(plan.getPerson().getAge());
			//model.setHHDimension(p.getHousehold().getPersonCount());
			model.setLicenseOwnership(plan.getPerson().hasLicense());
			model.setCar(plan.getPerson().getCarAvail());
			model.setTickets(plan.getPerson().getTravelcards());
			model.setLicenseOwnership(plan.getPerson().hasLicense());
			model.setBike(has_bike);
			model.setMale (plan.getPerson().getSex());
			int udeg = 3; // TODO The program should crash here, now only an initial value is given. afterwards something like that should replace it: int udeg = start.getMunicipality().getRegType();
			model.setUrbanDegree(udeg);
			model.setMainPurpose(mainpurpose);
			model.setDistanceTour(d); // model needs meters! TODO check dimensions of distances!!!!
			model.setPrevMode(prev_mode);
			model.setHomeCoord(home_coord);
			// getting the chosen mode
			int modechoice = model.calcModeChoice();
			String mode = null;
			if (modechoice == 0) { mode = CAR; }
			else if (modechoice == 1) { mode = PT; }
			else if (modechoice == 2) { mode = RIDE; }
			else if (modechoice == 3) { mode = BIKE; }
			else if (modechoice == 4) { mode = WALK; }
			else { Gbl.errorMsg("Mode choice returns undefined value!"); }
			
			modeSubTours.put(i,modechoice);
			System.out.println("mode sub tour = " + modeSubTours);
			System.out.println("prev_mode = " + prev_mode);
			System.out.println("modechoice " +  i + " = " + modechoice);
			System.out.println("subtour= " + subtour);
			
			for (int k=1; k<subtour.size(); k=k+1){
				((Leg)plan.getActsLegs().get(subtour.get(k)-1)).setMode(mode);
				System.out.println("leg = " + ((Leg)plan.getActsLegs().get(subtour.get(k)-1)));
			}
		}
	}
	 private final boolean checkLeafs (Plan plan,TreeMap<Integer, ArrayList<Integer>> subtours, int subtour_idx) {
		boolean last_leaf = false;
		ArrayList<Integer> last_subtour = new ArrayList<Integer>();
		last_subtour = subtours.get(subtour_idx);
		if (last_subtour.contains(plan.getActsLegs().size()-1) && last_subtour.contains(0)){
			last_leaf = true;
		}
		return last_leaf;
	}
	 
	 private final ArrayList<Integer> removeSubTour (int start, int end,ArrayList<Integer> tour) {
			for (int i=0; i<(end-start); i=i+1){
				tour.remove(start+1);
				System.out.println("tour " + tour);
			}
			return tour;
	 }
	
	private final TreeMap<Integer, ArrayList<Integer>> registerSubTour (Plan plan, ArrayList<Integer> start_end, ArrayList<Integer> tour, int subtour_idx, TreeMap<Integer, ArrayList<Integer>> subtours){
		ArrayList<Integer> subtour = new ArrayList<Integer>();
		int start = start_end.get(0);
		int end = start_end.get(1);
		for (int i=start; i<=end; i=i+1){
			 subtour.add(tour.get(i));
		}
		subtours.put(subtour_idx,subtour);
		return subtours;
	}
	
	private final ArrayList<Integer> extractSubTours(Plan plan, int start, int end, ArrayList<Integer> tour) {
		boolean is_leaf = false;
		int i=0;
		int leaf_start = start;
		int leaf_end = end;
		TreeMap<Integer,Act> acts = new	TreeMap<Integer,Act>();
		Act act0 = ((Act)plan.getActsLegs().get(tour.get(start)));
		acts.put(0,act0);
		while (is_leaf == false && i<=tour.size()-2){
			i=i+1;
			Act acti = ((Act)plan.getActsLegs().get(tour.get(i)));
			for (int j=i-1;j>=tour.get(start);j=j-1){
				Act actj = (Act)plan.getActsLegs().get(tour.get(j));
				if ((acti.getCoord().getX() == actj.getCoord().getX()) &&
					    (acti.getCoord().getY() == actj.getCoord().getY())){
					is_leaf=true;
					leaf_start = j;
					leaf_end = i; 	
					break;
				}
			}
			acts.put(i, acti);
		}
		ArrayList<Integer> start_end = new ArrayList<Integer>();
		start_end.add(0,leaf_start);
		start_end.add(1,leaf_end);
		return start_end;
	}
		
	private final void registerPlan (Plan plan, ArrayList<Integer> tour) {
		for (int i=0; i<=plan.getActsLegs().size()-1;i=i+2) {
			tour.add(i);
		}
	}
		
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
				
		Plan plan = person.getSelectedPlan();
		int subtour_idx =0;
		ArrayList<Integer> tour = new ArrayList<Integer>();
		ArrayList<Integer> start_end = new ArrayList<Integer>();
		boolean all_leafs = false;		
		TreeMap<Integer, ArrayList<Integer>> subtours = new TreeMap<Integer,ArrayList<Integer>>();
		this.registerPlan (plan,tour);
		
		while (all_leafs == false){
			start_end = this.extractSubTours(plan,0, tour.size()-1,tour);
			subtours = this.registerSubTour(plan,start_end,tour,subtour_idx,subtours);
			all_leafs = this.checkLeafs(plan, subtours,subtour_idx);
			subtour_idx = subtour_idx+1;
			this.removeSubTour(start_end.get(0),start_end.get(1), tour);
		}
		System.out.println("subtours fine = " + subtours);
		this.handleSubTours(plan,subtours,subtour_idx);	
	}
	
	public void run(Plan plan){
		}
}

