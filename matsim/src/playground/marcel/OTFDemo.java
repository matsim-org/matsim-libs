/* *********************************************************************** *
 * project: org.matsim.*
 * OTFDemo.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.marcel;

public class OTFDemo {

	public static void main(final String[] args) {
		// WU base cae
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/VSP/cvs/vsp-cvs/runs/run465/500.T.veh.gz", "/Volumes/Data/VSP/cvs/vsp-cvs/studies/schweiz-ivtch/network/ivtch-osm.xml"});

		// WU base case:
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/ETH/cvs/ivt/studies/switzerland/results/westumfahrung/run365/it.150/T.veh.gz", "/Volumes/Data/ETH/cvs/ivt/studies/switzerland/networks/ivtch-changed/network.xml"});

		// WU with new roads
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/ETH/cvs/ivt/studies/switzerland/results/westumfahrung/run370/it.220/T.veh.gz", "/Volumes/Data/ETH/cvs/ivt/studies/switzerland/networks/ivtch-changed-wu/network.xml"});

		// WU with new roads and with FlaMa
//		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/ETH/cvs/ivt/studies/switzerland/results/westumfahrung/run374/it.240/T.veh.gz", "/Volumes/Data/ETH/cvs/ivt/studies/switzerland/networks/ivtch-changed-wu-flama/network.xml"});

		// other cases
		org.matsim.run.OTFVis.main(new String[] {"/Volumes/Data/VSP/runs/itm3/100.T.veh.gz", "/Volumes/Data/VSP/cvs/vsp-cvs/studies/schweiz-ivtch/network/ivtch-osm.xml"});

	}

}
