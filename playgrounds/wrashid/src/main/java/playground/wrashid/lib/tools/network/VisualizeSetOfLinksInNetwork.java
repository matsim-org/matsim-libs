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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;

import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;

public class VisualizeSetOfLinksInNetwork {

	public static void main(String[] args) {
		String inputNetworkPath="H:/data/experiments/ARTEMIS/zh/dumb charging/output/run2/output_network.xml.gz";
		String outputFilePath="H:/data/experiments/ARTEMIS/zh/dumb charging/output/run2/analysis/linksWithEnergyConsumpHigherThanThreshholdValue.kml";
		
		LinkedList<Id<Link>> selectedLinks=new LinkedList<Id<Link>>();
		
		selectedLinks.add(Id.create("17560000662724TF", Link.class));
		selectedLinks.add(Id.create("17560002226916TF", Link.class));
		selectedLinks.add(Id.create("17560000114875TF", Link.class));
		selectedLinks.add(Id.create("17560000082333TF", Link.class));
		selectedLinks.add(Id.create("17560002149918FT", Link.class));
		selectedLinks.add(Id.create("17560000368213FT", Link.class));
		selectedLinks.add(Id.create("17560002188272FT", Link.class));
		selectedLinks.add(Id.create("17560001856956FT", Link.class));
		selectedLinks.add(Id.create("17560001229496TF", Link.class));
		selectedLinks.add(Id.create("17560001363425TF", Link.class));
		selectedLinks.add(Id.create("17560001607380FT-1", Link.class));
		selectedLinks.add(Id.create("17560000045386TF", Link.class));
		selectedLinks.add(Id.create("17560000109095TF", Link.class));
		selectedLinks.add(Id.create("17560001227588FT", Link.class));
		selectedLinks.add(Id.create("17560000043382FT", Link.class));
		selectedLinks.add(Id.create("17560000105015FT", Link.class));
		selectedLinks.add(Id.create("17560000109323TF", Link.class));
		selectedLinks.add(Id.create("17560001594646FT", Link.class));
		selectedLinks.add(Id.create("17560001380278TF", Link.class));
		
		BasicPointVisualizer basicPointVisualizer=new BasicPointVisualizer();
		
		
		Network network= GeneralLib.readNetwork(inputNetworkPath);
		
		for (Link link:network.getLinks().values()){
			if (selectedLinks.contains(link.getId())){
				basicPointVisualizer.addPointCoordinate(link.getCoord(), link.getId().toString(),Color.GREEN);
			}
		}
		
		basicPointVisualizer.write(outputFilePath);
	}
	
}
