package playground.tnicolai.matsim4opus.matsim4urbansim;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.scenario.ScenarioImpl;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.gis.GridUtils;
import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.gis.Zone;
import playground.tnicolai.matsim4opus.gis.ZoneLayer;
import playground.tnicolai.matsim4opus.interfaces.controlerinterface.AccessibilityControlerInterface;
import playground.tnicolai.matsim4opus.utils.String2BooleanConverter;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.ClusterObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.ZoneAccessibilityObject;

public class CellBasedAcessibilityControlerListener implements ShutdownListener, AccessibilityControlerInterface{
	
	private static final Logger log = Logger.getLogger(CellBasedAccessibilityShapeControlerListener.class);
	
	ClusterObject[] aggregatedWorkplaces;
	ZoneLayer<ZoneAccessibilityObject> startZones;
	
	SpatialGrid<Double> congestedTravelTimeAccessibilityGrid;
	SpatialGrid<Double> freespeedTravelTimeAccessibilityGrid;
	SpatialGrid<Double> walkTravelTimeAccessibilityGrid;
	
	// accessibility parameter
	boolean useRawSum	= false;
	double logitScaleParameter;
	double betaCarTT;
	double betaCarTTPower;
	double betaCarLnTT;
	double betaCarTD;
	double betaCarTDPower;
	double betaCarLnTD;
	double betaCarTC;
	double betaCarTCPower;
	double betaCarLnTC;
	double betaWalkTT;
	double betaWalkTTPower;
	double betaWalkLnTT;
	double betaWalkTD;
	double betaWalkTDPower;
	double betaWalkLnTD;
	double betaWalkTC;
	double betaWalkTCPower;
	double betaWalkLnTC;
	double depatureTime;
	double walkSpeedMeterPerMin = -1;
	double distanceCostRateCar;
	double distanceCostRateWalk;
	
	Benchmark benchmark;
	
	// use suffix for output file names to distinguish 
	// network ("nwSuffix") and shape-file ("sfSuffix") computations
	final String nwSuffix = "NW";
	final String sfSuffix = "SF";
	
	/**
	 * constructor
	 */
	public CellBasedAcessibilityControlerListener(ZoneLayer<ZoneAccessibilityObject> startZones, 			// needed for google earth plots (not supported by now tnicolai feb'12)
												 ClusterObject[] aggregatedOpportunities, 					// destinations (like workplaces)
												 SpatialGrid<Double> congestedTravelTimeAccessibilityGrid, 	// table for congested car travel times in accessibility computation
												 SpatialGrid<Double> freespeedTravelTimeAccessibilityGrid,	// table for freespeed car travel times in accessibility computation
												 SpatialGrid<Double> walkTravelTimeAccessibilityGrid, 		// table for walk travel times in accessibility computation
												 Benchmark benchmark,										// Benchmark tool
												 ScenarioImpl scenario){										
		assert (startZones != null);
		this.startZones = startZones;
		assert (aggregatedOpportunities != null);
		this.aggregatedWorkplaces = aggregatedOpportunities;
		assert (congestedTravelTimeAccessibilityGrid != null);
		this.congestedTravelTimeAccessibilityGrid = congestedTravelTimeAccessibilityGrid;
		assert (freespeedTravelTimeAccessibilityGrid != null);
		this.freespeedTravelTimeAccessibilityGrid = freespeedTravelTimeAccessibilityGrid;
		assert (walkTravelTimeAccessibilityGrid != null);
		this.walkTravelTimeAccessibilityGrid = walkTravelTimeAccessibilityGrid;
		assert (benchmark != null);
		this.benchmark = benchmark;
		
		initAccessibilityParameter(scenario);
	}
	
	public void notifyShutdown(ShutdownEvent event){
		log.warn("This needs to be implemented by an inherithed class!");
	}
	
	private void initAccessibilityParameter(ScenarioImpl scenario){
		
		useRawSum		= String2BooleanConverter.getBoolean( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.COMPUTE_RAW_SUM) );
		logitScaleParameter = Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.LOGIT_SCALE_PARAMETER) );
		walkSpeedMeterPerMin = scenario.getConfig().plansCalcRoute().getWalkSpeed() * 60.;
		
		betaCarTT 	   	= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_CAR_TRAVEL_TIMES) );
		betaCarTTPower	= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_CAR_TRAVEL_TIMES_POWER) );
		betaCarLnTT		= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_CAR_LN_TRAVEL_TIMES) );
		betaCarTD		= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_CAR_TRAVEL_DISTANCE) );
		betaCarTDPower	= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_CAR_TRAVEL_DISTANCE_POWER) );
		betaCarLnTD		= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_CAR_LN_TRAVEL_DISTANCE) );
		betaCarTC		= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_CAR_TRAVEL_COSTS) );
		betaCarTCPower	= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_CAR_TRAVEL_COSTS_POWER) );
		betaCarLnTC		= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_CAR_LN_TRAVEL_COSTS) );
		
		betaWalkTT		= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_WALK_TRAVEL_TIMES) );
		betaWalkTTPower	= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_WALK_TRAVEL_TIMES_POWER) );
		betaWalkLnTT	= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_WALK_LN_TRAVEL_TIMES) );
		betaWalkTD		= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_WALK_TRAVEL_DISTANCE) );
		betaWalkTDPower	= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_WALK_TRAVEL_DISTANCE_POWER) );
		betaWalkLnTD	= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_WALK_LN_TRAVEL_DISTANCE) );
		betaWalkTC		= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_WALK_TRAVEL_COSTS) );
		betaWalkTCPower	= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_WALK_TRAVEL_COSTS_POWER) );
		betaWalkLnTC	= Double.parseDouble( scenario.getConfig().getParam(Constants.ACCESSIBILITY_PARAMETER, Constants.BETA_WALK_LN_TRAVEL_COSTS) );
		
		depatureTime 	= 8.*3600;	// tnicolai: make configurable
		distanceCostRateCar = - scenario.getConfig().planCalcScore().getMonetaryDistanceCostRateCar();
		distanceCostRateWalk= 0.;//- scenario.getConfig().planCalcScore().getMonetaryDistanceCostRatePt();
	}
	
	public void printSettings(){
		log.info("Computing and writing grid based accessibility measures with following settings:" );
		log.info("Returning raw sum (not logsum): " + useRawSum);
		log.info("Logit Scale Parameter: " + logitScaleParameter);
		log.info("Walk speed (meter/sec): " + this.walkSpeedMeterPerMin/60.);
		log.info("Depature time (in seconds): " + depatureTime);
		log.info("Beta Car Travel Time: " + betaCarTT );
		log.info("Beta Car Travel Time Power2: " + betaCarTTPower );
		log.info("Beta Car Ln Travel Time: " + betaCarLnTT );
		log.info("Beta Car Travel Distance: " + betaCarTD );
		log.info("Beta Car Travel Distance Power2: " + betaCarTDPower );
		log.info("Beta Car Ln Travel Distance: " + betaCarLnTD );
		log.info("Beta Car Travel Cost: " + betaCarTC );
		log.info("Beta Car Travel Cost Power2: " + betaCarTCPower );
		log.info("Beta Car Ln Travel Cost: " + betaCarLnTC );
		log.info("Beta Walk Travel Time: " + betaWalkTT );
		log.info("Beta Walk Travel Time Power2: " + betaWalkTTPower );
		log.info("Beta Walk Ln Travel Time: " + betaWalkLnTT );
		log.info("Beta Walk Travel Distance: " + betaWalkTD );
		log.info("Beta Walk Travel Distance Power2: " + betaWalkTDPower );
		log.info("Beta Walk Ln Travel Distance: " + betaWalkLnTD );
		log.info("Beta Walk Travel Cost: " + betaWalkTC );
		log.info("Beta Walk Travel Cost Power2: " + betaWalkTCPower );
		log.info("Beta Walk Ln Travel Cost: " + betaWalkLnTC );
	}
	
	void dumpResults() throws IOException{
		log.info("Writing files ...");
		// finish and close writing
		GridUtils.writeSpatialGridTables(this, "UsingShapeFileBoundary");
		GridUtils.writeKMZFiles(this, "UsingShapeFileBoundary");
		log.info("Writing files done!");
	}
	
	/**
	 * @param startZone
	 * @param congestedTravelTimesCarLogSum
	 * @param freespeedTravelTimesCarLogSum
	 * @param accessibilityTravelDistance
	 */
	void setAccessibilityValues2StartZoneAndSpatialGrid(Zone<ZoneAccessibilityObject> startZone,
												 				double congestedTravelTimesCarLogSum, 
												 				double freespeedTravelTimesCarLogSum,
												 				double travelTimesWalkLogSum) {

		startZone.getAttribute().setCongestedTravelTimeAccessibility( congestedTravelTimesCarLogSum );
		startZone.getAttribute().setFreespeedTravelTimeAccessibility( freespeedTravelTimesCarLogSum );
		startZone.getAttribute().setWalkTravelTimeAccessibility(travelTimesWalkLogSum );
		
		congestedTravelTimeAccessibilityGrid.setValue(congestedTravelTimesCarLogSum , startZone.getGeometry().getCentroid());
		freespeedTravelTimeAccessibilityGrid.setValue(freespeedTravelTimesCarLogSum , startZone.getGeometry().getCentroid());
		walkTravelTimeAccessibilityGrid.setValue(travelTimesWalkLogSum , startZone.getGeometry().getCentroid());
	}
	
	// getter methods (this implements AccessibilityControlerInterface)
	
	public ZoneLayer<ZoneAccessibilityObject> getStartZones(){
		return startZones;
	}
	public ClusterObject[] getJobObjectMap(){
		return aggregatedWorkplaces;
	}
	public SpatialGrid<Double> getCongestedTravelTimeAccessibilityGrid(){
		return congestedTravelTimeAccessibilityGrid;
	}
	public SpatialGrid<Double> getFreespeedTravelTimeAccessibilityGrid(){
		return freespeedTravelTimeAccessibilityGrid;
	}
	public SpatialGrid<Double> getWalkTravelTimeAccessibilityGrid(){
		return walkTravelTimeAccessibilityGrid;
	}
	
}
