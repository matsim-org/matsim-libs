/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesSetCapacity.java
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

package playground.balmermi.census2000v2.modules;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;

import playground.balmermi.census2000v2.data.Household;
import playground.balmermi.census2000v2.data.Households;
import playground.balmermi.census2000v2.data.Human;
import playground.balmermi.census2000v2.data.Humans;

public class HumansCreateFromCensus2000 {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final String infile;
	private final Households households;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public HumansCreateFromCensus2000(final String infile, final Households households) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.infile = infile;
		this.households = households;
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(Humans humans) {
		System.out.println("    running " + this.getClass().getName() + " module...");

		if (!humans.getHumans().isEmpty()) { Gbl.errorMsg("Humans DB is not empty!"); }
		
		try {
			FileReader fr = new FileReader(this.infile);
			BufferedReader br = new BufferedReader(fr);
			int line_cnt = 0;
			
			// keep track of the P_PARTNR
			// HashMap<Id P_PERSON_ID,Id P_PARTNR>
			HashSet<Id> pids = new HashSet<Id>();

			// Skip header
			String curr_line = br.readLine(); line_cnt++;
			curr_line = br.readLine(); line_cnt++;
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				//P_ZGDE  P_GEBAEUDE_ID  P_HHNR  P_PERSON_ID  P_WKAT  P_GEM2  P_PARTNR
				//1       2              3       5            10      11      12
				
				// check for existing household
				Id hhnr = new IdImpl(entries[3]);
				Household hh = households.getHousehold(hhnr);
				if (hh == null) { Gbl.errorMsg("Line "+line_cnt+": Household id="+hhnr+" does not exist!"); }

				Id pid = new IdImpl(entries[5]);
				int wkat = Integer.parseInt(entries[10]);
				int gem2 = Integer.parseInt(entries[11]);
				int partnr = Integer.parseInt(entries[12]);
				
				// error heading
				String e_head = "Line "+line_cnt+": case("+pid+","+wkat+","+gem2+","+partnr+") ";

				// allowed combinations:
				// wkat  gem2  partnr  occurence  meaning
				// 1     -9    -9      1/person   person does have only one household (z and w)
				// 3     -7    -7      1/person   person is ONLY part of the 'wirtschaftliche wohnbevoelkerung' (w)
				// 4     -7    -7      1/person   person is ONLY part of the 'zivilrechtliche wohnbevoelkerung' (z)
				// 3     id    id      2/person   person is part of w and z. current line reflects w
				// 4     id    id      2/person   person is part of w and z. current line reflects z
				if ((wkat == 1) && (gem2 == -9) && (partnr == -9)) {
					Human h = humans.getHuman(pid);
					if (h != null) { Gbl.errorMsg(e_head+"human alread exists!"); }
					h = new Human(pid);
					h.setHouseholdW(hh);
					h.setHouseholdZ(hh);
					humans.addHuman(h);
				}
				else if ((wkat == 3) && (gem2 == -7) && (partnr == -7)) {
					Human h = humans.getHuman(pid);
					if (h != null) { Gbl.errorMsg(e_head+"human alread exists!"); }
					h = new Human(pid);
					h.setHouseholdW(hh);
					h.setHouseholdZ(null);
					humans.addHuman(h);
				}
				else if ((wkat == 4) && (gem2 == -7) && (partnr == -7)) {
					Human h = humans.getHuman(pid);
					if (h != null) { Gbl.errorMsg(e_head+"human alread exists!"); }
					h = new Human(pid);
					h.setHouseholdW(null);
					h.setHouseholdZ(hh);
					humans.addHuman(h);
				}
				else if ((wkat == 3) && ((1 <= gem2)&&(gem2 <= 7011)) && ((1 <= partnr)&&(partnr <= 999999999))) {
					Human h = humans.getHuman(new IdImpl(partnr));
					if (h == null) {
						if (!pids.add(pid)) { Gbl.errorMsg(e_head+"partner human not found, but pid found in the set!"); }
						h = new Human(pid);
						h.setHouseholdW(hh);
						h.setHouseholdZ(null);
						humans.addHuman(h);
					}
					else {
						if (!pids.remove(new IdImpl(partnr))) { Gbl.errorMsg(e_head+"partner human found, but not found in the set!"); }
						if (!((h.getHouseholdW() == null) && (h.getHouseholdZ() != null))) { Gbl.errorMsg(e_head+"something is wrong!"); }
						h.setHouseholdW(hh);
					}
				}
				else if ((wkat == 4) && ((1 <= gem2)&&(gem2 <= 7011)) && ((1 <= partnr)&&(partnr <= 999999999))) {
					Human h = humans.getHuman(new IdImpl(partnr));
					if (h == null) {
						if (!pids.add(pid)) { Gbl.errorMsg(e_head+"partner human not found, but pid found in the set!"); }
						h = new Human(pid);
						h.setHouseholdW(null);
						h.setHouseholdZ(hh);
						humans.addHuman(h);
					}
					else {
						if (!pids.remove(new IdImpl(partnr))) { Gbl.errorMsg(e_head+"partner human found, but not found in the set!"); }
						if (!((h.getHouseholdW() != null) && (h.getHouseholdZ() == null))) { Gbl.errorMsg(e_head+"something is wrong!"); }
						h.setHouseholdZ(hh);
					}
				}
				else { Gbl.errorMsg(e_head+"not allowed!"); }

				// progress report
				if (line_cnt % 100000 == 0) {
					System.out.println("    Line " + line_cnt + ": # humans = " + humans.getHumans().size() + "; # pids = " + pids.size());
				}
				line_cnt++;
			}
			br.close();
			fr.close();
			
			// some info
			System.out.println("    "+humans.getHumans().size()+" humans created! (#pids="+pids.size()+")");
			int same_cnt = 0; int diff_cnt = 0; int wonly_cnt = 0; int zonly_cnt = 0;
			for (Human h : humans.getHumans().values()) {
				if ((h.getHouseholdW()==null)&&(h.getHouseholdZ()==null)) { Gbl.errorMsg("WAHHH!"); }
				else if ((h.getHouseholdW()==null)&&(h.getHouseholdZ()!=null)) { zonly_cnt++; }
				else if ((h.getHouseholdW()!=null)&&(h.getHouseholdZ()==null)) { wonly_cnt++; }
				else { if (h.getHouseholdW().equals(h.getHouseholdZ())) { same_cnt++; } else { diff_cnt++; }
				}
			}
			System.out.println("    # zivil only          = " + zonly_cnt);
			System.out.println("    # econo only          = " + wonly_cnt);
			System.out.println("    # one household only  = " + same_cnt);
			System.out.println("    # two households      = " + diff_cnt);
			System.out.println("    left over pids:");
			for (Id pid : pids) { System.out.println("    "+pid); }
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		System.out.println("    done.");
	}
}
