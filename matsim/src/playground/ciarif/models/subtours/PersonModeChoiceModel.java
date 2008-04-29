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


import org.matsim.basic.v01.BasicActImpl;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.world.Layer;
import org.matsim.world.Location;
import org.matsim.world.World;

import playground.balmermi.census2000.data.Municipalities;
import playground.balmermi.census2000.data.Municipality;
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
	private static final String W = "w";
	private static final String H = "h";
	private static final Coord ZERO = new Coord(0.0,0.0);
	private final Persons persons;
	private final Municipalities municipalities;
	private ModelModeChoice model;
	private List<PersonSubtour> personSubtours = new Vector<PersonSubtour>();
	 
		
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	//public PersonModeChoiceModel(final Persons persons) {
	public PersonModeChoiceModel(final Persons persons, Municipalities municipalities) {
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.persons = persons;
		this.municipalities = municipalities;
		System.out.println("    done.");
	}
	
	//////////////////////////////////////////////////////////////////////
	// get/set methods
	//////////////////////////////////////////////////////////////////////

	public List<PersonSubtour> getPersonSubtours() {
		return personSubtours;
	}


	public void setPersonSubtours(List<PersonSubtour> personSubtours) {
		this.personSubtours = personSubtours;
	}
	
	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////
	
	//private final void setUpModeChoice(final Plan plan, final PersonSubtour personSubtour, final Municipalities municipalities) {
	private final void setUpModeChoice(final Plan plan, final PersonSubtour personSubtour) {	
		// setting subtour parameters
		if (plan == null) { Gbl.errorMsg("Person id=" + plan.getPerson().getId() + "does not have a selected plan."); }
		Iterator<BasicActImpl> act_it = plan.getIteratorAct();
		CoordI home_coord = null;
		CoordI work_coord = null;
		act_it.hasNext();
		double dist_h_w = 0.0;
		while (act_it.hasNext()) {
			Act act = (Act)act_it.next();
			if (H.equals(act.getType().substring(0,1))) { home_coord = act.getCoord();}
			else if (W.equals(act.getType().substring(0,1))) { work_coord = act.getCoord(); }
		}
		if ((home_coord == null) || (home_coord.equals(ZERO))) { Gbl.errorMsg("No home coord defined!"); }
		if ((work_coord != null) && (work_coord.equals(ZERO))) { Gbl.errorMsg("Weird work coord defined!!!"); }
		if (work_coord != null) { 
			dist_h_w = work_coord.calcDistance(home_coord); 
			dist_h_w = dist_h_w/1000.0;
		}
		//System.out.println("Work coord " + work_coord);

		int subtours_nr = personSubtour.getSubtours().size()-1;
		for (int i=0; i<=subtours_nr; i=i+1){
			
			Subtour sub = personSubtour.getSubtours().get(i);
			//System.out.println ("sub_in model = " + sub.getNodes());
			//System.out.println ("i = " + i);
			//System.out.println ("sub_mode before = " + sub.getMode());
			int mainpurpose = sub.getPurpose();
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
			boolean has_bike = false;
			if (Gbl.random.nextDouble() < 0.70) { has_bike = true; }			
			
			boolean ride_possible = false;
			
			model.setRide(ride_possible);
			if (Gbl.random.nextDouble () < 0.35) {ride_possible = true;} // to verify if it makes sense, but till then a mean to 
			// make things work the right way
			
			if (sub.getPrev_subtour()<5){
				if (i>=1) {
					//System.out.println ("prev_mode in database = " + personSubtour.getSubtours().get(i-1).getMode());
					model.setPrevMode (personSubtour.getSubtours().get(i-1).getMode());
				}
				//				model.setPrevMode(personSubtour.getSubtours().get(sub.getPrev_subtour()).getMode()); //It seeks the previous sub-tour and get the mode
				
			}
			else model.setPrevMode(5); // 5 means that the subtour starts at home
			//System.out.println("prev subtour's mode = " +  model.prev_mode);
			
			// setting person parameters
			//System.out.println(model);
			model.setDistanceHome2Work(dist_h_w);
			//System.out.println("dist_h_w: " + dist_h_w);
			model.setAge(plan.getPerson().getAge());
			//model.setHHDimension(p.getHousehold().getPersonCount());
			model.setLicenseOwnership(plan.getPerson().hasLicense());
			//System.out.println("license: " + model.license);
			model.setCar(plan.getPerson().getCarAvail());
			model.setTickets(plan.getPerson().getTravelcards());
			//System.out.println("Travelcards: " + model.tickets);
			model.setBike(has_bike);
			model.setMale (plan.getPerson().getSex());
			//int udeg = 4; // 
			Layer muni_layer = Gbl.getWorld().getLayer(Municipalities.MUNICIPALITY);
			ArrayList<Location> locs = muni_layer.getNearestLocations(sub.getStart_coord());
			Location loc = locs.get(Gbl.random.nextInt(locs.size()));
			Municipality m = municipalities.getMunicipality(new Integer(loc.getId().toString()));
			int udeg = m.getRegType();
			//System.out.println ("udeg");
			//Iterator<Location> l_it = Gbl.getWorld().getLayer(Municipalities.MUNICIPALITY).getLocations().values().iterator(); //TODO controllare se serve!!!!!
			
			
			model.setUrbanDegree(udeg);
			model.setMainPurpose(mainpurpose);
			model.setDistanceTour(sub.getDistance()); // 
			System.out.println("dist subtour: " + sub.getDistance());
			 
			model.setHomeCoord(home_coord);
			
			// getting the chosen mode
			//System.out.println("prev_mode_model = " + model.prev_mode);
			int modechoice = model.calcModeChoice();
			String mode = null;
			if (modechoice == 0) { mode = CAR; }
			else if (modechoice == 1) { mode = PT; }
			else if (modechoice == 2) { mode = RIDE; }
			else if (modechoice == 3) { mode = BIKE; }
			else if (modechoice == 4) { mode = WALK; }
			else { Gbl.errorMsg("Mode choice returns undefined value!"); }
			System.out.println("modechoice = " + modechoice);
			System.out.println();
			personSubtour.getSubtours().get(i).setMode(modechoice);
			personSubtour.getSubtours().get(i).setStart_udeg(udeg);
			
			for (int k=1; k<sub.getNodes().size(); k=k+1){
				((Leg)plan.getActsLegs().get(sub.getNodes().get(k)-1)).setMode(mode);
				System.out.println("leg = " + ((Leg)plan.getActsLegs().get(sub.getNodes().get(k)-1)));
								
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
		PersonSubTourExtractor pste = new PersonSubTourExtractor(this.persons);
		pste.run(person);
		subtours = pste.getSubtours();
		subtour_idx = pste.getSubtourIdx();
		System.out.println("person_id = " + person.getId());
		PersonSubtourHandler psh = new PersonSubtourHandler();
		PersonSubtour personSubtour = new PersonSubtour(); //TODO Change the constructor and place it below the psh.run
		psh.run(plan,subtours,subtour_idx);
		personSubtour = psh.getPers_sub();
		//this.setUpModeChoice(plan,personSubtour, municipalities);
		this.setUpModeChoice(plan,personSubtour);
		personSubtour.setPerson_id(person.getId());	
		this.personSubtours.add(personSubtour);	
	}
	
	
	public void run(Plan plan){
	}
}

