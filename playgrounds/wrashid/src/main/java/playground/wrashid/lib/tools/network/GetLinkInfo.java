/* *********************************************************************** *
 * project: org.matsim.*
 * GetLinkInfo.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.wrashid.lib.tools.network;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.lib.GeneralLib;

public class GetLinkInfo {

	public static void main(String[] args) {
		String networkFilePath="H:/data/experiments/ARTEMIS/zh/dumb charging/output/output_network.xml.gz";
		String linkIdString="17560001856956FT";
		
		NetworkImpl network = GeneralLib.readNetwork(networkFilePath);
		
		System.out.println(network.getLinks().get(new IdImpl(linkIdString)).toString());
		System.out.println(network.getLinks().get(new IdImpl(linkIdString)).getCoord().toString());
	}
	
}
