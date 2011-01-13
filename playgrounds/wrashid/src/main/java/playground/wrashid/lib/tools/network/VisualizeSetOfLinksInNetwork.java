/* *********************************************************************** *
 * project: org.matsim.*
 * VisualizeSetOfLinksInNetwork.java
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

import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;

public class VisualizeSetOfLinksInNetwork {

	public static void main(String[] args) {
		String inputNetworkPath="H:/data/experiments/ARTEMIS/zh/dumb charging/output/output_network.xml.gz";
		Coord coordInFocus=new CoordImpl(683702, 247854);
		double maxDistanceInMeters=1000;
		String outputFilePath="H:/data/experiments/ARTEMIS/zh/dumb charging/output/analysis/linksOfHub434.kml";
		
		LinkedList<Id> selectedLinks=new LinkedList<Id>();
		
		selectedLinks.add(new IdImpl("17560002212733FT"));
		selectedLinks.add(new IdImpl("17560001549600FT"));
		selectedLinks.add(new IdImpl("17560000111914TF"));
		selectedLinks.add(new IdImpl("17560000111914FT"));
		selectedLinks.add(new IdImpl("17560000111902FT"));
		selectedLinks.add(new IdImpl("17560002212733TF"));
		selectedLinks.add(new IdImpl("17560001549600TF"));
		selectedLinks.add(new IdImpl("17560002154618TF"));
		selectedLinks.add(new IdImpl("17560002154618FT"));
		selectedLinks.add(new IdImpl("17560002192318FT"));
		selectedLinks.add(new IdImpl("17560002192318TF"));
		selectedLinks.add(new IdImpl("17560002213717TF"));
		selectedLinks.add(new IdImpl("17560000111903TF"));
		selectedLinks.add(new IdImpl("17560000108024FT"));
		selectedLinks.add(new IdImpl("17560001856956TF"));
		selectedLinks.add(new IdImpl("17560000108024TF"));
		selectedLinks.add(new IdImpl("17560001856959FT"));
		selectedLinks.add(new IdImpl("17560000108019FT"));
		selectedLinks.add(new IdImpl("17560001856959TF"));
		selectedLinks.add(new IdImpl("17560002213717FT"));
		selectedLinks.add(new IdImpl("17560001856956FT"));
		selectedLinks.add(new IdImpl("17560000108019TF"));
		selectedLinks.add(new IdImpl("17560002162150FT"));
		selectedLinks.add(new IdImpl("17560002161834TF"));
		selectedLinks.add(new IdImpl("17560002162150TF"));
		selectedLinks.add(new IdImpl("17560000111903FT"));
		selectedLinks.add(new IdImpl("17560000111902TF"));
		
		BasicPointVisualizer basicPointVisualizer=new BasicPointVisualizer();
		
		
		NetworkImpl network= GeneralLib.readNetwork(inputNetworkPath);
		
		for (Link link:network.getLinks().values()){
			if (selectedLinks.contains(link.getId())){
				basicPointVisualizer.addPointCoordinate(link.getCoord(), link.getId().toString(),Color.GREEN);
			}
		}
		
		basicPointVisualizer.write(outputFilePath);
	}
	
}
