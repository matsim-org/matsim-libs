package playground.tnicolai.matsim4opus.matsim4urbansim;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.gis.GridUtils;
import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.gis.Zone;
import playground.tnicolai.matsim4opus.gis.ZoneLayer;
import playground.tnicolai.matsim4opus.interpolation.Interpolation;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.FreeSpeedTravelTimeCostCalculator;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelDistanceCalculator;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelTimeCostCalculator;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.CounterObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.Distances;
import playground.tnicolai.matsim4opus.utils.io.writer.AnalysisCellBasedAccessibilityCSVWriterV2;
import playground.tnicolai.matsim4opus.utils.io.writer.UrbanSimParcelCSVWriter;
import playground.tnicolai.matsim4opus.utils.misc.ProgressBar;
import playground.tnicolai.matsim4opus.utils.network.NetworkUtil;

import com.vividsolutions.jts.geom.Point;

/**
 * improvements sep'11:
 * 
 * Code improvements since last version (deadline ersa paper): - Aggregated
 * Workplaces: Workplaces with same parcel_id are aggregated to a weighted job
 * (see JobClusterObject) This means much less iteration cycles - Less time
 * consuming look-ups: All workplaces are assigned to their nearest node in an
 * pre-proscess step (see addNearestNodeToJobClusterArray) instead to do nearest
 * node look-ups in each iteration cycle - Distance based accessibility added:
 * like the travel time accessibility computation now also distances are
 * computed with LeastCostPathTree (tnicolai feb'12 distances are replaced by
 * walking times which is also linear and corresponds to distances)
 * 
 * improvements jan'12:
 * 
 * - Better readability: Removed unused methods such as
 * "addNearestNodeToJobClusterArray" (this is done while gathering/processing
 * workplaces). Also all results are now dumped directly from this class.
 * Before, the SpatialGrid tables were transfered to another class to dump out
 * the results. This also improves readability - Workplace data dump: Dumping
 * out the used workplace data was simplified, since the simulation now already
 * uses aggregated data. Corresponding subroutines aggregating the data are not
 * needed any more (see dumpWorkplaceData()). But coordinates of the origin
 * workplaces could not dumped out, this is now done in
 * ReadFromUrbansimParcelModel during processing the UrbnAism job data
 * 
 * improvements feb'12 - distance between square centroid and nearest node on
 * road network is considered in the accessibility computation as walk time of
 * the euclidian distance between both (centroid and nearest node). This walk
 * time is added as an offset to each measured travel times - using walk travel
 * times instead of travel distances. This is because of the betas that are
 * utils/time unit. The walk time corresponds to distances since this is also
 * linear.
 * 
 * improvements march'12 - revised distance measure from centroid to network:
 * using orthogonal distance from centroid to nearest network link! - merged
 * CellBasedAccessibilityNetworkControlerListener and
 * CellBasedAccessibilityShapefileControlerListener
 * 
 * improvements april'12 - accessibility calculation uses configurable betas
 * (coming from UrbanSim) for car/walk travel times, -distances and -costs -
 * replaced "SpatialGrid<Double>" by "SpatialGrid" using double instead of
 * Double-objects
 * 
 * improvements may'12 - including interpolated (spatial grid) feedback for each
 * parcel
 * 
 * improvements / changes june'12 
 * - the walk distance (measuring point -> nearest node) for accessibilities by 
 * car has changed: Now only the orthoganal distance (measuring point -> nearest 
 * link) is measured.
 * - re-added free-speed car travel time calculation
 * - Todo: implement a new "getNearestLink" method. The current approach uses nearest nodes to determine the link, 
 *         this leads to some artefacts in the accessibility plots.
 * 
 * @author thomas
 * 
 */
public class CellBasedAccessibilityControlerListenerV2 extends AccessibilityControlerListenerTemplate implements ShutdownListener{ // implements ShutdownListener
	
	private static final Logger log = Logger.getLogger(CellBasedAccessibilityControlerListener.class);
	
	/**
	 * constructor
	 */
	public CellBasedAccessibilityControlerListenerV2(ZoneLayer<CounterObject> startZones, 						// needed for google earth plots (not supported by now tnicolai feb'12)
													 AggregateObject2NearestNode[] aggregatedOpportunities, 	// destinations (like workplaces)
													 ActivityFacilitiesImpl parcels,							// parcel coordinates for accessibility feedback
													 SpatialGrid freeSpeedGrid,
													 SpatialGrid carGrid, 										// table for congested car travel times in accessibility computation
													 SpatialGrid walkGrid, 										// table for walk travel times in accessibility computation
													 String fileExtension,										// adds an extension to output files whether a shape-file or network boundaries are used for calculation
													 Benchmark benchmark,										// Benchmark tool
													 ScenarioImpl scenario){	
		log.info("Initializing CellBasedAccessibilityControlerListenerV2 ...");
		
		assert (startZones != null);
		this.measuringPoints = startZones;
		assert (aggregatedOpportunities != null);
		this.aggregatedOpportunities = aggregatedOpportunities;
		assert (parcels != null);
		this.parcels = parcels;
		assert (freeSpeedGrid != null);
		this.freeSpeedGrid = freeSpeedGrid;
		assert (carGrid != null);
		this.carGrid = carGrid;
		assert (walkGrid != null);
		this.walkGrid = walkGrid;
		assert (fileExtension != null);
		CellBasedAccessibilityControlerListenerV2.fileExtension = fileExtension;
		assert (benchmark != null);
		this.benchmark = benchmark;

		initAccessibilityParameter(scenario);
		log.info(".. done initializing CellBasedAccessibilityControlerListenerV2!");
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event){
		log.info("Entering notifyShutdown ..." );
		
		int benchmarkID = this.benchmark.addMeasure("1-Point accessibility computation");
		
		// get the controller and scenario
		Controler controler = event.getControler();
		
		TravelTime ttc = controler.getTravelTimeCalculator();
		// get the free-speed car travel times (in seconds)
		LeastCostPathTree lcptFreeSpeedCarTravelTime = new LeastCostPathTree( ttc, new FreeSpeedTravelTimeCostCalculator() );
		// get the congested car travel time (in seconds)
		LeastCostPathTree lcptCongestedCarTravelTime = new LeastCostPathTree( ttc, new TravelTimeCostCalculator(ttc) );
		// get travel distance (in meter)
		LeastCostPathTree lcptTravelDistance		 = new LeastCostPathTree( ttc, new TravelDistanceCalculator());
		
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		double logitScaleParameterPreFactor = 1/(logitScaleParameter);

		try{
			log.info("Computing and writing cell based accessibility measures ...");
			AnalysisCellBasedAccessibilityCSVWriterV2 accCsvWriter = new AnalysisCellBasedAccessibilityCSVWriterV2(fileExtension);
			printParameterSettings();
			
			Iterator<Zone<CounterObject>> measuringPointIterator = measuringPoints.getZones().iterator();
			log.info(measuringPoints.getZones().size() + " measurement points are now processing ...");
			
			ProgressBar bar = new ProgressBar( measuringPoints.getZones().size() );
		
			// iterates through all starting points (fromZone) and calculates their accessibility, e.g. to jobs
			while( measuringPointIterator.hasNext() ){
				
				bar.update();
				
				Zone<CounterObject> measurePoint = measuringPointIterator.next();
				
				Point point = measurePoint.getGeometry().getCentroid();
				// get coordinate from origin (start point)
				Coord coordFromZone = new CoordImpl( point.getX(), point.getY());
				assert( coordFromZone!=null );
				// determine nearest network node
				Node fromNode = network.getNearestNode(coordFromZone);
				assert( fromNode != null );
				// run dijkstra on network
				lcptFreeSpeedCarTravelTime.calculate(network, fromNode, depatureTime);
				lcptCongestedCarTravelTime.calculate(network, fromNode, depatureTime);		
				lcptTravelDistance.calculate(network, fromNode, depatureTime);
				
				// from here: accessibility computation for current starting point ("fromNode")
				
				Link nearestLink = network.getNearestRightEntryLink(coordFromZone); // tnicolai: testing new get nearest link method
				// captures the distance (as walk time) between a zone centroid and its nearest node
				
				Distances distance = NetworkUtil.getDistance2NodeV2(nearestLink, point, fromNode);
				
				double distanceMeasuringPoint2Road_meter 	= distance.getDisatancePoint2Road(); // distance measuring point 2 road (link or node)
				double distanceRoad2Node_meter 				= distance.getDistanceRoad2Node();	 // distance intersection 2 node (only for orthogonal distance)
				
				double offsetWalkTime2Node_h 				= distanceMeasuringPoint2Road_meter / this.walkSpeedMeterPerHour;
				double carTravelTime_meterpersec			= nearestLink.getLength() / ttc.getLinkTravelTime(nearestLink, depatureTime);
				double freeSpeedTravelTime_meterpersec 		= nearestLink.getLength() / nearestLink.getFreespeed();
				
				double offsetCongestedCarTime_h 			= distanceRoad2Node_meter / (carTravelTime_meterpersec * 3600.);
				double offsetFreeSpeedTime_h				= distanceRoad2Node_meter / (freeSpeedTravelTime_meterpersec * 3600);
				
				
//				double offsetDistance2NearestNode_meter = NetworkUtil.getDistance2NodeV2(nearestLink, point, fromNode); // NetworkUtil.getDistance2Node(nearestLink, point, fromNode);
//				double offsetWalkTime2NearestNode_h		= offsetDistance2NearestNode_meter / this.walkSpeedMeterPerHour;
				
				// Possible offsets to calculate the gap between measuring (start) point and start node (fromNode)
				// Euclidean Distance (measuring point 2 nearest node):
				// double walkTimeOffset_min = NetworkUtil.getEuclideanDistanceAsWalkTimeInSeconds(coordFromZone, fromNode.getCoord()) / 60.;
				// Orthogonal Distance (measuring point 2 nearest link, does not include remaining distance between link intersection and nearest node)
				// LinkImpl nearestLink = network.getNearestLink( coordFromZone );
				// double walkTimeOffset_min = (nearestLink.calcDistance( coordFromZone ) / this.walkSpeedMeterPerMin); 
				// or use NetworkUtil.getOrthogonalDistance(link, point) instead!
				
				double sumFREESPEED = 0.;
				double sumCAR = 0.;
				double sumWALK= 0.;	

				// goes through all opportunities, e.g. jobs, (nearest network node) and calculate the accessibility
				for ( int i = 0; i < this.aggregatedOpportunities.length; i++ ) {
					
					// get stored network node (this is the nearest node next to an aggregated workplace)
					Node destinationNode = this.aggregatedOpportunities[i].getNearestNode();
					Id nodeID = destinationNode.getId();

					// using number of aggregated opportunities as weight for log sum measure
					int opportunityWeight = this.aggregatedOpportunities[i].getNumberOfObjects();

					// free speed car travel times in hours
					double freeSpeedTravelTime_h = (lcptFreeSpeedCarTravelTime.getTree().get( nodeID ).getCost() / 3600.) + offsetFreeSpeedTime_h;
					// travel distance in meter
					double travelDistance_meter = lcptTravelDistance.getTree().get( nodeID ).getCost();
					// walk travel times in hours
					double walkTravelTime_h		= travelDistance_meter / this.walkSpeedMeterPerHour;
					// congested car travel times in hours
					double arrivalTime = lcptCongestedCarTravelTime.getTree().get( nodeID ).getTime(); // may also use .getCost() !!!
					double congestedCarTravelTime_h = ((arrivalTime - depatureTime) / 3600.) + offsetCongestedCarTime_h;
					
					// for debugging freespeed accessibility
					freeTT = getAsUtilCar(betaCarTT, freeSpeedTravelTime_h, betaWalkTT, offsetWalkTime2Node_h);
					freeTTPower = getAsUtilCar(betaCarTTPower, freeSpeedTravelTime_h * freeSpeedTravelTime_h, betaWalkTTPower, offsetWalkTime2Node_h * offsetWalkTime2Node_h);
					freeLnTT = getAsUtilCar(betaCarLnTT, Math.log(freeSpeedTravelTime_h), betaWalkLnTT, Math.log(offsetWalkTime2Node_h));
					
					freeTD = getAsUtilCar(betaCarTD, travelDistance_meter + distanceRoad2Node_meter, betaWalkTD, distanceMeasuringPoint2Road_meter);
					freeTDPower = getAsUtilCar(betaCarTDPower, Math.pow(travelDistance_meter + distanceRoad2Node_meter, 2), betaWalkTDPower, distanceMeasuringPoint2Road_meter * distanceMeasuringPoint2Road_meter);
					freeLnTD = getAsUtilCar(betaCarLnTD, Math.log(travelDistance_meter + distanceRoad2Node_meter), betaWalkLnTD, Math.log(distanceMeasuringPoint2Road_meter));
					
					freeTC 		= 0.;	// since MATSim doesn't gives monetary costs jet 
					freeTCPower = 0.;	// since MATSim doesn't gives monetary costs jet 
					freeLnTC 	= 0.;	// since MATSim doesn't gives monetary costs jet 
					
					sumFREESPEED += opportunityWeight
								  * Math.exp(logitScaleParameterPreFactor *
										    (freeTT + 
										     freeTTPower +
										     freeLnTT +
										     freeTD +
										     freeTDPower +
										     freeLnTD +
										     freeTC +
										     freeTCPower + 
										     freeLnTC));
					
					// for debugging car accessibility
					carTT = getAsUtilCar(betaCarTT, congestedCarTravelTime_h, betaWalkTT, offsetWalkTime2Node_h);
					carTTPower = getAsUtilCar(betaCarTTPower, congestedCarTravelTime_h * congestedCarTravelTime_h, betaWalkTTPower, offsetWalkTime2Node_h * offsetWalkTime2Node_h);
					carLnTT	= getAsUtilCar(betaCarLnTT, Math.log(congestedCarTravelTime_h), betaWalkLnTT, Math.log(offsetWalkTime2Node_h));
					
					carTD = getAsUtilCar(betaCarTD, travelDistance_meter + distanceRoad2Node_meter, betaWalkTD, distanceMeasuringPoint2Road_meter); // carOffsetWalkTime2NearestLink_meter
					carTDPower = getAsUtilCar(betaCarTDPower, Math.pow(travelDistance_meter + distanceRoad2Node_meter, 2), betaWalkTDPower, distanceMeasuringPoint2Road_meter * distanceMeasuringPoint2Road_meter);
					carLnTD = getAsUtilCar(betaCarLnTD, Math.log(travelDistance_meter + distanceRoad2Node_meter), betaWalkLnTD, Math.log(distanceMeasuringPoint2Road_meter));
					
					carTC 		= 0.; 	// since MATSim doesn't gives monetary costs jet 
					carTCPower 	= 0.;	// since MATSim doesn't gives monetary costs jet 
					carLnTC 	= 0.;	// since MATSim doesn't gives monetary costs jet 
					
					// sum congested travel times
					sumCAR += opportunityWeight
							* Math.exp(logitScaleParameterPreFactor *
									  (carTT +
									   carTTPower +
									   carLnTT +
									   carTD + 
									   carTDPower + 
									   carLnTD +
									   carTC + 
									   carTCPower + 
									   carLnTC ));
					
					// for debugging walk accessibility
					//walkTT = getAsUtilCar(betaWalkTT, (travelDistance_meter / 15000.) , betaWalkTT, offsetWalkTime2Node_h); // this is to measure bike travel times
					walkTT = getAsUtilWalk(betaWalkTT, walkTravelTime_h + ((distanceMeasuringPoint2Road_meter + distanceRoad2Node_meter)/this.walkSpeedMeterPerHour));
					walkTTPower = getAsUtilWalk(betaWalkTTPower, Math.pow(walkTravelTime_h + ((distanceMeasuringPoint2Road_meter + distanceRoad2Node_meter)/this.walkSpeedMeterPerHour), 2) );
					walkLnTT = getAsUtilWalk(betaWalkLnTT, Math.log( walkTravelTime_h + ((distanceMeasuringPoint2Road_meter + distanceRoad2Node_meter)/this.walkSpeedMeterPerHour) ));
					
					walkTD = getAsUtilWalk(betaWalkTD, travelDistance_meter + distanceMeasuringPoint2Road_meter + distanceRoad2Node_meter);
					walkTDPower = getAsUtilWalk(betaWalkTDPower, Math.pow(travelDistance_meter + distanceMeasuringPoint2Road_meter + distanceRoad2Node_meter, 2));
					walkLnTD = getAsUtilWalk(betaWalkLnTD, Math.log(travelDistance_meter + distanceMeasuringPoint2Road_meter + distanceRoad2Node_meter));
					
					walkTC 		= 0.;	// since MATSim doesn't gives monetary costs jet 
					walkTCPower = 0.;	// since MATSim doesn't gives monetary costs jet 
					walkLnTC 	= 0.;	// since MATSim doesn't gives monetary costs jet 

					// sum walk travel times (substitute for distances)
					sumWALK += opportunityWeight
							* Math.exp(logitScaleParameterPreFactor *
									(walkTT +
									 walkTTPower +
									 walkLnTT +
									 walkTD +
									 walkTDPower +
									 walkLnTD + 
									 walkTC +
									 walkTCPower +
									 walkLnTC ));
				}
				
				// aggregated value
				double freeSpeedAccessibility, carAccessibility, walkAccessibility;
				if(!useRawSum){ 	// get log sum
					freeSpeedAccessibility = logitScaleParameterPreFactor * Math.log( sumFREESPEED );
					carAccessibility = logitScaleParameterPreFactor * Math.log( sumCAR );
					walkAccessibility= logitScaleParameterPreFactor * Math.log( sumWALK );
				}
				else{ 				// get raw sum
					freeSpeedAccessibility = logitScaleParameterPreFactor * sumFREESPEED;
					carAccessibility = logitScaleParameterPreFactor * sumCAR;
					walkAccessibility= logitScaleParameterPreFactor * sumWALK;
				}
				
				// assign log sums to current starZone object and spatial grid
				freeSpeedGrid.setValue(freeSpeedAccessibility, measurePoint.getGeometry().getCentroid());
				carGrid.setValue(carAccessibility , measurePoint.getGeometry().getCentroid());
				walkGrid.setValue(walkAccessibility , measurePoint.getGeometry().getCentroid());
				
				// writing accessibility values (stored in starZone object) in csv format ...
				accCsvWriter.write(measurePoint, 
								   coordFromZone, 
								   fromNode, 
								   freeSpeedAccessibility,
								   carAccessibility, 
								   walkAccessibility);
			}
			System.out.println("");

			if (this.benchmark != null && benchmarkID > 0) {
				this.benchmark.stoppMeasurement(benchmarkID);
				log.info("Accessibility computation with "
						+ measuringPoints.getZones().size()
						+ " starting points (origins) and "
						+ this.aggregatedOpportunities.length
						+ " destinations (workplaces) took "
						+ this.benchmark.getDurationInSeconds(benchmarkID)
						+ " seconds ("
						+ this.benchmark.getDurationInSeconds(benchmarkID)
						/ 60. + " minutes).");
			}
			// tnicolai: for debugging (remove for relaease)
			log.info("Euclidian vs Othogonal Distance:");
			log.info("Total Counter:" + NetworkUtil.totalCounter);
			log.info("Euclidian Counter:" + NetworkUtil.euclidianCounter);
			log.info("Othogonal Counter:" + NetworkUtil.othogonalCounter);
			
			accCsvWriter.close();
			writePlottingData();
			writeParcelAccessibilities();
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// This needs to be executed only once at the end of the accessibility computation
	// A synchronization is may be not needed
	private void writePlottingData() throws IOException{
		
		log.info("Writing plotting files ...");
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(freeSpeedGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// freespeed results for plotting in R
				+ "freeSpeedAccessibility_cellsize_" + freeSpeedGrid.getResolution()
				+ CellBasedAccessibilityControlerListenerV2.fileExtension
				+ InternalConstants.FILE_TYPE_TXT);
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(carGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// car results for plotting in R
				+ "carAccessibility_cellsize_" + carGrid.getResolution()
				+ CellBasedAccessibilityControlerListenerV2.fileExtension
				+ InternalConstants.FILE_TYPE_TXT);
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(walkGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// walk results for plotting in R
				+ "walkAccessibility_cellsize_" + walkGrid.getResolution()
				+ CellBasedAccessibilityControlerListenerV2.fileExtension
				+ InternalConstants.FILE_TYPE_TXT);

		// tnicolai: google earth outputs can be left in final release since
		// this can be used for a quick analysis without any further scripts or
		// so...
		GridUtils.writeKMZFiles(measuringPoints,								// car results for google earth
							freeSpeedGrid,
							InternalConstants.MATSIM_4_OPUS_TEMP
										+ "freeSpeedAccessibility_cellsize_"
										+ freeSpeedGrid.getResolution()
										+ CellBasedAccessibilityControlerListenerV2.fileExtension
										+ InternalConstants.FILE_TYPE_KMZ);
		GridUtils.writeKMZFiles(measuringPoints,								// car results for google earth
								carGrid,
								InternalConstants.MATSIM_4_OPUS_TEMP
										+ "carAccessibility_cellsize_"
										+ carGrid.getResolution()
										+ CellBasedAccessibilityControlerListenerV2.fileExtension
										+ InternalConstants.FILE_TYPE_KMZ);
		GridUtils.writeKMZFiles(measuringPoints,								// walk results for google earth
								walkGrid,
								InternalConstants.MATSIM_4_OPUS_TEMP
										+ "walkAccessibility_cellsize_"
										+ walkGrid.getResolution()
										+ CellBasedAccessibilityControlerListenerV2.fileExtension
										+ InternalConstants.FILE_TYPE_KMZ);
		log.info("Writing plotting files done!");
	}
	
	/**
	 * 
	 */
	private void writeParcelAccessibilities() {
		// from here accessibility feedback for each parcel
		UrbanSimParcelCSVWriter.initUrbanSimZoneWriter();
		
		Interpolation freeSpeedGridInterpolation = new Interpolation(freeSpeedGrid, Interpolation.BILINEAR);
		Interpolation carGridInterpolation = new Interpolation(carGrid, Interpolation.BILINEAR);
		Interpolation walkGridInterpolation= new Interpolation(walkGrid, Interpolation.BILINEAR);
		
		if(this.parcels != null){
			
			int numberOfParcels = this.parcels.getFacilities().size();
			double freeSpeedAccessibility = Double.NaN;
			double carAccessibility = Double.NaN;
			double walkAccessibility= Double.NaN;
			
			log.info(numberOfParcels + " parcels are now processing ...");
			
			Iterator<ActivityFacility> parcelIterator = this.parcels.getFacilities().values().iterator();
			ProgressBar bar = new ProgressBar( numberOfParcels );
			
			while(parcelIterator.hasNext()){
				
				bar.update();
				
				ActivityFacility parcel = parcelIterator.next();
				
				// for testing
				// double car = carGrid.getValue(parcel.getCoord().getX(), parcel.getCoord().getY());
				// double walk= walkGrid.getValue(parcel.getCoord().getX(), parcel.getCoord().getY());
				
				freeSpeedAccessibility = freeSpeedGridInterpolation.interpolate( parcel.getCoord() );
				carAccessibility = carGridInterpolation.interpolate( parcel.getCoord() );
				walkAccessibility= walkGridInterpolation.interpolate( parcel.getCoord() );
				
				UrbanSimParcelCSVWriter.write(parcel.getId(), freeSpeedAccessibility, carAccessibility, walkAccessibility);
			}
			log.info("... done!");
			UrbanSimParcelCSVWriter.close();
		}
	}
}
