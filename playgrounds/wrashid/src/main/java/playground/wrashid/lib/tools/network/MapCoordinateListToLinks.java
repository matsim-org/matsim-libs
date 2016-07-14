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
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;

import java.util.LinkedList;

public class MapCoordinateListToLinks {

	public static void main(String[] args) {
		String inputNetworkPath="H:/data/experiments/ARTEMIS/output/run10/output_network.xml.gz";
		
		LinkedList<Coord> linkCoordinates=new LinkedList<Coord>();

		linkCoordinates.add(new Coord(682703.870, 248719.332));
		linkCoordinates.add(new Coord(682693.522, 248703.491));
		linkCoordinates.add(new Coord(682661.441, 248667.857));
		linkCoordinates.add(new Coord(682749.022, 248673.041));
		linkCoordinates.add(new Coord(682774.155, 248617.997));
		linkCoordinates.add(new Coord(682731.214, 248689.764));
		linkCoordinates.add(new Coord(682631.760, 248651.038));
		linkCoordinates.add(new Coord(682619.049, 248653.140));
		linkCoordinates.add(new Coord(682610.793, 248673.261));
		linkCoordinates.add(new Coord(682755.168, 248760.162));
		linkCoordinates.add(new Coord(682713.204, 248799.523));
		linkCoordinates.add(new Coord(682691.848, 248817.188));
		linkCoordinates.add(new Coord(682873.382, 248537.362));
		linkCoordinates.add(new Coord(682801.240, 248640.035));
		linkCoordinates.add(new Coord(682860.600, 248613.556));

		NetworkImpl network= (NetworkImpl) GeneralLib.readNetwork(inputNetworkPath);
		
		LinkedList<Link> selectedLinks=new LinkedList<Link>();
		
		for (Coord coordinate:linkCoordinates){
				Link nearestLink = NetworkUtils.getNearestLink(network, coordinate);
				System.out.println(nearestLink.getId());
				selectedLinks.add(nearestLink);
		}

		createKMLAtTempLocation(selectedLinks);
	}

	private static void createKMLAtTempLocation(LinkedList<Link> selectedLinks) {
		BasicPointVisualizer basicPointVisualizer=new BasicPointVisualizer();
		
		for (Link link : selectedLinks) {
			basicPointVisualizer.addPointCoordinate(link.getCoord(), link.getId().toString(),Color.GREEN);
		}
		
		basicPointVisualizer.write(GeneralLib.eclipseLocalTempPath + "/selectedLinks.kml");
		
	}

}
