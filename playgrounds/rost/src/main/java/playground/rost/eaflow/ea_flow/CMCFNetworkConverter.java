/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkReaderMatsimV1.java
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
package playground.rost.eaflow.ea_flow;
import java.io.IOException;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordImpl;
/**
 * 
 * @author Manuel Schneider
 *
 */
public class CMCFNetworkConverter {
	
	
	/**
	 * reads a cmcf graph file in xml format from filename 
	 * @param filename
	 * @return a NetworkLayer that is the graph represented in the cmcf file
	 * @throws JDOMException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static NetworkImpl readCMCFNetwork(String filename) throws JDOMException, IOException{
		NetworkImpl result = NetworkImpl.createNetwork();
		SAXBuilder builder = new SAXBuilder();
		Document cmcfGraph = builder.build(filename);
		Element basegraph = cmcfGraph.getRootElement();
		// read and set the nodes
		Element nodes = basegraph.getChild("nodes");
		 List<Element> nodelist = nodes.getChildren();
		 for (Element node : nodelist){
			 //read the values of the node xml Element as Strings
			 String id = node.getAttributeValue("id");
			 String x = node.getAttributeValue("x");
			 String y = node.getAttributeValue("y");
			 //build a new node in the NetworkLayer
			 Coord coord = new CoordImpl(x,y);
			 Id matsimid  = new IdImpl(id);
			 result.createAndAddNode(matsimid, coord);
		 }
		 //read the edges
		 Element edges = basegraph.getChild("edges");
		 List<Element> edgelist = edges.getChildren();
		 for (Element edge : edgelist){
			//read the values of the edge xml Element as Strings
			 String id = edge.getAttributeValue("id");
			 String from = edge.getChildText("from");
			 String to	= edge.getChildText("to");
			 String length = edge.getChildText("length");
			 String capacity = edge.getChildText("capacity");
			 //build a new edge in 
			 Id matsimid  = new IdImpl(id);
			 //TODO free speed is set to 1.3 find something better
			 result.createAndAddLink(matsimid, result.getNodes().get(new IdImpl(from)), result.getNodes().get(new IdImpl(to)),
					 Double.parseDouble(length),
					  1.3 ,
					 Double.parseDouble(capacity),
					 1.);
		 }
			
		return result;
	}

	/**
	 * @param usage: 1. argument inputfile 2. argument outfile (optional)
	 */
	public static void main(String[] args) { 
		if(args.length == 0 && args.length > 2){
			System.out.println("usage: 1. argument inputfile 2. argument outfile (optional)");
			return;
		}
		String inputfile = args[0].trim();
		String outfile = inputfile.substring(0, inputfile.length()-4)+"_msimNW.xml";
		if(args.length == 2){
			outfile = args[1];
		}
		try {
			NetworkImpl network = readCMCFNetwork(inputfile);
			new NetworkWriter(network).write(outfile);
			System.out.println(inputfile+"  converted successfully \n"+"output written in: "+outfile);
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}

}
