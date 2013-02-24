package playground.dhosse.bachelorarbeit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.matsim4opus.constants.InternalConstants;
import org.matsim.contrib.matsim4opus.gis.GridUtils;
import org.matsim.contrib.matsim4opus.gis.SpatialGrid;
import org.matsim.contrib.matsim4opus.gis.Zone;
import org.matsim.contrib.matsim4opus.gis.ZoneLayer;
import org.matsim.contrib.matsim4opus.matsim4urbansim.AccessibilityControlerListenerImpl.GeneralizedCostSum;
import org.matsim.contrib.matsim4opus.matsim4urbansim.costcalculators.FreeSpeedTravelTimeCostCalculator;
import org.matsim.contrib.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import org.matsim.contrib.matsim4opus.utils.helperObjects.Distances;
import org.matsim.contrib.matsim4opus.utils.misc.ProgressBar;
import org.matsim.contrib.matsim4opus.utils.network.NetworkBoundaryBox;
import org.matsim.contrib.matsim4opus.utils.network.NetworkUtil;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.LeastCostPathTree;

import com.vividsolutions.jts.geom.Point;

public class AccessibilityCalc {
	
	private static final Logger log = Logger.getLogger(AccessibilityCalc.class);
	
	private ActivityFacilitiesImpl parcels = new ActivityFacilitiesImpl();
	
	private SpatialGrid freeSpeedGrid;
	
	private ScenarioImpl scenario;
	
	private ZoneLayer<Id> measuringPoints;
	private String filename;
	
	protected AggregateObject2NearestNode[] aggregatedOpportunities;
	
	protected double walkSpeedMeterPerHour = 3000.;
	protected double logitScaleParameter = 1.;
	protected double inverseOfLogitScaleParameter = 1/this.logitScaleParameter;
	
	protected double VijFreeTT;
	protected double betaCarTT = -12.;
	protected double betaWalkTT = -12.;

	public AccessibilityCalc(ZoneLayer<Id> startZones, SpatialGrid freeSpeedGrid, ScenarioImpl scenario,String filename) {
		
		this.freeSpeedGrid = freeSpeedGrid;
		this.scenario = scenario;
		this.measuringPoints = startZones;
		this.filename = filename;
		
	}
	
	public void runAccessibilityComputation(){
		
		final NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
		
		if(this.scenario.getPopulation().getPersons().size()<1){
			log.warn("no population initialized. running on random population...");
			
			NetworkBoundaryBox bbox = new NetworkBoundaryBox();
			bbox.setDefaultBoundaryBox(network);
			
			
			Random rnd = new Random();
			
			for(int i=0;i<300000;i++){
				Coord coord = new CoordImpl(bbox.getXMin() + rnd.nextDouble() * (bbox.getXMax() - bbox.getXMin()),
						bbox.getYMin() + rnd.nextDouble() * (bbox.getYMax() - bbox.getYMin()));
				Id id = new IdImpl("parcel"+i);
				parcels.createAndAddFacility(id, coord);
			}
			
			
		} else{
			int i=0;
			for(Person p : scenario.getPopulation().getPersons().values()){
				PlanElement pe = p.getSelectedPlan().getPlanElements().get(0);
				PlanElement pe2 = p.getSelectedPlan().getPlanElements().get(2);
				Coord coord = new CoordImpl(0, 0);
				if(pe instanceof Activity)
					coord = ((Activity)pe).getCoord();
				Id id = new IdImpl("parcel"+i);
				i++;
				parcels.createAndAddFacility(id, coord);
				if(pe2 instanceof Activity)
					coord = ((Activity)pe).getCoord();
				id = new IdImpl("parcel"+i);
				i++;
				parcels.createAndAddFacility(id, coord);
			}
		}
		
		this.aggregatedOpportunities = aggregatedOpportunities(this.parcels, network);
		
		TravelTimeCalculator ttCalc = new TravelTimeCalculator(network, new TravelTimeCalculatorConfigGroup());
		TravelTime ttc = ttCalc.getLinkTravelTimes();
		
		LeastCostPathTree lcptFreeSpeedCarTravelTime = new LeastCostPathTree( ttc, new FreeSpeedTravelTimeCostCalculator() );
		
		log.info("Computing and writing cell based accessibility measures ...");
		
		Iterator<Zone<Id>> measuringPointIterator = this.measuringPoints.getZones().iterator();
		log.info(this.parcels.getFacilities().values().size() + " measurement points are now processing ...");
		
		accessibilityComputation(ttc, 
								 lcptFreeSpeedCarTravelTime,
								 network,
								 measuringPointIterator, 
								 this.measuringPoints.getZones().size());
		
		System.out.println();
		
//		AnalysisCellBasedAccessibilityCSVWriterV2.close(); 
		writePlottingData();						// plotting data for visual analysis via R
		writeInterpolatedParcelAccessibilities();	// UrbanSim input file with interpolated accessibilities on parcel level
		
	}
	
	private void accessibilityComputation(TravelTime ttc,
			LeastCostPathTree lcptFreeSpeedCarTravelTime, NetworkImpl network,
			Iterator<Zone<Id>> measuringPointIterator, int size) {
		
		GeneralizedCostSum gcs = new GeneralizedCostSum();
		
		Map<Id,ArrayList<Zone<Id>>> aggregatedMeasurementPoints = new HashMap<Id, ArrayList<Zone<Id>>>();
		
		while(measuringPointIterator.hasNext()){
			
			Zone<Id> measurePoint = measuringPointIterator.next();
			
			Point p = measurePoint.getGeometry().getCentroid();
			
			Coord coordFromZone = new CoordImpl(p.getX(),p.getY());
			
			Link nearestLink = network.getNearestLinkExactly(coordFromZone);
			
			Node fromNode = NetworkUtil.getNearestNode(coordFromZone, nearestLink);
			
			Id id = fromNode.getId();
			
			if(!aggregatedMeasurementPoints.containsKey(id))
				aggregatedMeasurementPoints.put(id, new ArrayList<Zone<Id>>());
			
			aggregatedMeasurementPoints.get(id).add(measurePoint);
			
		}
			
		log.info("");
		log.info("Number of measure points: " + size);
		log.info("Number of aggregated measure points: " + aggregatedMeasurementPoints.size());
		log.info("");
		
		ProgressBar bar = new ProgressBar(aggregatedMeasurementPoints.size());
			
		Iterator<Id> keyIterator = aggregatedMeasurementPoints.keySet().iterator();
			
		Map<Id, Node> networkNodesMap = network.getNodes();
			
		while(keyIterator.hasNext()){
				
			bar.update();
			
			Id nodeId = keyIterator.next();
				
			Node fromNode = networkNodesMap.get(nodeId);
				
			lcptFreeSpeedCarTravelTime.calculate(network, fromNode, 8.*3600);//value of dep time from accessibilityControlerListenerImpl
				
			ArrayList<Zone<Id>> origins = aggregatedMeasurementPoints.get(nodeId);
				
			Iterator<Zone<Id>> originsIterator = origins.iterator();
				
			while(originsIterator.hasNext()){
					
				Zone<Id> measurePoint = originsIterator.next();
					
				Point p = measurePoint.getGeometry().getCentroid();

				Coord coordFromZone = new CoordImpl(p.getX(),p.getY());
					
				assert(coordFromZone!=null);
					
				Link nearestLink = network.getNearestLinkExactly(coordFromZone);

				Distances distance = NetworkUtil.getDistance2Node(nearestLink, coordFromZone, fromNode);
					
				double distanceMeasuringPoint2Road_meter 	= distance.getDisatancePoint2Road(); // distance measuring point 2 road (link or node)
				double distanceRoad2Node_meter 				= distance.getDistanceRoad2Node();	 // distance intersection 2 node (only for orthogonal distance)
				
				double walkTravelTimePoint2Road_h 			= distanceMeasuringPoint2Road_meter / this.walkSpeedMeterPerHour;
					
				double freeSpeedTravelTimeOnNearestLink_meterpersec= nearestLink.getFreespeed();
					
				double road2NodeFreeSpeedTime_h				= distanceRoad2Node_meter / (freeSpeedTravelTimeOnNearestLink_meterpersec * 3600);
					
				gcs.reset();
					
				for(int i=0;i<this.aggregatedOpportunities.length;i++){
						
					Node destinationNode = this.aggregatedOpportunities[i].getNearestNode();
					Id nodeID = destinationNode.getId();
						
					double freeSpeedTravelTime_h= (lcptFreeSpeedCarTravelTime.getTree().get( nodeID ).getTime() / 3600.) + road2NodeFreeSpeedTime_h;
						
					sumDisutilityOfTravel(gcs, 
							this.aggregatedOpportunities[i],
							distanceMeasuringPoint2Road_meter,
							distanceRoad2Node_meter, 
							walkTravelTimePoint2Road_h,
							freeSpeedTravelTime_h);
					
				}
				
				double freeSpeedAccessibility = this.inverseOfLogitScaleParameter *Math.log(gcs.getFreeSpeedSum());
				
				this.freeSpeedGrid.setValue(freeSpeedAccessibility, p);
					
			}
				
		}
	}
	
	private void sumDisutilityOfTravel(GeneralizedCostSum gcs,
			AggregateObject2NearestNode aggregateObject2NearestNode,
			double distanceMeasuringPoint2Road_meter,
			double distanceRoad2Node_meter, double walkTravelTimePoint2Road_h,
			double freeSpeedTravelTime_h) {
		
		VijFreeTT 	= getAsUtil(betaCarTT, freeSpeedTravelTime_h, betaWalkTT, walkTravelTimePoint2Road_h);
		
		double expFreeSpeedVij = Math.exp(logitScaleParameter * VijFreeTT);
		
		gcs.addFreeSpeedCost( expFreeSpeedVij * aggregateObject2NearestNode.getSumVjk());
		
	}

	private double getAsUtil(final double betaModeX, final double ModeTravelCostX, final double betaWalkX, final double walkOrigin2NetworkX) {
		if(betaModeX != 0.)
			return (betaModeX * ModeTravelCostX + betaWalkX * walkOrigin2NetworkX);
		return 0.;
	}

	private void writeInterpolatedParcelAccessibilities() {
		
	}

	private void writePlottingData() {
		GridUtils.writeSpatialGridTable(freeSpeedGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// freespeed results for plotting in R
				+ "freespeedAccessibility_cellsize_" + freeSpeedGrid.getResolution() + filename
				+ InternalConstants.FILE_TYPE_TXT);
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

	public ZoneLayer<Id> getMeasuringPoints() {
		return measuringPoints;
	}

	public void setMeasuringPoints(ZoneLayer<Id> measuringPoints) {
		this.measuringPoints = measuringPoints;
	}
	
}