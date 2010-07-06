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

package playground.balmermi.census2000.modules;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.balmermi.census2000.data.Persons;
import playground.balmermi.census2000.models.ModelModeChoice;
import playground.balmermi.census2000.models.ModelModeChoiceEducation;
import playground.balmermi.census2000.models.ModelModeChoiceShopLeisure;
import playground.balmermi.census2000.models.ModelModeChoiceWork;

public class PersonModeChoiceModel extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String UNDEF = "undef";
	private static final String PT = "pt";
	private static final String CAR = "car";
	private static final String BIKE = "bike";
	private static final String WALK = "walk";
	private static final String E = "e";
	private static final String W = "w";

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
		playground.balmermi.census2000.data.MyPerson p = this.persons.getPerson(Integer.valueOf(person.getId().toString()));

		// calc plan distance and main purpose
		double plan_dist = 0.0;
		int mainpurpose = 2; // 0 := w; 1 := e; 2 := s|l
		boolean isFirst = true;
		Activity prevAct = null;
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Activity) {
				if (isFirst) {
					isFirst = false; // first act is always 'home', ignore it
				} else {
					Activity act = (Activity) pe;
					if (prevAct != null) {
						plan_dist += CoordUtils.calcDistance(act.getCoord(), prevAct.getCoord());
						String type = act.getType();
						if (mainpurpose == 1){
							if (type == W) { mainpurpose = 0; break; }
						}
						else if (mainpurpose == 2) {
							if (type == W) { mainpurpose = 0; break; }
							else if (type == E) { mainpurpose = 1; }
						}
					}
					prevAct = act;
				}
			}
		}

		// choose mode choice model based on main purpose
		if (mainpurpose == 0) {model = new ModelModeChoiceWork();}
		else if (mainpurpose == 1) {model = new ModelModeChoiceEducation();}
		else if (mainpurpose == 2) {model = new ModelModeChoiceShopLeisure();}
		else { Gbl.errorMsg("This should never happen!"); }

		// generating a random bike ownership (see STRC2007 paper Ciari for more details)
		boolean has_bike = true;
		if (MatsimRandom.getRandom().nextDouble() < 0.44) { has_bike = false; }

		// setting parameters
		model.setAge(p.getAge());
		model.setHHDimension(p.getHousehold().getPersonCount());
		model.setLicenseOwnership(((PersonImpl) person).hasLicense());
		model.setUrbanDegree(p.getHousehold().getMunicipality().getRegType());
		model.setCar(p.getCarAvail());
		model.setTickets(((PersonImpl) person).getTravelcards());
		model.setDistanceTour(plan_dist/1000.0); // model needs meters!
		model.setLicenseOwnership(p.hasLicense());
		model.setMainPurpose(mainpurpose);
		model.setBike(has_bike);

		// getting the chosen mode
		int modechoice = model.calcModeChoice();
		String mode = null;
		if (modechoice == 0) { mode = TransportMode.walk; }
		else if (modechoice == 1) { mode = TransportMode.bike; }
		else if (modechoice == 2) { mode = TransportMode.car; }
		else if (modechoice == 3) { mode = TransportMode.pt; }
		else if (modechoice == 4) { mode = "undefined"; }
		else { Gbl.errorMsg("Mode choice returns undefined value!"); }

		// setting mode to plan
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				leg.setMode(mode);
			}
		}
	}

	@Override
	public void run(Plan plan) {
	}
}
