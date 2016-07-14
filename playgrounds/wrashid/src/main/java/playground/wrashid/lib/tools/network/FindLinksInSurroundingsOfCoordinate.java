/* *********************************************************************** *
 * project: org.matsim.*
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;

import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;


/**
 * @author wrashid
 *
 */
public class FindLinksInSurroundingsOfCoordinate {

	public static void main(String[] args) {
		String inputNetworkPath="H:/data/experiments/ARTEMIS/zh/dumb charging/output/run2/output_network.xml.gz";
		Coord coordInFocus= new Coord((double) 683912, (double) 247663);
		double maxDistanceInMeters=1000;
		String outputFilePath="H:/data/experiments/ARTEMIS/zh/dumb charging/output/run2/anlysis/surroundingLinks-17560001607380FT-1.kml";
		
		BasicPointVisualizer basicPointVisualizer=new BasicPointVisualizer();
		
		Network network= GeneralLib.readNetwork(inputNetworkPath);
		
		for (Link link:network.getLinks().values()){
			if (GeneralLib.getDistance(coordInFocus, link.getCoord())<maxDistanceInMeters){
				basicPointVisualizer.addPointCoordinate(link.getCoord(), link.getId().toString(),Color.GREEN);
			}
		}
		
		basicPointVisualizer.write(outputFilePath);
	}
	
}
