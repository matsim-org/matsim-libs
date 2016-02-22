/* *********************************************************************** *
 * project: org.matsim.*
 * CBNodeCreator.java
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

package playground.jbischoff.cottbusLinks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author jbischoff
 *
 */

public class CottbusTramLinkCreator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		String netfile = "\\\\vsp-nas\\jbischoff\\WinHome\\Docs\\cottbus\\cottbus_feb_fix\\network_wgs84_utm33n.xml.gz";
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netfile);
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
				TransformationFactory.WGS84_UTM33N);
		
		List<Node> nodeList = new ArrayList<Node>();
		
		
		FileReader fr;
		FileReader frr;
		try {
			fr = new FileReader(new File ("\\\\vsp-nas\\jbischoff\\WinHome\\Docs\\cottbus\\cottbus_feb_fix\\Cottbus-pt\\lines\\ptadditions.csv"));
		
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			while ((line = br.readLine()) != null) {
			    
				String[] result = line.split(";");
				
			    if (result[0].equals("b")){

					Coord xy =ct.transform(new Coord(Double.parseDouble(result[4]), Double.parseDouble(result[5])));
			    	Id<Node> nodeId = Id.create("pt"+result[1], Node.class);
			    	Node n = network.getFactory().createNode(nodeId, xy);
			    	nodeList.add(n);
			    	network.addNode(n);
			    	
			    }
			    else if (result[0].equals("c")){
			    	if (!result[1].equals("0")){
			    	Node n = network.getNodes().get(Id.create(result[1], Node.class));
			    	nodeList.add(n);
			    	}
			
			    }}
			br.close();			
			frr = new FileReader(new File ("\\\\vsp-nas\\jbischoff\\WinHome\\Docs\\cottbus\\cottbus_feb_fix\\Cottbus-pt\\lines\\ptadditions.csv"));	
			BufferedReader brr = new BufferedReader(frr);
			String ine = null;
			int i = 0;
			Id<Node> lastId = Id.create("243180738", Node.class);
			Set<String> modes = new HashSet<String>();
			modes.add("tram");
			Id<Node> currentId = null;
			while ((ine = brr.readLine()) != null) {
					
					
				
					String[] esult = ine.split(";");
					
					if (esult[0].equals("c")){
						
						currentId = Id.create(esult[1], Node.class);
						lastId = currentId;
						System.out.println(currentId);
						
					}
					else if (esult[0].equals("b")){
						lastId = currentId;
						currentId = Id.create("pt"+esult[1], Node.class);
					}
					else if (esult[0].equals("e")){
						lastId = currentId;
						currentId = Id.create(esult[1], Node.class);
					}
					else {
						
						continue;
					}
					if (currentId.equals(lastId)){
						continue;
					}
					Link l = null;
				
					
					try{
					 l = network.getFactory().createLink(Id.create("ptl"+i, Link.class), network.getNodes().get(currentId), network.getNodes().get(lastId));
					 
					}
					catch (NullPointerException ee){
						ee.printStackTrace();
						System.err.println("Could not create link from" + currentId + " to "+lastId);
					}
					System.out.println(l.getFromNode().getId()+" to "+l.getToNode().getId());
					
					Link ll = network.getFactory().createLink(Id.create("ptb"+i, Link.class), network.getNodes().get(lastId), network.getNodes().get(currentId));
					
					
					Double d = CoordUtils.calcEuclideanDistance(l.getFromNode().getCoord(), l.getToNode().getCoord());
					l.setCapacity(30);
					l.setAllowedModes(modes);
					l.setFreespeed(14);
					l.setLength(d);
					ll.setCapacity(30);
					ll.setAllowedModes(modes);
					ll.setFreespeed(14);
					ll.setLength(d);
					
					network.addLink(l);
					network.addLink(ll);
					i++;
					
				   
			}	
			
									
			    
//			manual fixes
			network.removeLink(Id.create("ptb45", Link.class));
			network.removeLink(Id.create("ptl45", Link.class));
			Link l = network.getFactory().createLink(Id.create("ptl999", Link.class), network.getNodes().get(Id.create("26999281", Node.class)), network.getNodes().get(Id.create("243180738", Node.class)));
			
			Link ll = network.getFactory().createLink(Id.create("ptb999", Link.class), network.getNodes().get(Id.create("243180738", Node.class)), network.getNodes().get(Id.create("26999281", Node.class)));
			Double d = CoordUtils.calcEuclideanDistance(l.getFromNode().getCoord(), l.getToNode().getCoord());

			l.setCapacity(30);
			l.setAllowedModes(modes);
			l.setFreespeed(14);
			l.setLength(d);
			ll.setLength(d);
			ll.setCapacity(30);
			ll.setAllowedModes(modes);
			ll.setFreespeed(14);
			network.addLink(ll);
			network.addLink(l);
			
			
			new NetworkWriter(network).write("\\\\vsp-nas\\jbischoff\\WinHome\\Docs\\cottbus\\cottbus_feb_fix\\Cottbus-pt\\network_pt.xml"); 
		}
			
		catch (Exception e){
			e.printStackTrace();
		}	
	}}



