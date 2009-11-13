/* *********************************************************************** *
 * project: org.matsim.*
 * PersonCensusSummaryTables.java
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

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.balmermi.census2000.data.Household;
import playground.balmermi.census2000.data.Municipality;
import playground.balmermi.census2000.data.Persons;

public class PersonCensusSummaryTables extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private FileWriter fw = null;
	private BufferedWriter out = null;
	private final Persons persons;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonCensusSummaryTables(String outfile, Persons persons) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.persons = persons;
		try {
			fw = new FileWriter(outfile);
			out = new BufferedWriter(fw);
			out.write("p_id\thh_id\tx_coord\ty_coord\tp_age\tp_sex\t" +
			          "p_swiss\tp_employed\tp_caravail\tp_license\t" +
			          "hh_cat\thh_pcnt\thh_kidcnt\t" +
			          "muni_id\tkt_id\tmuni_regtype\tmuni_eink\tmuni_fuel\n");
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
		try {
			out.write(person.getId() + "\t");
			out.write(hh.getId() + "\t");
			out.write(hh.getCoord().getX() + "\t");
			out.write(hh.getCoord().getY() + "\t");
			out.write(person.getAge() + "\t");
			out.write(person.getSex() + "\t");
			out.write(p.isSwiss() + "\t");
			out.write(person.isEmployed() + "\t");
			out.write(person.getCarAvail() + "\t");
			out.write(person.getLicense() + "\t");
			out.write(hh.getCategory() + "\t");
			out.write(hh.getPersonCount() + "\t");
			out.write(hh.getKidCount() + "\t");
			out.write(muni.getId() + "\t");
			out.write(muni.getCantonId() + "\t");
			out.write(muni.getRegType() + "\t");
			out.write(muni.getIncome() + "\t");
			out.write(muni.getFuelCost() + "\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void run(Plan plan) {
	}
}
