package playground.dhosse.bachelorarbeit;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.matsim4opus.gis.SpatialGrid;
import org.matsim.contrib.matsim4opus.matsim4urbansim.costcalculators.FreeSpeedTravelTimeCostCalculator;
import org.matsim.contrib.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import org.matsim.contrib.matsim4opus.utils.io.writer.AnalysisCellBasedAccessibilityCSVWriterV2;
import org.matsim.contrib.matsim4opus.utils.misc.ProgressBar;
import org.matsim.contrib.matsim4opus.utils.network.NetworkUtil;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.utils.LeastCostPathTree;

public class AccessibilityCalc {//neue klasse für accessibilityComputation
	
	private static final Logger log = Logger.getLogger(MyParcelBasedAccessibilityControlerListener.class);
	
	private ActivityFacilitiesImpl parcels;
	
	private SpatialGrid freeSpeedGrid;
	
	private ScenarioImpl scenario;
	
	protected AggregateObject2NearestNode[] aggregatedOpportunities;
	
	protected double walkSpeedMeterPerHour;
	protected double logitScaleParameter = 1.;

	public AccessibilityCalc(ActivityFacilitiesImpl parcels, SpatialGrid freeSpeedGrid, ScenarioImpl scenario) {
		
		this.setParcels(parcels);
		this.setFreeSpeedGrid(freeSpeedGrid);
		this.setScenario(scenario);
		
	}
	
	public void runAccessibilityComputation(){
		
		final NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
		
		this.aggregatedOpportunities = aggregatedOpportunities(this.parcels, network);
		
		TravelTimeCalculator ttCalc = new TravelTimeCalculator(network, new TravelTimeCalculatorConfigGroup());
		TravelTime ttc = ttCalc.getLinkTravelTimes();
			
		LeastCostPathTree lcptFreeSpeedCarTravelTime = new LeastCostPathTree( ttc, new FreeSpeedTravelTimeCostCalculator() );
		
		log.info("Computing and writing cell based accessibility measures ...");
		
		Iterator<ActivityFacility> measuringPointIterator = this.parcels.getFacilities().values().iterator();
		log.info(this.parcels.getFacilities().values().size() + " measurement points are now processing ...");
		
		accessibilityComputation(ttc, 
								 lcptFreeSpeedCarTravelTime,
								 network,
								 measuringPointIterator, 
								 this.parcels.getFacilities().values().size());
		
		System.out.println();

//			if (this.benchmark != null && benchmarkID > 0) {
//				this.benchmark.stoppMeasurement(benchmarkID);
//				log.info("Accessibility computation with "
//						+ measuringPointsCell.getZones().size()
//						+ " starting points (origins) and "
//						+ this.aggregatedOpportunities.length
//						+ " destinations (opportunities) took "
//						+ this.benchmark.getDurationInSeconds(benchmarkID)
//						+ " seconds ("
//						+ this.benchmark.getDurationInSeconds(benchmarkID)
//						/ 60. + " minutes).");
//			}
		// tnicolai: for debugging (remove for release)
		//log.info("Euclidian vs Othogonal Distance:");
		//log.info("Total Counter:" + NetworkUtil.totalCounter);
		//log.info("Euclidian Counter:" + NetworkUtil.euclidianCounter);
		//log.info("Othogonal Counter:" + NetworkUtil.othogonalCounter);
		
		AnalysisCellBasedAccessibilityCSVWriterV2.close(); 
		writePlottingData();						// plotting data for visual analysis via R
		writeInterpolatedParcelAccessibilities();	// UrbanSim input file with interpolated accessibilities on parcel level
		
	}
	
	private void accessibilityComputation(TravelTime ttc,
			LeastCostPathTree lcptFreeSpeedCarTravelTime, NetworkImpl network,
			Iterator<ActivityFacility> measuringPointIterator, int size) {
		//TODO
	}
	
	private void writeInterpolatedParcelAccessibilities() {
		
	}

	private void writePlottingData() {
		
	}

	protected AggregateObject2NearestNode[] aggregatedOpportunities(final ActivityFacilitiesImpl parcels, NetworkImpl network){
		
		log.info("Aggregating workplaces with identical nearest node ...");
		Map<Id, AggregateObject2NearestNode> opportunityClusterMap = new HashMap<Id, AggregateObject2NearestNode>();
		
		ProgressBar bar = new ProgressBar( parcels.getFacilities().size() );
		
		for(ActivityFacility facility : parcels.getFacilities().values()){
			
			bar.update();
			
			Node nearestNode = network.getNearestNode( facility.getCoord() );
			assert( nearestNode != null );
			
			// get euclidian distance to nearest node
			double distance_meter 	= NetworkUtil.getEuclidianDistance(facility.getCoord(), nearestNode.getCoord());
			double walkTravelTime_h = distance_meter / this.walkSpeedMeterPerHour;
			
			double Vjk					= Math.exp(this.logitScaleParameter * walkTravelTime_h );
			// add Vjk to sum
			if( opportunityClusterMap.containsKey( nearestNode.getId() ) ){
				AggregateObject2NearestNode jco = opportunityClusterMap.get( nearestNode.getId() );
				jco.addObject( facility.getId(), Vjk);
			}
			
			// assign Vjk to given network node
			else
				opportunityClusterMap.put(
						nearestNode.getId(),
						new AggregateObject2NearestNode(facility.getId(), 
														facility.getId(), 
														facility.getId(), 
														nearestNode.getCoord(), 
														nearestNode, 
														Vjk));
			
		}
		
		// convert map to array
		AggregateObject2NearestNode jobClusterArray []  = new AggregateObject2NearestNode[ opportunityClusterMap.size() ];
		Iterator<AggregateObject2NearestNode> jobClusterIterator = opportunityClusterMap.values().iterator();

		for(int i = 0; jobClusterIterator.hasNext(); i++)
			jobClusterArray[i] = jobClusterIterator.next();
		
		log.info("Aggregated " + parcels.getFacilities().size() + " number of workplaces (sampling rate: " + parcels.getFacilities().size() + ") to " + jobClusterArray.length + " nodes.");
		
		return jobClusterArray;
		
	}
	
	public ActivityFacilitiesImpl getParcels() {
		return parcels;
	}

	public void setParcels(ActivityFacilitiesImpl parcels) {
		this.parcels = parcels;
	}

	public SpatialGrid getFreeSpeedGrid() {
		return freeSpeedGrid;
	}

	public void setFreeSpeedGrid(SpatialGrid freeSpeedGrid) {
		this.freeSpeedGrid = freeSpeedGrid;
	}

	public ScenarioImpl getScenario() {
		return scenario;
	}

	public void setScenario(ScenarioImpl scenario) {
		this.scenario = scenario;
	}
	
}
