package playground.dhosse.scenarios.generic.network;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.dhosse.utils.GeometryUtils;
import playground.dhosse.utils.osm.reader.CustomizedOsmNetworkReader;

public class NetworkCreator {

	/**
	 * 
	 * @param args
	 * <ol>
	 * <li>target coordinate system
	 * <li>input osm file
	 * <li>output matsim network (xml) file
	 * <li>output shapefile (optional)
	 * </ol>
	 */
	public static void main(String args[]){
		
		if(args.length < 3){
			throw new RuntimeException("Too few arguments! Shutting down!");
		}
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		if(args.length == 3){
			NetworkCreator.generateNetwork(scenario, TransformationFactory.WGS84, args[0], args[1], true);
		}
		
		if(args.length == 4){
			NetworkCreator.generateNetwork(scenario, TransformationFactory.WGS84, args[0], args[1], true, args[3]);
		}
		
		new NetworkWriter(scenario.getNetwork()).write(args[2]);
		
	}
	
	/**
	 * Creates a network from a given osm file.
	 * 
	 * @param scenario The scenario for which a network is to be created.
	 * @param fromCrs origin Coordinate Reference System (CRS)
	 * @param toCrs destination CRS
	 * @param osmFile input OpenStreetMap file
	 * @param cleanNetwork Defines whether the resulting MATSim network should be cleaned up or not.
	 */	
	public static void generateNetwork(final Scenario scenario, String fromCrs, String toCrs,
			String osmFile, boolean cleanNetwork){
		
		generateNetwork(scenario, fromCrs, toCrs, osmFile, cleanNetwork, null);
		
	}
	
	/**
	 * Creates a network from a given osm file. In addition, a shapefile of the network is created.
	 * 
	 * @param scenario The scenario for which a network is to be created.
	 * @param fromCrs origin Coordinate Reference System (CRS)
	 * @param toCrs destination CRS
	 * @param osmFile input OpenStreetMap file
	 * @param cleanNetwork Defines whether the resulting MATSim network should be cleaned up or not.
	 * @param outputShapefile
	 */	
	public static void generateNetwork(final Scenario scenario, String fromCrs, String toCrs,
			String osmFile, boolean cleanNetwork,String outputShapefile){
		
		Network network = scenario.getNetwork();
		scenario.getConfig().transit().setUseTransit(true);
		TransitSchedule schedule = scenario.getTransitSchedule();
		
		CoordinateTransformation transformation = 
				TransformationFactory.getCoordinateTransformation(fromCrs, toCrs);

		CustomizedOsmNetworkReader onr = new CustomizedOsmNetworkReader(network, schedule, transformation);
		onr.parse(osmFile);
		
		if(cleanNetwork){
		
			new NetworkCleaner().run(network);
		
		}
		
		if(outputShapefile != null){
			
			GeometryUtils.writeNetwork2Shapefile(network, outputShapefile, toCrs);
			
		}
		
	}
	
}
