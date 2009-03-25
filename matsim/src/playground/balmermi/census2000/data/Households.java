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

package playground.balmermi.census2000.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordImpl;

public class Households {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final HashMap<Integer,Household> households = new HashMap<Integer, Household>();
	private final String inputfile;
	private final Municipalities municipalities;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Households(Municipalities municipalities, String inputfile) {
		super();
		this.inputfile = inputfile;
		this.municipalities = municipalities;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final int getHouseholdCategory(int hhtpz, int hhtpw) {
		if ((hhtpz == 9121) || (hhtpz == 9122) || (hhtpz == 9129) || (hhtpz == 9804) ||
				(1000 <= hhtpz && hhtpz <= 3222) ||
				(9150 <= hhtpz && hhtpz <= 9160) || (9200 <= hhtpz && hhtpz <= 9300)) {
			return hhtpz;
		}
		if ((hhtpw == 9121) || (hhtpw == 9122) || (hhtpw == 9129) || (hhtpw == 9804) ||
				(1000 <= hhtpw && hhtpw <= 3222) ||
				(9150 <= hhtpw && hhtpw <= 9160) || (9200 <= hhtpw && hhtpw <= 9300)) {
			return hhtpw;
		}
		return -1;
	}

	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final Household getHousehold(Integer hh_id) {
		return this.households.get(hh_id);
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////
	
	public final void setHH(Household hh) {
		this.households.put(hh.getId(),hh);
	}

	//////////////////////////////////////////////////////////////////////
	// public methods
	//////////////////////////////////////////////////////////////////////

	public final void parse() {
		int hh_ignore_cnt = 0;
		int line_cnt = 0;
		try {
			FileReader file_reader = new FileReader(inputfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine(); line_cnt++;
			curr_line = buffered_reader.readLine(); line_cnt++;
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);

				// KANT  ZGDE  HHNR  PERSON_ID  WKAT  ELTERN  ZKIND  HHTPZ  HHTPW  APERZ  APERW  XACH  YACH
				// 0     1     2     3          4     5       6      7      8      9      10     11    12

				// extract useful households
				int hhtpz = Integer.parseInt(entries[7].trim());
				int hhtpw = Integer.parseInt(entries[8].trim());
				int hh_cat = this.getHouseholdCategory(hhtpz,hhtpw);
				if (hh_cat > -1) {
					Integer hh_id = Integer.valueOf(entries[2].trim());
					Household hh = this.households.get(hh_id);
					if (hh == null) {
						int muni_id = Integer.parseInt(entries[1].trim());
						Municipality m = this.municipalities.getMunicipality(muni_id);
						if (m == null) {
							System.out.println("    Household id=" + hh_id + " ignored. (Municipality id=" + muni_id + " does not exist)");
							hh_ignore_cnt++;
						} else {
							hh = new Household(hh_id,m);
							this.households.put(hh_id,hh);

							hh.hh_cat = hh_cat;

							double x = Double.parseDouble(entries[11].trim());
							double y = Double.parseDouble(entries[12].trim());
							hh.coord = new CoordImpl(x,y);
						}
					} else {
						if (hh_cat != hh.hh_cat) {
							Gbl.errorMsg("Line " + line_cnt + ", hh_cat: " + hh_cat + " != " + hh.hh_cat);
						}
						double x = Double.parseDouble(entries[11].trim());
						double y = Double.parseDouble(entries[12].trim());
						if ((x != hh.coord.getX()) || (y != hh.coord.getY())) {
							Gbl.errorMsg("Line " + line_cnt + ", coord: Household coordinate not equals!");
						}
					}
				}
				// progress report
				if (line_cnt % 100000 == 0) {
					System.out.println("    Line " + line_cnt + ": # households = " + this.households.size());
				}
				line_cnt++;
			}
			buffered_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		System.out.println("    # Households = " + this.households.size());
		System.out.println("    # Households ignored = " + hh_ignore_cnt);
		System.out.println("    # lines = " + line_cnt);
	}

	//////////////////////////////////////////////////////////////////////

	public final void print() {
		Iterator<Household> hh_it = this.households.values().iterator();
		while (hh_it.hasNext()) {
			System.out.println(hh_it.next().toString());
		}
	}

	public final void writeTable(String outfile) {
		try {
			FileWriter fw = new FileWriter(outfile);
			BufferedWriter out = new BufferedWriter(fw);
			out.write("hh_id\tx_coord\ty_coord\thh_cat\thh_pcnt\thh_kidcnt\tmuni_id\tkt_id\tmuni_regtype\tmuni_eink\tmuni_fuel\n");
			out.flush();
			Iterator<Household> hh_it = this.households.values().iterator();
			while (hh_it.hasNext()) {
				Household hh = hh_it.next();
				out.write(hh.coord.getX() + "\t" + hh.coord.getY() + "\t" +
				          hh.hh_id + "\t" + hh.hh_cat + "\t" +
				          hh.getPersonCount() + "\t" + hh.getKidCount() + "\t" +
				          hh.getMunicipality().getId() + "\t" +
				          hh.getMunicipality().k_id + "\t" + hh.getMunicipality().reg_type + "\t" +
				          hh.getMunicipality().income + "\t" + hh.getMunicipality().fuelcost + "\n");
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
