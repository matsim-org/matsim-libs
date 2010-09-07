/* *********************************************************************** *
 * project: org.matsim.*
 * VisLineStringGen.java
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
package playground.gregor.osmevacuation;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;

public class VisLineStringGen {
	
	
	private String netLs;
	private String network;
	private String rawLs;
	
	
	public VisLineStringGen(String rawLs, String network, String netLs) {
		this.rawLs = rawLs;
		this.network = network;
		this.netLs = netLs;
	}

	private void run() {
		ScenarioImpl sc = new ScenarioImpl();
		new MatsimNetworkReader(sc).readFile(this.network);
		buildLsQuad();
		
	}
	private void buildLsQuad() {
		
		
	}

	public static void main (String [] args) {
		
		String rawLs = "/home/laemmel/devel/evac_tutorial/inputs/raw_ls.shp";
		String network = "/home/laemmel/devel/evac_tutorial/inputs/raw_ls.shp";
		String netLs = "/home/laemmel/devel/evac_tutorial/inputs/net_ls.shp";
		
		new VisLineStringGen(rawLs, network,netLs).run();
		
	}


}
