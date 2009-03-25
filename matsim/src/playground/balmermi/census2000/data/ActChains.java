/* *********************************************************************** *
 * project: org.matsim.*
 * ActChains.java
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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.core.gbl.Gbl;

public class ActChains {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String L = "l";
	private static final String S = "s";
	private static final String E = "e";
	private static final String W = "w";
	private static final String H = "h";

	// bit coding: wesl = [0000,1111]
	// chain_type[i] = ArrayList<ArrayList<Integer>>
	private final ArrayList<ArrayList<Integer>>[] chain_types = new ArrayList[16];
	private final String inputfile;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public ActChains(String inputfile) {
		this.inputfile = inputfile;
		for (int i=0; i<this.chain_types.length; i++) {
			this.chain_types[i] = new ArrayList<ArrayList<Integer>>();
		}
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	public final ArrayList<ArrayList<Integer>> getChains(int bitcode) {
		return this.chain_types[bitcode];
	}
	
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////

	public final void parse() {
		int line_cnt = 0;
		try {
			FileReader file_reader = new FileReader(inputfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			String curr_line;
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);

				// ActType1  end_time1 ActType2  dur2  ...  durN-1  ActTypeN
				// 0         1         2         3     ...
				
				ArrayList<Integer> chain = new ArrayList<Integer>();
				int w = 0;
				int e = 0;
				int s = 0;
				int l = 0;
				for (int i=0; i<entries.length; i++) {
					String entry = entries[i].trim();
					if (i%2 == 0) { // activity
						if (entry.equals(H)) { chain.add(new Integer(16)); }
						else if (entry.equals(W)) { chain.add(new Integer(8)); w = 8; }
						else if (entry.equals(E)) { chain.add(new Integer(4)); e = 4; }
						else if (entry.equals(S)) { chain.add(new Integer(2)); s = 2; }
						else if (entry.equals(L)) { chain.add(new Integer(1)); l = 1; }
						else { Gbl.errorMsg("Line " + curr_line + ": act_type=" + entry + " not known!"); }
					} else { // time
						Integer time = Integer.parseInt(entry);
						chain.add(time);
					}
				}
				int index = w + e + s + l; // bit coding
				if (index != 0) { // ignoring h-h chains!!!
					ArrayList<ArrayList<Integer>> list = this.chain_types[index];
					list.add(chain);
				}
				line_cnt++;
			}	
			buffered_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	//////////////////////////////////////////////////////////////////////

	public final void print() {
		for (int i=0; i<this.chain_types.length; i++) {
			ArrayList<ArrayList<Integer>> list = this.chain_types[i];
			System.out.println("index=" + i + ": size=" + list.size());
		}
	}
}
