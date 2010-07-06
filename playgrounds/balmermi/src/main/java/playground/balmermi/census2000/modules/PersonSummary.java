/* *********************************************************************** *
 * project: org.matsim.*
 * PersonSummary.java
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

import java.util.List;

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

public class PersonSummary extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String UNDEF = "undef";
	private static final String PT = "pt";
	private static final String CAR = "car";
	private static final String BIKE = "bike";
	private static final String WALK = "walk";
	private static final String SOMETIMES = "sometimes";
	private static final String ALWAYS = "always";
	private static final String YES = "yes";

	private int person_cnt = 0;
	private int plan_cnt = 0;
	private int act_cnt = 0;
	private double av_act_per_plan = 0.0;

	private final int [] ages = new int[100];
	private final int [] age_groups = new int[5];
	private final int [] license_groups = new int[5];
	private final int [][] caravail_groups = new int[5][3];
	private final int [] employed_groups = new int[5];

	private final int [][] trip_mode_cnts = new int[5][5];
	private final int [][] plan_mode_cnts = new int[5][5];

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSummary() {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		for (int i=0; i<100; i++) {
			this.ages[i] = 0;
		}
		for (int i=0; i<5; i++) {
			this.age_groups[i] = 0;
			this.license_groups[i] = 0;
			for (int j=0; j<3; j++) {
				this.caravail_groups[i][j] = 0;
			}
			this.employed_groups[i] = 0;
		}
		for (int i=0; i<5; i++) {
			for (int j=0; j<5; j++) {
				trip_mode_cnts[i][j] = 0;
				plan_mode_cnts[i][j] = 0;
			}
		}
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person pp) {
		PersonImpl person = (PersonImpl) pp;
		if (person.getAge() > 99) {
			return;
		}

		person_cnt++;

		if (person.getAge() != Integer.MIN_VALUE) {
			this.ages[person.getAge()]++;
		}

		if (person.getAge() < 5) {
			age_groups[0]++;
			if (person.getLicense().equals(YES)) { this.license_groups[0]++; }
			if (person.getCarAvail().equals(ALWAYS)) { this.caravail_groups[0][0]++; }
			else if (person.getCarAvail().equals(SOMETIMES)) { this.caravail_groups[0][1]++; }
			else { this.caravail_groups[0][2]++; }
			if (person.getEmployed().equals(YES)) { this.employed_groups[0]++; }
		}
		else if (person.getAge() < 7) {
			age_groups[1]++;
			if (person.getLicense().equals(YES)) { this.license_groups[1]++; }
			if (person.getCarAvail().equals(ALWAYS)) { this.caravail_groups[1][0]++; }
			else if (person.getCarAvail().equals(SOMETIMES)) { this.caravail_groups[1][1]++; }
			else { this.caravail_groups[1][2]++; }
			if (person.getEmployed().equals(YES)) { this.employed_groups[1]++; }
		}
		else if (person.getAge() < 66) {
			age_groups[2]++;
			if (person.getLicense().equals(YES)) { this.license_groups[2]++; }
			if (person.getCarAvail().equals(ALWAYS)) { this.caravail_groups[2][0]++; }
			else if (person.getCarAvail().equals(SOMETIMES)) { this.caravail_groups[2][1]++; }
			else { this.caravail_groups[2][2]++; }
			if (person.getEmployed().equals(YES)) { this.employed_groups[2]++; }
		}
		else if (person.getAge() < 1000) {
			age_groups[3]++;
			if (person.getLicense().equals(YES)) { this.license_groups[3]++; }
			if (person.getCarAvail().equals(ALWAYS)) { this.caravail_groups[3][0]++; }
			else if (person.getCarAvail().equals(SOMETIMES)) { this.caravail_groups[3][1]++; }
			else { this.caravail_groups[3][2]++; }
			if (person.getEmployed().equals(YES)) { this.employed_groups[3]++; }
		}
		else {
			age_groups[4]++;
			if (person.getLicense().equals(YES)) { this.license_groups[4]++; }
			if (person.getCarAvail().equals(ALWAYS)) { this.caravail_groups[4][0]++; }
			else if (person.getCarAvail().equals(SOMETIMES)) { this.caravail_groups[4][1]++; }
			else { this.caravail_groups[4][2]++; }
			if (person.getEmployed().equals(YES)) { this.employed_groups[4]++; }
		}

		for (int i=0; i<person.getPlans().size(); i++) {
			Plan plan = person.getPlans().get(i);

			int acts = 0;
			for (int j=0; j<plan.getPlanElements().size(); j=j+2) {
				acts++;
				act_cnt++;
			}

			av_act_per_plan = av_act_per_plan * plan_cnt + acts;
			plan_cnt++;
			av_act_per_plan = av_act_per_plan / plan_cnt;
		}

		List<? extends PlanElement> acts_legs = person.getSelectedPlan().getPlanElements();
		double plan_dist = 0.0;
		int plan_row = -1; // plan mode defined as last mode
		                   // (it's just a trick, since the mode is the same for a plan) (just temporary)
		for (int i=1; i<acts_legs.size()-1; i=i+2) {
			ActivityImpl prev_act = (ActivityImpl)acts_legs.get(i-1);
			LegImpl leg = (LegImpl)acts_legs.get(i);
			ActivityImpl next_act = (ActivityImpl)acts_legs.get(i+1);

			// get row (mode type)
			String trip_mode = leg.getMode();
			int trip_row = -1;
			if (WALK.equals(trip_mode)) { trip_row = 0; }
			else if (BIKE.equals(trip_mode)) { trip_row = 1; }
			else if (CAR.equals(trip_mode)) { trip_row = 2; }
			else if (PT.equals(trip_mode)) { trip_row = 3; }
			else if (UNDEF.equals(trip_mode)) { trip_row = 4; }
			else { Gbl.errorMsg("mode=" + trip_mode + " not known!"); }

			// get col (trip dist)
			double trip_dist = CoordUtils.calcDistance(prev_act.getCoord(), next_act.getCoord());
			int trip_col = -1;
			if (trip_dist < 1000) { trip_col = 0; }
			else if (trip_dist < 5000) { trip_col = 1; }
			else if (trip_dist < 10000) { trip_col = 2; }
			else if (trip_dist < 20000) { trip_col = 3; }
			else { trip_col = 4; }

			this.trip_mode_cnts[trip_row][trip_col]++;

			plan_row = trip_row;
			plan_dist += trip_dist;
		}

		// get plan col (plan dist)
		int plan_col = -1;
		if (plan_dist < 5000) { plan_col = 0; }
		else if (plan_dist < 20000) { plan_col = 1; }
		else if (plan_dist < 50000) { plan_col = 2; }
		else if (plan_dist < 100000) { plan_col = 3; }
		else { plan_col = 4; }

		this.plan_mode_cnts[plan_row][plan_col]++;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public final void print() {
		System.out.println("----------------------------------------");
		System.out.println(this.getClass().getName() + ":");
		System.out.println("person_cnt:      " + this.person_cnt);
		System.out.println("plan_cnt:        " + this.plan_cnt);
		System.out.println("act_cnt:         " + this.act_cnt);
		System.out.println("av_act_per_plan: " + this.av_act_per_plan);

		System.out.print("Ages:");
		for (int i=0; i<100; i++) {
			System.out.print("\t" + i);
		}
		System.out.print("\n");
		System.out.print("cnt: ");
		for (int i=0; i<100; i++) {
			System.out.print("\t" + this.ages[i]);
		}
		System.out.print("\n\n");

		System.out.print("Age_groups:\t0-5\t6-7\t8-65\t66-1000\txxx\n");
		System.out.print("cnt:");
		for (int i=0; i<5; i++) {
			System.out.print("\t" + this.age_groups[i]);
		}
		System.out.print("\n");
		System.out.print("nof_license:");
		for (int i=0; i<5; i++) {
			System.out.print("\t" + this.license_groups[i]);
		}
		System.out.print("\n");
		System.out.print("car_always:");
		for (int i=0; i<5; i++) {
			System.out.print("\t" + this.caravail_groups[i][0]);
		}
		System.out.print("\n");
		System.out.print("car_sometimes:");
		for (int i=0; i<5; i++) {
			System.out.print("\t" + this.caravail_groups[i][1]);
		}
		System.out.print("\n");
		System.out.print("car_never:");
		for (int i=0; i<5; i++) {
			System.out.print("\t" + this.caravail_groups[i][2]);
		}
		System.out.print("\n");
		System.out.print("employed:");
		for (int i=0; i<5; i++) {
			System.out.print("\t" + this.employed_groups[i]);
		}
		System.out.print("\n");
		System.out.println("----------------------------------------");
		System.out.println("mode to trip distance of selected plan:");
		System.out.println("\t01\t15\t510\t1020\t20inf");
		for (int i=0; i<5; i++) {
			if (i == 0) { System.out.print(WALK); }
			else if (i == 1) { System.out.print(BIKE); }
			else if (i == 2) { System.out.print(CAR); }
			else if (i == 3) { System.out.print(PT); }
			else { System.out.print(UNDEF); }
			for (int j=0; j<5; j++) {
				System.out.print("\t" + this.trip_mode_cnts[i][j]);
			}
			System.out.print("\n");
		}
		System.out.println("----------------------------------------");
		System.out.println("mode to plan distance of selected plan:");
		System.out.println("\t05\t520\t2050\t50100\t100inf");
		for (int i=0; i<5; i++) {
			if (i == 0) { System.out.print(WALK); }
			else if (i == 1) { System.out.print(BIKE); }
			else if (i == 2) { System.out.print(CAR); }
			else if (i == 3) { System.out.print(PT); }
			else { System.out.print(UNDEF); }
			for (int j=0; j<5; j++) {
				System.out.print("\t" + this.plan_mode_cnts[i][j]);
			}
			System.out.print("\n");
		}
		System.out.println("----------------------------------------");
	}

	@Override
	public void run(Plan plan) {
	}
}
