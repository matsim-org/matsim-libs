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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.basic.v01.Id;

import playground.balmermi.census2000.data.Municipalities;

public class Households {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final HashMap<Id,Household> households = new HashMap<Id, Household>();
	private final Municipalities municipalities;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Households(Municipalities municipalities) {
		super();
		this.municipalities = municipalities;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final Household getHousehold(final Id id) {
		return this.households.get(id);
	}
	
	public final HashMap<Id,Household> getHouseholds() {
		return this.households;
	}
	
	public final Municipalities getMunicipalities() {
		return this.municipalities;
	}

	//////////////////////////////////////////////////////////////////////
	// set/create methods
	//////////////////////////////////////////////////////////////////////
	
	public final void addHH(Household hh) {
		this.households.put(hh.getId(),hh);
	}

	//////////////////////////////////////////////////////////////////////
	// public methods
	//////////////////////////////////////////////////////////////////////

	public final void print() {
		System.out.println("---------- printing households ----------");
		System.out.println(this.toString());
		for (Household hh : this.households.values()) {
			System.out.println(hh.toString());
		}
		System.out.println("------- printing households done. -------");
	}
	
	//////////////////////////////////////////////////////////////////////

	public final void writeTable(String outfile) {
		try {
			FileWriter fw = new FileWriter(outfile);
			BufferedWriter out = new BufferedWriter(fw);
			out.write("hh_id\tz_id\tf_id\thhtpw\thhtpz\tp_w_list\tp_z_list\n");
			out.flush();
			for (Household hh : this.households.values()) {
				out.write(hh.getId()+"\t"+
				          hh.getMunicipality().getZone().getId()+"\t"+
				          hh.getFacility().getId()+"\t"+
				          hh.getHHTPW()+"\t"+
				          hh.getHHTPZ()+"\t");
				for (Id id : hh.getPersonsW().keySet()) { out.write(id+";"); } out.write("\t");
				for (Id id : hh.getPersonsZ().keySet()) { out.write(id+";"); } out.write("\n");
			}
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[nof_munis=" + this.municipalities.getMunicipalities().size() + "]" +
			"[nof_hhs=" + this.households.size() + "]";
	}
}
