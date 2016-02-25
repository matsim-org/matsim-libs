/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreNetworkWriter.java
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

import java.io.BufferedWriter;
import java.io.File;
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
 * Class to write a {@link DigicoreNetwork} in different formats to file. Current
 * formats supported:
 * <ul>
 * 		<li> own node/arc format;
 * 		<li> node coordinates with in-, out- and total order;
 * </ul>
 * TODO Formats that must still be completed:
 * <ul>
 * 		<li> GraphML (partially complete);
 * 		<li> GXL;
 * </ul>
 * @author jwjoubert
 *
 */
public class DigicoreNetworkWriter {
	private final Logger log = Logger.getLogger(DigicoreNetworkWriter.class);
	private final DigicoreNetwork network;

	public DigicoreNetworkWriter(DigicoreNetwork network) {
		this.network = network;
	}
	
	public void writeNetwork(String filename) throws IOException{
		this.writeNetwork(filename, false);
	}
	

	/**
	 * Writing the {@link DigicoreNetwork} to file in our own format:
	 * <h5>Format example:</h5>
	 * <blockquote>
	 * <code>
	 * NODES<br>
	 * Node_Id,Long,Lat<br>
	 * 1,3234.00,-2134.00<br>
	 * 2,2132.76,-3234.23<br>
	 * ARCS<br>
	 * From_Id,To_Id,From_Type,To_Type,Weight<br>
	 * 1,2,major,minor,3<br>
	 * 2,1,minor,minor,1
	 * </code></blockquote>
	 * @param filename absolute path of the output;
	 * @param overwrite indicating if existing files be overwritten;
	 * @throws IOException if the output file already exists, and permission 
	 * 		   is denied to overwrite the file.
	 */
	public void writeNetwork(String filename, boolean overwrite) throws IOException {
		if(!overwrite){
			File f = new File(filename);
			if(f.exists()){
				log.warn("File " + filename + " exists and may not be overwritten!!");
				throw new IOException("Cannot overwrite " + filename);
			}			
		}
		log.info("Writing network to " + filename);
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		
		/* Write the nodes/vertices. */
		log.info("  Writing nodes (" + network.getVertexCount() + ")");
		Counter nodeCounter = new Counter("   nodes: ");
		bw.write("NODES");
		bw.newLine();
		bw.write("NodeId,Long,Lat");
		bw.newLine();
		for(Id<ActivityFacility> id : network.getVertices()){
			bw.write(id.toString());
			bw.write(",");
			bw.write(String.format("%.4f,%.4f\n", network.getCoordinates().get(id).getX(), network.getCoordinates().get(id).getY()));
			nodeCounter.incCounter();
		}
		nodeCounter.printCounter();
		
		/* Write the arcs/edges. */
		log.info("  Writing arcs (" + network.getWeights().size() + ")");
		log.info("  (Note: number of edges written is type-specific. It is likely to be more than )");
		log.info("  (      the actual number of edges stipulated in the weight-less graph itself. )");
		Counter arcCounter = new Counter("   arcs: ");
		bw.write("ARCS");
		bw.newLine();
		bw.write("From_Id,To_Id,From_Type,To_Type,Weight");
		bw.newLine();
		for(Tuple<Pair<Id<ActivityFacility>>, Pair<String> > tuple : network.getWeights().keySet()){
			bw.write(tuple.getFirst().getFirst().toString());
			bw.write(",");
			bw.write(tuple.getFirst().getSecond().toString());
			bw.write(",");
			bw.write(tuple.getSecond().getFirst());
			bw.write(",");
			bw.write(tuple.getSecond().getSecond());
			bw.write(",");
			bw.write(String.valueOf(network.getWeights().get(tuple)));
			bw.newLine();
			arcCounter.incCounter();
		}
		arcCounter.printCounter();
		
		bw.close();
	}
	
//	public void writeGraphML(String fGraphML, String year, String run) {
//		GraphMetadata graphMetadata = new GraphMetadata();
//		graphMetadata.setProperty("year", year);
//		graphMetadata.setProperty("run", run);
//
//		graphMetadata.setDescription("Digicore graph");
//		graphMetadata.setEdgeDefault(EdgeDefault.DIRECTED);
//		graphMetadata.setGraph(this.network);
//		graphMetadata.setId("digicore");
//		
//		
//
//		Transformer<Pair<Id>, String> edgeWeightTransformer	= new DigicoreEdgeWeightTransformer(network.getWeights());
//		Transformer<Id, String> vertexCoordinateTransformer	= new DigicoreNodeCoordinateTransformer(network.getCoordinates());
//	
//		GraphMLWriter<Id, Pair<Id>> gmw = new GraphMLWriter<Id, Pair<Id>>();
//		
////		gmw.setGraphData("graph", this.graphMetadata.)
//		for(Id id : network.getCoordinates().keySet()){
//			gmw.addVertexData("coord", "Coordinate", "[0 ; 0]", vertexCoordinateTransformer);
//		}
//		for(Pair<Id> arc : network.getWeights().keySet()){
//			gmw.addEdgeData("weight", "Number of commercial vehicle trips on directed edge", "0", edgeWeightTransformer);	
//		}
//		try {
//			FileWriter fw = new FileWriter(fGraphML, false);
//			gmw.save(this.network, fw);
//		} catch (IOException e) {
//			log.error("Maybe output file already exists! Delete and rerun.");
//			throw new RuntimeException("Could not write GraphML to " + fGraphML);
//		}
//	}
	
	/**
	 * Writes for each vertex (facility) the {@link Id}, in-order, the out-order, 
	 * and as the total order. 
	 * @param filename
	 * @throws IOException 
	 */
	public void writeGraphOrderToFile(String filename) throws IOException{
		log.info("Writing network order to " + filename);
		log.info("Number of vertices to write: " + network.getVertexCount());
		Counter counter = new Counter("   vertices written: ");
		
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			bw.write("FacilityId,Long,Lat,InOrder,OutOrder,Order");
			bw.newLine();
			
			for(Id<ActivityFacility> id : this.network.getVertices()){
				bw.write(id.toString());
				bw.write(",");
				Coord c = network.getCoordinates().get(id);
				if(c == null){
					log.warn("Null Coord found.");
				}
				bw.write(String.format("%.2f,%.2f,", network.getCoordinates().get(id).getX(), network.getCoordinates().get(id).getY()));
				
				/* Get the total weighted in-degree. */
				int in = 0;
				for(Tuple<Pair<Id<ActivityFacility>>, Pair<String>> tuple : network.getWeights().keySet()){
					if(tuple.getFirst().getSecond() == id){
						in += network.getWeights().get(tuple);
					}
				}
				bw.write(String.valueOf(in));
				bw.write(","); 
				
				/* Get the total weighted out-degree. */
				int out = 0;
				for(Tuple<Pair<Id<ActivityFacility>>, Pair<String>> tuple : network.getWeights().keySet()){
					if(tuple.getFirst().getFirst() == id){
						out += network.getWeights().get(tuple);
					}
				}
				bw.write(String.valueOf(out));
				bw.write(","); 
				
				bw.write(String.valueOf(in + out));
				bw.newLine();
				
				counter.incCounter();
			}
			counter.printCounter();
			
		} catch (IOException e) {
			throw new IOException("IOException when writing to " + filename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new IOException("IOException when trying to close BufferedWriter " + filename);
			}
		}		
	}



}

