/* *********************************************************************** *
 * project: org.matsim.*
 * WayDataElement
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.jbischoff.waySplitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
/**
 * @author jbischoff
 *
 */

public class LongSheetReaderWriter {
	private static final Logger log = Logger.getLogger(LongSheetReaderWriter.class);

	private Map<Id, PersonDataElement> personMap;
	private Map<Id, List<WayDataElement>> wayMap;

	public static void main(String args0[]) throws Exception {

		LongSheetReaderWriter longSheetReaderWriter = new LongSheetReaderWriter();
		longSheetReaderWriter.run();

	}

	public void run() throws Exception {
		this.personMap = new TreeMap<Id, PersonDataElement>();
		this.wayMap = new TreeMap<Id, List<WayDataElement>>();
		this.readFile("C:\\Users\\Joschka Bischoff\\Desktop\\Daten von Andreas\\H&P 321 - BVG - Touristenbefragung - Datensatz.csv");
		this.writeFile("C:\\Users\\Joschka Bischoff\\Desktop\\Daten von Andreas\\bvg_befragung");
	}

	void readFile(String filename) throws Exception {
		FileReader fr = new FileReader(new File(filename));
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] r = line.split(";");
			PersonDataElement pde = new PersonDataElement();
			pde.setPersonId(r[0]);
			pde.f1 = r[1];
			pde.f2 = r[2];
			pde.fa1 = r[3];
			pde.fa1a = r[4];
			pde.fa1at = r[5];

			pde.fa81 = r[720];
			pde.fa82 = r[721];
			pde.fa82n = r[722];
			pde.fa83 = r[723];
			pde.fa83n = r[724];
			pde.fa896 = r[725];
			pde.fs1 = r[726];
			pde.fs2 = r[727];
			pde.fs3 = r[728];
			pde.fz1 = r[729];
			pde.fz2 = r[730];
			pde.fz2tn = r[731];
			pde.fz3 = r[732];
			pde.fz41 = r[733];
			pde.fz42 = r[734];
			pde.fz43 = r[735];
			pde.fz496 = r[736];
			pde.fz5 = r[737];
			pde.fz5n = r[738];
			pde.fz6 = r[739];
			pde.fz6n = r[740];
			pde.fz7 = r[741];
			pde.sprache = r[742];
			this.personMap.put(pde.personId, pde);
			List<WayDataElement> ways = new ArrayList<WayDataElement>();

			for (int i = 0; i < 13; i++) {
				WayDataElement wde = new WayDataElement();
				
				if (r[6 + 51 * i].equals("1")) {
			
				wde.setPID(pde.personId.toString());
				wde.setWayID(i + 1);
				wde.a2 = r[7 + 51 * i];
				wde.a2_n = r[8 + 51 * i];
				wde.a3 = r[9 + 51 * i];
				wde.a3_n = r[10 + 51 * i];
				wde.a41 = r[11 + 51 * i];
				wde.a42 = r[12 + 51 * i];
				wde.a43 = r[13 + 51 * i];
				wde.a44 = r[14 + 51 * i];
				wde.a45 = r[15 + 51 * i];
				wde.a46 = r[16 + 51 * i];
				wde.a47 = r[17 + 51 * i];
				wde.a48 = r[18 + 51 * i];
				wde.a49 = r[19 + 51 * i];
				wde.a410 = r[20 + 51 * i];
				wde.a411 = r[21 + 51 * i];
				wde.a412 = r[22 + 51 * i];
				wde.a498 = r[23 + 51 * i];
				wde.a498t = r[24 + 51 * i];
				wde.a5 = r[25 + 51 * i];
				wde.a5t = r[26 + 51 * i];
				wde.a6a = r[27 + 51 * i];
				wde.a6an = r[28 + 51 * i];
				wde.a6b = r[29 + 51 * i];
				wde.a6bn = r[30 + 51 * i];
				wde.a6c = r[31 + 51 * i];
				wde.a6cn = r[32 + 51 * i];
				wde.a6d = r[33 + 51 * i];
				wde.a6dn = r[34 + 51 * i];
				wde.a6e = r[35 + 51 * i];
				wde.a6en = r[36 + 51 * i];
				wde.a6f = r[37 + 51 * i];
				wde.a6fn = r[38 + 51 * i];
				wde.a6g = r[39 + 51 * i];
				wde.a6gn = r[40 + 51 * i];
				wde.a6h = r[41 + 51 * i];
				wde.a6hn = r[42 + 51 * i];
				wde.a6i = r[43 + 51 * i];
				wde.a6in = r[44 + 51 * i];
				wde.a6j = r[45 + 51 * i];
				wde.a6jn = r[46 + 51 * i];
				wde.a6k = r[47 + 51 * i];
				wde.a6kn = r[48 + 51 * i];
				wde.a6l = r[49 + 51 * i];
				wde.a6ln = r[50 + 51 * i];
				wde.a6m = r[51 + 51 * i];
				wde.a6mn = r[52 + 51 * i];
				wde.a6n = r[53 + 51 * i];
				wde.a6nn = r[54 + 51 * i];
				wde.a7 = r[55 + 51 * i];
				wde.a7tn = r[56 + 51 * i];
				ways.add(wde);
				}
			}
			this.wayMap.put(pde.personId, ways);

		}
	}

	void writeFile(String filename) throws Exception{
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename+"_persons.csv")));
		
		for (PersonDataElement pde : this.personMap.values()){
			bw.append(pde.personId+";"+	pde.f1+";"
	+pde.f2+";"
	+pde.fa1+";"
	+pde.fa1a+";"
	+pde.fa1at+";"
	+pde.fa81+";"
	+pde.fa82+";"
	+pde.fa82n+";"
	+pde.fa83+";"
	+pde.fa83n+";"
	+pde.fa896+";"
	+pde.fs1+";"
	+pde.fs2+";"
	+pde.fs3+";"
	+pde.fz1+";"
	+pde.fz2+";"
	+pde.fz2tn+";"
	+pde.fz3+";"
	+pde.fz41+";"
	+pde.fz42+";"
	+pde.fz43+";"
	+pde.fz496+";"
	+pde.fz5+";"
	+pde.fz5n+";"
	+pde.fz6+";"
	+pde.fz6n+";"
	+pde.fz7+";"
	+pde.sprache+";\n");
			
		}
		bw.flush();
		bw.close();
		log.info("Wrote: "+filename+"_persons.csv");
	BufferedWriter ww = new BufferedWriter(new FileWriter(new File(filename+"_ways.csv")));
	ww.append("PersonId;Weg;FrageA2;FrageA2_n;FrageA3;FrageA3_n;FrageA4_1;FrageA4_2;FrageA4_3;FrageA4_4;FrageA4_5;FrageA4_6;FrageA4_7;FrageA4_8;FrageA4_9;FrageA4_10;FrageA4_11;FrageA4_12;FrageA4_98;FrageA4_98_t;FrageA5;FrageA5_t;FrageA6a;FrageA6a_n;FrageA6b;FrageA6b_n;FrageA6c;FrageA6c_n;FrageA6d;FrageA6d_n;FrageA6e;FrageA6e_n;FrageA6f;FrageA6f_n;FrageA6g;FrageA6g_n;FrageA6h;FrageA6h_n;FrageA6i;FrageA6i_n;FrageA6j;FrageA6j_n;FrageA6k;FrageA6k_n;FrageA6l;FrageA6l_n;FrageA6m;FrageA6m_n;FrageA6n;FrageA6n_t;FrageA7;FrageA7_tn\n");
	for (Id pid : this.wayMap.keySet()){
		for (WayDataElement wde : this.wayMap.get(pid)){
			ww.append(pid.toString()+";"+wde.wayID.toString()+";"
					+wde.a2+";"
					+wde.a2_n+";"
					+wde.a3+";"
					+wde.a3_n+";"
					+wde.a41+";"
					+wde.a42+";"
					+wde.a43+";"
					+wde.a44+";"
					+wde.a45+";"
					+wde.a46+";"
					+wde.a47+";"
					+wde.a48+";"
					+wde.a49+";"
					+wde.a410+";"
					+wde.a411+";"
					+wde.a412+";"
					+wde.a498+";"
					+wde.a498t+";"
					+wde.a5+";"
					+wde.a5t+";"
					+wde.a6a+";"
					+wde.a6an+";"
					+wde.a6b+";"
					+wde.a6bn+";"
					+wde.a6c+";"
					+wde.a6cn+";"
					+wde.a6d+";"
					+wde.a6dn+";"
					+wde.a6e+";"
					+wde.a6en+";"
					+wde.a6f+";"
					+wde.a6fn+";"
					+wde.a6g+";"
					+wde.a6gn+";"
					+wde.a6h+";"
					+wde.a6hn+";"
					+wde.a6i+";"
					+wde.a6in+";"
					+wde.a6j+";"
					+wde.a6jn+";"
					+wde.a6k+";"
					+wde.a6kn+";"
					+wde.a6l+";"
					+wde.a6ln+";"
					+wde.a6m+";"
					+wde.a6mn+";"
					+wde.a6n+";"
					+wde.a6nn+";"
					+wde.a7+";"
					+wde.a7tn+";\n");
		}
		
	}
	ww.flush();
	ww.close();	
	log.info("Wrote: "+filename+"_ways.csv");
	}
}
