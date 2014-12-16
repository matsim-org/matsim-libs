/**
 * 
 */
package playground.southafrica.projects.receiverBehaviour;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.projects.complexNetworks.pathDependence.DigicorePathDependentNetworkReader_v1;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork.PathDependentNode;
import playground.southafrica.utilities.Header;

/**
 * Class to extract all the connections from the identified establishments.
 * The establishments were identified following a visualisation in R of the 
 * results generated from {@link IdentifyEstablishments}.
 * 
 * @author jwjoubert
 */
public class ParseEstablishmentConnectivity {
	final private static Logger LOG = Logger.getLogger(ParseEstablishmentConnectivity.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ParseEstablishmentConnectivity.class.toString(), args);
		
		String networkFile = args[0];
		String rFile = args[1];
		
		DigicorePathDependentNetworkReader_v1 nr = new DigicorePathDependentNetworkReader_v1();
		nr.parse(networkFile);
		PathDependentNetwork network = nr.getPathDependentNetwork();

		int[] vertices = {39371, 39324, 35418, 36506, 39890};

		BufferedWriter bw = IOUtils.getBufferedWriter(rFile);
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		try{
			/* Write the header. */
			bw.write("id,ox,oy,olong,olat,dx,dy,dlong,dlat,weight");
			bw.newLine();

			for(int establishment : vertices){
				LOG.info(" === Establishment " + establishment);
				Id<Node> nodeId = Id.createNodeId(establishment);
				PathDependentNode node = network.getPathDependentNode(nodeId);
				
				/* Get all the node's path-dependent connections. */
				Map<Id<Node>, Map<Id<Node>, Double>> links = node.getPathDependence();
				List<Id<Node>> oList = new ArrayList<>();
				List<Id<Node>> dList = new ArrayList<>();
				
				for(Id<Node> origin : links.keySet()){
					/* Add the origin to the origin list, but only if it is not
					 * a source. */
					if(!origin.equals(Id.createNodeId("source")) &&
							!origin.equals(Id.createNodeId("unknown"))){
						oList.add(origin);
					}
					
					/* Check each path-dependent destination, and add if not 
					 * already in the destination list. Ignore the 'sink' */
					Map<Id<Node>, Double> dest = links.get(origin);
					for(Id<Node> destId : dest.keySet()){
						if(!destId.equals(Id.createNodeId("sink")) &&
								!destId.equals(Id.createNodeId("unknown"))){
							if(!dList.contains(destId)){
								dList.add(destId);
							}
						}
					}
				}
				
				/* Write all the incoming edges' weights. */
				for(Id<Node> id : oList){
					bw.write(String.valueOf(establishment));
					bw.write(",");
					
					/* Write the origin's coordinates. */
					PathDependentNode oNode = network.getPathDependentNode(id);
					if(oNode == null){
						LOG.debug("Why is there a null node?!");
					}
					Coord o1 = oNode.getCoord();
					bw.write(String.format("%.0f, %.0f,", o1.getX(), o1.getY()));
					Coord o1c = ct.transform(o1);
					bw.write(String.format("%.6f, %.6f,", o1c.getX(), o1c.getY()));
					
					/* Write establishment's coordinates. */
					Coord e1 = node.getCoord();
					bw.write(String.format("%.0f, %.0f,", e1.getX(), e1.getY()));
					Coord e1c = ct.transform(e1);
					bw.write(String.format("%.6f, %.6f,", e1c.getX(), e1c.getY()));
					
					/* Write the weight. */
					bw.write(String.format("%.2f\n", network.getWeight(id, nodeId)));
				}
				
				/* Write all the outgoing edges' weights. */
				for(Id<Node> id : dList){
					bw.write(String.valueOf(establishment));
					bw.write(",");
					
					/* Write establishment's coordinates. */
					Coord e1 = node.getCoord();
					bw.write(String.format("%.0f, %.0f,", e1.getX(), e1.getY()));
					Coord e1c = ct.transform(e1);
					bw.write(String.format("%.6f, %.6f,", e1c.getX(), e1c.getY()));
					
					/* Write the destination's coordinates. */
					Coord d1 = network.getPathDependentNode(id).getCoord();
					bw.write(String.format("%.0f, %.0f,", d1.getX(), d1.getY()));
					Coord d1c = ct.transform(d1);
					bw.write(String.format("%.6f, %.6f,", d1c.getX(), d1c.getY()));
					
					/* Write the weight. */
					bw.write(String.format("%.2f\n", network.getWeight(nodeId, id)));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + rFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + rFile);
			}
		}
		
		Header.printFooter();
	}

}
