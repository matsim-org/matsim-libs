/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.thelma.psl;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;

public class PrintNetworkLinkCenter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String networkFilePath="C:/tmp2/matsim2030/multimodalNetwork2010final.xml.gz";
		
		Network network = GeneralLib.readNetwork(networkFilePath);
		
		System.out.println("linkId\tx\ty");
		
		for (Link link:network.getLinks().values()){
			if (link.getAllowedModes().contains(TransportMode.car)){
				System.out.println(link.getId() + "\t" + link.getCoord().getX() + "\t" + link.getCoord().getY());
			}
		}
	}

}

