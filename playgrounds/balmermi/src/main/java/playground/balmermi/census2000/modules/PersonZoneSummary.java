/* *********************************************************************** *
 * project: org.matsim.*
 * PersonZoneSummary.java
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.world.MappedLocation;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

import playground.balmermi.census2000.data.Persons;

public class PersonZoneSummary extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String UNDEF = "undef";
	private static final String PT = "pt";
	private static final String CAR = "car";
	private static final String BIKE = "bike";
	private static final String WALK = "walk";
	private static final String ALWAYS = "always";
	private static final String SOMETIMES = "sometimes";
	private static final String NEVER = "never";
	private static final String NO = "no";
	private static final String YES = "yes";
	private static final String F = "f";
	private static final String M = "m";
	private static final String L = "l";
	private static final String S = "s";
	private static final String E = "e";
	private static final String W = "w";
	private static final String H = "h";

	private FileWriter fw = null;
	private BufferedWriter out = null;
	private final ZoneLayer layer;
	private final Persons persons;
	private final String outfile;

	private final HashMap<Id, int[]> zones = new HashMap<Id, int[]>();
	private final String[] heads = {"p","male","female",
	                                "age05","age67","age814","age1517","age1865","age66inf",
	                                "licyes","licno",
	                                "carnever","carsometimes","caralways",
	                                "emplyes","emplno",
	                                "ptyes","ptno",
	                                "chain","chainL","chainS","chainSL",
	                                "chainE","chainEL","chainES","chainESL",
	                                "chainW","chainWL","chainWS","chainWSL",
	                                "chainWE","chainWEL","chainWES","chainWESL",
	                                "act",
	                                "trip01","trip15","trip520","trip2050","trip50inf",
	                                "plan05","plan520","plan2050","plan50100","plan100inf",
	                                WALK,BIKE,CAR,PT,UNDEF,"muni_id"};

    //////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonZoneSummary(ZoneLayer layer, Persons persons, String outfile) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.layer = layer;
		this.persons = persons;
		this.outfile = outfile;
		this.initHash();
		this.open();
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// init methods
	//////////////////////////////////////////////////////////////////////

	private final void initHash() {
		int z_cnt = this.layer.getLocations().size();
		int att_cnt = this.heads.length-1; // Note: muni_id is not part of the array
		Iterator<? extends MappedLocation> z_it = this.layer.getLocations().values().iterator();
		while (z_it.hasNext()) {
			Zone z = (Zone)z_it.next();
			int[] atts = new int[att_cnt];
			for (int i=0; i<att_cnt; i++) { atts[i] = 0; }
			this.zones.put(z.getId(),atts);
		}
	}

	private final void open() {
		try {
			this.fw = new FileWriter(this.outfile);
			this.out = new BufferedWriter(this.fw);
			this.out.write(this.heads[0]);
			for (int i=1; i<this.heads.length; i++) { this.out.write("\t" + this.heads[i]); }
			this.out.write("\n");
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// final method
	//////////////////////////////////////////////////////////////////////

	public final void close() {
		try {
			Iterator<Id> id_it = this.zones.keySet().iterator();
			while (id_it.hasNext()) {
				Id id = id_it.next();
				int[] vals = this.zones.get(id);
				for (int i=0; i<vals.length; i++) { this.out.write(vals[i] + "\t"); }
				this.out.write(id + "\n");
				this.out.flush();
			}
			this.out.flush();
			this.out.close();
			this.fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	private final int calcChainIndex(Plan plan, int offset) {
		int w = 0; int e = 0; int s = 0; int l = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) pe;
				if (H.equals(act.getType())) { ; }
				else if (W.equals(act.getType())) { w = 1; }
				else if (E.equals(act.getType())) { e = 1; }
				else if (S.equals(act.getType())) { s = 1; }
				else if (L.equals(act.getType())) { l = 1; }
				else { Gbl.errorMsg("Act type=" + act.getType() + " not known!"); }
			}
		}
		int index = w*2*2*2 + e*2*2 + s*2 + l;
		return index + offset;
	}

	private final int calcPlanIndex(Plan plan, int offset) {
		double dist = 0.0;
		ActivityImpl prevAct = null;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) pe;
				if (prevAct != null) {
					double curr_dist = CoordUtils.calcDistance(act.getCoord(), prevAct.getCoord());
					dist += curr_dist;
				}
				prevAct = act;
			}
		}
		if (dist < 5000.0) { return 0 + offset; }
		else if (dist < 20000.0) { return 1 + offset; }
		else if (dist < 50000.0) { return 2 + offset; }
		else if (dist < 100000.0) { return 3 + offset; }
		else { return 4 + offset; }
	}

	private final int countActs(Plan plan) {
		int cnt = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				cnt++;
			}
		}
		return cnt;
	}

	private final int[] countTrips(Plan plan) {
		int[] cnts = {0,0,0,0,0};
		ActivityImpl prevAct = null;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) pe;
				if (prevAct != null) {
					double dist = CoordUtils.calcDistance(act.getCoord(), prevAct.getCoord());
					if (dist < 1000.0) { cnts[0]++; }
					else if (dist < 5000.0) { cnts[1]++; }
					else if (dist < 20000.0) { cnts[2]++; }
					else if (dist < 50000.0) { cnts[3]++; }
					else { cnts[4]++; }
				}
				prevAct = act;
			}
		}
		return cnts;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person pp) {
		PersonImpl person = (PersonImpl) pp;
		playground.balmermi.census2000.data.MyPerson p = this.persons.getPerson(Integer.parseInt(person.getId().toString()));
		Id zone_id = p.getHousehold().getMunicipality().getZone().getId();
		int[] vals = this.zones.get(zone_id);

		vals[0]++;

		if (M.equals(person.getSex())) { vals[1]++; }
		else if (F.equals(person.getSex())) { vals[2]++; }
		else { Gbl.errorMsg("Person id=" + person.getId() + ": Attribute 'sex' is wrong!"); }

		if (person.getAge() < 6) { vals[3]++; }
		else if (person.getAge() < 8) { vals[4]++; }
		else if (person.getAge() < 15) { vals[5]++; }
		else if (person.getAge() < 18) { vals[6]++; }
		else if (person.getAge() < 66) { vals[7]++; }
		else { vals[8]++; }

		if (YES.equals(person.getLicense())) { vals[9]++; }
		else if (NO.equals(person.getLicense())) { vals[10]++; }
		else { Gbl.errorMsg("Person id=" + person.getId() + ": Attribute 'license' is wrong!"); }

		if (NEVER.equals(person.getCarAvail())) { vals[11]++; }
		else if (SOMETIMES.equals(person.getCarAvail())) { vals[12]++; }
		else if (ALWAYS.equals(person.getCarAvail())) { vals[13]++; }
		else { Gbl.errorMsg("Person id=" + person.getId() + ": Attribute 'car_avail' is wrong!"); }

		if (person.isEmployed()) { vals[14]++; }
		else { vals[15]++; }

		if (!person.getTravelcards().isEmpty()) { vals[16]++; }
		else { vals[17]++; }

		int index = this.calcChainIndex(person.getSelectedPlan(),18); // returns 18-33
		if ((index < 18) || (33 < index)) { Gbl.errorMsg("Person id=" + person.getId() + ": returning wrong index!"); }
		vals[index]++;

		vals[34] += this.countActs(person.getSelectedPlan());

		int[] cnts = this.countTrips(person.getSelectedPlan()); // returns an array of size = 5
		for (int i=0; i<cnts.length; i++) { vals[35+i] += cnts[i]; }

		index = this.calcPlanIndex(person.getSelectedPlan(),40); // returns 40-44
		if ((index < 40) || (45 < index)) { Gbl.errorMsg("Person id=" + person.getId() + ": returning wrong index!"); }
		vals[index]++;

		String mode = ((LegImpl)person.getSelectedPlan().getPlanElements().get(1)).getMode();
		if (WALK.equals(mode)) { vals[45]++; }
		else if (BIKE.equals(mode)) { vals[46]++; }
		else if (CAR.equals(mode)) { vals[47]++; }
		else if (PT.equals(mode)) { vals[48]++; }
		else if (UNDEF.equals(mode)) { vals[49]++; }
		else { Gbl.errorMsg("mode=" + mode + " not known!"); }
	}

	@Override
	public void run(Plan plan) {
	}
}
