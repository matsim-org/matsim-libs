package playground.dhosse.bachelorarbeit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.matsim4opus.config.AccessibilityParameterConfigModule;
import org.matsim.contrib.matsim4opus.constants.InternalConstants;
import org.matsim.contrib.matsim4opus.gis.GridUtils;
import org.matsim.contrib.matsim4opus.gis.SpatialGrid;
import org.matsim.contrib.matsim4opus.gis.Zone;
import org.matsim.contrib.matsim4opus.gis.ZoneLayer;
import org.matsim.contrib.matsim4opus.interfaces.MATSim4UrbanSimInterface;
import org.matsim.contrib.matsim4opus.interpolation.Interpolation;
import org.matsim.contrib.matsim4opus.matsim4urbansim.ParcelBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.matsim4opus.matsim4urbansim.AccessibilityControlerListenerImpl.GeneralizedCostSum;
import org.matsim.contrib.matsim4opus.matsim4urbansim.costcalculators.FreeSpeedTravelTimeCostCalculator;
import org.matsim.contrib.matsim4opus.matsim4urbansim.costcalculators.TravelDistanceCalculator;
import org.matsim.contrib.matsim4opus.matsim4urbansim.costcalculators.TravelTimeCostCalculator;
import org.matsim.contrib.matsim4opus.matsim4urbansim.router.PtMatrix;
import org.matsim.contrib.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import org.matsim.contrib.matsim4opus.utils.helperObjects.Benchmark;
import org.matsim.contrib.matsim4opus.utils.helperObjects.Distances;
import org.matsim.contrib.matsim4opus.utils.helperObjects.SpatialReferenceObject;
import org.matsim.contrib.matsim4opus.utils.io.writer.AnalysisCellBasedAccessibilityCSVWriterV2;
import org.matsim.contrib.matsim4opus.utils.io.writer.AnalysisWorkplaceCSVWriter;
import org.matsim.contrib.matsim4opus.utils.io.writer.UrbanSimParcelCSVWriter;
import org.matsim.contrib.matsim4opus.utils.misc.ProgressBar;
import org.matsim.contrib.matsim4opus.utils.network.NetworkUtil;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.LeastCostPathTree;

import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.CoordinateSequenceComparator;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryComponentFilter;
import com.vividsolutions.jts.geom.GeometryFilter;
import com.vividsolutions.jts.geom.Point;

public class MyParcelBasedAccessibilityControlerListener extends
		ParcelBasedAccessibilityControlerListenerV3 {
	
	private static final Logger log = Logger.getLogger(MyParcelBasedAccessibilityControlerListener.class);
	private static InternalConstants internalConstants = new InternalConstants();

	public MyParcelBasedAccessibilityControlerListener(
			MATSim4UrbanSimInterface main, ZoneLayer<Id> startZones,
			ActivityFacilitiesImpl parcels, SpatialGrid freeSpeedGrid,
			SpatialGrid carGrid, SpatialGrid bikeGrid, SpatialGrid walkGrid,
			SpatialGrid ptGrid, PtMatrix ptMatrix, Benchmark benchmark,
			ScenarioImpl scenario) {
		super(main, startZones, parcels, freeSpeedGrid, carGrid, bikeGrid, walkGrid,
				ptGrid, ptMatrix, benchmark, scenario);
		internalConstants.setOpusHomeDirectory("C:/Users/Daniel/Dropbox/bsc");
	}
	
	protected AggregateObject2NearestNode[] aggregatedOpportunities(final ActivityFacilitiesImpl parcelsOrZones, /*final double jobSample,*/ final NetworkImpl network/*, final boolean isParcelMode*/){
		
		// readJobs creates a hash map of job with key = job id
		// this hash map includes jobs according to job sample size
//		List<SpatialReferenceObject> jobSampleList = this.main.getReadFromUrbanSimModel().readJobs(parcelsOrZones, jobSample, isParcelMode);
//		assert( jobSampleList != null );
		
		// Since the aggregated opportunities in jobClusterArray does contain coordinates of their nearest node 
		// this result is dumped out here    tnicolai dec'12
//		AnalysisWorkplaceCSVWriter.writeWorkplaceData2CSV(InternalConstants.MATSIM_4_OPUS_TEMP + "workplaces.csv", jobSampleList);
		
		log.info("Aggregating workplaces with identical nearest node ...");
		Map<Id, AggregateObject2NearestNode> opportunityClusterMap = new HashMap<Id, AggregateObject2NearestNode>();
		
		ProgressBar bar = new ProgressBar( parcelsOrZones.getFacilities().size() );

		for(ActivityFacility facility : parcelsOrZones.getFacilities().values()){
			bar.update();
			
//			SpatialReferenceObject sro = (SpatialReferenceObject) parcelsOrZones.getFacilities().get( i );
//			assert( sro.getCoord() != null );
			Node nearestNode = network.getNearestNode( facility.getCoord() );
			assert( nearestNode != null );

			// get euclidian distance to nearest node
			double distance_meter 	= NetworkUtil.getEuclidianDistance(facility.getCoord(), nearestNode.getCoord());
			double walkTravelTime_h = distance_meter / this.walkSpeedMeterPerHour;
			
			double VjkWalkTravelTime	= this.betaWalkTT * walkTravelTime_h;
			double VjkWalkPowerTravelTime=this.betaWalkTTPower * (walkTravelTime_h * walkTravelTime_h);
			double VjkWalkLnTravelTime	= this.betaWalkLnTT * Math.log(walkTravelTime_h);
			
			double VjkWalkDistance 		= this.betaWalkTD * distance_meter;
			double VjkWalkPowerDistnace	= this.betaWalkTDPower * (distance_meter * distance_meter);
			double VjkWalkLnDistance 	= this.betaWalkLnTD * Math.log(distance_meter);
			
			double VjkWalkMoney			= 0.;
			double VjkWalkPowerMoney	= 0.;
			double VjkWalkLnMoney		= 0.;

			double Vjk					= Math.exp(this.logitScaleParameter * (VjkWalkTravelTime + VjkWalkPowerTravelTime + VjkWalkLnTravelTime +
																			   VjkWalkDistance   + VjkWalkPowerDistnace   + VjkWalkLnDistance +
																			   VjkWalkMoney      + VjkWalkPowerMoney      + VjkWalkLnMoney) );
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
		
		log.info("Aggregated " + parcelsOrZones.getFacilities().size() + " number of workplaces (sampling rate: " + parcelsOrZones.getFacilities().size() + ") to " + jobClusterArray.length + " nodes.");
		
		return jobClusterArray;
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event){
		log.info("Entering notifyShutdown ..." );
		
		// get the controller and scenario
		Controler controler = event.getControler();
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		
		this.aggregatedOpportunities = this.aggregatedOpportunities(this.parcels, /*this.main.getOpportunitySampleRate(),*/ network/*, this.main.isParcelMode()*/);
		
//		int benchmarkID = this.benchmark.addMeasure("cell-based accessibility computation");
		
		TravelTime ttc = controler.getLinkTravelTimes();
		// get the free-speed car travel times (in seconds)
		LeastCostPathTree lcptFreeSpeedCarTravelTime = new LeastCostPathTree( ttc, new FreeSpeedTravelTimeCostCalculator() );
		// get the congested car travel time (in seconds)
		LeastCostPathTree lcptCongestedCarTravelTime = new LeastCostPathTree( ttc, new TravelTimeCostCalculator(ttc) );
		// get travel distance (in meter)
		LeastCostPathTree lcptTravelDistance		 = new LeastCostPathTree( ttc, new TravelDistanceCalculator());

		try{
			log.info("Computing and writing cell based accessibility measures ...");
			printParameterSettings();
			
			Iterator<Zone<Id>> measuringPointIterator = measuringPointsCell.getZones().iterator();
			log.info(measuringPointsCell.getZones().size() + " measurement points are now processing ...");
			
			accessibilityComputation(ttc, 
									 lcptFreeSpeedCarTravelTime,
									 lcptCongestedCarTravelTime, 
									 lcptTravelDistance, 
									 ptMatrix,
									 network,
									 measuringPointIterator, 
									 measuringPointsCell.getZones().size(),
									 PARCEL_BASED);
			
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
//			writeInterpolatedParcelAccessibilities();	// UrbanSim input file with interpolated accessibilities on parcel level
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writePlottingData() throws IOException{
		
		log.info("Writing plotting files ...");
		// tnicolai: can be disabled for final release
		
		GridUtils.writeSpatialGridTable(freeSpeedGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// freespeed results for plotting in R
				+ FREESEED_FILENAME + freeSpeedGrid.getResolution()
				+ InternalConstants.FILE_TYPE_TXT);
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(carGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// car results for plotting in R
				+ CAR_FILENAME + carGrid.getResolution()
				+ InternalConstants.FILE_TYPE_TXT);
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(bikeGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// car results for plotting in R
				+ BIKE_FILENAME + bikeGrid.getResolution()
				+ InternalConstants.FILE_TYPE_TXT);
		// tnicolai: can be disabled for final release
		GridUtils.writeSpatialGridTable(walkGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// walk results for plotting in R
				+ WALK_FILENAME + walkGrid.getResolution()
				+ InternalConstants.FILE_TYPE_TXT);
		// tnicolai: can be disabled for final release
//		GridUtils.writeSpatialGridTable(ptGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// walk results for plotting in R
//				+ PT_FILENAME + ptGrid.getResolution()
//				+ InternalConstants.FILE_TYPE_TXT);

		log.info("Writing plotting files done!");
	}
	
	private void writeInterpolatedParcelAccessibilities() {
		// from here accessibility feedback for each parcel
		UrbanSimParcelCSVWriter.initUrbanSimZoneWriter();
		
		Interpolation freeSpeedGridInterpolation = new Interpolation(freeSpeedGrid, Interpolation.BILINEAR);
		Interpolation carGridInterpolation = new Interpolation(carGrid, Interpolation.BILINEAR);
		Interpolation bikeGridInterpolation= new Interpolation(bikeGrid, Interpolation.BILINEAR);
		Interpolation walkGridInterpolation= new Interpolation(walkGrid, Interpolation.BILINEAR);
		Interpolation ptGridInterpolation  = new Interpolation(ptGrid, Interpolation.BILINEAR);
		
		if(this.parcels != null){
			
			int numberOfParcels = this.parcels.getFacilities().size();
			double freeSpeedAccessibility = Double.NaN;
			double carAccessibility = Double.NaN;
			double bikeAccessibility= Double.NaN;
			double walkAccessibility= Double.NaN;
			double ptAccessibility  = Double.NaN;
			
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
				bikeAccessibility= bikeGridInterpolation.interpolate( parcel.getCoord() );
				walkAccessibility= walkGridInterpolation.interpolate( parcel.getCoord() );
				ptAccessibility  = ptGridInterpolation.interpolate( parcel.getCoord() );
				
				UrbanSimParcelCSVWriter.write(parcel.getId(), freeSpeedAccessibility, carAccessibility, bikeAccessibility, walkAccessibility, ptAccessibility);
			}
			log.info("... done!");
			UrbanSimParcelCSVWriter.close();
		}
	}
		
		@Override
		protected void initAccessibilityParameter(ScenarioImpl scenario){
			
			AccessibilityParameterConfigModule moduleAPCM = new AccessibilityParameterConfigModule("ACPM");
			// tnicolai TODO: use MATSimControlerConfigModuleV3 to get "timeofday", implement ConfigurationModuleVx which returns the current config modules
			
			moduleAPCM.setBetaBikeTravelTime(-12);
			moduleAPCM.setBetaBikeLnTravelCost(0);
			moduleAPCM.setBetaBikeLnTravelDistance(0);
			moduleAPCM.setBetaBikeLnTravelTime(0);
			moduleAPCM.setBetaBikeTravelCost(0);
			moduleAPCM.setBetaBikeTravelCostPower2(0);
			moduleAPCM.setBetaBikeTravelDistance(0);
			moduleAPCM.setBetaBikeTravelDistancePower2(0);
			moduleAPCM.setBetaBikeTravelTimePower2(0);
			
			moduleAPCM.setBetaCarTravelTime(-12);
			moduleAPCM.setBetaCarLnTravelCost(0);
			moduleAPCM.setBetaCarLnTravelDistance(0);
			moduleAPCM.setBetaCarLnTravelTime(0);
			moduleAPCM.setBetaCarTravelCost(0);
			moduleAPCM.setBetaCarTravelCostPower2(0);
			moduleAPCM.setBetaCarTravelDistance(0);
			moduleAPCM.setBetaCarTravelDistancePower2(0);
			moduleAPCM.setBetaCarTravelTimePower2(0);
			
			moduleAPCM.setBetaWalkTravelTime(-12);
			moduleAPCM.setBetaWalkLnTravelCost(0);
			moduleAPCM.setBetaWalkLnTravelDistance(0);
			moduleAPCM.setBetaWalkLnTravelTime(0);
			moduleAPCM.setBetaWalkTravelCost(0);
			moduleAPCM.setBetaWalkTravelCostPower2(0);
			moduleAPCM.setBetaWalkTravelDistance(0);
			moduleAPCM.setBetaWalkTravelDistancePower2(0);
			moduleAPCM.setBetaWalkTravelTimePower2(0);
			
			moduleAPCM.setAccessibilityDestinationSamplingRate(1);
			moduleAPCM.setLogitScaleParameter(1);
			moduleAPCM.setUseBikeParameterFromMATSim(true);
			moduleAPCM.setUseCarParameterFromMATSim(true);
			moduleAPCM.setUseLogitScaleParameterFromMATSim(true);
			moduleAPCM.setUseRawSumsWithoutLn(true);
			moduleAPCM.setUseWalkParameterFromMATSim(true);
			
			useRawSum			= moduleAPCM.isUseRawSumsWithoutLn();
			logitScaleParameter = moduleAPCM.getLogitScaleParameter();
			inverseOfLogitScaleParameter = 1/(logitScaleParameter); // logitScaleParameter = same as brainExpBeta on 2-aug-12. kai
			walkSpeedMeterPerHour = scenario.getConfig().plansCalcRoute().getWalkSpeed() * 3600.;
			bikeSpeedMeterPerHour = scenario.getConfig().plansCalcRoute().getBikeSpeed() * 3600.; // should be something like 15000
			
			betaCarTT 	   	= moduleAPCM.getBetaCarTravelTime();
			betaCarTTPower	= moduleAPCM.getBetaCarTravelTimePower2();
			betaCarLnTT		= moduleAPCM.getBetaCarLnTravelTime();
			betaCarTD		= moduleAPCM.getBetaCarTravelDistance();
			betaCarTDPower	= moduleAPCM.getBetaCarTravelDistancePower2();
			betaCarLnTD		= moduleAPCM.getBetaCarLnTravelDistance();
			betaCarTC		= moduleAPCM.getBetaCarTravelCost();
			betaCarTCPower	= moduleAPCM.getBetaCarTravelCostPower2();
			betaCarLnTC		= moduleAPCM.getBetaCarLnTravelCost();
			
			betaBikeTT		= moduleAPCM.getBetaBikeTravelTime();
			betaBikeTTPower	= moduleAPCM.getBetaBikeTravelTimePower2();
			betaBikeLnTT	= moduleAPCM.getBetaBikeLnTravelTime();
			betaBikeTD		= moduleAPCM.getBetaBikeTravelDistance();
			betaBikeTDPower	= moduleAPCM.getBetaBikeTravelDistancePower2();
			betaBikeLnTD	= moduleAPCM.getBetaBikeLnTravelDistance();
			betaBikeTC		= moduleAPCM.getBetaBikeTravelCost();
			betaBikeTCPower	= moduleAPCM.getBetaBikeTravelCostPower2();
			betaBikeLnTC	= moduleAPCM.getBetaBikeLnTravelCost();
			
			betaWalkTT		= moduleAPCM.getBetaWalkTravelTime();
			betaWalkTTPower	= moduleAPCM.getBetaWalkTravelTimePower2();
			betaWalkLnTT	= moduleAPCM.getBetaWalkLnTravelTime();
			betaWalkTD		= moduleAPCM.getBetaWalkTravelDistance();
			betaWalkTDPower	= moduleAPCM.getBetaWalkTravelDistancePower2();
			betaWalkLnTD	= moduleAPCM.getBetaWalkLnTravelDistance();
			betaWalkTC		= moduleAPCM.getBetaWalkTravelCost();
			betaWalkTCPower	= moduleAPCM.getBetaWalkTravelCostPower2();
			betaWalkLnTC	= moduleAPCM.getBetaWalkLnTravelCost();
			
			betaPtTT		= moduleAPCM.getBetaPtTravelTime();
			betaPtTTPower	= moduleAPCM.getBetaPtTravelTimePower2();
			betaPtLnTT		= moduleAPCM.getBetaPtLnTravelTime();
			betaPtTD		= moduleAPCM.getBetaPtTravelDistance();
			betaPtTDPower	= moduleAPCM.getBetaPtTravelDistancePower2();
			betaPtLnTD		= moduleAPCM.getBetaPtLnTravelDistance();
			betaPtTC		= moduleAPCM.getBetaPtTravelCost();
			betaPtTCPower	= moduleAPCM.getBetaPtTravelCostPower2();
			betaPtLnTC		= moduleAPCM.getBetaPtLnTravelCost();
			
			depatureTime 	= 8.*3600;	
			printParameterSettings();
		}
		
		@Override
		protected void accessibilityComputation(TravelTime ttc,
				LeastCostPathTree lcptFreeSpeedCarTravelTime,
				LeastCostPathTree lcptCongestedCarTravelTime,
				LeastCostPathTree lcptTravelDistance, 
				PtMatrix ptMatrix,
				NetworkImpl network,
				Iterator<Zone<Id>> measuringPointIterator,
				int numberOfMeasuringPoints, 
				int mode) {

			GeneralizedCostSum gcs = new GeneralizedCostSum();

			//// tnicolai: only for testing, disable afterwards
			//ZoneLayer<Id> testSet = createTestPoints();
			//measuringPointIterator = testSet.getZones().iterator();

			// this data structure condense measuring points (origins) that have the same nearest node on the network ...
			Map<Id,ArrayList<Zone<Id>>> aggregatedMeasurementPoints = new HashMap<Id, ArrayList<Zone<Id>>>();
			
			// go through all measuring points ...
			while( measuringPointIterator.hasNext() ){

				Zone<Id> measurePoint = measuringPointIterator.next();
				Point point = measurePoint.getGeometry().getCentroid();
				// get coordinate from origin (start point)
				Coord coordFromZone = new CoordImpl( point.getX(), point.getY());
				// captures the distance (as walk time) between a cell centroid and the road network
				Link nearestLink = network.getNearestLinkExactly(coordFromZone);
				// determine nearest network node (from- or toNode) based on the link 
				Node fromNode = NetworkUtil.getNearestNode(coordFromZone, nearestLink);

				// this is used as a key for hash map lookups
				Id id = fromNode.getId();

				// create new entry if key does not exist!
				if(!aggregatedMeasurementPoints.containsKey(id))
					aggregatedMeasurementPoints.put(id, new ArrayList<Zone<Id>>());
				// assign measure point (origin) to it's nearest node
				aggregatedMeasurementPoints.get(id).add(measurePoint);
			}

			log.info("");
			log.info("Number of measure points: " + numberOfMeasuringPoints);
			log.info("Number of aggregated measure points: " + aggregatedMeasurementPoints.size());
			log.info("");


			ProgressBar bar = new ProgressBar( aggregatedMeasurementPoints.size() );

			// contains all nodes that have a measuring point (origin) assigned
			Iterator<Id> keyIterator = aggregatedMeasurementPoints.keySet().iterator();
			// contains all network nodes
			Map<Id, Node> networkNodesMap = network.getNodes();

			// go through all nodes (key's) that have a measuring point (origin) assigned
			while( keyIterator.hasNext() ){

				bar.update();

				Id nodeId = keyIterator.next();
				Node fromNode = networkNodesMap.get( nodeId );

				// run dijkstra on network
				// this is done once for all origins in the "origins" list, see below
				lcptFreeSpeedCarTravelTime.calculate(network, fromNode, depatureTime);
				lcptCongestedCarTravelTime.calculate(network, fromNode, depatureTime);		
				lcptTravelDistance.calculate(network, fromNode, depatureTime);

				// get list with origins that are assigned to "fromNode"
				ArrayList<Zone<Id>> origins = aggregatedMeasurementPoints.get( nodeId );
				Iterator<Zone<Id>> originsIterator = origins.iterator();

				while( originsIterator.hasNext() ){

					Zone<Id> measurePoint = originsIterator.next();
					Point point = measurePoint.getGeometry().getCentroid();
					// get coordinate from origin (start point)
					Coord coordFromZone = new CoordImpl( point.getX(), point.getY());
					assert( coordFromZone!=null );
					// captures the distance (as walk time) between a cell centroid and the road network
					Link nearestLink = network.getNearestLinkExactly(coordFromZone);

					// captures the distance (as walk time) between a zone centroid and its nearest node
					Distances distance = NetworkUtil.getDistance2NodeV2(nearestLink, point, fromNode);

					double distanceMeasuringPoint2Road_meter 	= distance.getDisatancePoint2Road(); // distance measuring point 2 road (link or node)
					double distanceRoad2Node_meter 				= distance.getDistanceRoad2Node();	 // distance intersection 2 node (only for orthogonal distance)

					double walkTravelTimePoint2Road_h 			= distanceMeasuringPoint2Road_meter / this.walkSpeedMeterPerHour;

					double freeSpeedTravelTimeOnNearestLink_meterpersec= nearestLink.getFreespeed();
					double carTravelTimeOnNearestLink_meterpersec= nearestLink.getLength() / ttc.getLinkTravelTime(nearestLink, depatureTime, null, null);

					double road2NodeFreeSpeedTime_h				= distanceRoad2Node_meter / (freeSpeedTravelTimeOnNearestLink_meterpersec * 3600);
					double road2NodeCongestedCarTime_h 			= distanceRoad2Node_meter / (carTravelTimeOnNearestLink_meterpersec * 3600.);
					double road2NodeBikeTime_h					= distanceRoad2Node_meter / this.bikeSpeedMeterPerHour;
					double road2NodeWalkTime_h					= distanceRoad2Node_meter / this.walkSpeedMeterPerHour;


					// Possible offsets to calculate the gap between measuring (start) point and start node (fromNode)
					// Euclidean Distance (measuring point 2 nearest node):
					// double walkTimeOffset_min = NetworkUtil.getEuclideanDistanceAsWalkTimeInSeconds(coordFromZone, fromNode.getCoord()) / 60.;
					// Orthogonal Distance (measuring point 2 nearest link, does not include remaining distance between link intersection and nearest node)
					// LinkImpl nearestLink = network.getNearestLink( coordFromZone );
					// double walkTimeOffset_min = (nearestLink.calcDistance( coordFromZone ) / this.walkSpeedMeterPerMin); 
					// or use NetworkUtil.getOrthogonalDistance(link, point) instead!

					gcs.reset();

					// goes through all opportunities, e.g. jobs, (nearest network node) and calculate the accessibility
					for ( int i = 0; i < this.aggregatedOpportunities.length; i++ ) {

						// get stored network node (this is the nearest node next to an aggregated work place)
						Node destinationNode = this.aggregatedOpportunities[i].getNearestNode();
						Id nodeID = destinationNode.getId();

						// tnicolai not needed anymore since having the aggregated costs on the opportunity side
						// using number of aggregated opportunities as weight for log sum measure
						// int opportunityWeight = this.aggregatedOpportunities[i].getNumberOfObjects(); 

						// congested car travel times in hours
						double arrivalTime 			= lcptCongestedCarTravelTime.getTree().get( nodeID ).getTime(); // may also use .getCost() !!!
						double congestedCarTravelTime_h = ((arrivalTime - depatureTime) / 3600.) + road2NodeCongestedCarTime_h;
						// free speed car travel times in hours
						double freeSpeedTravelTime_h= (lcptFreeSpeedCarTravelTime.getTree().get( nodeID ).getCost() / 3600.) + road2NodeFreeSpeedTime_h;
						// travel distance in meter
						double travelDistance_meter = lcptTravelDistance.getTree().get( nodeID ).getCost();
						// bike travel times in hours
						double bikeTravelTime_h 	= (travelDistance_meter / this.bikeSpeedMeterPerHour) + road2NodeBikeTime_h; // using a constant speed of 15km/h
						// walk travel times in hours
						double walkTravelTime_h		= (travelDistance_meter / this.walkSpeedMeterPerHour) + road2NodeWalkTime_h;

						// travel times and distances for pseudo pt
						double ptTravelTime_h		= Double.MAX_VALUE;	// travel time with pt
						double ptTotalWalkTime_h	= Double.MAX_VALUE;	// total walking time including (i) to get to pt stop and (ii) to get from destination pt stop to destination location
						double ptTravelDistance_meter=Double.MAX_VALUE; // total travel distance including walking and pt distance from/to origin/destination location
						double ptTotalWalkDistance_meter=Double.MAX_VALUE;// total walk distance  including (i) to get to pt stop and (ii) to get from destination pt stop to destination location
						if(ptMatrix != null){
							ptTravelTime_h 			= ptMatrix.getPtTravelTime(fromNode.getCoord(), destinationNode.getCoord()) / 3600.;
							ptTotalWalkTime_h		=ptMatrix.getTotalWalkTravelTime(fromNode.getCoord(), destinationNode.getCoord()) / 3600.;

							ptTotalWalkDistance_meter=ptMatrix.getTotalWalkTravelDistance(fromNode.getCoord(), destinationNode.getCoord());
							ptTravelDistance_meter  = ptMatrix.getPtTravelDistance(fromNode.getCoord(), destinationNode.getCoord());
						}

						sumDisutilityOfTravel(gcs, 
								this.aggregatedOpportunities[i],
								distanceMeasuringPoint2Road_meter,
								distanceRoad2Node_meter, 
								travelDistance_meter,
								walkTravelTimePoint2Road_h,
								freeSpeedTravelTime_h,
								bikeTravelTime_h,
								walkTravelTime_h, 
								congestedCarTravelTime_h,
								ptTravelTime_h,
								ptTotalWalkTime_h,
								ptTravelDistance_meter,
								ptTotalWalkDistance_meter);
					}

					// aggregated value
					double freeSpeedAccessibility, carAccessibility, bikeAccessibility, walkAccessibility, ptAccessibility;
					if(!useRawSum){ 	// get log sum
						freeSpeedAccessibility = inverseOfLogitScaleParameter * Math.log( gcs.getFreeSpeedSum() );
						carAccessibility = inverseOfLogitScaleParameter * Math.log( gcs.getCarSum() );
						bikeAccessibility= inverseOfLogitScaleParameter * Math.log( gcs.getBikeSum() );
						walkAccessibility= inverseOfLogitScaleParameter * Math.log( gcs.getWalkSum() );
						ptAccessibility	 = inverseOfLogitScaleParameter * Math.log( gcs.getPtSum() );
					}
					else{ 				// get raw sum
						freeSpeedAccessibility = inverseOfLogitScaleParameter * gcs.getFreeSpeedSum();
						carAccessibility = inverseOfLogitScaleParameter * gcs.getCarSum();
						bikeAccessibility= inverseOfLogitScaleParameter * gcs.getBikeSum();
						walkAccessibility= inverseOfLogitScaleParameter * gcs.getWalkSum();
						ptAccessibility  = inverseOfLogitScaleParameter * gcs.getPtSum();
					}

					if(mode == PARCEL_BASED){ // only for cell-based accessibility computation
//						 assign log sums to current starZone object and spatial grid
						freeSpeedGrid.setValue(freeSpeedAccessibility, measurePoint.getGeometry().getCentroid());
						carGrid.setValue(carAccessibility , measurePoint.getGeometry().getCentroid());
						bikeGrid.setValue(bikeAccessibility , measurePoint.getGeometry().getCentroid());
						walkGrid.setValue(walkAccessibility , measurePoint.getGeometry().getCentroid());
						ptGrid.setValue(ptAccessibility, measurePoint.getGeometry().getCentroid());
//						log.info(freeSpeedGrid.getValue(measurePoint.getGeometry().getCentroid()));
					}

//					writeCSVData(measurePoint, coordFromZone, fromNode, 
//							freeSpeedAccessibility, carAccessibility,
//							bikeAccessibility, walkAccessibility, ptAccessibility);
				}
			}
		}

}