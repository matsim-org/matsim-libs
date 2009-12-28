/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCreateLSA.java
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

package playground.balmermi.lsa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;

public class NetworkCreateLSA {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final String inputfolder = "../../input/";
	private final String outputfolder = "../../output/";
	private final NetworkLayer network;
	private final HashMap<Integer,Intersection> intersections = new HashMap<Integer, Intersection>();
	private final HashMap<Id,HashSet<LSA>> lsalinklist = new HashMap<Id, HashSet<LSA>>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkCreateLSA(NetworkLayer network) {
		this.network = network;
		this.readIntersections(this.inputfolder+"Knoten.txt");
		this.readLSAs(this.inputfolder+"LSAs.txt");
		this.readLanes(this.inputfolder+"Spuren.txt");
		this.readLaneLaneMapping(this.inputfolder+"Spur-Spur-Mapping.txt");
		this.readLSALaneMapping(this.inputfolder+"LSA-Spur-Mapping.txt");
		this.readLaneLinkMapping(this.inputfolder+"Spur-Link-Mapping.txt",5);
		this.readLSAGreentimes(this.inputfolder+"gruen_10_15_09.txt");
		this.writeData(this.outputfolder+"greentimes.xml");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	public final void readIntersections(String inputfile) {
		try {
			FileReader file_reader = new FileReader(inputfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine();
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// head:    KNOTENNR  KNOTENBEZ
				// example: 1         Badener-/Sihlfeldstr (Hst Nacht Lochergut)
				// index:   0         1

				Integer knotennr  = new Integer(entries[0].trim());
				String knotenbez = entries[1].trim();
				if (this.intersections.containsKey(knotennr)) { Gbl.errorMsg("Intersection id = " + knotennr + " aleady exists!"); }
				Intersection intersec = new Intersection(knotennr,knotenbez);
				this.intersections.put(knotennr,intersec);
			}
			buffered_reader.close();
			file_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		System.out.println("  " + this.intersections.size() + " intersections read and stored.");
	}

	//////////////////////////////////////////////////////////////////////

	public final void readLSAs(String inputfile) {
		int lsa_cnt = 0;
		try {
			FileReader file_reader = new FileReader(inputfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine();
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// head:    KNOTENNR  LSANR
				// example: 1         1
				// index:   0         1

				Integer knotennr  = new Integer(entries[0].trim());
				Integer lsanr = new Integer(entries[1].trim());
				if (!this.intersections.containsKey(knotennr)) { Gbl.errorMsg("Intersection id = " + knotennr + " does not exist!"); }
				Intersection intersec = this.intersections.get(knotennr);
				LSA lsa = new LSA(lsanr,intersec);
				intersec.addLSA(lsa);
				lsa_cnt++;
			}
			buffered_reader.close();
			file_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		System.out.println("  " + lsa_cnt + " LSA's read and assigned to the intersections.");
	}

	//////////////////////////////////////////////////////////////////////

	public final void readLanes(String inputfile) {
		int l_cnt = 0;
		try {
			FileReader file_reader = new FileReader(inputfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine();
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// head:    KNOTENNR  SPURNR
				// example: 53        1
				// index:   0         1

				Integer knotennr  = new Integer(entries[0].trim());
				Integer lnr = new Integer(entries[1].trim());
				if (!this.intersections.containsKey(knotennr)) { Gbl.errorMsg("Intersection id = " + knotennr + " does not exist!"); }
				Intersection intersec = this.intersections.get(knotennr);
				Lane l = new Lane(lnr,intersec);
				intersec.addLane(l);
				l_cnt++;
			}
			buffered_reader.close();
			file_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		System.out.println("  " + l_cnt + " Lanes read and assigned to the intersections.");
	}

	//////////////////////////////////////////////////////////////////////

	public final void readLaneLaneMapping(String inputfile) {
		int map_cnt = 0;
		try {
			FileReader file_reader = new FileReader(inputfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine();
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// head:    KNOTENNR  VONSPURNR  NACHSPURNR
				// example: 53        1          4
				// index:   0         1          2

				Integer knotennr  = new Integer(entries[0].trim());
				Integer flnr = new Integer(entries[1].trim());
				Integer tlnr = new Integer(entries[2].trim());
				if (!this.intersections.containsKey(knotennr)) { Gbl.errorMsg("Intersection id = " + knotennr + " does not exist!"); }
				Intersection intersec = this.intersections.get(knotennr);
				Lane fl = intersec.lanes.get(flnr);
				if (fl == null) { Gbl.errorMsg("Intersec_id=" + knotennr + ": lane_nr=" + flnr + " does not exist!"); }
				Lane tl = intersec.lanes.get(tlnr);
				if (tl == null) { Gbl.errorMsg("Intersec_id=" + knotennr + ": lane_nr=" + tlnr + " does not exist!"); }
				fl.addToLane(tl);
				map_cnt++;
			}
			buffered_reader.close();
			file_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		System.out.println("  " + map_cnt + " fromlane-tolane mappings read and assigned to the fromlane.");
	}

	//////////////////////////////////////////////////////////////////////

	public final void readLSALaneMapping(String inputfile) {
		int map_cnt = 0;
		try {
			FileReader file_reader = new FileReader(inputfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine();
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// head:    KNOTENNR  LSANR  SPURNR
				// example: 53        1      5
				// index:   0         1      2

				Integer knotennr  = new Integer(entries[0].trim());
				Integer lsanr = new Integer(entries[1].trim());
				Integer lnr = new Integer(entries[2].trim());
				if (!this.intersections.containsKey(knotennr)) { Gbl.errorMsg("Intersection id = " + knotennr + " does not exist!"); }
				Intersection intersec = this.intersections.get(knotennr);
				LSA lsa = intersec.lsas.get(lsanr);
				if (lsa == null) { Gbl.errorMsg("Intersec_id=" + knotennr + ": lsa_nr=" + lsanr + " does not exist!"); }
				Lane l = intersec.lanes.get(lnr);
				if (l == null) { Gbl.errorMsg("Intersec_id=" + knotennr + ": lane_nr=" + lnr + " does not exist!"); }
				lsa.addLane(l);
				l.addLSA(lsa);
				map_cnt++;
			}
			buffered_reader.close();
			file_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		System.out.println("  " + map_cnt + " LSA-lane mappings read and assigned to the LSA and to the lane.");
	}

	//////////////////////////////////////////////////////////////////////

	public final void readLaneLinkMapping(String inputfile, int net_idx) {
		if ((net_idx < 2) || (5 < net_idx)) { Gbl.errorMsg("Index must be in range [2,5]"); }
		int map_cnt = 0;
		int ignored = 0;
		Id ignore_flag = new IdImpl("-");
		try {
			FileReader file_reader = new FileReader(inputfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			// Skip header
			String curr_line = buffered_reader.readLine();
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// head:    KNOTENNR  SPURNR  LinkId_Teleatlas  LinkId_Navteq  LinkId_ARE  LinkId_IVTCH  toCheck
				// example: 53        1       95005             48908          2352        106721
				// index:   0         1       2                 3              4           5             6

				Integer knotennr  = new Integer(entries[0].trim());
				Integer lnr = new Integer(entries[1].trim());
				Id linkid = new IdImpl(entries[net_idx].trim());
				if (!linkid.equals(ignore_flag)) {
					if (!this.intersections.containsKey(knotennr)) { Gbl.errorMsg("Intersection id = " + knotennr + " does not exist!"); }
					Intersection intersec = this.intersections.get(knotennr);
					Lane l = intersec.lanes.get(lnr);
					if (l == null) { Gbl.errorMsg("Intersec_id=" + knotennr + ": lane_nr=" + lnr + " does not exist!"); }
					LinkImpl link = this.network.getLink(linkid);
					if (link == null) { Gbl.errorMsg("Intersec_id=" + knotennr + ": link_nr=" + linkid.toString() + " does not exist!"); }
					l.addLink(link);

					if (!this.lsalinklist.containsKey(linkid)) {
						this.lsalinklist.put(linkid,new HashSet<LSA>());
					}
					HashSet<LSA> lsas = this.lsalinklist.get(linkid);
					lsas.addAll(l.lsas.values());

					map_cnt++;
				}
				else {
					ignored++;
				}
			}
			buffered_reader.close();
			file_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		System.out.println("  " + map_cnt + " lane-link mappings read and assigned to the lane. (" + ignored + " entries ignored)");
	}

	//////////////////////////////////////////////////////////////////////

	public final void readLSAGreentimes(String inputfile) {
		int cnt = 0;
		int bin_starttime = -1; // seconds after 2007-09-01 00:00:00
		int bin_endtime = 0; // seconds after 2007-09-01 00:00:00
		try {
			FileReader file_reader = new FileReader(inputfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			String curr_line;
			while ((curr_line = buffered_reader.readLine()) != null) {
				cnt++;
				String[] entries = curr_line.split(" +", -1);
				// block:   DATA_DP: 01.09.2007 00:03:00 id=39 numMissInt=0 totalMissInt=0 totalEle=1525 numOfEle=1525 dataFormat=1
				// index:   0        1          2        3     4            5              6             7             8
				// desc:      PointId    valid suspect state posEd tOnEd        changeTime  min0  min1  minU  max0  max1  maxU  sum0  sum1 last1      sumQ0       sumQ1     sumQU
				// example:   S1 1 0        -1       0     0     0    -1 31.08.07 22:00:40     0     0     0     0     0     0     0     0     0          0          0          0
				// example:   S2 1 0        -1       0     0     2    -1 01.09.07 00:02:08   460    80   590   550   130   680  1010   340    80     211599      40200     810500
				// index:   0 1  2 3         4       5     6     7    8  9        10         11     12   13    14    15    16   17     18     19     20          21        22

				if (entries[0].trim().equals("")) {
					// data line
					int valid = Integer.parseInt(entries[4].trim());
//					if (valid == -1) { // a valid entry --> use it
						int lsanr = Integer.parseInt(entries[1].trim().substring(1));
						int intersectionnr = Integer.parseInt(entries[2].trim());
						LSA lsa = this.intersections.get(intersectionnr).lsas.get(lsanr);

						double rtime = Integer.parseInt(entries[17].trim())/10.0; // 0.1s to s
						double gtime = Integer.parseInt(entries[18].trim())/10.0; // 0.1s to s
						if ((rtime == 0.0) && (gtime == 0.0)) {gtime = 1.0; } // yellow flashing light => always green
						lsa.addEntry(bin_starttime,rtime,gtime);
//						if ((intersectionnr == 53) && (lsa.nr == 1)) {
//							System.out.println("--- " + intersectionnr + ":" + lsa.nr + " ---");
//							System.out.println(lsa.toString());
//						}
//					}
				}
				else {
					// a new time block starts
					bin_starttime = bin_endtime;
					String date = entries[1].trim();
					String time = entries[2].trim();
					int sec = (int)Time.parseTime(time);
					int day = Integer.parseInt(date.split("\\.",-1)[0]);
					bin_endtime = (day-1)*24*3600+sec;
					System.out.println(cnt + ";" + date + ";" + time + ": time bin=[" + bin_starttime + "," + bin_endtime + "]");
				}
			}
			buffered_reader.close();
			file_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public final void writeData(String outfile) {
		// HARDCODED: links with LSA's which should be ignored
		// for the ivtch network
		ArrayList<Id> links_to_ignore = new ArrayList<Id>();
		links_to_ignore.add(new IdImpl(106026));
		links_to_ignore.add(new IdImpl(106305));
		links_to_ignore.add(new IdImpl(106307));
		links_to_ignore.add(new IdImpl(106308));
		links_to_ignore.add(new IdImpl(106309));
		links_to_ignore.add(new IdImpl(106310));
		links_to_ignore.add(new IdImpl(106312));
		links_to_ignore.add(new IdImpl(106315));
		links_to_ignore.add(new IdImpl(106317));
		links_to_ignore.add(new IdImpl(106318));
		links_to_ignore.add(new IdImpl(106319));
		links_to_ignore.add(new IdImpl(106320));
		links_to_ignore.add(new IdImpl(106322));
		links_to_ignore.add(new IdImpl(108042));

		try {
			FileWriter fw = new FileWriter(outfile);
			BufferedWriter out = new BufferedWriter(fw);
			out.write("<greentimefractions desc=\"based on LSA data from City of Zurich\">\n");
			out.flush();
			Iterator<Id> id_it = this.lsalinklist.keySet().iterator();
			while (id_it.hasNext()) {
				Id id = id_it.next();
				LinkImpl link = this.network.getLink(id);
				HashSet<LSA> lsas = this.lsalinklist.get(id);
				if (links_to_ignore.contains(id)){
					System.out.println("Link id=" + id + " will be ignored. (no LSA for that link)!");
				}
				else if (!lsas.isEmpty()) {
					// create description
					String desc = "StadtZH: ";
					Iterator<LSA> lsa_it = lsas.iterator();
					while (lsa_it.hasNext()) {
						LSA lsa = lsa_it.next();
						desc = desc + "K" + lsa.intersection.id + "-" + lsa.nr + "(";
						Iterator<Lane> lane_it = lsa.lanes.values().iterator();
						while (lane_it.hasNext()) { desc = desc + lane_it.next().nr + ","; }
						desc = desc + "); ";
					}

					out.write("\t<linkgtfs id=\"" + id.toString() + "\" time_period=\"24:00:00\" desc=\"" + desc + "\">\n");
					for (int h=0; h<24; h++) {
						// building the average over all LSA of that link
						double gtf = 0.0;
						lsa_it = lsas.iterator();
						int isnan_cnt = 0;
						while (lsa_it.hasNext()) {
							LSA lsa = lsa_it.next();
							if (Double.isNaN(lsa.getGTF(h))) {
								System.out.println("gtf is NaN for: [intersec_id="+lsa.intersection.id + "][lsa_nr="+lsa.nr+"][h="+h+"]: ignoring LSA...");
								isnan_cnt++;
							}
							else {
								gtf += lsa.getGTF(h);
							}
						}
						if (isnan_cnt > 0) { System.out.println("isnan_cnt="+isnan_cnt); }
						gtf = gtf/(lsas.size()-isnan_cnt);
						if (Double.isNaN(gtf)) { Gbl.errorMsg("THAT SHOULD NOT HAPPEN!"); }
						out.write("\t\t<gtf time=\"" + Time.writeTime(h*3600) + "\" val=\"" + gtf + "\"/>\n");
					}
					out.write("\t</linkgtfs>\n");
					out.flush();
				}
			}
			out.write("</greentimefractions>\n");
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	//////////////////////////////////////////////////////////////////////

	public final void printData() {
		Iterator<Intersection> i_it = this.intersections.values().iterator();
		System.out.println("intersec_id" + "\t" + "nof_lsa" + "\t" + "nof_lanes" + "\t" + "intersec_desc");
		while (i_it.hasNext()) {
			Intersection i = i_it.next();
			System.out.println("K" + i.id + "\t" + i.lsas.size() + "\t" + i.lanes.size() + "\t" + i.desc);

			Iterator<LSA> lsa_it = i.lsas.values().iterator();
			System.out.print("  LSA_nr:");
			while (lsa_it.hasNext()) {
				LSA lsa = lsa_it.next();
				System.out.print("," + lsa.nr);
			}
			System.out.print("\n");

			Iterator<Lane> l_it = i.lanes.values().iterator();
			while (l_it.hasNext()) {
				Lane l = l_it.next();
				System.out.println("  Lane_nr:" + l.nr + "\tnof_tolanes=" +l.tolanes.size());
			}
		}
	}
}
