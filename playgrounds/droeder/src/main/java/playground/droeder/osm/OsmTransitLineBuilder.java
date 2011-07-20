/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.osm;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.droeder.DaPaths;
import playground.droeder.gis.DaShapeWriter;

/**
 * @author droeder
 *
 */
public class OsmTransitLineBuilder {
	
	private String osmFile;
	private String fromCoord;
	private String toCoord;
	private TransitSchedule timeTable;

	public OsmTransitLineBuilder(final String osmFile, final String fromCoord, final String toCoord, final TransitSchedule timeTableTomatch){
		this.osmFile = osmFile;
		this.fromCoord = fromCoord;
		this.toCoord = toCoord;
		this.timeTable = timeTableTomatch;
	}
	
	@SuppressWarnings("unchecked")
	public void run(final String outDir){
		String[] modes = new String[1];
		modes[0] = "subway";
		Osm2TransitlineNetworks networkBuilder = new Osm2TransitlineNetworks(this.osmFile, this.fromCoord, this.toCoord);
		networkBuilder.convertOsm2Matsim(modes);
		
		for(Entry<Id, Map<String, NetworkImpl>> n : networkBuilder.getLine2Net().entrySet()){
			for(Entry<String, NetworkImpl> nn: n.getValue().entrySet()){
				DaShapeWriter.writeLinks2Shape(outDir + n.getKey().toString() + "_" + nn.getKey() + ".shp", nn.getValue().getLinks(), null);
				DaShapeWriter.writeNodes2Shape(outDir + n.getKey().toString() + "_" + nn.getKey() + "_startNode.shp", findStartNode(nn.getValue()));
			}
		}
	}
	
	public Map<Id, Node> findStartNode(Network n){
		Map<Id, Node> nodes = new HashMap<Id, Node>();
		
		for(Node no: n.getNodes().values()){
			if(no.getInLinks().size() == 0 && no.getOutLinks().size() == 1){
				nodes.put(no.getId(), no);
			}
		}
		return nodes;
	}
	
	public static void main(String[] args){
		final String DIR = DaPaths.OUTPUT + "osm2/";
		final String INFILE = DIR + "berlin_subway.osm";
		new OsmTransitLineBuilder(INFILE, TransformationFactory.WGS84, TransformationFactory.DHDN_GK4, null).run(DIR);
		
	}

}
