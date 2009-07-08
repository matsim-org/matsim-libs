/* *********************************************************************** *
 * project: org.matsim.*
 * Persons.java
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

package playground.balmermi.census2000.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.matsim.core.gbl.Gbl;

public class Persons {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String NEVER = "never";
	private static final String ALWAYS = "always";
	private static final String SOMETIMES = "sometimes";

	public final HashMap<Integer,MyPerson> persons = new HashMap<Integer,MyPerson>();
	public Households households;
	private final String inputfile;

	private Random random = new Random(101);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Persons(Households households,String inputfile) {
		super();
		this.households = households;
		this.inputfile = inputfile;
		this.random.nextDouble(); // ignore first number
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final boolean isEmployed(int ams) {
		if ((11 <= ams) && (ams <= 14)) { return true; }
		else if ((ams == 20) || ((31 <= ams) && (ams <= 35)) || (ams == 4) || (ams == 40)) { return false; }
		else {
			Gbl.errorMsg("wrong value of AMS param!");
			return false;
		}
	}
	
	private final String carAvailability(int amrad, int apkwl, int apkwm, int smrad, int spkwl, int spkwm) {
		if ((apkwm == 1) || (spkwm == 1)) { return SOMETIMES; }
		else if ((amrad == 1) || (apkwl == 1) || (smrad == 1) || (spkwl == 1)) { return ALWAYS; }
		else if (((-8 <= amrad) && (amrad <= 0)) || ((-8 <= apkwl) && (apkwl <= 0)) ||
		         ((-8 <= smrad) && (smrad <= 0)) || ((-8 <= spkwl) && (spkwl <= 0))) {
			// assign car_avail randomly such that the overall distribution is:
			// always ~ 56%
			// sometimes ~ 10%
			// never ~ 34%
			double r = this.random.nextDouble();
			if (r < 0.55) { return ALWAYS; }
			else if (r < 0.70) { return SOMETIMES; }
			else { return NEVER; }
		} else { return NEVER; }
	}

	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////

	public final MyPerson getPerson(Integer id) {
		return this.persons.get(id);
	}

	public final HashMap<Integer,MyPerson> getPersons() {
		return this.persons;
	}
	
	public final void parse() {
		int p_ignore_cnt = 0;
		int line_cnt = 0;
		try {
			FileReader file_reader = new FileReader(inputfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine(); line_cnt++;
			curr_line = buffered_reader.readLine(); line_cnt++;
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);

				// HHNR  PERSON_ID  WKAT  ALTJ  GESL  HMAT  ELTERN  ZKIND  STHHZ  STHHW  GEGW  HABG
				// 0     1          2     3     4     5     6       7      8      9      10    11
				//
				// AMS  KAMS  AVEMI  AMRAD  APKWL  APKWM  SVEMI  SMRAD  SPKWL  SPKWM
				// 12   13    14     15     16     17     18     19     20     21
				
				// extract useful households
				Integer hh_id = Integer.parseInt(entries[0].trim());
				Household hh = this.households.getHousehold(hh_id);
				if (hh != null) {
					Integer p_id = Integer.parseInt(entries[1].trim());
					MyPerson p = new MyPerson(p_id,hh);
					boolean is_new = hh.addPerson(p);
					if (!is_new) {
						Gbl.errorMsg("Line " + line_cnt + ", p_id: " + p_id + " already exists in household id=" + hh.hh_id);
					}
					this.persons.put(p_id,p);

					p.age = Integer.parseInt(entries[3].trim());

					int sex = Integer.parseInt(entries[4].trim());
					if (sex == 1) { p.male = true; } else { p.male = false; }
					
					int nat = Integer.parseInt(entries[5].trim());
					if (nat == 1) { p.swiss = true; } else { p.swiss = false; }
					
					int ams = Integer.parseInt(entries[12].trim());
					p.employed = this.isEmployed(ams);
					
					p.curr_educ = Integer.parseInt(entries[10].trim());
					p.passed_educ = Integer.parseInt(entries[10].trim());

					int amrad = Integer.parseInt(entries[15].trim());
					int apkwl = Integer.parseInt(entries[16].trim());
					int apkwm = Integer.parseInt(entries[17].trim());
					int smrad = Integer.parseInt(entries[19].trim());
					int spkwl = Integer.parseInt(entries[20].trim());
					int spkwm = Integer.parseInt(entries[21].trim());
					String car_avail = this.carAvailability(amrad,apkwl,apkwm,smrad,spkwl,spkwm);
					if (p.age < 15) { car_avail = NEVER; }
					p.car_avail = car_avail.intern();
					
					if (p.car_avail == NEVER) { p.license = false; }
					else { p.license = true; }
				} else {
					p_ignore_cnt++;
				}
				
				// progress report
				if (line_cnt % 100000 == 0) {
					System.out.println("    Line " + line_cnt);
				}
				line_cnt++;
			}	
			buffered_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		System.out.println("    # persons = " + this.persons.size());
		System.out.println("    # persons ignored = " + p_ignore_cnt);
		System.out.println("    # lines = " + line_cnt);
	}

	//////////////////////////////////////////////////////////////////////

	public final void print() {
		Iterator<MyPerson> p_it = this.persons.values().iterator();
		while (p_it.hasNext()) {
			System.out.println(p_it.next().toString());
		}
	}

	public final void writeTable(String outfile) {
		try {
			FileWriter fw = new FileWriter(outfile);
			BufferedWriter out = new BufferedWriter(fw);
			out.write("p_id\thh_id\tx_coord\ty_coord\tp_age\tp_sex\tp_swiss\tp_employed\tp_caravail\tp_license\n");
			out.flush();
			Iterator<MyPerson> p_it = this.persons.values().iterator();
			while (p_it.hasNext()) {
				MyPerson p = p_it.next();
				char sex = 'f'; if (p.male) { sex = 'm'; }
				int swiss = 0; if (p.swiss) { swiss = 1; }
				int empl = 0; if (p.employed) { empl = 1; }
				int lic = 0; if (p.license) { lic = 1; }
				out.write(p.p_id + "\t" + p.hh.hh_id + "\t" +
				          p.hh.coord.getX() + "\t" + p.hh.coord.getY() + "\t" + 
				          p.age + "\t" + sex + "\t" + swiss + "\t" + empl + "\t" +
				          p.car_avail + "\t" + lic + "\n");
				out.flush();
			}
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
