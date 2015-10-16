/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreNetworkParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.freight.digicore.algorithms.complexNetwork;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;

import playground.southafrica.freight.digicore.containers.DigicoreNetwork;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * Class to parse a {@link DigicoreNetwork} from different format files. Current
 * formats supported:
 * <ul>
 * 		<li> own node/arc format;
 * </ul>
 * TODO Formats that must still be completed:
 * <ul>
 * 		<li> GraphML;
 * 		<li> GXL;
 * </ul>
 * @author johanwjoubert
 *
 */
public class DigicoreNetworkParser {
	private final Logger log = Logger.getLogger(DigicoreNetworkParser.class);
	
	public DigicoreNetworkParser() {
		
	}
	
	/**
	 * Parsing the {@link DigicoreNetwork} from a file in our own format:
	 * <h5>Format example:</h5>
	 * <blockquote>
	 * <code>
	 * NODES<br>
	 * Node_Id,Long,Lat,Major<br>
	 * 1,3234.00,-2134.00,true<br>
	 * 2,2132.76,-3234.23,false<br>
	 * ARCS<br>
	 * From_Id,To_Id,Weight<br>
	 * 1,2,3<br>
	 * 2,1,0
	 * </code></blockquote>
	 * @param filename absolute path of the input file;
	 * @throws IOException
	 */
	public DigicoreNetwork parseNetwork(String filename) throws IOException{
		DigicoreNetwork dn = new DigicoreNetwork();
		
		log.info("Parsing network from " + filename);
		long startTime = System.currentTimeMillis();
		Counter nodeCounter = new Counter("   nodes: ");
		Counter arcCounter = new Counter("   arcs: " );

		BufferedReader br = IOUtils.getBufferedReader(filename);
		try {
			String line = null;
			String type = null;
			while((line = br.readLine()) != null){
				if(line.equalsIgnoreCase("NODES")){
					line = br.readLine();
					line = br.readLine();
					type = "node";
				} else if(line.equalsIgnoreCase("ARCS")){
					nodeCounter.printCounter();
					line = br.readLine();
					line = br.readLine();
					type = "arc";
				}
				String[] sa = line.split(",");
				if(type.equals("node")){
					Id<ActivityFacility> id = Id.create(sa[0], ActivityFacility.class);
					if(!dn.containsVertex(id)){
						dn.addVertex(id);
						dn.getCoordinates().put(id, new Coord(Double.parseDouble(sa[1]), Double.parseDouble(sa[2])));
					}
					nodeCounter.incCounter();
				} else if(type.equals("arc")){
					Id<ActivityFacility> o = Id.create(sa[0], ActivityFacility.class);
					Id<ActivityFacility> d = Id.create(sa[1], ActivityFacility.class);
					String oType = sa[2];
					String dType = sa[3];
					if(!dn.containsVertex(o) || !dn.containsVertex(d)){
						throw new IOException("Parsing an edge [" + o.toString() + " --> " + d.toString() + "] of which one or both of the vertices do not exist in the network.");
					}
					Pair<Id<ActivityFacility>> idPair = new Pair<Id<ActivityFacility>>(o, d);
					Pair<String> typePair = new Pair<String>(oType, dType);
					dn.addEdge(idPair, o, d);
					dn.getWeights().put(new Tuple<Pair<Id<ActivityFacility>>, Pair<String>>(idPair, typePair), Integer.parseInt(sa[4]));
					arcCounter.incCounter();
				} else{
					log.error("Could not find an entry type.");
					log.error("Check input file format of " + filename);
				}
			}
			arcCounter.printCounter();
		} catch (IOException e) {
			throw new IOException("Could not read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				throw new IOException("Could not closed BufferedReader for" + filename);
			}
		}
		long stopTime = System.currentTimeMillis();
		double time =  ((double)stopTime - (double)startTime)/1000.0;
		log.info(String.format("Network read time (s): %.2f",time));
		dn.printBasicNetworkStatistics();
		return dn;
	}
	
}

