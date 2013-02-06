package playground.dhosse.bachelorarbeit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
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
import org.matsim.contrib.matsim4opus.matsim4urbansim.costcalculators.FreeSpeedTravelTimeCostCalculator;
import org.matsim.contrib.matsim4opus.matsim4urbansim.costcalculators.TravelDistanceCalculator;
import org.matsim.contrib.matsim4opus.matsim4urbansim.costcalculators.TravelTimeCostCalculator;
import org.matsim.contrib.matsim4opus.matsim4urbansim.router.PtMatrix;
import org.matsim.contrib.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import org.matsim.contrib.matsim4opus.utils.helperObjects.Benchmark;
import org.matsim.contrib.matsim4opus.utils.io.writer.AnalysisCellBasedAccessibilityCSVWriterV2;
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
import org.matsim.utils.LeastCostPathTree;

public class MyParcelBasedAccessibilityControlerListener extends
		ParcelBasedAccessibilityControlerListenerV3 {
	
	private static final Logger log = Logger.getLogger(MyParcelBasedAccessibilityControlerListener.class);

	public MyParcelBasedAccessibilityControlerListener(
			MATSim4UrbanSimInterface main, ZoneLayer<Id> startZones,
			ActivityFacilitiesImpl parcels, SpatialGrid freeSpeedGrid,
			SpatialGrid carGrid, SpatialGrid bikeGrid, SpatialGrid walkGrid,
			SpatialGrid ptGrid, PtMatrix ptMatrix, Benchmark benchmark,
			ScenarioImpl scenario) {
		super(main, startZones, parcels, freeSpeedGrid, carGrid, bikeGrid, walkGrid,
				ptGrid, ptMatrix, benchmark, scenario);
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
			writeInterpolatedParcelAccessibilities();	// UrbanSim input file with interpolated accessibilities on parcel level
		
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
		GridUtils.writeSpatialGridTable(ptGrid, InternalConstants.MATSIM_4_OPUS_TEMP	// walk results for plotting in R
				+ PT_FILENAME + ptGrid.getResolution()
				+ InternalConstants.FILE_TYPE_TXT);

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
		
}