package playground.tnicolai.matsim4opus.matsim4urbansim;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.scenario.ScenarioImpl;

import playground.tnicolai.matsim4opus.config.AccessibilityParameterConfigModule;
import playground.tnicolai.matsim4opus.config.ConfigurationModule;
import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.gis.Zone;
import playground.tnicolai.matsim4opus.gis.ZoneLayer;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.CounterObject;

public class CellBasedAccessibilityControlerListenerV2 implements ShutdownListener{
	
	private static final Logger log = Logger.getLogger(CellBasedAccessibilityControlerListener.class);
	public static final String SHAPE_FILE = "SF";
	public static final String NETWORK = "NW";
	
	// start points, measuring accessibility
	ZoneLayer<CounterObject> measuringPoints;
	// destinations, opportunities like jobs etc ...
	AggregateObject2NearestNode[] aggregatedOpportunities;
	
	// storing the accessibility results
	SpatialGrid<Double> carGrid;
	SpatialGrid<Double> walkGrid;
	
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
	
	/**
	 * constructor
	 */
	public CellBasedAccessibilityControlerListenerV2(ZoneLayer<CounterObject> startZones, 		// needed for google earth plots (not supported by now tnicolai feb'12)
												 AggregateObject2NearestNode[] aggregatedOpportunities, 	// destinations (like workplaces)
												 SpatialGrid<Double> carGrid, 								// table for congested car travel times in accessibility computation
												 SpatialGrid<Double> walkGrid, 								// table for walk travel times in accessibility computation
												 Benchmark benchmark,										// Benchmark tool
												 ScenarioImpl scenario){										
		assert (startZones != null);
		this.measuringPoints = startZones;
		assert (aggregatedOpportunities != null);
		this.aggregatedOpportunities = aggregatedOpportunities;
		assert (carGrid != null);
		this.carGrid = carGrid;
		assert (walkGrid != null);
		this.walkGrid = walkGrid;
		assert (benchmark != null);
		this.benchmark = benchmark;
		
		initAccessibilityParameter(scenario);
	}
	
	public void notifyShutdown(ShutdownEvent event){
		log.warn("This needs to be implemented by an inherithed class!");
	}
	
	private void initAccessibilityParameter(ScenarioImpl scenario){
		
		AccessibilityParameterConfigModule module = ConfigurationModule.getAccessibilityParameterConfigModule(scenario);
		
		useRawSum		= module.isUseRawSumsWithoutLn();
		logitScaleParameter = module.getLogitScaleParameter();
		walkSpeedMeterPerMin = scenario.getConfig().plansCalcRoute().getWalkSpeed() * 60.;
		
		betaCarTT 	   	= module.getBetaCarTravelTime();
		betaCarTTPower	= module.getBetaCarTravelTimePower2();
		betaCarLnTT		= module.getBetaCarLnTravelTime();
		betaCarTD		= module.getBetaCarTravelDistance();
		betaCarTDPower	= module.getBetaCarTravelDistancePower2();
		betaCarLnTD		= module.getBetaCarLnTravelDistance();
		betaCarTC		= module.getBetaCarTravelCost();
		betaCarTCPower	= module.getBetaCarTravelCostPower2();
		betaCarLnTC		= module.getBetaCarLnTravelCost();
		
		betaWalkTT		= module.getBetaWalkTravelTime();
		betaWalkTTPower	= module.getBetaWalkTravelTimePower2();
		betaWalkLnTT	= module.getBetaWalkLnTravelTime();
		betaWalkTD		= module.getBetaWalkTravelDistance();
		betaWalkTDPower	= module.getBetaWalkTravelDistancePower2();
		betaWalkLnTD	= module.getBetaWalkLnTravelDistance();
		betaWalkTC		= module.getBetaWalkTravelCost();
		betaWalkTCPower	= module.getBetaWalkTravelCostPower2();
		betaWalkLnTC	= module.getBetaWalkLnTravelCost();
		
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
	
	synchronized void dumpResults() throws IOException{
		log.info("Writing files ...");
		// finish and close writing
		
		// TODO
		
		log.info("Writing files done!");
	}
	
	/**
	 * @param startZone
	 * @param carAccessibility
	 * @param freespeedTravelTimesCarLogSum
	 * @param accessibilityTravelDistance
	 */
	synchronized void setAccessibilityValues2StartZoneAndSpatialGrid(Zone<CounterObject> startZone,
												 				double carAccessibility, 
												 				double walkAccessibility) {

		carGrid.setValue(carAccessibility , startZone.getGeometry().getCentroid());
		walkGrid.setValue(walkAccessibility , startZone.getGeometry().getCentroid());
	}
}
