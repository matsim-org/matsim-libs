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

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;

import playground.balmermi.census2000v2.data.Household;
import playground.balmermi.census2000v2.data.Households;

public class PlansCreateFromCensus2000 {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String HH_Z = "hh_z";
	private static final String HH_W = "hh_w";
	private static final String SWISS = "swiss";
	private final String infile;
	private final Households households;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCreateFromCensus2000(final String infile, final Households households) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.infile = infile;
		this.households = households;
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void addDemographics(final Person person, final String[] entries) {
		Map<String,Object> p_atts = person.getCustomAttributes();
//		16	P_ALTJ
		person.setAge(Integer.parseInt(entries[16]));
//		23	P_GESL
		if (entries[23].equals("1")) { person.setSex("m"); } else { person.setSex("f"); }
//		26	P_HMAT
		if (entries[26].equals("1")) { p_atts.put(SWISS,true); } else { p_atts.put(SWISS,false); }
//		43	P_STHHZ
//		44	P_STHHW
//		49	P_HHTPZ
//		50	P_HHTPW
//		51	P_APERZ
//		52	P_APERW
//		53	P_WKATA
//		54	P_SPRA
//		76	P_GEGW
//		77	P_HABG
//		78	P_UHAB
//		79	P_FHAB
//		80	P_HFAB
//		81	P_HBAB
//		82	P_LSAB
//		83	P_MPAB
//		84	P_BLAB
//		85	P_BSAB
//		86	P_OSAB
//		87	P_KAUS
//		88	P_AMS
//		89	P_BGRAD
//		90	P_KAZEIT
//		91	P_ERWS
//		93	P_KAMS
//		102	P_IAUS
//		103	P_RENT
//		112	P_ERBE 
//		113	P_PBER
//		114	P_ISCO
//		115	P_SOPK
//		118	P_ANOGA
//		120	P_AGDE
//		127	P_APEND
//		129	P_AWOFT
//		131	P_AVEMI
//		133	P_AVMKE
//		134	P_AVELO
//		135	P_AMOFA
//		136	P_AMRAD
//		137	P_APKWL
//		138	P_APKWM
//		139	P_AWBUS
//		140	P_ABAHN
//		141	P_ATRAM
//		142	P_APOST
//		143	P_AVAND
//		144	P_SGDE
//		151	P_SPEND
//		155	P_SVEMI
//		157	P_SVMKE
//		158	P_SVELO
//		159	P_SMOFA
//		160	P_SMRAD
//		161	P_SPKWL
//		162	P_SPKWM
//		163	P_SSBUS
//		164	P_SBAHN
//		165	P_STRAM
//		166	P_SPOST
//		167	P_SVAND
//		168	P_SNOGA
//		169	P_SREFO
	}
	
	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final Plans plans) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

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
					Person p = plans.getPerson(pid);
					if (p != null) { Gbl.errorMsg(e_head+"person alread exists!"); }
					p = new Person(pid);
					Map<String,Object> p_atts = p.getCustomAttributes();
					p_atts.put(PlansCreateFromCensus2000.HH_W,hh);
					p_atts.put(HH_Z,hh);
					plans.addPerson(p);
					this.addDemographics(p,entries);
				}
				else if ((wkat == 3) && (gem2 == -7) && (partnr == -7)) {
					Person p = plans.getPerson(pid);
					if (p != null) { Gbl.errorMsg(e_head+"person alread exists!"); }
					p = new Person(pid);
					Map<String,Object> p_atts = p.getCustomAttributes();
					p_atts.put(PlansCreateFromCensus2000.HH_W,hh);
					p_atts.put(HH_Z,null);
					plans.addPerson(p);
					this.addDemographics(p,entries);
				}
				else if ((wkat == 4) && (gem2 == -7) && (partnr == -7)) {
					Person p = plans.getPerson(pid);
					if (p != null) { Gbl.errorMsg(e_head+"person alread exists!"); }
					p = new Person(pid);
					Map<String,Object> p_atts = p.getCustomAttributes();
					p_atts.put(PlansCreateFromCensus2000.HH_W,null);
					p_atts.put(HH_Z,hh);
					plans.addPerson(p);
					this.addDemographics(p,entries);
				}
				else if ((wkat == 3) && ((1 <= gem2)&&(gem2 <= 7011)) && ((1 <= partnr)&&(partnr <= 999999999))) {
					Person p = plans.getPerson(new IdImpl(partnr));
					if (p == null) {
						if (!pids.add(pid)) { Gbl.errorMsg(e_head+"partner person not found, but pid found in the set!"); }
						p = new Person(pid);
						Map<String,Object> p_atts = p.getCustomAttributes();
						p_atts.put(PlansCreateFromCensus2000.HH_W,hh);
						p_atts.put(HH_Z,null);
						plans.addPerson(p);
						this.addDemographics(p,entries);
					}
					else {
						if (!pids.remove(new IdImpl(partnr))) { Gbl.errorMsg(e_head+"partner person found, but not found in the set!"); }
						Map<String,Object> p_atts = p.getCustomAttributes();
						if (!((p_atts.get(PlansCreateFromCensus2000.HH_W) == null) && (p_atts.get(HH_Z) != null))) { Gbl.errorMsg(e_head+"something is wrong!"); }
						p_atts.put(PlansCreateFromCensus2000.HH_W,hh);
						// att check
					}
				}
				else if ((wkat == 4) && ((1 <= gem2)&&(gem2 <= 7011)) && ((1 <= partnr)&&(partnr <= 999999999))) {
					Person p = plans.getPerson(new IdImpl(partnr));
					if (p == null) {
						if (!pids.add(pid)) { Gbl.errorMsg(e_head+"partner person not found, but pid found in the set!"); }
						p = new Person(pid);
						Map<String,Object> p_atts = p.getCustomAttributes();
						p_atts.put(PlansCreateFromCensus2000.HH_W,null);
						p_atts.put(HH_Z,hh);
						plans.addPerson(p);
						this.addDemographics(p,entries);
					}
					else {
						if (!pids.remove(new IdImpl(partnr))) { Gbl.errorMsg(e_head+"partner person found, but not found in the set!"); }
						Map<String,Object> p_atts = p.getCustomAttributes();
						if (!((p_atts.get(PlansCreateFromCensus2000.HH_W) != null) && (p_atts.get(HH_Z) == null))) { Gbl.errorMsg(e_head+"something is wrong!"); }
						p_atts.put(HH_Z,hh);
						// att check
					}
				}
				else { Gbl.errorMsg(e_head+"not allowed!"); }

				// progress report
				if (line_cnt % 100000 == 0) {
					System.out.println("    Line " + line_cnt + ": # persons = " + plans.getPersons().size() + "; # pids = " + pids.size());
				}
				line_cnt++;
			}
			br.close();
			fr.close();
			
			// some info
			System.out.println("    "+plans.getPersons().size()+" persons created! (#pids="+pids.size()+")");
			int same_cnt = 0; int diff_cnt = 0; int wonly_cnt = 0; int zonly_cnt = 0;
			for (Person p : plans.getPersons().values()) {
				Map<String,Object> p_atts = p.getCustomAttributes();
				if ((p_atts.get(PlansCreateFromCensus2000.HH_W)==null)&&(p_atts.get(HH_Z)==null)) { Gbl.errorMsg("WAHHH!"); }
				else if ((p_atts.get(PlansCreateFromCensus2000.HH_W)==null)&&(p_atts.get(HH_Z)!=null)) { zonly_cnt++; }
				else if ((p_atts.get(PlansCreateFromCensus2000.HH_W)!=null)&&(p_atts.get(HH_Z)==null)) { wonly_cnt++; }
				else { if (p_atts.get(PlansCreateFromCensus2000.HH_W).equals(p_atts.get(HH_Z))) { same_cnt++; } else { diff_cnt++; } }
			}
			System.out.println("    # civil only          = " + zonly_cnt);
			System.out.println("    # econo only          = " + wonly_cnt);
			System.out.println("    # one household only  = " + same_cnt);
			System.out.println("    # two households      = " + diff_cnt);
			System.out.println("    left over pids:");
			for (Id pid : pids) { System.out.println("    "+pid); }
		} catch (Exception e) {
			Gbl.errorMsg(e);
		}
		System.out.println("    done.");
	}
}
