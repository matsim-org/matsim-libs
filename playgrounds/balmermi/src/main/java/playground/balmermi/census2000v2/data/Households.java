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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;

import playground.balmermi.census2000.data.Municipalities;
import playground.balmermi.census2000.data.Municipality;

public class Households {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(Households.class);

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

	public final void parse(String infile, Population plans, ActivityFacilitiesImpl facilities) {
		int line_cnt = 0;
		try {
			FileReader fr = new FileReader(infile);
			BufferedReader br = new BufferedReader(fr);

			// Skip header
			String curr_line = br.readLine(); line_cnt++;
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// hh_id    z_id  f_id    hhtpw  hhtpz  p_w_list          p_z_list
				// 1404079  3103  914921  2111   2111   5742752;5757646;  5742752;5757646;
				// 0        1     2       3      4      5                 6

				Id hhid = new IdImpl(entries[0]);
				Id zid = new IdImpl(entries[1]);
				int mid = Integer.parseInt(zid.toString());
				Id fid = new IdImpl(entries[2]);
				int hhtpw = Integer.parseInt(entries[3]);
				int hhtpz = Integer.parseInt(entries[4]);

				Municipality m = this.municipalities.getMunicipality(mid);
				ActivityFacilityImpl f = facilities.getFacilities().get(fid);

				Household hh = new Household(hhid,m,f);
				if (hhtpw != Integer.MIN_VALUE) { hh.setHHTPW(hhtpw); }
				if (hhtpz != Integer.MIN_VALUE) { hh.setHHTPZ(hhtpz); }
				this.addHH(hh);

				String p_w_list = entries[5];
				String p_z_list = entries[6];
				entries = p_w_list.split(";",-1);
				for (int i=0; i<entries.length-1; i++) {
					Id pid = new IdImpl(entries[i]);
					Person p = plans.getPersons().get(pid);
					if (p == null) { Gbl.errorMsg("that should not happen!"); }
					if (hh.getPersonsW().put(p.getId(),p) !=  null) { Gbl.errorMsg("that should not happen!"); }
					if (p.getCustomAttributes().put(CAtts.HH_W,hh) != null) {
						Gbl.errorMsg("hhid="+hh.getId()+", pid="+p.getId()+": person does already have a "+CAtts.HH_W+" assigned!");
					}
				}
				entries = p_z_list.split(";",-1);
				for (int i=0; i<entries.length-1; i++) {
					Id pid = new IdImpl(entries[i]);
					Person p = plans.getPersons().get(pid);
					if (p == null) { Gbl.errorMsg("that should not happen!"); }
					if (hh.getPersonsZ().put(p.getId(),p) != null) { Gbl.errorMsg("that should not happen!"); }
					if (p.getCustomAttributes().put(CAtts.HH_Z,hh) != null) {
						Gbl.errorMsg("hhid="+hh.getId()+", pid="+p.getId()+": person does already have a "+CAtts.HH_Z+" assigned!");
					}
				}
				// progress report
				if (line_cnt % 100000 == 0) { log.info("      Line " + line_cnt); }
				line_cnt++;
			}
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

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
