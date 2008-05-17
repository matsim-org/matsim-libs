/* *********************************************************************** *
 * project: org.matsim.*
 * PlansFilterArea.java
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
import java.util.HashSet;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;

import playground.balmermi.census2000v2.data.CAtts;
import playground.balmermi.census2000v2.data.Household;
import playground.balmermi.census2000v2.data.Households;

public class PlansCreateFromCensus2000 {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PlansCreateFromCensus2000.class);

	private final String infile;
	private final Households households;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCreateFromCensus2000(final String infile, final Households households) {
		super();
		log.info("    init " + this.getClass().getName() + " module...");
		this.infile = infile;
		this.households = households;
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void addDemographics(final Person person, final String[] entries, int wkat) {
		Map<String,Object> p_atts = person.getCustomAttributes();
		person.setAge(Integer.parseInt(entries[CAtts.I_ALTJ]));
		if (entries[CAtts.I_GESL].equals("1")) { person.setSex("m"); } else { person.setSex("f"); }
		if (entries[CAtts.I_HMAT].equals("1")) { p_atts.put(CAtts.P_HMAT,true); } else { p_atts.put(CAtts.P_HMAT,false); }
	}
	
	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final Plans plans) {
		log.info("    running " + this.getClass().getName() + " algorithm...");

		if (!plans.getPersons().isEmpty()) { Gbl.errorMsg("plans DB is not empty!"); }
		plans.setName("created by '" + this.getClass().getName() + "'");

		try {
			FileReader fr = new FileReader(this.infile);
			BufferedReader br = new BufferedReader(fr);
			int line_cnt = 0;
			
			// keep track of the pids which need to be found twice
			HashSet<Id> pids = new HashSet<Id>();

			// Skip header
			String curr_line = br.readLine(); line_cnt++;
			curr_line = br.readLine(); line_cnt++;
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				//P_ZGDE  P_GEBAEUDE_ID  P_HHNR  P_PERSON_ID  P_WKAT  P_GEM2  P_PARTNR
				//1       2              3       5            10      11      12
				
				// check for existing household
				Id hhnr = new IdImpl(entries[CAtts.I_HHNR]);
				Household hh = households.getHousehold(hhnr);
				if (hh == null) { Gbl.errorMsg("Line "+line_cnt+": Household id="+hhnr+" does not exist!"); }

				Id pid = new IdImpl(entries[5]);
				int wkat = Integer.parseInt(entries[CAtts.I_WKAT]);
				int gem2 = Integer.parseInt(entries[CAtts.I_GEM2]);
				int partnr = Integer.parseInt(entries[CAtts.I_PARTNR]);
				
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
					Person p = plans.getPerson(pid);
					if (p != null) { Gbl.errorMsg(e_head+"person alread exists!"); }
					p = new Person(pid);
					Map<String,Object> p_atts = p.getCustomAttributes();
					p_atts.put(CAtts.HH_W,hh); hh.getPersonsW().put(p.getId(),p);
					p_atts.put(CAtts.HH_Z,hh); hh.getPersonsZ().put(p.getId(),p);
					plans.addPerson(p);
					this.addDemographics(p,entries,wkat);
				}
				else if ((wkat == 3) && (gem2 == -7) && (partnr == -7)) {
					Person p = plans.getPerson(pid);
					if (p != null) { Gbl.errorMsg(e_head+"person alread exists!"); }
					p = new Person(pid);
					Map<String,Object> p_atts = p.getCustomAttributes();
					p_atts.put(CAtts.HH_W,hh); hh.getPersonsW().put(p.getId(),p);
					p_atts.put(CAtts.HH_Z,null);
					plans.addPerson(p);
					this.addDemographics(p,entries,wkat);
				}
				else if ((wkat == 4) && (gem2 == -7) && (partnr == -7)) {
					Person p = plans.getPerson(pid);
					if (p != null) { Gbl.errorMsg(e_head+"person alread exists!"); }
					p = new Person(pid);
					Map<String,Object> p_atts = p.getCustomAttributes();
					p_atts.put(CAtts.HH_W,null);
					p_atts.put(CAtts.HH_Z,hh); hh.getPersonsZ().put(p.getId(),p);
					plans.addPerson(p);
					this.addDemographics(p,entries,wkat);
				}
				else if ((wkat == 3) && ((1 <= gem2)&&(gem2 <= 7011)) && ((1 <= partnr)&&(partnr <= 999999999))) {
					Person p = plans.getPerson(new IdImpl(partnr));
					if (p == null) {
						if (!pids.add(pid)) { Gbl.errorMsg(e_head+"partner person not found, but pid found in the set!"); }
						p = new Person(pid);
						Map<String,Object> p_atts = p.getCustomAttributes();
						p_atts.put(CAtts.HH_W,hh); hh.getPersonsW().put(p.getId(),p);
						p_atts.put(CAtts.HH_Z,null);
						plans.addPerson(p);
						this.addDemographics(p,entries,wkat);
					}
					else {
						if (!pids.remove(new IdImpl(partnr))) { Gbl.errorMsg(e_head+"partner person found, but not found in the set!"); }
						Map<String,Object> p_atts = p.getCustomAttributes();
						if (!((p_atts.get(CAtts.HH_W) == null) && (p_atts.get(CAtts.HH_Z) != null))) { Gbl.errorMsg(e_head+"something is wrong!"); }
						p_atts.put(CAtts.HH_W,hh); hh.getPersonsW().put(p.getId(),p);
						this.addDemographics(p,entries,wkat);
					}
				}
				else if ((wkat == 4) && ((1 <= gem2)&&(gem2 <= 7011)) && ((1 <= partnr)&&(partnr <= 999999999))) {
					Person p = plans.getPerson(new IdImpl(partnr));
					if (p == null) {
						if (!pids.add(pid)) { Gbl.errorMsg(e_head+"partner person not found, but pid found in the set!"); }
						p = new Person(pid);
						Map<String,Object> p_atts = p.getCustomAttributes();
						p_atts.put(CAtts.HH_W,null);
						p_atts.put(CAtts.HH_Z,hh); hh.getPersonsZ().put(p.getId(),p);
						plans.addPerson(p);
						this.addDemographics(p,entries,wkat);
					}
					else {
						if (!pids.remove(new IdImpl(partnr))) { Gbl.errorMsg(e_head+"partner person found, but not found in the set!"); }
						Map<String,Object> p_atts = p.getCustomAttributes();
						if (!((p_atts.get(CAtts.HH_W) != null) && (p_atts.get(CAtts.HH_Z) == null))) { Gbl.errorMsg(e_head+"something is wrong!"); }
						p_atts.put(CAtts.HH_Z,hh); hh.getPersonsZ().put(p.getId(),p);
						this.addDemographics(p,entries,wkat);
					}
				}
				else { Gbl.errorMsg(e_head+"not allowed!"); }

				// progress report
				if (line_cnt % 100000 == 0) {
					log.info("    Line " + line_cnt + ": # persons = " + plans.getPersons().size() + "; # pids = " + pids.size());
				}
				line_cnt++;
			}
			br.close();
			fr.close();
			
			// some info
			log.info("    "+plans.getPersons().size()+" persons created! (#pids="+pids.size()+")");
			int same_cnt = 0; int diff_cnt = 0; int wonly_cnt = 0; int zonly_cnt = 0;
			for (Person p : plans.getPersons().values()) {
				Map<String,Object> p_atts = p.getCustomAttributes();
				if ((p_atts.get(CAtts.HH_W)==null)&&(p_atts.get(CAtts.HH_Z)==null)) { Gbl.errorMsg("WAHHH!"); }
				else if ((p_atts.get(CAtts.HH_W)==null)&&(p_atts.get(CAtts.HH_Z)!=null)) { zonly_cnt++; }
				else if ((p_atts.get(CAtts.HH_W)!=null)&&(p_atts.get(CAtts.HH_Z)==null)) { wonly_cnt++; }
				else { if (p_atts.get(CAtts.HH_W).equals(p_atts.get(CAtts.HH_Z))) { same_cnt++; } else { diff_cnt++; } }
			}
			log.info("    # civil only          = " + zonly_cnt);
			log.info("    # econo only          = " + wonly_cnt);
			log.info("    # one household only  = " + same_cnt);
			log.info("    # two households      = " + diff_cnt);
			log.info("    left over pids:");
			for (Id pid : pids) { log.info("    "+pid); }
		} catch (Exception e) {
			Gbl.errorMsg(e);
		}
		log.info("    done.");
	}
}
