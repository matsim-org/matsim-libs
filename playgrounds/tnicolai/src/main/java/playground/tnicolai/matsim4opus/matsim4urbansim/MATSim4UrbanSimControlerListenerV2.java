package playground.tnicolai.matsim4opus.matsim4urbansim;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.utils.ProgressBar;
import playground.tnicolai.matsim4opus.utils.helperObjects.AccessibilityHelperObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.WorkplaceObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.ZoneInfoObject;
import playground.toronto.ttimematrix.LeastCostPathTree;

/**
 *
 * @author nagel
 * @author thomas
 *
 */
public class MATSim4UrbanSimControlerListenerV2 implements ShutdownListener {
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimControlerListenerV2.class);

	private ActivityFacilitiesImpl zones;
	private ActivityFacilitiesImpl facilities;
	private Map<Id,WorkplaceObject> numberOfWorkplacesPerZone;
	private String travelDataPath;
	private String zonesPath;
	private double beta_per_minute;
	private double beta, betaTravelTimes, betaLnTravelTimes, betaPowerTravelTimes, betaTravelCosts, betaLnTravelCosts, betaPowerTravelCosts, betaTravelDistance, betaLnTravelDistance, betaPowerTravelDistance;

	/**
	 * constructor
	 * @param zones 
	 */
	MATSim4UrbanSimControlerListenerV2( final ActivityFacilitiesImpl zones, final Map<Id,WorkplaceObject> numberOfWorkplacesPerZone, ActivityFacilitiesImpl facilities, ScenarioImpl scenario ) {
		this.zones = zones;
		this.facilities = facilities;
		this.numberOfWorkplacesPerZone = numberOfWorkplacesPerZone;
		this.travelDataPath = scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.MATSIM_4_OPUS_TEMP_PARAM) + "travel_data.csv";
		this.zonesPath = scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.MATSIM_4_OPUS_TEMP_PARAM) + "zones.csv";
	}
	
	/**
	 *	calculating and dumping the following outcomes:
	 *  - zone2zone impedances including travel times, travel costs and am walk time
	 *  - logsum computation of workplace accessibility
	 */
	public void notifyShutdown(ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." ) ;

		// get the controller and scenario
		Controler controler = event.getControler();
		Scenario sc = controler.getScenario();
		
		initCostfunctionParameter(sc);
		
		// init spannig tree in order to calculate travel times and travel costs
		TravelTime ttc = controler.getTravelTimeCalculator();
		LeastCostPathTree lcptTravelTime = new LeastCostPathTree(ttc,new TravelTimeDistanceCostCalculator(ttc, controler.getConfig().planCalcScore()));
		// tnicolai: calculate distance -> add "single_vehicle_to_work_travel_distance.lf4" to header
		// SpanningTree stTravelDistance = new SpanningTree(ttc, new TravelDistanceCostCalculator(ttc, controler.getConfig().planCalcScore()));
		
		NetworkImpl network = controler.getNetwork() ;
		double depatureTime = 8.*3600 ;	// tnicolai: make configurable
		lcptTravelTime.setDepartureTime(depatureTime);
		// stTravelDistance.setDepartureTime(depatureTime);
		
		// od-trip matrix (zonal based)
		Matrix originDestinationMatrix = new Matrix("tripMatrix", "Zone to Zone origin destination trip matrix");
		
		try {
			BufferedWriter travelDataWriter = initZone2ZoneImpedaceWriter(); // creating zone-to-zone impedance matrix with header
			BufferedWriter zonesWriter = initWorkplaceAccessibilityWriter(); // creating workplace accessibility table with header
			
			computeZoneToZoneTrips(sc, originDestinationMatrix);

			log.info("Computing and writing travel_data ..." );
			
			// initialize accessibility for origin (from) zone
			AccessibilityHelperObject workplaceAccessibility = new AccessibilityHelperObject(this.numberOfWorkplacesPerZone);

			// init array with zone informations
			ZoneInfoObject[] zones = preProcessZoneData(network);
			// init progress bar
			ProgressBar bar = new ProgressBar( zones.length );
			log.info("Processing " + zones.length + " UrbanSim zones ...");

			// main for loop, dumping out zone2zone impedances (travel times) and workplace accessibility in two separate files
			for(int fromZoneIndex = 0; fromZoneIndex < zones.length; fromZoneIndex++){
				
				// progress bar
				bar.update();
				
				// get nearest network node and zone id for origin zone
				Node fromNode = zones[fromZoneIndex].getNearestNode();
				Id originZoneID = zones[fromZoneIndex].getZoneID();
				
				// run dijksrtra for current node as origin
				lcptTravelTime.setOrigin( fromNode );
				lcptTravelTime.run( network );
				
				for(int toZoneIndex = 0; toZoneIndex < zones.length; toZoneIndex++){
					
					// get nearest network node and zone id for destination zone
					Node toNode = zones[toZoneIndex].getNearestNode();
					Id destinationZoneID = zones[toZoneIndex].getZoneID();
					
					// get arrival time
					double arrivalTime = lcptTravelTime.getTree().get( toNode.getId() ).getTime();
					// travel times in minutes (for UrbanSim)
					double travelTime_min = (arrivalTime - depatureTime) / 60.;
					// we guess that any value less than 1.2 leads to errors on the UrbanSim side
					// since ln(0) is not defined or ln(1) = 0 causes trouble as a denominator ...
					if(travelTime_min < 1.2)
						travelTime_min = 1.2;
					
					// double distance_meter = stTravelDistance.getTree().get(toNode.getId()).getCost(); // in meter
					
					// query trips in OD Matrix
					double trips = 0.0;
					Entry e = originDestinationMatrix.getEntry( originZoneID, destinationZoneID );
					if(e != null)
						trips = e.getValue();
					
					travelDataWriter.write ( originZoneID.toString()	//origin zone id
							+ "," + destinationZoneID.toString()		//destination zone id
							+ "," + travelTime_min 						//tcost
							+ "," + travelTime_min 						//ttimes
							+ "," + travelTime_min*10.					//walk ttimes
							+ "," + trips								//vehicle trips
							// + "," + distance_meter					// distance
							);		
					travelDataWriter.newLine();
					
					// from here workplace accessibility computation
					
					// skip workplace accessibility computation if origin and destination zone are equal
					// computation follows below when all other pairs of zones are processed since (total minimum travel time/2) is used for within zone accessibility
					if( originZoneID.compareTo( destinationZoneID ) == 0)
						continue;
					
					workplaceAccessibility.addNextAddend( this.beta_per_minute, destinationZoneID, travelTime_min );
					// yyyy should only be work facilities!!!! kai & thomas, dec'10
				}
				workplaceAccessibility.finalizeLogSum( this.beta_per_minute, originZoneID );
				
				// it is possible to get a negative log sum term (for accessibility < 1)
				zonesWriter.write( originZoneID.toString() + "," +  workplaceAccessibility.getLogSum() ) ;
				zonesWriter.newLine();
				
			}

			// finish progress bar
			System.out.println();
			// flush and close writers
			travelDataWriter.flush();
			travelDataWriter.close();
			log.info("... done with writing travel_data.csv" );
			zonesWriter.flush();
			zonesWriter.flush();
			log.info("... done with writing zones.csv" );
			
			System.out.println(" ... done");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.info("... done with notifyShutdown.") ;
	}

	/**
	 * Initializing an array with zone information for computing 
	 * 2-point accessibilities (from zone / to zone) 
	 * @param network
	 */
	private ZoneInfoObject[] preProcessZoneData(NetworkImpl network) {
		
		assert( network != null );
		int numberOfZones = zones.getFacilities().values().size();
		ZoneInfoObject zoneArray[] = new ZoneInfoObject[numberOfZones];
		Iterator<ActivityFacility> zonesIterator = zones.getFacilities().values().iterator();

		int counter = 0;
		while( zonesIterator.hasNext() ){

			ActivityFacility zone = zonesIterator.next();
			assert (zone != null );
			assert( zone.getCoord() != null );
			Coord zoneCoordinate = zone.getCoord();
			Node networkNode = network.getNearestNode( zoneCoordinate );
			assert( networkNode != null );
			
			if(counter >= numberOfZones){
				log.error("Error while generating array of \"ZoneInfoObject\" ...");
				System.exit(-1);
			}
				
			zoneArray[counter] = new ZoneInfoObject(zone.getId(), zoneCoordinate, networkNode);
			counter++;
		}
		return zoneArray;
	}

	/**
	 * Returns a BufferedWriter writing accessibility measures for each zone
	 * 
	 * @return BufferedWriter
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private BufferedWriter initWorkplaceAccessibilityWriter()
			throws FileNotFoundException, IOException {
		BufferedWriter zonesWriter = IOUtils.getBufferedWriter( zonesPath );
		
		// Zone Header (workplace accessibility)
		zonesWriter.write( "zone_id:i4,workplace_accessibility:f4") ;
		zonesWriter.newLine();
		return zonesWriter;
	}

	/**
	 * Returns a BufferedWriter writing a zone-to-zone impedance matrix for UrbanSim
	 * 
	 * @return BufferedWriter
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private BufferedWriter initZone2ZoneImpedaceWriter()
			throws FileNotFoundException, IOException {
		BufferedWriter travelDataWriter = IOUtils.getBufferedWriter( travelDataPath );
		
		// Travel Data Header
		travelDataWriter.write ( "from_zone_id:i4,to_zone_id:i4,single_vehicle_to_work_travel_cost:f4,am_single_vehicle_to_work_travel_time:f4,am_walk_time_in_minutes:f4,am_pk_period_drive_alone_vehicle_trips:f4" ) ; 
		// tnicolai: add single_vehicle_to_work_travel_distance:f4 for distance output ...
		Logger.getLogger(this.getClass()).error( "add new fields" ) ; // remove when all travel data attributes are updated...
		travelDataWriter.newLine();
		return travelDataWriter;
	}
	
	/**
	 * goes through all person ant their plans and stores their trips into a matrix
	 * 
	 * @param sc
	 * @param originDestinationMatrix
	 */
	private void computeZoneToZoneTrips(Scenario sc, Matrix originDestinationMatrix){
		log.info("Computing zone2zone trip numbers ...") ;
		// yyyy might make even more sense to do this via events.  kai, feb'11
		Entry matrixEntry = null;
			
		for ( Person person : sc.getPopulation().getPersons().values() ) {
			
			Plan plan = person.getSelectedPlan() ;
			
			boolean isFirstPlanActivity = true;
			String lastZoneId = null;
			
			if(plan.getPlanElements().size() <= 1) // check if activities available (then size of plan elements > 1)
				continue;
			
			for ( PlanElement pe : plan.getPlanElements() ) {
				if ( pe instanceof Activity ) {
					Activity act = (Activity) pe;
					Id id = act.getFacilityId();
					if( id == null) // that person plan doesn't contain any activity, continue with next person
						continue;
					
					Map<Id, ActivityFacility> allFacilities = facilities.getFacilities();
					ActivityFacility fac = allFacilities.get(id);
					if(fac == null)
						continue;
					String zone_ID = ((Id) fac.getCustomAttributes().get(Constants.ZONE_ID)).toString() ;

					if (isFirstPlanActivity)
						isFirstPlanActivity = false; 
					else {
						matrixEntry = originDestinationMatrix.getEntry(new IdImpl(lastZoneId), new IdImpl(zone_ID));
						if(matrixEntry != null){
							double value = matrixEntry.getValue();
							originDestinationMatrix.setEntry(new IdImpl(lastZoneId), new IdImpl(zone_ID), value+1.);
						}
						else	
							originDestinationMatrix.createEntry(new IdImpl(lastZoneId), new IdImpl(zone_ID), 1.);
							// see PtPlanToPlanStepBasedOnEvents.addPersonToVehicleContainer (or zone coordinate addition)
					}
					lastZoneId = zone_ID; // stores the first activity (e. g. "home")
				}
			}
				
		}
		// tnicolai: debugging
//		for(Id fromId : originDestinationMatrix.getFromLocations().keySet()){
//			System.out.println("From Zone: " + fromId.toString());
//			for(Entry e : originDestinationMatrix.getFromLocEntries(fromId)){
//				System.out.println("To Zone: " + e.getToLocation() + " value = " + e.getValue());
//			}
//		}
//		
//		for(Id ToId : originDestinationMatrix.getToLocations().keySet()){
//			System.out.println("To Zone: " + ToId.toString());
//			for(Entry e : originDestinationMatrix.getToLocEntries(ToId)){
//				System.out.println("From Zone: " + e.getFromLocation() + " value = " + e.getValue());
//			}
//		}
		
		log.info("DONE with computing zone2zone trip numbers ...") ;
	}
	
	/**
	 * Experimental
	 * initialize betas for logsum cost function
	 * 
	 * @param scenario
	 */
	private void initCostfunctionParameter(Scenario scenario){
		
		// beta per hr should be -12 (by default configuration)
		double beta_per_hr = scenario.getConfig().planCalcScore().getTraveling_utils_hr() - scenario.getConfig().planCalcScore().getPerforming_utils_hr() ;
		this.beta_per_minute = beta_per_hr / 60.; // get utility per second
		
		try{
			beta = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA) );
			betaTravelTimes = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_TRAVEL_TIMES) );
			betaLnTravelTimes = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_LN_TRAVEL_TIMES) );
			betaPowerTravelTimes = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_POWER_TRAVEL_TIMES) );
			betaTravelCosts = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_TRAVEL_COSTS) );
			betaLnTravelCosts = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_LN_TRAVEL_COSTS) );
			betaPowerTravelCosts = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_POWER_TRAVEL_COSTS) );
			betaTravelDistance = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_TRAVEL_DISTANCE) );
			betaLnTravelDistance = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_LN_TRAVEL_DISTANCE) );
			betaPowerTravelDistance = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.BETA_POWER_TRAVEL_DISTANCE) );
		}
		catch(NumberFormatException e){
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	// From here Travel Distance Calculator
	
	class TravelDistanceCostCalculator implements TravelMinCost {

		protected final TravelTime timeCalculator;
		private final double marginalCostOfDistance;

		public TravelDistanceCostCalculator(final TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
			this.timeCalculator = timeCalculator;
			/* Usually, the travel-utility should be negative (it's a disutility)
			 * but the cost should be positive. Thus negate the utility.
			 */
			if( (- cnScoringGroup.getMonetaryDistanceCostRateCar() * cnScoringGroup.getMarginalUtilityOfMoney()) == 0)
				this.marginalCostOfDistance = 1.; // tnicolai: fix this -> monetaryDistanceCostRateCar == 0
			else
				this.marginalCostOfDistance = - cnScoringGroup.getMonetaryDistanceCostRateCar() * cnScoringGroup.getMarginalUtilityOfMoney();
			System.out.println(this.marginalCostOfDistance);
		}

		@Override
		public double getLinkGeneralizedTravelCost(final Link link, final double time) {
			
			return this.marginalCostOfDistance * link.getLength(); // link length in meter
		}

		@Override
		public double getLinkMinimumTravelCost(final Link link) {
			return this.marginalCostOfDistance * link.getLength(); // link length in meter
		}
	} 

}
