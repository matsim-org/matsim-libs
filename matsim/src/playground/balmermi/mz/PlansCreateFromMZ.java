/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCreateFromNetwork.java
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

package playground.balmermi.mz;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PlansAlgorithm;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.misc.Time;

public class PlansCreateFromMZ extends PlansAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final String inputfile;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCreateFromMZ(final String inputfile) {
		super();
		this.inputfile = inputfile;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Plans plans) {
		System.out.println("    running " + this.getClass().getName() + " module...");

		if (plans.getName() == null) { plans.setName("created by '" + this.getClass().getName() + "'"); }
		if (!plans.getPersons().isEmpty()) { Gbl.errorMsg("[plans=" + plans + " is not empty]"); }
		
		TreeMap<IdI,Person> person_err = new TreeMap<IdI, Person>();

		System.out.println("      reading and creating persons with plans from file...");

		try {
			FileReader file_reader = new FileReader(inputfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);
			int line_nr = 1;
			// Skip header
			String curr_line = buffered_reader.readLine();
			line_nr++;
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// head:    ID_PERSON  ID_TOUR  ID_TRIP  F58  S_X     S_Y     Z_X     Z_Y     F514  DURATION_1  DURATION_2  PURPOSE  TRIP_MODE  HHNR  ZIELPNR  WEGNR
				// example: 5751       57511    5751101  540  507720  137360  493620  120600  560   20          20          1        3          575   1        1
				// index:   0          1        2        3    4       5       6       7       8     9           10          11       12         13    14       15

				IdI pid = new Id(entries[0].trim());

				int m = Integer.parseInt(entries[12].trim());
				String mode = null;
				if (m == 1) { mode = "walk"; }
				else if (m == 2) { mode = "bike"; }
				else if (m == 3) { mode = "car"; }
				else if (m == 4) { mode = "pt"; }
				else if (m == 5) { mode = "ride"; }
				else if (m == 6) { mode = "undef"; }
				else { Gbl.errorMsg("pid=" + pid + ": m=" + m + " not known!");}

				int p = Integer.parseInt(entries[11].trim());
				String acttype = null;
				if (p == 1) { acttype = "w"; }
				else if (p == 2) { acttype = "e"; }
				else if (p == 3) { acttype = "s"; }
				else if (p == 4) { acttype = "l"; }
				else if (p == 5) { acttype = "o"; }
				else { Gbl.errorMsg("pid=" + pid + ": m=" + m + " not known!");}

				CoordI from = new Coord(entries[4].trim(),entries[5].trim());
				from.setX(Math.round(from.getX()/100.0)*100);
				from.setY(Math.round(from.getY()/100.0)*100);
				CoordI to = new Coord(entries[6].trim(),entries[7].trim());
				to.setX(Math.round(to.getX()/100.0)*100);
				to.setY(Math.round(to.getY()/100.0)*100);
				
				Person person = plans.getPerson(pid);
				if (person == null) { person = new Person(pid,null,Integer.MIN_VALUE,null,null,null); plans.addPerson(person); }
				Plan plan = person.getSelectedPlan();
				if (plan == null) { person.createPlan(null,"yes"); plan = person.getSelectedPlan(); }
				if (plan.getActsLegs().size() != 0) {
					plan.createLeg(0,mode,Time.UNDEFINED_TIME,Time.UNDEFINED_TIME,Time.UNDEFINED_TIME);
					plan.createAct(acttype,to.getX(),to.getY(),null,0,0,0,false);
					
					// a check
					Act fromact = (Act)plan.getActsLegs().get(plan.getActsLegs().size()-3);
					if ((fromact.getCoord().getX() != from.getX()) || (fromact.getCoord().getY() != from.getY())) {
						System.out.println("        line " + line_nr + "; pid=" + person.getId() + ": previous destination not equal to the current origin (dist=" + fromact.getCoord().calcDistance(from) + ")");
						person_err.put(person.getId(),person);
					}
				}
				else {
					plan.createAct("h",from.getX(),from.getY(),null,0,0,0,false);
					plan.createLeg(0,mode,Time.UNDEFINED_TIME,Time.UNDEFINED_TIME,Time.UNDEFINED_TIME);
					plan.createAct(acttype,to.getX(),to.getY(),null,0,0,0,false);
				}
				line_nr++;
			}
			buffered_reader.close();
			file_reader.close();
		} catch (Exception e) {
			Gbl.errorMsg(e);
		}
		System.out.println("      done.");
		System.out.println("      => # Persons: " + plans.getPersons().size());

		System.out.println("      removing " + person_err.size() + " persons...");
		for (Person p : person_err.values()) {
			plans.getPersons().remove(p.getId());
			System.out.println("        pid=" + p.getId() + " removed.");
		}
		System.out.println("      done.");
		System.out.println("      => # Persons: " + plans.getPersons().size());
		
		System.out.println("      identify and set home locations...");
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			Act home = plan.getFirstActivity();
			for (int i=2; i<plan.getActsLegs().size(); i=i+2) {
				Act act = (Act)plan.getActsLegs().get(i);
				if ((act.getCoord().getX() == home.getCoord().getX()) && (act.getCoord().getY() == home.getCoord().getY())) {
					if (!act.getType().equals("h")) {
						act.setType("h");
						System.out.println("        pid=" + p.getId() + "; act_nr=" + (i/2) + ": set type to 'h'");
					}
				}
			}
		}
		System.out.println("      done.");
		System.out.println("      => # Persons: " + plans.getPersons().size());

//		System.out.println("      identifying single trip day plans...");
//		person_err.clear();
//		for (Person p : plans.getPersons().values()) {
//			Plan plan = p.getSelectedPlan();
//			if (plan.getActsLegs().size() < 4) { person_err.put(p.getId(),p); }
//		}
//		System.out.println("      done.");
//		System.out.println("      => # Persons: " + plans.getPersons().size());
//
//		System.out.println("      removing " + person_err.size() + " persons with single trip day plans...");
//		for (Person p : person_err.values()) {
//			plans.getPersons().remove(p.getId());
//			System.out.println("        pid=" + p.getId() + " removed.");
//		}
//		System.out.println("      done.");
//		System.out.println("      => # Persons: " + plans.getPersons().size());

		System.out.println("      identifying non-home-based day plans...");
		person_err.clear();
		for (Person p : plans.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			Act last = (Act)plan.getActsLegs().get(plan.getActsLegs().size()-1);
			if (!last.getType().equals("h")) { person_err.put(p.getId(),p); }
		}
		System.out.println("      done.");
		System.out.println("      => # Persons: " + plans.getPersons().size());
		
		System.out.println("      removing " + person_err.size() + " persons with non-home-based day plans...");
		for (Person p : person_err.values()) {
			plans.getPersons().remove(p.getId());
			System.out.println("        pid=" + p.getId() + " removed.");
		}
		System.out.println("      done.");
		System.out.println("      => # Persons: " + plans.getPersons().size());

//		System.out.println("      identifying day plans with type 'o'...");
//		person_err.clear();
//		for (Person p : plans.getPersons().values()) {
//			Plan plan = p.getSelectedPlan();
//			Iterator<?> act_it = plan.getIteratorAct();
//			while (act_it.hasNext()) {
//				Act act = (Act)act_it.next();
//				if (act.getType().equals("o")) { person_err.put(p.getId(),p); }
//			}
//		}
//		System.out.println("      done.");
//		System.out.println("      => # Persons: " + plans.getPersons().size());
//
//		System.out.println("      removing " + person_err.size() + " persons with day plans with type 'o's...");
//		for (Person p : person_err.values()) {
//			plans.getPersons().remove(p.getId());
//			System.out.println("        pid=" + p.getId() + " removed.");
//		}
//		System.out.println("      done.");
//		System.out.println("      => # Persons: " + plans.getPersons().size());
//
//		System.out.println("    done.");
	}
}
