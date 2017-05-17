package playground.fzwick;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;


/**
 * "P" has to do with "Potsdam" and "Z" with "Zurich", but P and Z are mostly used to show which classes belong together.
 */
public class OSM2MATSimNetwork {
	
	public static void main(String[] args) {
		
		/*
		 * The input file name.
		 */
		String osm = "C:/Users/Felix/Documents/VSP/Berlin-Netz/merged-filtered.osm";
		
		
		/*
		 * The coordinate system to use. OpenStreetMap uses WGS84, but for MATSim, we need a projection where distances
		 * are (roughly) euclidean distances in meters.
		 * 
		 * UTM 33N is one such possibility (for parts of Europe, at least).
		 * 
		 */
		CoordinateTransformation ct = 
			 TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);
		
		/*
		 * First, create a new Config and a new Scenario. One always has to do this when working with the MATSim 
		 * data containers.
		 * 
		 */
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		/*
		 * Pick the Network from the Scenario for convenience.
		 */
		Network network = scenario.getNetwork();
		
		OsmNetworkReader onr = new OsmNetworkReader(network,ct);
		onr.parse(osm);
		
		/*
		 * Clean the Network. Cleaning means removing disconnected components, so that afterwards there is a route from every link
		 * to every other link. This may not be the case in the initial network converted from OpenStreetMap.
		 */
		new NetworkCleaner().run(network);
		reduceSpeedSantiago(network);
		/*
		 * Write the Network to a MATSim network file.
		 */
		new NetworkWriter(network).write("C:/Users/Felix/Documents/VSP/Berlin-Netz/mergedFilteredReducedSpeedSantiago.xml");
		
	}
	
	private static void reduceSpeedKN(Network network){
		
		for ( Link link : network.getLinks().values() ) {
			if ( link.getFreespeed() < 77/3.6 ) {
				if ( link.getCapacity() >= 1001. ) { // cap >= 1000 is nearly everything 
					link.setFreespeed( 1. * link.getFreespeed() );
				}else {
					link.setFreespeed( 0.5 * link.getFreespeed() );
				}
			}
			if ( link.getLength()<100 ) {
				link.setCapacity( 2. * link.getCapacity() ); // double capacity on short links, often roundabouts or short u-turns, etc., with the usual problem
			}
		}	
		
	}
	
	
	private static void reduceSpeedSantiago(Network network){
		for(Link ll : network.getLinks().values()){
			double fs = ll.getFreespeed();
			if(fs <= 8.333333334){ //30kmh
				((Link) ll).setFreespeed(0.5 * ll.getFreespeed());
			} else if(fs <= 11.111111112){ //40kmh
				((Link) ll).setFreespeed(0.5 * ll.getFreespeed());
			} else if(fs <= 13.888888889){ //50kmh
				double lanes = ll.getNumberOfLanes();
				if(lanes <= 1.0){
					((Link) ll).setFreespeed(0.5 * ll.getFreespeed());
				} else if(lanes <= 2.0){
					((Link) ll).setFreespeed(0.75 * ll.getFreespeed());
				} else if(lanes > 2.0){
					// link assumed to not have second-row parking, traffic lights, bikers/pedestrians crossing etc.
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(fs <= 16.666666667){ //60kmh
				double lanes = ll.getNumberOfLanes();
				if(lanes <= 1.0){
					((Link) ll).setFreespeed(0.5 * ll.getFreespeed());
				} else if(lanes <= 2.0){
					((Link) ll).setFreespeed(0.75 * ll.getFreespeed());
				} else if(lanes > 2.0){
					// link assumed to not have second-row parking, traffic lights, bikers/pedestrians crossing etc.
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(fs > 16.666666667){
				// link assumed to not have second-row parking, traffic lights, bikers/pedestrians crossing etc.
			} else{
				throw new RuntimeException("Link not considered...");
			}
			if ( ll.getLength()<100 ) {
				ll.setCapacity( 2. * ll.getCapacity() ); // double capacity on short links, often roundabouts or short u-turns, etc., with the usual problem
			}
		}
	}
}