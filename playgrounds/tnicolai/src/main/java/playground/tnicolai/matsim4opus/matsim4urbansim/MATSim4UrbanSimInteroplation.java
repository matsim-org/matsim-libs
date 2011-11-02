package playground.tnicolai.matsim4opus.matsim4urbansim;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.matsim4opus.gis.SpatialGrid;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.utils.UtilityCollection;
import playground.tnicolai.matsim4opus.utils.helperObjects.JobClusterObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.NetworkBoundary;
import playground.tnicolai.matsim4opus.utils.helperObjects.SquareLayer;
import playground.tnicolai.matsim4opus.utils.io.ReadFromUrbansimParcelModel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class MATSim4UrbanSimInteroplation extends MATSim4UrbanSim{

	// logger
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimInteroplation.class);
	
	// resolution
	private static int resolution = -1;
	// job sample
	private static double jobSample = 1.;

	/**
	 * constructor
	 * @param args
	 */
	public MATSim4UrbanSimInteroplation(String args[]){
		super(args);
		initResolutionAndJobSample();
	}
	
	private void initResolutionAndJobSample(){
		
		try{
			String params[] = scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.TEST_PARAMETER_PARAM).split(",");
			
			for(int i = 0; i < params.length; i++){
				
				if(params[i].startsWith("resolution")){
					String s[] = params[i].split("=");
					this.resolution = Integer.parseInt(s[1]);
				}
				else if(params[i].startsWith("jobsample")){
					String s[] = params[i].split("=");
					this.jobSample = Double.parseDouble(s[1]);
				}	
			}
		}
		catch(Exception e){ System.exit(-1); }
	}

	void runMATSim(){
		
		log.info("Starting MATSim from Urbansim");	
		// get the network. Always cleaning it seems a good idea since someone may have modified the input files manually in
		// order to implement policy measures.  Get network early so readXXX can check if links still exist.
		NetworkImpl network = scenario.getNetwork();
		modifyNetwork(network);
		cleanNetwork(network);
		
		ReadFromUrbansimParcelModel readUrbanSimData = new ReadFromUrbansimParcelModel( Integer.parseInt( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.YEAR) ) );
		// read urbansim facilities (these are simply those entities that have the coordinates!)
		ActivityFacilitiesImpl parcels = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
		ActivityFacilitiesImpl zones   = new ActivityFacilitiesImpl("urbansim zones");
		readUrbanSimData.readFacilities(parcels, zones);
		
		// set population in scenario
		scenario.setPopulation( readUrbansimPersons(readUrbanSimData, parcels, network) );
		
		SpatialGrid<SquareLayer> grid = initGrid(network);
		JobClusterObject[] jobClusterArray = readUrbanSimData.readAndBuildJobsObject(parcels, jobSample);
		
//		runControler(zones, numberOfWorkplacesPerZone, parcels, readUrbanSimData);
		
		if( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BACKUP_RUN_DATA_PARAM).equalsIgnoreCase("TRUE") ){ // tnicolai: Experimental, comment out for MATSim4UrbanSim release
			// saving results from current run
			saveRunOutputs();			
			cleanUrbanSimOutput();
		}
	}
	
	/**
	 * 
	 * @param network
	 * @return
	 */
	private SpatialGrid<SquareLayer> initGrid(final NetworkImpl network){
		
		NetworkBoundary nb = UtilityCollection.getNetworkBoundary(network);
		SpatialGrid<SquareLayer> grid = new SpatialGrid<SquareLayer>(nb.getMinX(), nb.getMinY(), nb.getMaxX(), nb.getMaxY(), resolution);
		
		GeometryFactory factory = new GeometryFactory();
		Iterator<Node> nodeIterator = network.getNodes().values().iterator();
		
		// assign nodes to right square
		for(;nodeIterator.hasNext();){
			Node node = nodeIterator.next();
			Coord coord = node.getCoord();
			
			if(grid.getValue(factory.createPoint( new Coordinate(coord.getX(), coord.getY()))) == null)
				grid.setValue(new SquareLayer(), factory.createPoint( new Coordinate(coord.getX(), coord.getY())) );
			
			SquareLayer io = grid.getValue(factory.createPoint( new Coordinate(coord.getX(), coord.getY())));
			io.addNode( node );			
		}
		// determine centroid and nearest node
		for(double x = grid.getXmin(); x < grid.getXmax(); x += grid.getResolution()){
			for(double y = grid.getYmin(); y < grid.getYmax(); y += grid.getResolution()){
				
				Coord centroid = new CoordImpl(x + (grid.getResolution()/2) -1, y + (grid.getResolution()/2) - 1);
				Node nearestNode = network.getNearestNode( centroid );
				
				if(grid.getValue(factory.createPoint( new Coordinate(centroid.getX(), centroid.getY()))) == null)
					grid.setValue(new SquareLayer(), factory.createPoint(new Coordinate(centroid.getX(), centroid.getY())) );
				
				SquareLayer io = grid.getValue(factory.createPoint(new Coordinate(centroid.getX(), centroid.getY())));
				io.setSquareCentroid(centroid, nearestNode);
			}
		}		
		return grid;
	}

	
	/**
	 * Entry point
	 * @param args
	 */
	public static void main(String[] args) {		
		MATSim4UrbanSimInteroplation m4ui = new MATSim4UrbanSimInteroplation(new String[]{"/Users/thomas/Development/opus_home/matsim4opus/matsim_config/seattle_parcel_interpolation_config.xml"});
		m4ui.runMATSim();		
	}
	
}
