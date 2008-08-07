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

package org.matsim.population.algorithms;

import org.matsim.population.Person;
import org.matsim.population.Plan;

public class PersonSummary extends PersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private int person_cnt = 0;
	private int plan_cnt = 0;
	private int act_cnt = 0;
	private double av_act_per_plan = 0.0;

	private final int [] ages = new int[100];
	private final int [] age_groups = new int[5];
	private final int [] license_groups = new int[5];
	private final int [][] caravail_groups = new int[5][3];
	private final int [] employed_groups = new int[5];

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSummary() {
		super();
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
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		if (person.getAge() > 99) {
//			Gbl.warningMsg(this.getClass(),"run(...)","[person_id=" + person.getId() + ", older than 99 years. Excluded from summary]");
			return;
		}

		this.person_cnt++;

		if (person.getAge() != Integer.MIN_VALUE) {
			this.ages[person.getAge()]++;
		}

		if (person.getAge() < 7) {
			this.age_groups[0]++;
			if (person.getLicense().equals("yes")) { this.license_groups[0]++; }
			if (person.getCarAvail().equals("always")) { this.caravail_groups[0][0]++; }
			else if (person.getCarAvail().equals("sometimes")) { this.caravail_groups[0][1]++; }
			else { this.caravail_groups[0][2]++; }
			if (person.getEmployed().equals("yes")) { this.employed_groups[0]++; }
		}
		else if (person.getAge() < 18) {
			this.age_groups[1]++;
			if (person.getLicense().equals("yes")) { this.license_groups[1]++; }
			if (person.getCarAvail().equals("always")) { this.caravail_groups[1][0]++; }
			else if (person.getCarAvail().equals("sometimes")) { this.caravail_groups[1][1]++; }
			else { this.caravail_groups[1][2]++; }
			if (person.getEmployed().equals("yes")) { this.employed_groups[1]++; }
		}
		else if (person.getAge() < 28) {
			this.age_groups[2]++;
			if (person.getLicense().equals("yes")) { this.license_groups[2]++; }
			if (person.getCarAvail().equals("always")) { this.caravail_groups[2][0]++; }
			else if (person.getCarAvail().equals("sometimes")) { this.caravail_groups[2][1]++; }
			else { this.caravail_groups[2][2]++; }
			if (person.getEmployed().equals("yes")) { this.employed_groups[2]++; }
		}
		else if (person.getAge() < 65) {
			this.age_groups[3]++;
			if (person.getLicense().equals("yes")) { this.license_groups[3]++; }
			if (person.getCarAvail().equals("always")) { this.caravail_groups[3][0]++; }
			else if (person.getCarAvail().equals("sometimes")) { this.caravail_groups[3][1]++; }
			else { this.caravail_groups[3][2]++; }
			if (person.getEmployed().equals("yes")) { this.employed_groups[3]++; }
		}
		else {
			this.age_groups[4]++;
			if (person.getLicense().equals("yes")) { this.license_groups[4]++; }
			if (person.getCarAvail().equals("always")) { this.caravail_groups[4][0]++; }
			else if (person.getCarAvail().equals("sometimes")) { this.caravail_groups[4][1]++; }
			else { this.caravail_groups[4][2]++; }
			if (person.getEmployed().equals("yes")) { this.employed_groups[4]++; }
		}

		for (int i=0; i<person.getPlans().size(); i++) {
			Plan plan = person.getPlans().get(i);

			int acts = 0;
			for (int j=0; j<plan.getActsLegs().size(); j=j+2) {
//				Act act = (Act)plan.getActsLegs().get(j);
				acts++;
				this.act_cnt++;
			}

			this.av_act_per_plan = this.av_act_per_plan * this.plan_cnt + acts;
			this.plan_cnt++;
			this.av_act_per_plan = this.av_act_per_plan / this.plan_cnt;
		}
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

		System.out.print("Age_groups:\t0-6\t7-17\t18-27\t28-64\t65-99\n");
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
	}
}
