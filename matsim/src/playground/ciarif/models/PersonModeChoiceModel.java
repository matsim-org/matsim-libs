/* *********************************************************************** *
 * project: org.matsim.*
 * ModelMobilityTools.java
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

package playground.ciarif.models;

import java.util.Iterator;

import org.matsim.basic.v01.BasicActImpl;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;

import playground.balmermi.census2000.data.Persons;

public class PersonModeChoiceModel extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String E = "e";
	private static final String W = "w";
	private static final String S = "s";
	private static final String H = "h";
	private static final CoordImpl ZERO = new CoordImpl(0.0,0.0);

	private ModelModeChoice model;
	private final Persons persons;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonModeChoiceModel(final Persons persons) {
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.persons = persons;
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		playground.balmermi.census2000.data.Person p = this.persons.getPerson(Integer.parseInt(person.getId().toString()));

		// calc plan distance and main purpose
		double plan_dist = 0.0;
		int mainpurpose = 3; // 0 := w; 1 := e; 2 := s 3:=l
		Iterator<BasicActImpl> act_it = person.getSelectedPlan().getIteratorAct();
		Coord home_coord = null;
		Coord work_coord = null;
		act_it.hasNext(); // first act is always 'home'
		Act prev_act = (Act)act_it.next();
		while (act_it.hasNext()) {
			Act act = (Act)act_it.next();
			if (H.equals(act.getType())) { home_coord = act.getCoord(); }
			else if (W.equals(act.getType())) { work_coord = act.getCoord(); }
			plan_dist += act.getCoord().calcDistance(prev_act.getCoord());
			String type = act.getType();
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

			prev_act = act;
		}	
			double distance = 0.0;
			if ((home_coord == null) || (home_coord.equals(ZERO))) { Gbl.errorMsg("No home coord defined!"); }
			if ((work_coord != null) && (work_coord.equals(ZERO))) { Gbl.errorMsg("Weird work coord defined!!!"); }
			if (work_coord != null) {
				distance = work_coord.calcDistance(home_coord);
			}


			// choose mode choice model based on main purpose
			if (person.getAge()>=18)
				if (mainpurpose == 0) {model = new ModelModeChoiceWork18Plus();}
				else if (mainpurpose == 1) {model = new ModelModeChoiceEducation18Plus();}
				else if (mainpurpose == 2) {model = new ModelModeChoiceShop18Plus();}
				else if (mainpurpose == 3) {model = new ModelModeChoiceLeisure18Plus();}
				else { Gbl.errorMsg("This should never happen!"); }
			else
				if (mainpurpose == 1) {model = new ModelModeChoiceEducation18Minus ();}
				else {model = new ModelModeChoiceOther18Minus ();}

			// generating a random bike ownership (see STRC2007 paper Ciari for more details)
			boolean has_bike = true;
			if (MatsimRandom.random.nextDouble() < 0.44) { has_bike = false; }

			// setting parameters
			model.setAge(person.getAge());
			model.setDistanceHome2Work(distance);
			model.setHHDimension(p.getHousehold().getPersonCount());
			model.setLicenseOwnership(person.hasLicense());
			model.setUrbanDegree(p.getHousehold().getMunicipality().getRegType());
			model.setCar(p.getCarAvail());
			model.setTickets(person.getTravelcards());
			model.setDistanceTour(plan_dist/1000.0); // model needs meters!
			model.setLicenseOwnership(p.hasLicense());
			model.setMainPurpose(mainpurpose);
			model.setBike(has_bike);

			// getting the chosen mode
			int modechoice = model.calcModeChoice();
			BasicLeg.Mode mode = null;
			if (modechoice == 0) { mode = BasicLeg.Mode.walk; }
			else if (modechoice == 1) { mode = BasicLeg.Mode.bike; }
			else if (modechoice == 2) { mode = BasicLeg.Mode.car; }
			else if (modechoice == 3) { mode = BasicLeg.Mode.pt; }
			else if (modechoice == 4) { mode = BasicLeg.Mode.ride; }
			else { Gbl.errorMsg("Mode choice returns undefined value!"); }

			// setting mode to plan
			Iterator<Leg> leg_it = person.getSelectedPlan().getIteratorLeg();
			while (leg_it.hasNext()) {
				Leg leg = leg_it.next();
				leg.setMode(mode);
			}
		
	}

	public void run(Plan plan) {
	}
}
