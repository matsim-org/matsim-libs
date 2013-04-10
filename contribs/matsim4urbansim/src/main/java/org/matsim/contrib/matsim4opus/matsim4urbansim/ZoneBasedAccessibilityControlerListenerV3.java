package org.matsim.contrib.matsim4opus.matsim4urbansim;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.matsim4opus.matsim4urbansim.costcalculators.FreeSpeedTravelTimeCostCalculator;
import org.matsim.contrib.matsim4opus.matsim4urbansim.costcalculators.TravelDistanceCalculator;
import org.matsim.contrib.matsim4opus.matsim4urbansim.costcalculators.TravelTimeCostCalculator;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.utils.LeastCostPathTree;

import org.matsim.contrib.matsim4opus.gis.Zone;
import org.matsim.contrib.matsim4opus.gis.ZoneLayer;
import org.matsim.contrib.matsim4opus.improvedpseudopt.PtMatrix;
import org.matsim.contrib.matsim4opus.interfaces.MATSim4UrbanSimInterface;
import org.matsim.contrib.matsim4opus.utils.helperObjects.Benchmark;
import org.matsim.contrib.matsim4opus.utils.io.writer.AnalysisZoneCSVWriterV2;
import org.matsim.contrib.matsim4opus.utils.io.writer.UrbanSimZoneCSVWriterV2;

/**
 *  improvements feb'12
 *  - distance between zone centroid and nearest node on road network is considered in the accessibility computation
 *  as walk time of the euclidian distance between both (centroid and nearest node). This walk time is added as an offset 
 *  to each measured travel times
 *  - using walk travel times instead of travel distances. This is because of the betas that are utils/time unit. The walk time
 *  corresponds to distances since this is also linear.
 * 
 * This works for UrbanSim Zone and Parcel Applications !!! (march'12)
 * 
 *  improvements april'12
 *  - accessibility calculation uses configurable betas (coming from UrbanSim) for car/walk travel times, -distances and -costs
 *  
 * improvements / changes july'12 
 * - fixed error: used pre-factor (1/beta scale) in deterrence function instead of beta scale (fixed now!)
 * 
 * todo (sep'12):
 * - set external costs to opportunities within the same zone ...
 * 
 * improvements jan'13
 * - added pt for accessibility calculation
 * 
 * @author thomas
 *
 */
public class ZoneBasedAccessibilityControlerListenerV3 extends AccessibilityControlerListenerImpl implements ShutdownListener{
	
	private static final Logger log = Logger.getLogger(ZoneBasedAccessibilityControlerListenerV3.class);
	

	// ////////////////////////////////////////////////////////////////////
	// constructors
	// ////////////////////////////////////////////////////////////////////
	
	public ZoneBasedAccessibilityControlerListenerV3(MATSim4UrbanSimInterface main,
												   ZoneLayer<Id>  startZones, 
												   ActivityFacilitiesImpl zones,
												   PtMatrix ptMatrix,
												   Benchmark benchmark,
												   ScenarioImpl scenario){
		
		log.info("Initializing ZoneBasedAccessibilityControlerListenerV3 ...");
		
		assert (main != null);
		this.main = main;
		assert(startZones != null);
		this.measuringPointsZone = startZones;
		assert(zones != null);
		this.zones = zones;
		this.ptMatrix = ptMatrix; // this could be zero of no input files for pseudo pt are given ...
		assert(benchmark != null);
		this.benchmark = benchmark;
		assert(scenario != null);

		// writing accessibility measures continuously into "zone.csv"-file. Naming of this 
		// files is given by the UrbanSim convention importing a csv file into a identically named 
		// data set table. THIS PRODUCES URBANSIM INPUT
		UrbanSimZoneCSVWriterV2.initUrbanSimZoneWriter();
		// in contrast to the file above this contains all information about
		// zones but is not dedicated as input for UrbanSim, use for analysis
		AnalysisZoneCSVWriterV2.initAccessiblityWriter();
		
		initAccessibilityParameter(scenario);
		log.info(".. done initializing ZoneBasedAccessibilityControlerListenerV3");
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." );
		
		// get the controller and scenario
		Controler controler = event.getControler();
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		
		this.aggregatedOpportunities = this.aggregatedOpportunities(this.zones, this.main.getOpportunitySampleRate(), network, this.main.isParcelMode());
		
		int benchmarkID = this.benchmark.addMeasure("zone-based accessibility computation");
		
		TravelTime ttc = controler.getLinkTravelTimes();
		// get the free-speed car travel times (in seconds)
		LeastCostPathTree lcptFreeSpeedCarTravelTime = new LeastCostPathTree( ttc, new FreeSpeedTravelTimeCostCalculator() );
		// get the congested car travel time (in seconds)
		LeastCostPathTree lcptCongestedCarTravelTime = new LeastCostPathTree( ttc, new TravelTimeCostCalculator(ttc) );
		// get travel distance (in meter)
		LeastCostPathTree lcptTravelDistance		 = new LeastCostPathTree( ttc, new TravelDistanceCalculator());

		// some ideas about how to use a more correct approach:
//		LeastCostPathTree lcptCar ;
//		
//		boolean usingMatsimParams = true ;
//		if ( usingMatsimParams ) {
//			lcptCar = new LeastCostPathTree( ttc, controler.createTravelCostCalculator() ) ; 
//			System.exit(-1) ;
//		} else {
//			// After some thinking, I am (again) of the opinion that also here we need to take the s.p. tree from the simulation.
//			// Reason: Assume the simulation equilibrates according to travel time (as usual).  A short _distance_ path may be
//			// available, but not have enough capacity.  If we run the accessibility on a distance-based s.p. tree, we get travel
//			// disutilities which are unrealistic since they are not congested.
//			// (However, need to be careful since someone may want walk accessibilities.  Those are not obtained by using the car s.p. tree
//			// and then using distance.)
//			// kai, apr'13
//			
//			TravelDisutilityFactory factory = controler.getTravelDisutilityFactory();
//			
//			// faking a scoring group with the urbansim params:
//			PlanCalcScoreConfigGroup cnScoringGroup = new PlanCalcScoreConfigGroup() ;
//
//			// marginal utility of money (should usually be positive):
//			final double margUtlOfMoney = this.betaCarTC;
//			cnScoringGroup.setMarginalUtilityOfMoney( margUtlOfMoney ) ; // (!!)
//			log.error("is the sign correct??") ; System.exit(-1) ;
//
//			// marginal utility (should usually be negative):
//			cnScoringGroup.setTraveling_utils_hr( this.betaCarTT ) ; 
//			log.error("units of betaCarTT = ??") ; System.exit(-1) ;
//			log.error("need to add utl of perf?  probably not since this is matsim-indep (may be confusing??)") ;
//			
//			// monetaryDistanceCostRateCar (should usually be positive??? negative???):
//			double monetaryDistanceCostRateCar /* money/meter */ = this.betaCarTD /* utl/meter*/ / margUtlOfMoney /* utl/money */ ;
//			log.error("is the sign correct??") ; System.exit(-1) ;
//			cnScoringGroup.setMonetaryDistanceCostRateCar(monetaryDistanceCostRateCar) ;
//			
//			cnScoringGroup.setConstantCar(0.) ; // no information; not a problem as long as we compute mode-based accessibilities 
//			// separately
//			
//			lcptCar = new LeastCostPathTree( ttc, factory.createTravelDisutility(ttc, cnScoringGroup) ) ;
//		}
		
		try{
			log.info("Computing and writing zone based accessibility measures ..." );
			printParameterSettings();
			
			Iterator<Zone<Id>> measuringPointIterator = measuringPointsZone.getZones().iterator();
			log.info(measuringPointsZone.getZones().size() + "  measurement points are now processing ...");
			
			accessibilityComputation(ttc, lcptFreeSpeedCarTravelTime,
					lcptCongestedCarTravelTime, lcptTravelDistance, ptMatrix, network,
					measuringPointIterator, measuringPointsZone.getZones().size(),
					ZONE_BASED);
			
			System.out.println();
			// finalizing/closing csv file containing accessibility measures
			UrbanSimZoneCSVWriterV2.close();
			AnalysisZoneCSVWriterV2.close();
			
			if (this.benchmark != null && benchmarkID > 0) {
				this.benchmark.stoppMeasurement(benchmarkID);
				log.info("Accessibility computation with " 
						+ measuringPointsZone.getZones().size()
						+ " zones (origins) and "
						+ this.aggregatedOpportunities.length
						+ " destinations (opportunities) took "
						+ this.benchmark.getDurationInSeconds(benchmarkID)
						+ " seconds ("
						+ this.benchmark.getDurationInSeconds(benchmarkID)
						/ 60. + " minutes).");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	protected void writeCSVData(
			Zone<Id> measurePoint, Coord coordFromZone,
			Node fromNode, double freeSpeedAccessibility,
			double carAccessibility, double bikeAccessibility,
			double walkAccessibility, double ptAccessibility) {
		// writing accessibility measures of current node in csv format (UrbanSim input)
		UrbanSimZoneCSVWriterV2.write(measurePoint,
									  freeSpeedAccessibility,
									  carAccessibility,
									  bikeAccessibility,
									  walkAccessibility, 
									  ptAccessibility);
		// writing complete zones information for further analysis
		AnalysisZoneCSVWriterV2.write(measurePoint,
									coordFromZone, 
									fromNode.getCoord(), 
									freeSpeedAccessibility,
									carAccessibility,
									bikeAccessibility,
									walkAccessibility,
									ptAccessibility);
	}
}
