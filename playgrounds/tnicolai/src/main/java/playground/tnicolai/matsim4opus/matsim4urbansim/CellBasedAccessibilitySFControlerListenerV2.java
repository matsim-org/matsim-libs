package playground.tnicolai.matsim4opus.matsim4urbansim;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.gis.Zone;
import playground.tnicolai.matsim4opus.gis.ZoneLayer;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelDistanceCalculator;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelTimeCostCalculator;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelWalkTimeCostCalculator;
import playground.tnicolai.matsim4opus.utils.ProgressBar;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.CounterObject;
import playground.tnicolai.matsim4opus.utils.io.writer.AnalysisCellBasedAccessibilityCSVWriter;

import com.vividsolutions.jts.geom.Point;

public class CellBasedAccessibilitySFControlerListenerV2 extends CellBasedAccessibilityControlerListenerV2{
	
	private static final Logger log = Logger.getLogger(CellBasedAccessibilitySFControlerListenerV2.class);
	
	public CellBasedAccessibilitySFControlerListenerV2(ZoneLayer<CounterObject> startZones, 					// needed for google earth plots (not supported by now tnicolai feb'12)
													 AggregateObject2NearestNode[] aggregatedOpportunities, 	// destinations (like workplaces)
													 SpatialGrid<Double> carGrid, 								// table for congested car travel times in accessibility computation
													 SpatialGrid<Double> walkGrid, 								// table for walk travel times in accessibility computation
													 Benchmark benchmark, 										// Benchmark tool
													 ScenarioImpl scenario){
		super(startZones, 
				aggregatedOpportunities,
				carGrid,
				walkGrid, 
				benchmark,
				scenario);
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event){
		
		log.info("Entering notifyShutdown ..." );
		
		int benchmarkID = this.benchmark.addMeasure("1-Point accessibility computation");
		
		// get the controller and scenario
		Controler controler = event.getControler();
		Scenario sc = controler.getScenario();
		
		TravelTime ttc = controler.getTravelTimeCalculator();
		// get the congested car travel time (in seconds)
		LeastCostPathTree lcptCongestedCarTravelTime = new LeastCostPathTree( ttc, new TravelTimeCostCalculator(ttc) );
		// get the walk speed tavel time (in seconds) (tnicolai: changed from distance calculator to walk time feb'12)
		LeastCostPathTree lcptWalkTravelTime 		 = new LeastCostPathTree( ttc, new TravelWalkTimeCostCalculator( sc.getConfig().plansCalcRoute().getWalkSpeed() ) );
		// get travel distance (in meter)
		LeastCostPathTree lcptTravelDistance		 = new LeastCostPathTree( ttc, new TravelDistanceCalculator());
		
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		
		double logitScaleParameterPreFactor = 1/(logitScaleParameter);

		try{
			AnalysisCellBasedAccessibilityCSVWriter accCsvWriter = new AnalysisCellBasedAccessibilityCSVWriter("");
			
			printSettings();
			
			Iterator<Zone<CounterObject>> startZoneIterator = measuringPoints.getZones().iterator();
			log.info(measuringPoints.getZones().size() + " measurement points are now processing ...");
			
			ProgressBar bar = new ProgressBar( measuringPoints.getZones().size() );
		
			// iterates through all starting points (fromZone) and calculates their workplace accessibility
			while( startZoneIterator.hasNext() ){
				
				bar.update();
				
				Zone<CounterObject> startZone = startZoneIterator.next();
				
				Point point = startZone.getGeometry().getCentroid();
				// get coordinate from origin (start point)
				Coord coordFromZone = new CoordImpl( point.getX(), point.getY());
				assert( coordFromZone!=null );
				// determine nearest network node
				Node fromNode = network.getNearestNode(coordFromZone);
				assert( fromNode != null );
				// run dijkstra on network
				lcptCongestedCarTravelTime.calculate(network, fromNode, depatureTime);		
				lcptWalkTravelTime.calculate(network, fromNode, depatureTime);
				lcptTravelDistance.calculate(network, fromNode, depatureTime);
				
				// from here: accessibility computation for current starting point ("fromNode")
				
				// captures the euclidean distance between a square centroid and its nearest node
				LinkImpl nearestLink = network.getNearestLink( coordFromZone );
				double distCentroid2Link = nearestLink.calcDistance(coordFromZone);
				double walkOffset = betaWalkTT * distanceCostRateWalk * (distCentroid2Link / this.walkSpeedMeterPerMin); 
//				double walkOffset = EuclideanDistance.getEuclideanDistanceAsWalkTimeInSeconds(coordFromZone, fromNode.getCoord()) / 60.;
				double sumCAR = 0.;
				double sumWALK= 0.;	

				// go through all jobs (nearest network node) and calculate workplace accessibility
				for ( int i = 0; i < this.aggregatedOpportunities.length; i++ ) {
					
					// get stored network node (this is the nearest node next to an aggregated workplace)
					Node destinationNode = this.aggregatedOpportunities[i].getNearestNode();
					Id nodeID = destinationNode.getId();

					// using number of aggregated workplaces as weight for log sum measure
					int opportunityWeight = this.aggregatedOpportunities[i].getNumberOfObjects();

					// congested car travel times in hours
					double arrivalTime = lcptCongestedCarTravelTime.getTree().get( nodeID ).getTime();
					double congestedTravelTime_per_h = (arrivalTime - depatureTime) / 3600.;
					// walk travel times in hours
					double walkTravelTime_per_h = lcptWalkTravelTime.getTree().get( nodeID ).getCost() / 60.;
					// travel distance in meter
					double travelDistance_meter = lcptTravelDistance.getTree().get( nodeID ).getCost();
					
					// sum congested travel times
					sumCAR += opportunityWeight				// tnicolai: betas and values do not match (check measures)
							* Math.exp(logitScaleParameterPreFactor *
									  (betaCarLnTT * congestedTravelTime_per_h +
									   betaCarTTPower * Math.pow(congestedTravelTime_per_h, 2) +
									   betaCarLnTT * Math.log(congestedTravelTime_per_h) +
									   betaCarTD * travelDistance_meter + 
									   betaCarTDPower * Math.pow(travelDistance_meter, 2) + 
									   betaCarLnTD * Math.log(travelDistance_meter) +
									   betaCarTC * distanceCostRateCar * travelDistance_meter + 
									   betaCarTCPower * Math.pow(distanceCostRateCar * travelDistance_meter, 2) + 
									   betaCarLnTC * Math.log( distanceCostRateCar * travelDistance_meter) + 
									   walkOffset));

					// sum walk travel times (substitute for distances)
					sumWALK += opportunityWeight
							* Math.exp(logitScaleParameterPreFactor *
									(betaWalkTT * walkTravelTime_per_h +
									 betaWalkTTPower * Math.pow( walkTravelTime_per_h, 2) +
									 betaWalkLnTT * Math.log( walkTravelTime_per_h ) +
									 betaWalkTD * travelDistance_meter +
									 betaWalkTDPower * Math.pow( travelDistance_meter, 2) +
									 betaWalkLnTD * Math.log( travelDistance_meter ) + 
									 betaWalkTC * distanceCostRateWalk * travelDistance_meter +
									 betaWalkTCPower * Math.pow( distanceCostRateWalk * travelDistance_meter, 2) +
									 betaWalkLnTC * Math.log( distanceCostRateWalk * travelDistance_meter) + 
									 walkOffset));
				}
				
				// get log sum
				double carAccessibility, walkAccessibility;
				if(!useRawSum){
					carAccessibility = logitScaleParameterPreFactor * Math.log( sumCAR );
					walkAccessibility= logitScaleParameterPreFactor * Math.log( sumWALK );
				}
				else{
					carAccessibility = logitScaleParameterPreFactor * sumCAR;
					walkAccessibility= logitScaleParameterPreFactor * sumWALK;
				}
				
				// assign log sums to current starZone object and spatial grid
				setAccessibilityValues2StartZoneAndSpatialGrid(startZone,
															   carAccessibility, 
															   walkAccessibility);
				
				// writing accessibility values (stored in starZone object) in csv format ...
				accCsvWriter.write(startZone, 
								   coordFromZone, 
								   fromNode, 
								   carAccessibility, 
								   -1, 
								   walkAccessibility);
			}
			System.out.println("");
			
			if( this.benchmark != null && benchmarkID > 0 ){
				this.benchmark.stoppMeasurement(benchmarkID);
				log.info("Accessibility computation with " + measuringPoints.getZones().size() + " starting points (origins) and " + this.aggregatedOpportunities.length + " destinations (workplaces) took " + this.benchmark.getDurationInSeconds(benchmarkID) + " seconds (" + this.benchmark.getDurationInSeconds(benchmarkID) / 60. + " minutes).");
			}
			accCsvWriter.close();
			dumpResults();
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
