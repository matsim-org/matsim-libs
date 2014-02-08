package playground.tnicolai.matsim4opus.utils.network;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

public class NetworkBoundaryBox {
	
	// Logger
	private final Logger log = Logger.getLogger(NetworkBoundaryBox.class);
	private double boundingBox [] = null;
	
	public void setCustomBoundaryBox(double xmin, double ymin, double xmax, double ymax){
		
		log.info("Setting custom bounding box ...");
		
		boundingBox = new double[4];
		boundingBox[0] = xmin;
		boundingBox[1] = ymin;
		boundingBox[2] = xmax;
		boundingBox[3] = ymax;
		
		log.info("...done!");
	}
	
	public void setDefaultBoundaryBox(Network network){
		
		if(boundingBox != null)
			log.warn("Bounding box is already initialized and will not be overwritten!");
		else{
			log.warn("Setting bounding box from network! For large networks this may lead to memory issues depending on available memory and/or grid resolution. In this case define a custom bounding box.");
			// The bounding box of all the given nodes as double[] = {minX, minY, maxX, maxY}
			boundingBox = NetworkUtils.getBoundingBox(network.getNodes().values());
			log.info("... done!");
		}
	}
	
	public double[] getBoundingBox(){
		return this.boundingBox;
	}
}
