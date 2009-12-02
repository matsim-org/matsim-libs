/* *********************************************************************** *
 * project: org.matsim.*
 * Households.java
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

package playground.balmermi.census2000v2.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.collections.Tuple;

public class MicroCensus {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(MicroCensus.class);

	private final Group[] groups = new Group[128];
	private static final String WORK = "w";
	private static final String EDUC = "e";
	private static final String MALE = "m";
	private static final String FEMALE = "f";
	private static final String YES = "yes";
	private static final String NO = "no";

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public MicroCensus(PopulationImpl pop) {
		for (int i=0; i<groups.length; i++) { groups[i] = null; }
		this.create(pop);
		MatsimRandom.getRandom().nextDouble();
	}

	//////////////////////////////////////////////////////////////////////
	// inner classes
	//////////////////////////////////////////////////////////////////////

	class Group {
		private final List<Tuple<Double,Person>> list = new ArrayList<Tuple<Double,Person>>();

		private final void addTuple(Double weight, Person person) {
			Tuple<Double,Person> t = new Tuple<Double, Person>(weight,person);
			list.add(t);
		}

		private final void setWeight(double weight, int index) {
			Tuple<Double,Person> t = list.remove(index);
			if (t == null) { Gbl.errorMsg("No tuple at index "+ index); }
			Tuple<Double,Person> t_new = new Tuple<Double, Person>(weight,t.getSecond());
			list.add(index,t_new);
		}

		private final void print() {
			for (int i=0; i<list.size(); i++) {
				Tuple<Double,Person> t = list.get(i);
				System.out.println(i+": ("+t.getFirst()+","+t.getSecond().getId()+")");
			}
		}

		private final Person getRandomPerson() {
			int i = MatsimRandom.getRandom().nextInt(list.size());
			return list.get(i).getSecond();
		}

		private final Person getRandomWeightedPerson() {
			double r = MatsimRandom.getRandom().nextDouble();
			double weight_sum = 0.0;
			for (int i=0; i<list.size(); i++) {
				weight_sum += list.get(i).getFirst();
				if (r < weight_sum) { return list.get(i).getSecond(); }
			}
			Gbl.errorMsg("It should never reach this line!");
			return null;
		}
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final int group2index(int age, String sex, String lic, boolean has_work, boolean has_educ) {
		int index = 0;
		if (age < 7) {index += 0; }
		else if (age < 15) { index += 1; }
		else if (age < 18) { index += 2; }
		else if (age < 66) { index += 3; }
		else { index += 4; }

		if (sex.equals(MALE)) { index += 8; }
		if (lic.equals(YES)) { index += 16; }
		if (has_work) { index += 32; }
		if (has_educ) { index += 64; }
		return index;
	}

	private final String index2group(final int index, int age, String sex, String lic, boolean has_work, boolean has_educ) {
		int i = index;
		if (i/64 == 1) { has_educ = true; } else { has_educ = false; }
		i = i % 64;
		if (i/32 == 1) { has_work = true; } else { has_work = false; }
		i = i % 32;
		if (i/16 == 1) { lic = YES; } else { lic = NO; }
		i = i % 16;
		if (i/8 == 1) { sex = MALE; } else { sex = FEMALE; }
		i = i % 8;
		if (i == 0) { age = 6; }
		else if (i == 1) { age = 14; }
		else if (i == 2) { age = 17; }
		else if (i == 3) { age = 65; }
		else if (i == 4) { age = 66; }
		else { Gbl.errorMsg("index="+index+" not allowed!"); }
		return "e("+has_educ+");w("+has_work+");lic("+lic+");sex("+sex+");age("+age+")";
	}

	private final void create(PopulationImpl pop) {
		double weight_sum = 0.0;
		for (Person p : pop.getPersons().values()) {
			weight_sum += p.getSelectedPlan().getScore().doubleValue();
		}
		for (Person pp : pop.getPersons().values()) {
			PersonImpl p = (PersonImpl) pp;
			int age = p.getAge();
			String sex = p.getSex();
			String lic = p.getLicense();
			boolean has_work = false;
			boolean has_educ = false;
			for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl a = (ActivityImpl) pe;
					if (a.getType().equals(WORK)) { has_work = true; }
					if (a.getType().equals(EDUC)) { has_educ = true; }
				}
			}

			int index = this.group2index(age,sex,lic,has_work,has_educ);
			Group g = groups[index];
			if (g == null) { g = new Group(); groups[index] = g; }
			g.addTuple(p.getSelectedPlan().getScore().doubleValue()/weight_sum,p);
		}

		for (int i=0; i<this.groups.length; i++) {
			Group g = this.groups[i];
			if (g != null) {
				double group_weight_sum = 0.0;
				for (int j=0; j<g.list.size(); j++) {
					group_weight_sum += g.list.get(j).getFirst();
				}
				for (int j=0; j<g.list.size(); j++) {
					Tuple<Double,Person> t = g.list.get(j);
					g.setWeight(t.getFirst()/group_weight_sum,j);
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final Person getRandomMZPerson(int age, String sex, String lic, boolean has_work, boolean has_educ) {
		int index = this.group2index(age,sex,lic,has_work,has_educ);
		Group g = groups[index];
		if (g == null) {
			log.warn("index="+index+", group="+index2group(index,age,sex,lic,has_work,has_educ)+": group does not exist!");
			return null;
		}
		return g.getRandomPerson();
	}

	public final Person getRandomWeightedMZPerson(int age, String sex, String lic, boolean has_work, boolean has_educ) {
		int index = this.group2index(age,sex,lic,has_work,has_educ);
		Group g = groups[index];
		if (g == null) {
			log.warn("index="+index+", group="+index2group(index,age,sex,lic,has_work,has_educ)+": group does not exist!");
			return null;
		}
		return g.getRandomWeightedPerson();
	}

	//////////////////////////////////////////////////////////////////////
	// set/create methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// public methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public final void print() {
		for (int i=0; i<groups.length; i++) {
			Group g = groups[i];
			if (g != null) {
				System.out.println("group["+i+"]: "+this.index2group(i,-1,null,null,false,false));
				g.print();
			}
			else {
				System.out.println("group["+i+"]: empty");
			}
		}
	}
}
