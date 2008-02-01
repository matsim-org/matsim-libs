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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
import org.matsim.world.Location;

import playground.balmermi.census2000.data.Household;
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
	ArrayList<Object> subToursRegister = null;
	List<Integer> modeSubTours = null;
	 
		
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
	
	private final void handleSubTour(final Plan plan, final List<Integer> subtour) {
	
		int prev_mode = 0;
		subToursRegister.add(subtour);
		for (int i=0;i<=subToursRegister.size();i=i+1) {
			if (subToursRegister.get(i).equals(subtour.get(0))) {
				prev_mode = modeSubTours.get(i); break;
			}
		}
		// calculate the SubTour distance (in 1 Km bins)
		int mainpurpose =  3; //mainpurpose:  0 := work; 1 := edu; 2 := shop 3:=leisure
		double d = 0.0;
		CoordI prev = ((Act)plan.getActsLegs().get(subtour.get(0))).getCoord();
		String type = null;
			for (int k=0; k<=subtour.size(); k=k+1) { //Verificare da dove deve partire!!!!!!!!
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
		//CoordI coord_start = ((Act)plan.getActsLegs().get(start)).getCoord();
		//Location l = Gbl.getWorld().//getLayer(MUNICIPALITY).getLocation(m_id);
		
		
		// setting subtour parameters
		
		model.setMainPurpose(mainpurpose);
		model.setDistanceTour(d); // model needs meters!
		model.setPrevMode(prev_mode);
		
		// choose mode choice model based on main purpose
		if (model.age >=18)
			if (mainpurpose == 0) {model = new ModelModeChoiceWork18Plus();}
			else if (mainpurpose == 1) {model = new ModelModeChoiceEducation18Plus();}
			else if (mainpurpose == 2) {model = new ModelModeChoiceShop18Plus();}
			else if (mainpurpose == 3) {model = new ModelModeChoiceLeisure18Plus();}
			else { Gbl.errorMsg("This should never happen!"); }
		else
			if (mainpurpose == 1) {model = new ModelModeChoiceEducation18Minus ();}
			else {model = new ModelModeChoiceOther18Minus ();}

		// getting the chosen mode
		int modechoice = model.calcModeChoice();
		String mode = null;
		if (modechoice == 0) { mode = CAR; }
		else if (modechoice == 1) { mode = PT; }
		else if (modechoice == 2) { mode = RIDE; }
		else if (modechoice == 3) { mode = BIKE; }
		else if (modechoice == 4) { mode = WALK; }
		else { Gbl.errorMsg("Mode choice returns undefined value!"); }
		
		modeSubTours.add(modechoice);
		for (int k=0; k<=subtour.size(); k=k+1){
			((Act)plan.getActsLegs().get(subtour.get(k))).setType(mode);
		}
}
	
	private final void registerSubTours (Plan plan,List<Integer> subtours) {
		
		List<Integer> subtour = null;
		// TODO check if it is an homebased tour { Gbl.errorMsg("This should never happen!");
		for (int i=0; i<=subtours.size();i=i+2) {
			// TODO check if the subtour variable must be cleared or not, at the moment IT IS CLEARED!!!
			subtour.clear();
			int starti = subtours.get(i);
			int endi = subtours.get(i+1);
			subtour.add(0,starti); // all tours start at home
			int j = starti+2;
			while (j<endi) {
				if (subtours.contains(j)){
					subtour.add(j);
					j=subtours.get(subtours.indexOf(j)+1)+2;
					}
				else {
					subtour.add(j);
					j=j+2;
				}
			}
			subtour.add(endi);
			handleSubTour (plan,subtour);
		}
	}
	
	private final void extractSubTours(Plan plan, int start, int end) {
		
		boolean is_leaf = true;
		List<Integer> subtours= null;
		subtours.add(0,start);
		subtours.add(1,end);
		int k=2;
		for (int i=start+2; i<end-1; i=i+2) {
			Act acti = (Act)plan.getActsLegs().get(i);
			for (int j=end-2; j>i; j=j-2) {
				Act actj = (Act)plan.getActsLegs().get(j);
				if ((acti.getCoord().getX() == actj.getCoord().getX()) &&
				    (acti.getCoord().getY() == actj.getCoord().getY())) {
					// subtour found: start..i & j..end
					is_leaf = false;
					subtours.add(k,i);
					subtours.add(k+1,j);
					k=k+1;
					this.registerSubTours(plan,subtours);
					
					// next recursive step
					int ii = i;
					Act actii = acti;
					for (int jj=i+2; jj<=j; jj=jj+2) {
						Act actjj = (Act)plan.getActsLegs().get(jj);
						if ((actii.getCoord().getX() == actjj.getCoord().getX()) &&
						    (actii.getCoord().getY() == actjj.getCoord().getY())) {
							subtours.add(k,ii);
							subtours.add(k+1,jj);
							k=k+1;
							this.extractSubTours(plan,ii,jj);
							ii = jj;
							actii = (Act)plan.getActsLegs().get(ii);
						}
					}
					return;
				}
			}
		}
		if (is_leaf) {
			// leaf-sub-tour: start..end
			subtours.add(k,start);
			subtours.add(k,end);
			k=k+1;
			this.registerSubTours(plan,subtours);
			
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		playground.balmermi.census2000.data.Person p = this.persons.getPerson(Integer.parseInt(person.getId().toString()));
		
		Plan plan = person.getSelectedPlan();
		if (plan == null) { Gbl.errorMsg("Person id=" + person.getId() + "does not have a selected plan."); }
		Iterator<BasicAct> act_it = plan.getIteratorAct();
		CoordI home_coord = null;
		CoordI work_coord = null;
		//act_it.hasNext(); // first act is always 'home'
		while (act_it.hasNext()) {
			Act act = (Act)act_it.next();
			if (H.equals(act.getType())) { home_coord = act.getCoord(); }
			else if (W.equals(act.getType())) { work_coord = act.getCoord(); }
		}
			double dist_h_w = 0.0;
			if ((home_coord == null) || (home_coord.equals(ZERO))) { Gbl.errorMsg("No home coord defined!"); }
			if ((work_coord != null) && (work_coord.equals(ZERO))) { Gbl.errorMsg("Weird work coord defined!!!"); }
			if (work_coord != null) {
			dist_h_w = work_coord.calcDistance(home_coord);
		
			// generating a random bike ownership (see STRC2007 paper Ciari for more details)
			boolean has_bike = true;
			if (Gbl.random.nextDouble() < 0.44) { has_bike = false; }	
			
			// setting person parameters
			model.setDistanceHome2Work(dist_h_w);
			model.setAge(p.getAge());
			model.setHHDimension(p.getHousehold().getPersonCount());
			model.setLicenseOwnership(person.hasLicense());
			model.setCar(p.getCarAvail());
			model.setTickets(person.getTravelcards());
			model.setLicenseOwnership(p.hasLicense());
			model.setBike(has_bike);
			model.setMale (p.getSex());
			this.extractSubTours(plan,0,plan.getActsLegs().size()-1);
		
		}				
	}	
	public void run(Plan plan) {
	}
	
}

