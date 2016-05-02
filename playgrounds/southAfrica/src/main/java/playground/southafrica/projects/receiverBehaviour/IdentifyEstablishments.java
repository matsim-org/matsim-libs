/**
 * 
 */
package playground.southafrica.projects.receiverBehaviour;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.projects.complexNetworks.pathDependence.DigicorePathDependentNetworkReader_v1;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork.PathDependentNode;
import playground.southafrica.utilities.Header;

/**
 * Utility class to just identify all the path-dependent network vertices that
 * are within 1 kilometre from one of the identified (hard coded) facilities. 
 * 
 * @author jwjoubert
 */
public class IdentifyEstablishments {
	final private static Logger LOG = Logger.getLogger(IdentifyEstablishments.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(IdentifyEstablishments.class.toString(), args);
		
		String networkFile = args[0];
		String rFile = args[1];
		
		DigicorePathDependentNetworkReader_v1 nr = new DigicorePathDependentNetworkReader_v1();
		nr.parse(networkFile);

		/* Set up the three shopping area locations. */
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");
		final double y2 = -25.774736;
		Coord waterkloof = ct.transform(new Coord(28.240537, y2));
		final double y1 = -25.770383;
		Coord menlo = ct.transform(new Coord(28.257851, y1));
		final double y = -25.744117;
		Coord watermeyer = ct.transform(new Coord(28.294479, y));
		
		/* Check all network vertices, and count how many are within 1km */
		LOG.info("Checking node distances...");
		Counter counter = new Counter("   node # ");
		PathDependentNetwork network = nr.getPathDependentNetwork();
		Map<Id<Node>, Coord> nodeMap = new HashMap<Id<Node>, Coord>();
		for(PathDependentNode node: network.getPathDependentNodes().values()){
			double dMenlo = CoordUtils.calcEuclideanDistance(menlo, node.getCoord());
			double dWaterkloof = CoordUtils.calcEuclideanDistance(waterkloof, node.getCoord());
			double dWatermeyer = CoordUtils.calcEuclideanDistance(watermeyer, node.getCoord());
			if(dMenlo <= 1000 || dWaterkloof <= 1000 || dWatermeyer <= 1000){
				nodeMap.put(node.getId(), node.getCoord());
			}
			counter.incCounter();
		}
		LOG.info("Number of nodes within 1km of at least ONE facility: " + nodeMap.size());
		
		LOG.info("Writing nodes to file...");
		BufferedWriter bw = IOUtils.getBufferedWriter(rFile);
		CoordinateTransformation ctBack = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		try{
			bw.write("id,x,y,long,lat");
			bw.newLine();
			for(Id<Node> nodeId : nodeMap.keySet()){
				Coord cSaAlbers = nodeMap.get(nodeId);
				Coord cWgs = ctBack.transform(cSaAlbers);
				bw.write(String.format("%s,%.0f,%.0f,%.6f,%.6f\n", 
						nodeId.toString(), cSaAlbers.getX(), cSaAlbers.getY(),
						cWgs.getX(), cWgs.getY()));
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
