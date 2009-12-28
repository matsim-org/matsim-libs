/* *********************************************************************** *
 * project: org.matsim.*
 * InitDemandCreation.java
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

package playground.anhorni.crossborder;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.algorithms.NetworkAdaptLength;
import org.matsim.core.network.algorithms.NetworkSummary;

import playground.anhorni.crossborder.verification.Verification;

public class InitDemandCreation {

	private NetworkLayer network;
	private ArrayList<String> files;
	private Verification verification;

	public InitDemandCreation() {
		this.files=new ArrayList<String>();
		this.verification=new Verification();
	}

	public void createInitDemand(){

		// Create Zones
		System.out.println("Parsing Zones2Nodes");
		Zones2NodesParser z2nParser=new Zones2NodesParser(Config.zones2NodesFile);
		z2nParser.parse();
		Hashtable<Integer, Zone> zones = z2nParser.getZones();

		// Parse the matrix (fma) files
		Vector<String> activityTypes=new Vector<String>();
		activityTypes.add("E"); activityTypes.add("P"); activityTypes.add("N"); activityTypes.add("S");

		int actNumberOfPersons=0;
		for (Enumeration<String> e = activityTypes.elements() ; e.hasMoreElements() ;) {

			String s=e.nextElement();
			for (int i=0; i<24; i++) {
					String matrixPath="input/"+s+"/miv_" +s+ "_" +i+ ".fma";

					System.out.print("Parsing matrix: \""+matrixPath+ "\" and writing file \""+s +i +"\" \t");
					FMAParser parser=new FMAParser(this.network, matrixPath, zones);
					parser.setVerification(this.verification);

					int addNumberOfPersons=parser.parse(s, i, actNumberOfPersons);
					System.out.println("Number of added plans: "+ addNumberOfPersons);
					actNumberOfPersons+=addNumberOfPersons;
					this.files.add("output/"+s+i);
			}//for
		}

		System.out.println("Writing final xml: \""+Config.OUTFILE +"\"");
		FinalWriter finalWriter=new FinalWriter(this.files);
		finalWriter.write();

		System.out.println("Writing verification");
		this.verification.writeVerification();
		System.out.println("finished");

	}


	private void readNetwork() {
		this.network = new NetworkLayer();
		new MatsimNetworkReader(this.network).readFile(Config.networkFile);

		// running Network adaptation algorithms
		new NetworkSummary().run(this.network);
		new NetworkAdaptLength().run(this.network);
		new NetworkSummary().run(this.network);
	}


	public static void main(final String[] args) {
		Gbl.startMeasurement();

		InitDemandCreation ic=new InitDemandCreation();
		ic.readNetwork();
		ic.createInitDemand();

		Gbl.printElapsedTime();
	}
}
