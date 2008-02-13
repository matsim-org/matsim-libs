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
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import org.matsim.basic.v01.BasicAct;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;

import playground.balmermi.census2000.data.Persons;


public class PersonModeChoiceModel extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String RIDE = "ride";
	private static final String PT = "pt";
	private static final String CAR = "car";
	private static final String BIKE = "bike";
	private static final String WALK = "walk";
	private static final String E = "e";
	private static final String W = "w";
	private static final String S = "s";
	private static final String H = "h";
	private static final Coord ZERO = new Coord(0.0,0.0);
	private final Persons persons;
	private ModelModeChoice model;
	//ArrayList<List<Integer>> subToursRegister = new 
	//private List<Integer> modeSubTours = null;
	 
		
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
	
	private final void handleSubTours(final Plan plan, final TreeMap<Integer, ArrayList<Integer>> registerSubTours) {
		
		// setting subtour parameters
		if (plan == null) { Gbl.errorMsg("Person id=" + plan.getPerson().getId() + "does not have a selected plan."); }
		Iterator<BasicAct> act_it = plan.getIteratorAct();
		CoordI home_coord = null;
		CoordI work_coord = null;
		//act_it.hasNext(); // first act is always 'home'
		while (act_it.hasNext()) {
			Act act = (Act)act_it.next();
			if (H.equals(act.getType())) { home_coord = act.getCoord(); System.out.println(home_coord); }
			else if (W.equals(act.getType())) { work_coord = act.getCoord(); }
		}
		double dist_h_w = 0.0;
		System.out.println(home_coord);
		if ((home_coord == null) || (home_coord.equals(ZERO))) { Gbl.errorMsg("No home coord defined!"); }
		if ((work_coord != null) && (work_coord.equals(ZERO))) { Gbl.errorMsg("Weird work coord defined!!!"); }
		if (work_coord != null) { dist_h_w = work_coord.calcDistance(home_coord); }
		
		//CoordI home = plan.getFirstActivity().getCoord(); //FORSE IL PROBLEMA VIENE DA QUI VERIFICARE!!!!!
		// TODO check if it is an homebased tour { Gbl.errorMsg("This should never happen!"); 
		// calculate the SubTour distance (in 1 Km bins)
		for (int i=0; i<registerSubTours.size(); i=i+1) {
			List<Integer> subtour = registerSubTours.get(i);
			int mainpurpose = 3; //mainpurpose:  0 := work; 1 := edu; 2 := shop 3:=leisure
			double d = 0.0;
			CoordI start = ((Act)plan.getActsLegs().get(subtour.get(0))).getCoord();
			CoordI prev = start;
			String type = null;
			for (int k=0; k<subtour.size(); k=k+1) { // TODO Verificare da dove deve partire!!!!!!!!
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
			
			// Defining urban degree for the starting point of the subtour
			
			
			// Defining previous mode
			
			// TODO check also if in the tour the person is passing again from home. 
			//In that case the agent is allowed to use any of the modes.
			TreeMap<Integer, Integer> modeSubTours = new TreeMap<Integer, Integer>();
			int prev_mode = 0;
			if (start == home_coord) {}
			else {
				for (int j=0; j<registerSubTours.size(); j=j+1) {
					if (registerSubTours.get(j).contains(start)) {
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
			System.out.println(dist_h_w);
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
			model.setPrevMode(prev_mode); //TODO rivedere il posizionamento del'inizializzazione del prev_mode
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
			for (int k=0; k<subtour.size(); k=k+1){
				((Act)plan.getActsLegs().get(subtour.get(k))).setType(mode);
			}
		}
	}
	
	private final void removeSubTour (Plan plan, TreeMap<Integer, List<Integer>> subtours, int start, int end,int subtour_idx) {
		for (int i=start+2; i<=end; i=i+2){
			plan.removeAct(i);
			//Cambiare!!!! Così non funziona sicuramente perchè 
			//non tornanon piü i numeri di act_leg, la soluzione
			// migliore probabilmente è creare un'altro oggetto
			// fin dall'inizio e non usare i plan
		}
		
		extractSubTours3(plan, 0, plan.getActsLegs().size()-1, subtour_idx, subtours);
	}
	private final void registerSubTour2 (Plan plan, TreeMap<Integer, List<Integer>> subtours, int start, int end,int subtour_idx) {
		List<Integer> l = null;
		for (int i=start; i<=end; i=i+2){
			 l.add(i);
		}
		subtours.put(subtour_idx,l);
		removeSubTour(plan, subtours,start, end, subtour_idx);
	}
		
	private	final void extractSubTours3(Plan plan,int start, int end,int subtour_idx, TreeMap<Integer,List<Integer>> subtours) {
		boolean is_leaf = true;
		for (int i=start;i<end;i=i+2){
			Act acti = (Act)plan.getActsLegs().get(i);
			for (int j=start+2;j<end;j=j+2){
				Act actj = (Act)plan.getActsLegs().get(j);
				if ((acti.getCoord().getX() == actj.getCoord().getX()) &&
					    (acti.getCoord().getY() == actj.getCoord().getY())) {
					is_leaf = false;
					// subtour found: start=i & end=j
					subtour_idx++;
					extractSubTours3(plan, i, j, subtour_idx, subtours);
				}
			}
		}
		if (is_leaf) {
			registerSubTour2(plan, subtours, start,end, subtour_idx);
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		//playground.balmermi.census2000.data.Person p = this.persons.getPerson(Integer.parseInt(person.getId().toString()));
		
		Plan plan = person.getSelectedPlan();
		int subtour_idx =0;
		int sub_order =0;
		TreeMap<Integer, List<Integer>> subtours = new TreeMap<Integer,List<Integer>>();
		//this.extractSubTours2(plan,0,plan.getActsLegs().size()-1, subtour_idx);
		this.extractSubTours3(plan,0,plan.getActsLegs().size()-1,subtour_idx,subtours);
		//this.extractSubTours(plan,0,plan.getActsLegs().size()-1,0,subtours);
		
		
		// register subtours
		// handle subtours
	}
	
	public void run(Plan plan){
	}
}

