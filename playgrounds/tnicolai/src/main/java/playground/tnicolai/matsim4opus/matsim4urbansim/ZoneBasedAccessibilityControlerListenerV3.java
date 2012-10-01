package playground.tnicolai.matsim4opus.matsim4urbansim;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.gis.Zone;
import playground.tnicolai.matsim4opus.gis.ZoneLayer;
import playground.tnicolai.matsim4opus.interfaces.MATSim4UrbanSimInterface;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.FreeSpeedTravelTimeCostCalculator;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelDistanceCalculator;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelTimeCostCalculator;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.io.writer.AnalysisZoneCSVWriterV2;
import playground.tnicolai.matsim4opus.utils.io.writer.UrbanSimZoneCSVWriterV2;

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
												   Benchmark benchmark,
												   ScenarioImpl scenario){
		
		log.info("Initializing ZoneBasedAccessibilityControlerListenerV3 ...");
		
		assert (main != null);
		this.main = main;
		assert(startZones != null);
		this.measuringPointsZone = startZones;
		assert(zones != null);
		this.zones = zones;
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
		
		TravelTime ttc = controler.getTravelTimeCalculator();
		// get the free-speed car travel times (in seconds)
		LeastCostPathTree lcptFreeSpeedCarTravelTime = new LeastCostPathTree( ttc, new FreeSpeedTravelTimeCostCalculator() );
		// get the congested car travel time (in seconds)
		LeastCostPathTree lcptCongestedCarTravelTime = new LeastCostPathTree( ttc, new TravelTimeCostCalculator(ttc) );
		// get travel distance (in meter)
		LeastCostPathTree lcptTravelDistance		 = new LeastCostPathTree( ttc, new TravelDistanceCalculator());
		
		try{
			log.info("Computing and writing zone based accessibility measures ..." );
			printParameterSettings();
			
			Iterator<Zone<Id>> measuringPointIterator = measuringPointsZone.getZones().iterator();
			log.info(measuringPointsZone.getZones().size() + "  measurement points are now processing ...");
			
			accessibilityComputation(ttc, lcptFreeSpeedCarTravelTime,
					lcptCongestedCarTravelTime, lcptTravelDistance, network,
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
			double walkAccessibility) {
		// writing accessibility measures of current node in csv format (UrbanSim input)
		UrbanSimZoneCSVWriterV2.write(measurePoint,
									  freeSpeedAccessibility,
									  carAccessibility,
									  bikeAccessibility,
									  walkAccessibility);
		// writing complete zones information for further analysis
		AnalysisZoneCSVWriterV2.write(measurePoint,
									coordFromZone, 
									fromNode.getCoord(), 
									freeSpeedAccessibility,
									carAccessibility,
									bikeAccessibility,
									walkAccessibility);
	}
}
