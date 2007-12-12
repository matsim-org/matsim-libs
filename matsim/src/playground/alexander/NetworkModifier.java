/* *********************************************************************** *
 * project: org.matsim.*
 * ShapeFileConverterDLR2KML_v2.java
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

package playground.alexander;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkWriter;
import org.matsim.world.World;

public class NetworkModifier {
	    	   	
	public static void main( String[] args )
	{				
		
		String readFileNameNet;
		String writeFileNameNet;
		String fileNameNodes;
		String fileNameLinks;
		
		if (args.length == 4){
			readFileNameNet = args[0];
			writeFileNameNet = args[1];
			fileNameNodes = args[2];
			fileNameLinks = args[3];
		}
		
		else{
			readFileNameNet = "./padang/padang_net_new_261207.xml";
			writeFileNameNet = "./padang/padang_net_new_131207.xml";
			fileNameNodes = "./padang/padang_change_nodes.txt";
			fileNameLinks = "./padang/padang_change_links.txt";
		}
		
		World world = Gbl.createWorld();
		Config config = Gbl.createConfig(new String[] {"./evacuationConf.xml"});
		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(readFileNameNet);
	
		Reader reader = new Reader(network);
		reader.readfile(fileNameNodes,"node");		
		reader.readfile(fileNameLinks, "link");
		
		NetworkWriter nw = new NetworkWriter(network, writeFileNameNet);
		nw.write();
				
	}

}

