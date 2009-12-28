/* *********************************************************************** *
 * project: org.matsim.*
 * PersonMunicipalitySummaryTable.java
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
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.balmermi.census2000.data.Household;
import playground.balmermi.census2000.data.Municipality;
import playground.balmermi.census2000.data.Persons;

public class PersonMunicipalitySummaryTable extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String YES = "yes";

	private FileWriter fw = null;
	private BufferedWriter out = null;
	private final Persons persons;
	TreeMap<Municipality,int[]> munis = new TreeMap<Municipality,int[]>();
	TreeSet<Household> hhs = new TreeSet<Household>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonMunicipalitySummaryTable(String outfile, Persons persons) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.persons = persons;
		try {
			fw = new FileWriter(outfile);
			out = new BufferedWriter(fw);
			out.write("m_id\tk_id\treg_type\tincome\tfuelcost\t" +
			          "p_cnt\thh_cnt\tage0-17\tage18-65\tage66-\t" +
			          "male\tswiss\temployed\tlicense\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// final method
	//////////////////////////////////////////////////////////////////////

	public final void close() {
		try {
			Iterator<Municipality> m_it = this.munis.keySet().iterator();
			while (m_it.hasNext()) {
				Municipality muni = m_it.next();
				int[] v = this.munis.get(muni);

				out.write(muni.getId() + "\t");
				out.write(muni.getCantonId() + "\t");
				out.write(muni.getRegType() + "\t");
				out.write(muni.getIncome() + "\t");
				out.write(muni.getFuelCost() + "");
				for (int i=0; i<v.length; i++) { out.write("\t" + v[i]); }
				out.write("\n");
				out.flush();
			}
			out.flush();
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person pp) {
		PersonImpl person = (PersonImpl) pp;
		playground.balmermi.census2000.data.MyPerson p = persons.getPerson(Integer.valueOf(person.getId().toString()));
		Household hh = p.getHousehold();
		Municipality muni = hh.getMunicipality();

		// {p_cnt, hh_cnt, age[0-17], age[18-65], age[66-...],
		//  male, swiss, employed, license}
		int[] v;
		if(this.munis.containsKey(muni)) { v = this.munis.get(muni); }
		else {
			v = new int[9];
			v[0] = v[1] = v[2] = v[3] = v[4] = v[5] = v[6] = v[7] = v[8] = 0;
			this.munis.put(muni,v);
		}
		v[0]++;
		if (!this.hhs.contains(hh)) { v[1]++; this.hhs.add(hh); }

		if (person.getAge() < 18) { v[2]++; }
		else if (person.getAge() < 66) { v[3]++; }
		else { v[4]++; }

		if (p.isMale()) { v[5]++; }
		if (p.isSwiss()) { v[6]++; }
		if (person.isEmployed()) { v[7]++; }
		if (YES.equals(person.getLicense())) { v[8]++; }
	}

	public void run(Plan plan) {
	}
}
