/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.southafrica.projects.complexNetworks.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.utilities.Header;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;

/**
 * Class to read a MATSim {@link Network} and transform it so that it can be
 * read, as CSV file, into Gephi.
 * 
 * @author jwjoubert
 */
public class GraphMLConverter {
	private final static Logger LOG = Logger.getLogger(GraphMLConverter.class); 
	

	/**
	 * Implementation to convert a MATSim {@link Network} into a GraphML file.
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GraphMLConverter.class.toString(), args);
		String networkFile = args[0];
		String gephiFile = args[1];
		
		GraphMLConverter.convertToGraphML(networkFile, gephiFile);
		
		Header.printFooter();
	}
	
	/**
	 * Parses a MATSim-format {@link Network}, and translates it into a GraphML 
	 * format using the {@link com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter}. Then writes the resulting GraphML format to disk.
	 * @param networkFile
	 * @param gephiFile
	 */
	public static void convertToGraphML(String networkFile, String graphMLFile){
		/* Parse the MATSim network. */
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader mnr = new MatsimNetworkReader(sc.getNetwork());
		mnr.readFile(networkFile);
		NetworkImpl n = (NetworkImpl) sc.getNetwork();
		LOG.info("Total number of links to add: " + sc.getNetwork().getLinks().size());
		Counter counter = new Counter("  links # ");
		
		/* Create an empty graph. */
		TinkerGraph g = null;
		try{
			g = new TinkerGraph();
			
			/* Populate the graph with the network elements. */
			Iterator<Link> li = n.getLinks().values().iterator();
			List<Id> nodeList = new ArrayList<Id>();
			while(li.hasNext()){
				Link l = li.next();
				Node o = l.getFromNode();
				Node d = l.getToNode();
				
				/* Parse the origin and destination node of each link, but only 
				 * if the node has not already been added to the graph. The node
				 * data includes:
				 *  - longitude (x);
				 *  - latitude (y)
				 */
				Vertex vo = null;
				if(!nodeList.contains(o.getId())){
					vo = g.addVertex(o.getId());
					vo.setProperty("longitude", Double.valueOf(o.getCoord().getX()));
					vo.setProperty("latitude", Double.valueOf(o.getCoord().getY()));
					nodeList.add(o.getId());
				} else{
					vo = g.getVertex(o.getId());
				}
		
				Vertex vd = null;
				if(!nodeList.contains(d.getId())){
					vd = g.addVertex(d.getId().toString());
					vd.setProperty("longitude", Double.valueOf(d.getCoord().getX()));
					vd.setProperty("latitude", Double.valueOf(d.getCoord().getY()));
					nodeList.add(d.getId());
				} else{
					vd = g.getVertex(d.getId());
				}
				
				/* Link data, including:
				 *  - length;
				 *  - number of lanes;
				 *  - capacity (derivative of the above two, assuming 7m per vehicle);
				 */
				Edge e = g.addEdge(l.getId(), vo, vd, l.getId().toString());
				e.setProperty("length", Double.valueOf(l.getLength()));
				e.setProperty("lanes", Double.valueOf(l.getNumberOfLanes()));
				e.setProperty("capacity", Double.valueOf(l.getCapacity()));
				counter.incCounter();
			}
			counter.printCounter();
			
			LOG.info("Writing GraphML file to " + graphMLFile);
			
			Map<String, String> vertexKeyTypes = new TreeMap<String, String>();
			vertexKeyTypes.put("longitude", "double");
			vertexKeyTypes.put("latitude", "double");
			Map<String, String> edgeKeyTypes = new TreeMap<String, String>();
			edgeKeyTypes.put("length", "double");
			edgeKeyTypes.put("lanes", "double");
			edgeKeyTypes.put("capacity", "double");
			
			GraphMLWriter gw = new GraphMLWriter(g);
			gw.setEdgeLabelKey("Label");
			gw.setVertexKeyTypes(vertexKeyTypes);
			gw.setEdgeKeyTypes(edgeKeyTypes);
			gw.setNormalize(true);
			OutputStream os = IOUtils.getOutputStream(graphMLFile);
			try{
				gw.outputGraph(os);
			} catch (IOException e) {
				throw new RuntimeException("Could not write to OutputStream for GraphML output.");
			} finally{
				try {
					os.close();
				} catch (IOException e) {
					throw new RuntimeException("Could not close OutputStream for GraphML output.");
				}
			}
		} finally{
			if(g != null){
				g.shutdown();
			}
		}
	}	
	
	public static Graph convertFromGraphML(String filename){
		Graph graph = new TinkerGraph();
		InputStream is =  IOUtils.getInputStream(filename);
		try{
			GraphMLReader gmr = new GraphMLReader(graph);
			gmr.inputGraph(is);
		} catch (IOException e) {
			throw new RuntimeException("Could not read from InputStream.");
		} finally{
			try {
				is.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close InputStream.");
			}
		}
		return graph;
	}
}
