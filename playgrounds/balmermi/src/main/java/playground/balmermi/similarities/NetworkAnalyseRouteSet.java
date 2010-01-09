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

package playground.balmermi.similarities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;

public class NetworkAnalyseRouteSet {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final NumberFormat formatter = new DecimalFormat("0.000000");


	private static final int LAENGE = 0;
	private static final int VWEG = 1;
	private static final int VWEGEM = 2;
	private static final int PARKW = 3;
	private static final int AMPEL = 4;
	private static final int BRUECKE = 5;
	private static final int TUNNEL = 6;
	private static final int NATBEL = 7;
	private static final int DTVKAT = 8;

	private final TreeMap<Id,Double> node_heights = new TreeMap<Id, Double>();
	// Link atts: Laenge  VWeg  VWegEm  ParkW  Ampel  Bruecke  Tunnel  NatBel  DTVKat
	// idx:       0       1     2       3      4      5        6       7       8
	private final TreeMap<Id,double[]> link_atts = new TreeMap<Id, double[]>();
	private final TreeMap<Id,ArrayList<Node>> routes = new TreeMap<Id, ArrayList<Node>>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkAnalyseRouteSet() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void readNodeHeight(String inputfile, NetworkLayer network) {
		try {
			FileReader file_reader = new FileReader(inputfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			String curr_line = buffered_reader.readLine(); // Skip header
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// head:    Node_ID  Hoehe
				// example: 10001    404.60
				// index:   0        1

				Id nodeid = new IdImpl(entries[0].trim());
				if (!network.getNodes().containsKey(nodeid)) { Gbl.errorMsg("Node id=" + nodeid + " does not exist in the network!"); }
				Double height = new Double(entries[1].trim());
				if (this.node_heights.put(nodeid,height) != null) { Gbl.errorMsg("Node id=" + nodeid + " already has a height assigned!"); }
			}
			buffered_reader.close();
			file_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		System.out.println("      => # nodes  : " + network.getNodes().size());
		System.out.println("      => # heights: " + this.node_heights.size());
	}

	//////////////////////////////////////////////////////////////////////

	private final void readLinkAtts(String inputfile, NetworkLayer network) {
		try {
			FileReader file_reader = new FileReader(inputfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			String curr_line = buffered_reader.readLine(); // Skip header
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// head:    #ID  FromNode  ToNode  Laenge  VWeg  VWegEm  ParkW  Ampel  Bruecke  Tunnel  NatBel  DTVKat
				// example: 1    10001     10002   11.71   1     1       0      0      0        0       0       5
				// index:   0    1         2       3       4     5       6      7      8        9       10      11

				Id linkid = new IdImpl(entries[0].trim());
				if (!network.getLinks().containsKey(linkid)) { Gbl.errorMsg("Link id=" + linkid + " does not exist in the network!"); }
				this.link_atts.put(linkid,new double[9]);
				double[] atts = this.link_atts.get(linkid);
				for (int i=0; i<atts.length; i++) {
					Double att = new Double(entries[i+3].trim());
					atts[i] = att;
				}
			}
			buffered_reader.close();
			file_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		System.out.println("      => # links  : " + network.getLinks().size());
		System.out.println("      => # Atts   : " + this.link_atts.size());
	}

	//////////////////////////////////////////////////////////////////////

	private final void readRoutes(String inputfile, NetworkLayer network) {
		try {
			FileReader file_reader = new FileReader(inputfile);
			BufferedReader buffered_reader = new BufferedReader(file_reader);

			String curr_line = buffered_reader.readLine(); // Skip header
			while ((curr_line = buffered_reader.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				// head:    #ID     FROM_NODE  TO_NODE  ROUTE(linklist)...  -1  LEASTCOSTROUTE(0,1)  -1
				// example: 100000  13846      13703    9001  ...  8803     -1  1                    -1
				// index:   0       1          2        3     4  ...

				ArrayList<Node> node_routes = new ArrayList<Node>();
				Node node = network.getNodes().get(new IdImpl(entries[1].trim()));
				if (node == null) { Gbl.errorMsg("Node id=" + entries[1].trim() + " does not exist!"); }
				node_routes.add(node);

				int idx = 3;
				while (!entries[idx].trim().equals("-1")) {
					node = network.getLinks().get(new IdImpl(entries[idx].trim())).getToNode();
					node_routes.add(node);
					idx++;
				}
				NodeImpl last = network.getNodes().get(new IdImpl(entries[2].trim()));
				if (last == null) { Gbl.errorMsg("Node id=" + entries[1].trim() + " does not exist!"); }
				if (!last.getId().equals(node_routes.get(node_routes.size()-1).getId())) {
					Gbl.errorMsg("Last node does not fit!");
				}
				Id routeid = new IdImpl(entries[0].trim());
				if (this.routes.put(routeid,node_routes) != null) { Gbl.errorMsg("Route id=" + routeid + " already exists!"); }
			}
			buffered_reader.close();
			file_reader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		System.out.println("      => # routes: " + this.routes.size());
	}

	private final void analysis(Id routeid, ArrayList<Node> route) {
		double length = 0.0;
		double rise_av = 0.0;
		double rise_min = 0.0;
		double rise_max = 0.0;
		double fall_av = 0.0;
		double fall_min = 0.0;
		double fall_max = 0.0;
		double heightness = 0.0;
		double deepness = 0.0;
		double dtvkat_av = 0.0;
		double dtvkat_min = 1000000.0;
		double dtvkat_max = -1.0;
		double dtvkat1_frac = 0.0;
		double dtvkat2_frac = 0.0;
		double dtvkat3_frac = 0.0;
		double dtvkat4_frac = 0.0;
		double dtvkat5_frac = 0.0;
		double natbel_av = 0.0;
		double vweg_av = 0.0;
		double vwegem_av = 0.0;
		double parkw_av = 0.0;
		double bridge_av = 0.0;
		double bridge_nofl = 0.0;
		double tunnel_av = 0.0;
		double tunnel_nofl = 0.0;
		double ampel_nofl = 0.0;
		for (int i=1; i<route.size(); i++) {
			Node from = route.get(i-1);
			Node to = route.get(i);
			Link link = null;
			for (Link l : from.getOutLinks().values()) { if (l.getToNode().getId().equals(to.getId())) { link = l; } }
			if (link == null) { Gbl.errorMsg("Something is wrong!"); }
			double[] atts = this.link_atts.get(link.getId());

			length += atts[LAENGE];

			double gradient = this.node_heights.get(to.getId())-this.node_heights.get(from.getId());
			if (gradient > 0.0) {
				rise_av += gradient;
				if (gradient/atts[LAENGE] > rise_max) { rise_max = gradient/atts[LAENGE]; }
				if (gradient/atts[LAENGE] < rise_min) { rise_min = gradient/atts[LAENGE]; }
				heightness += gradient;
			}
			else {
				gradient = Math.abs(gradient);
				fall_av += gradient;
				if (gradient/atts[LAENGE] > fall_max) { fall_max = gradient/atts[LAENGE]; }
				if (gradient/atts[LAENGE] < fall_min) { fall_min = gradient/atts[LAENGE]; }
				deepness += gradient;
			}

			dtvkat_av += atts[DTVKAT]*atts[LAENGE];
			if (atts[DTVKAT] > dtvkat_max) { dtvkat_max = atts[DTVKAT]; }
			if (atts[DTVKAT] < dtvkat_min) { dtvkat_min = atts[DTVKAT]; }

			if (atts[DTVKAT] == 1.0) { dtvkat1_frac += atts[LAENGE]; }
			else if (atts[DTVKAT] == 2.0) { dtvkat2_frac += atts[LAENGE]; }
			else if (atts[DTVKAT] == 3.0) { dtvkat3_frac += atts[LAENGE]; }
			else if (atts[DTVKAT] == 4.0) { dtvkat4_frac += atts[LAENGE]; }
			else if (atts[DTVKAT] == 5.0) { dtvkat5_frac += atts[LAENGE]; }
			else { Gbl.errorMsg("dtvkat=" + atts[DTVKAT] + " not allowed!"); }

			natbel_av += atts[NATBEL]* atts[LAENGE];
			vweg_av += atts[VWEG]* atts[LAENGE];
			vwegem_av += atts[VWEGEM]* atts[LAENGE];
			parkw_av += atts[PARKW]* atts[LAENGE];

			bridge_av += atts[BRUECKE]* atts[LAENGE];
			bridge_nofl += atts[BRUECKE];

			tunnel_av += atts[TUNNEL]* atts[LAENGE];
			tunnel_nofl += atts[TUNNEL];

			ampel_nofl += atts[AMPEL];
		}
		rise_av /= length;
		fall_av /= length;
		dtvkat_av /= length;

		dtvkat1_frac /= length;
		dtvkat2_frac /= length;
		dtvkat3_frac /= length;
		dtvkat4_frac /= length;
		dtvkat5_frac /= length;

		natbel_av /= length;
		vweg_av /= length;
		vwegem_av /= length;
		parkw_av /= length;
		bridge_av /= length;
		tunnel_av /= length;

		System.out.print(routeid + "\t");
		System.out.print(formatter.format(length) + "\t");
		System.out.print(formatter.format(rise_av) + "\t");
		System.out.print(formatter.format(rise_min) + "\t");
		System.out.print(formatter.format(rise_max) + "\t");
		System.out.print(formatter.format(fall_av) + "\t");
		System.out.print(formatter.format(fall_min) + "\t");
		System.out.print(formatter.format(fall_max) + "\t");
		System.out.print(formatter.format(heightness) + "\t");
		System.out.print(formatter.format(deepness) + "\t");
		System.out.print(formatter.format(dtvkat_av) + "\t");
		System.out.print(formatter.format(dtvkat_min) + "\t");
		System.out.print(formatter.format(dtvkat_max) + "\t");

		System.out.print(formatter.format(dtvkat1_frac) + "\t");
		System.out.print(formatter.format(dtvkat2_frac) + "\t");
		System.out.print(formatter.format(dtvkat3_frac) + "\t");
		System.out.print(formatter.format(dtvkat4_frac) + "\t");
		System.out.print(formatter.format(dtvkat5_frac) + "\t");

		System.out.print(formatter.format(natbel_av) + "\t");
		System.out.print(formatter.format(vweg_av) + "\t");
		System.out.print(formatter.format(vwegem_av) + "\t");
		System.out.print(formatter.format(parkw_av) + "\t");
		System.out.print(formatter.format(bridge_av) + "\t");
		System.out.print(formatter.format(bridge_nofl) + "\t");
		System.out.print(formatter.format(tunnel_av) + "\t");
		System.out.print(formatter.format(tunnel_nofl) + "\t");
		System.out.print(formatter.format(ampel_nofl) + "\n");
	}

	private final void analysis(NetworkLayer network) {
		double length = 0.0;
		double rise_av = 0.0;
		double rise_min = 0.0;
		double rise_max = 0.0;
		double fall_av = 0.0;
		double fall_min = 0.0;
		double fall_max = 0.0;
		double heightness = 0.0;
		double deepness = 0.0;
		double dtvkat_av = 0.0;
		double dtvkat_min = 1000000.0;
		double dtvkat_max = -1.0;
		double dtvkat1_frac = 0.0;
		double dtvkat2_frac = 0.0;
		double dtvkat3_frac = 0.0;
		double dtvkat4_frac = 0.0;
		double dtvkat5_frac = 0.0;
		double natbel_av = 0.0;
		double vweg_av = 0.0;
		double vwegem_av = 0.0;
		double parkw_av = 0.0;
		double bridge_av = 0.0;
		double bridge_nofl = 0.0;
		double tunnel_av = 0.0;
		double tunnel_nofl = 0.0;
		double ampel_nofl = 0.0;
		for (LinkImpl link : network.getLinks().values()) {
			Node from = link.getFromNode();
			Node to = link.getToNode();
			double[] atts = this.link_atts.get(link.getId());
			length += atts[LAENGE];
			double gradient = this.node_heights.get(to.getId())-this.node_heights.get(from.getId());
			if (gradient > 0.0) {
				rise_av += gradient;
				if (gradient/atts[LAENGE] > rise_max) { rise_max = gradient/atts[LAENGE]; }
				if (gradient/atts[LAENGE] < rise_min) { rise_min = gradient/atts[LAENGE]; }
				heightness += gradient;
			}
			else {
				gradient = Math.abs(gradient);
				fall_av += gradient;
				if (gradient/atts[LAENGE] > fall_max) { fall_max = gradient/atts[LAENGE]; }
				if (gradient/atts[LAENGE] < fall_min) { fall_min = gradient/atts[LAENGE]; }
				deepness += gradient;
			}
			dtvkat_av += atts[DTVKAT]*atts[LAENGE];
			if (atts[DTVKAT] > dtvkat_max) { dtvkat_max = atts[DTVKAT]; }
			if (atts[DTVKAT] < dtvkat_min) { dtvkat_min = atts[DTVKAT]; }

			if (atts[DTVKAT] == 1.0) { dtvkat1_frac += atts[LAENGE]; }
			else if (atts[DTVKAT] == 2.0) { dtvkat2_frac += atts[LAENGE]; }
			else if (atts[DTVKAT] == 3.0) { dtvkat3_frac += atts[LAENGE]; }
			else if (atts[DTVKAT] == 4.0) { dtvkat4_frac += atts[LAENGE]; }
			else if (atts[DTVKAT] == 5.0) { dtvkat5_frac += atts[LAENGE]; }
			else { Gbl.errorMsg("dtvkat=" + atts[DTVKAT] + " not allowed!"); }

			natbel_av += atts[NATBEL]* atts[LAENGE];
			vweg_av += atts[VWEG]* atts[LAENGE];
			vwegem_av += atts[VWEGEM]* atts[LAENGE];
			parkw_av += atts[PARKW]* atts[LAENGE];

			bridge_av += atts[BRUECKE]* atts[LAENGE];
			bridge_nofl += atts[BRUECKE];

			tunnel_av += atts[TUNNEL]* atts[LAENGE];
			tunnel_nofl += atts[TUNNEL];

			ampel_nofl += atts[AMPEL];
		}
		rise_av /= length;
		fall_av /= length;
		dtvkat_av /= length;

		dtvkat1_frac /= length;
		dtvkat2_frac /= length;
		dtvkat3_frac /= length;
		dtvkat4_frac /= length;
		dtvkat5_frac /= length;

		natbel_av /= length;
		vweg_av /= length;
		vwegem_av /= length;
		parkw_av /= length;
		bridge_av /= length;
		tunnel_av /= length;

		System.out.print(formatter.format(length) + "\t");
		System.out.print(formatter.format(rise_av) + "\t");
		System.out.print(formatter.format(rise_min) + "\t");
		System.out.print(formatter.format(rise_max) + "\t");
		System.out.print(formatter.format(fall_av) + "\t");
		System.out.print(formatter.format(fall_min) + "\t");
		System.out.print(formatter.format(fall_max) + "\t");
		System.out.print(formatter.format(heightness) + "\t");
		System.out.print(formatter.format(deepness) + "\t");
		System.out.print(formatter.format(dtvkat_av) + "\t");
		System.out.print(formatter.format(dtvkat_min) + "\t");
		System.out.print(formatter.format(dtvkat_max) + "\t");

		System.out.print(formatter.format(dtvkat1_frac) + "\t");
		System.out.print(formatter.format(dtvkat2_frac) + "\t");
		System.out.print(formatter.format(dtvkat3_frac) + "\t");
		System.out.print(formatter.format(dtvkat4_frac) + "\t");
		System.out.print(formatter.format(dtvkat5_frac) + "\t");

		System.out.print(formatter.format(natbel_av) + "\t");
		System.out.print(formatter.format(vweg_av) + "\t");
		System.out.print(formatter.format(vwegem_av) + "\t");
		System.out.print(formatter.format(parkw_av) + "\t");
		System.out.print(formatter.format(bridge_av) + "\t");
		System.out.print(formatter.format(bridge_nofl) + "\t");
		System.out.print(formatter.format(tunnel_av) + "\t");
		System.out.print(formatter.format(tunnel_nofl) + "\t");
		System.out.print(formatter.format(ampel_nofl) + "\n");
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public final void writeData(String outfile) {
		try {
			FileWriter fw = new FileWriter(outfile);
			BufferedWriter out = new BufferedWriter(fw);
			out.write("<greentimefractions desc=\"based on LSA data from City of Zurich\">\n");
			out.flush();
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(NetworkLayer network) {
		System.out.println("    running " + this.getClass().getName() + " module...");

		System.out.println("      reading in heights of the nodes...");
		this.readNodeHeight("input/strassenknoten_mit_hoehe.txt",network);
		System.out.println("      done.");

		System.out.println("      reading in atts of the links...");
		this.readLinkAtts("input/strassennetz.txt",network);
		System.out.println("      done.");

		System.out.println("      reading in route sets...");
		this.readRoutes("input/routeset.txt",network);
		System.out.println("      done.");

		System.out.println("      analysing routes...");
		System.out.print("route_id\t");
		System.out.print("length\t");
		System.out.print("rise_av\t");
		System.out.print("rise_min\t");
		System.out.print("rise_max\t");
		System.out.print("fall_av\t");
		System.out.print("fall_min\t");
		System.out.print("fall_max\t");
		System.out.print("uphill\t");
		System.out.print("downhill\t");
		System.out.print("dtv_av\t");
		System.out.print("dtv_min\t");
		System.out.print("dtv_max\t");
		System.out.print("dtvkat1_frac\t");
		System.out.print("dtvkat2_frac\t");
		System.out.print("dtvkat3_frac\t");
		System.out.print("dtvkat4_frac\t");
		System.out.print("dtvkat5_frac\t");
		System.out.print("covering_av\t");
		System.out.print("bikeroute_av\t");
		System.out.print("bikeroute_reco_av\t");
		System.out.print("parkroute_av\t");
		System.out.print("bridge_av\t");
		System.out.print("bridge_linkcnt\t");
		System.out.print("tunnel_av\t");
		System.out.print("tunnel_linkcnt\t");
		System.out.print("tlights_linkcnt\n");
		for (Id routeid : this.routes.keySet()) {
			this.analysis(routeid,this.routes.get(routeid));
		}
		System.out.println("      done.");

		System.out.println("      analysing network...");
		System.out.print("length\t");
		System.out.print("rise_av\t");
		System.out.print("rise_min\t");
		System.out.print("rise_max\t");
		System.out.print("fall_av\t");
		System.out.print("fall_min\t");
		System.out.print("fall_max\t");
		System.out.print("uphill\t");
		System.out.print("downhill\t");
		System.out.print("dtv_av\t");
		System.out.print("dtv_min\t");
		System.out.print("dtv_max\t");
		System.out.print("dtvkat1_frac\t");
		System.out.print("dtvkat2_frac\t");
		System.out.print("dtvkat3_frac\t");
		System.out.print("dtvkat4_frac\t");
		System.out.print("dtvkat5_frac\t");
		System.out.print("covering_av\t");
		System.out.print("bikeroute_av\t");
		System.out.print("bikeroute_reco_av\t");
		System.out.print("parkroute_av\t");
		System.out.print("bridge_av\t");
		System.out.print("bridge_linkcnt\t");
		System.out.print("tunnel_av\t");
		System.out.print("tunnel_linkcnt\t");
		System.out.print("tlights_linkcnt\n");
		this.analysis(network);
		System.out.println("      done.");

		System.out.println("    done.");
	}
}
