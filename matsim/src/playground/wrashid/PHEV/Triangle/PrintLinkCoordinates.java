package playground.wrashid.PHEV.Triangle;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;

public class PrintLinkCoordinates {

	public static void main(String[] args) throws Exception {
		Config config = Gbl.createConfig(args);

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer) Gbl.getWorld().createLayer(
				NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(config.network()
				.getInputFile());
		System.out.println("  done.");
		
		System.out.println("linkId\tx\ty");
		for (Link link:network.getLinks().values()){
			System.out.println(link.getId().toString() +"\t"+ getXCoordinate(link) +"\t"+  getYCoordinate(link));
		}
		
		
	}
	
	public static double getXCoordinate(Link link){
		return (link.getFromNode().getCoord().getX()+ link.getToNode().getCoord().getX())/2;
	}
	
	public static double getYCoordinate(Link link){
		return (link.getFromNode().getCoord().getY()+ link.getToNode().getCoord().getY())/2;
	}
	
}
