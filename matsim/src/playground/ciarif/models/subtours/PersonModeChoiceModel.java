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
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;


import org.matsim.basic.v01.BasicAct;
import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.identifiers.IdI;

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
	private  List<PersonSubtour> personSubtours;
	 
		
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonModeChoiceModel(final Persons persons) {
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.persons = persons;
		System.out.println("    done.");
		this.personSubtours = new Vector<PersonSubtour>();
	}
	
	public List<PersonSubtour> getPersonSubtours() {
		return personSubtours;
	}


	public void setPersonSubtours(List<PersonSubtour> personSubtours) {
		this.personSubtours = personSubtours;
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
			//String act_type = act.getType().substring(0,1);
			//System.out.println("type" + act_type);
			if (H.equals(act.getType().substring(0,1))) { home_coord = act.getCoord();}
			else if (W.equals(act.getType().substring(0,1))) { work_coord = act.getCoord(); }
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
				type = ((Act)plan.getActsLegs().get(subtour.get(k))).getType().substring(0,1);
				System.out.println("Activity = " + type);
				if (mainpurpose == 1){
					if (type.equals(W)) { mainpurpose = 0; break; }
				}
				else if (mainpurpose == 2) {
					if (type.equals(W)) { mainpurpose = 0; break; }
					else if (type.equals(E)) { mainpurpose = 1;}
				}
				else if (mainpurpose == 3) {
					if (type.equals(W)) {mainpurpose = 0; break; }
					else if (type.equals(E)) {mainpurpose = 1; break;}
					else if (type.equals (S)) {mainpurpose = 2;}
				} 
				CoordI curr = ((Act)plan.getActsLegs().get(subtour.get(k))).getCoord();
				d = d + curr.calcDistance(prev);
				prev = curr;
			}
			d = d/1000.0;
			dist_h_w = dist_h_w/1000.0;
			System.out.println("dist = " + d);
			System.out.println("dist_h_w = " + dist_h_w);
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
			System.out.println("dist_h_w: " + dist_h_w);
			model.setAge(plan.getPerson().getAge());
			//model.setHHDimension(p.getHousehold().getPersonCount());
			model.setLicenseOwnership(plan.getPerson().hasLicense());
			System.out.println("license: " + model.license);
			model.setCar(plan.getPerson().getCarAvail());
			model.setTickets(plan.getPerson().getTravelcards());
			System.out.println("Travelcards: " + model.tickets);
			model.setBike(has_bike);
			model.setMale (plan.getPerson().getSex());
			int udeg = 5; // TODO The program should crash here, now only an initial value is given. afterwards something like that should replace it: int udeg = start.getMunicipality().getRegType();
			model.setUrbanDegree(udeg);
			model.setMainPurpose(mainpurpose);
			model.setDistanceTour(d); // model needs meters! 
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

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
				
		Plan plan = person.getSelectedPlan();
		int subtour_idx =0;
		TreeMap<Integer, ArrayList<Integer>> subtours = new TreeMap<Integer,ArrayList<Integer>>();
		PersonSubTourExtractor pste = new PersonSubTourExtractor(persons);
		pste.run(person);
		subtours = pste.getSubtours();
		subtour_idx = pste.getSubtourIdx();
		System.out.println("subtour_idx != " + subtour_idx );
		this.handleSubTours(plan,subtours,subtour_idx);
		PersonSubtourHandler psh = new PersonSubtourHandler ();
		PersonSubtour personSubtour = new PersonSubtour(); //TODO Change the constructor and place it below the psh.run
		psh.run (plan,subtours,subtour_idx);
		personSubtour = psh.getPers_sub();
		personSubtour.setPerson_id(person.getId());	
		
//		System.out.println("subtours != " + subtours );
//		Iterator i= subtours.entrySet().iterator();
//		while (i.hasNext()) {
//			Map.Entry e = (Map.Entry)i.next();
//			Integer key=(Integer)e.getKey();
//			ArrayList<Integer> values =(ArrayList<Integer>)e.getValue();
//			Subtour subtour = new Subtour();
//			//subtour.setId(key);
//			//subtour.setPurpose(1);
//			
//			Iterator<Integer> j = values.iterator();
//			while (j.hasNext()) {
//				subtour.setNode(new Integer(j.next()));
//			}
//			personSubtour.setSubtour(subtour);			
//		}
		
		this.personSubtours.add(personSubtour);		
	}
	
	
	
	public void run(Plan plan){
	}
}

