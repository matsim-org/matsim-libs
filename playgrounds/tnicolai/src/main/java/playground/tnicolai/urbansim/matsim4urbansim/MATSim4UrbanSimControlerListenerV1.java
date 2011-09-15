package playground.tnicolai.urbansim.matsim4urbansim;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.population.algorithms.PersonPrepareForSim;

import playground.tnicolai.urbansim.constants.Constants;
import playground.tnicolai.urbansim.utils.CommonMATSimUtilities;
import playground.tnicolai.urbansim.utils.helperObjects.WorkplaceObject;
import playground.toronto.ttimematrix.LeastCostPathTree;

/**
 *
 * @author nagel
 * @author thomas
 *
 */
public class MATSim4UrbanSimControlerListenerV1 implements ShutdownListener {
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimControlerListenerV1.class);

	private ActivityFacilitiesImpl zones;
	private ActivityFacilitiesImpl facilities;
	private Map<Id,WorkplaceObject> numberOfWorkplacesPerZone;
	private String travelDataPath;
	private String zonesPath;
	private double beta, betaTravelTimes, betaLnTravelTimes, betaPowerTravelTimes, betaTravelCosts, betaLnTravelCosts, betaPowerTravelCosts, betaTravelDistance, betaLnTravelDistance, betaPowerTravelDistance;

	/**
	 * constructor
	 * @param zones 
	 */
	public MATSim4UrbanSimControlerListenerV1( final ActivityFacilitiesImpl zones, final Map<Id,WorkplaceObject> numberOfWorkplacesPerZone, ActivityFacilitiesImpl facilities, ScenarioImpl scenario ) {
		this.zones = zones;
		this.facilities = facilities;
		this.numberOfWorkplacesPerZone = numberOfWorkplacesPerZone;
		this.travelDataPath = scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM, Constants.MATSIM_4_OPUS_TEMP_DIRECTORY) + "travel_data.csv";
		this.zonesPath = scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM, Constants.MATSIM_4_OPUS_TEMP_DIRECTORY) + "zones.csv";
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

		TravelTime ttc = controler.getTravelTimeCalculator();
		LeastCostPathTree st = new LeastCostPathTree(ttc,new TravelTimeDistanceCostCalculator(ttc, controler.getConfig().planCalcScore()));

		NetworkImpl network = controler.getNetwork() ;
		double depatureTime = 8.*3600 ;
		st.setDepartureTime(depatureTime);
		
		// od-trip matrix (zonal based)
		Matrix originDestinationMatrix = new Matrix("tripMatrix", "Zone to Zone origin destination trip matrix");
		
		try {
			BufferedWriter travelDataWriter = initZone2ZoneImpedaceWriter();
			BufferedWriter zonesWriter = initWorkplaceAccessibilityWriter();
			
			computeZoneToZoneTrips(sc, originDestinationMatrix);

			log.info("Computing and writing travel_data" );
			// log.warn("Can't feed floats to urbansim; am thus feeding ints for the ttime.") ;
			// solved 3dec08 by travis
			
			// Progress bar
			System.out.println("|--------------------------------------------------------------------------------------------------|") ;
			long cnt = 0; 
			long percentDone = 0;
			
			// main for loop, dumping out zone2zone impedances (travel times) and workplace accessibility in two separate files
			for ( ActivityFacility fromZone : zones.getFacilities().values() ) {
				// progress bar
				if ( (int) (100.*cnt/zones.getFacilities().size()) > percentDone ) {
					percentDone++ ; System.out.print('=') ;
				}
				cnt++;

				// running through network from given origin (from) zone
				Coord coord = fromZone.getCoord();
				assert( coord != null );
				Node fromNode = network.getNearestNode( coord );
				assert( fromNode != null );
				st.setOrigin( fromNode );
				st.run(network);
				
				// initialize accessibility for origin (from) zone
				double accessibility 	= 0.;

				// beta per hr should be -12 (by default configuration
				double beta_per_hr = sc.getConfig().planCalcScore().getTraveling_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr() ;
				double beta 	   = beta_per_hr/3600.; // get utility per second
				
				double minTravelTime 	= Double.MAX_VALUE;

				for ( ActivityFacility toZone : zones.getFacilities().values() ) {
					
					Coord toCoord = toZone.getCoord();
					Node toNode = network.getNearestNode( toCoord );
					assert( toNode != null );
					
					double arrivalTime = st.getTree().get(toNode.getId()).getTime();
					// travel times in sec
					double ttime = arrivalTime - depatureTime;
					// convert travel times in minutes for urbansim
					ttime = ttime / 60.;
					// we guess that any value less than 1.2 leads to errors on the urbansim side
					// since ln(0) is not defined or ln(1) = 0 causes trouble as a denominator ...
					if(ttime < 1.2)
						ttime = 1.2;
					
					// query trips in OD Matrix
					double trips = 0.0;
					Entry e = originDestinationMatrix.getEntry(fromZone.getId(), toZone.getId());
					if(e != null)
						trips = e.getValue();
					
					// tnicolai test to calculate travel costs
					// LinkImpl toLink = network.getNearestLink( toCoord );
					// double tcost = st.getTravelCostCalulator().getLinkGeneralizedTravelCost(toLink, depatureTime); // .getLinkTravelCost(toLink, depatureTime);
					
					// tnicolai: add "single_vehicle_to_work_travel_distance.lf4" to header
//					double distance = getZone2ZoneDistance(fromZone, toZone, network, controler);
//					double meterInMilesFaktor = 0.000621371; // use this to convert distance in meter to distance in miles.
//					System.out.println("Distance from zone" + fromZone.getId() + " to zone " + toZone.getId() + " in meter : " + distance + " in miles: " + distance*meterInMilesFaktor);
					
					travelDataWriter.write ( fromZone.getId().toString()	//origin zone id
							+ "," + toZone.getId().toString()				//destination zone id
							+ "," + ttime 									//tcost
							+ "," + ttime 									//ttimes
							+ "," + ttime*10.								//walk ttimes
							+ "," + trips									//vehicle trips
							);		
					travelDataWriter.newLine();
					
					// from here workplace accessibility computation
					
					// skip workplace accessibility computation if origin and destination zone are equal
					// computation of this case follows below on same zone computation
					if(fromZone.getId().compareTo(toZone.getId()) == 0)
						continue;
					
					// get minimum travel time for in zone accessibility (see below)
					minTravelTime = Math.min(ttime, minTravelTime);
					
					// this sum corresponds to the sum term of the log sum computation
					if(numberOfWorkplacesPerZone.get(toZone.getId()) != null){ // skipping zones no workplaces
						long weight = numberOfWorkplacesPerZone.get(toZone.getId()).counter;
						double costFunction = Math.exp( beta * ttime ); // tnicolai: implement cost function as: Math.exp ( alpha * traveltime + beta * traveltime**2 + gamma * ln(traveltime) + delta * distance + epsilon * distance**2 + phi * ln(distance) ) 
						accessibility += weight * costFunction;
					}
					// yyyy should only be work facilities!!!! kai & thomas, dec'10
				}
				// add in zone accessibility (same zone computation)
				if(numberOfWorkplacesPerZone.get(fromZone.getId()) != null){ // skipping zones no workplaces
					long weight = numberOfWorkplacesPerZone.get(fromZone.getId()).counter;
					double costFunction = Math.exp( beta * (minTravelTime / 2) ); // tnicolai : see above computation of cost function ...
					accessibility += weight * costFunction;
				}
				
				// it is possible to get a negative log sum term (for accessibility < 1)
				zonesWriter.write( fromZone.getId().toString() + "," +  Math.log( accessibility ) ) ;
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
		Logger.getLogger(this.getClass()).error( "add new fields" ) ; // remove when all travel data attributes are updated...
		travelDataWriter.newLine();
		return travelDataWriter;
	}
	
	
	/**
	 * Calculates the travel distance (in meters) between an origin and destination zone on the traffic network.
	 * 
	 * @param fromZone
	 * @param toZone
	 * @param network
	 * @param controler
	 * @return distance between 2 zones in meter
	 */
	private double getZone2ZoneDistance(ActivityFacility fromZone, ActivityFacility toZone, NetworkImpl network, Controler controler){
		
		double distance = 0.0; // tnicolai: should we take another default value???
		
		PersonImpl dummyPerson = new PersonImpl( fromZone.getId() );
		PlanImpl plan = dummyPerson.createAndAddPlan(true);
		CommonMATSimUtilities.makeHomePlan(plan, fromZone.getCoord(), new ActivityFacilitiesImpl("centroid_zone_"+fromZone.getId()).createFacility(fromZone.getId(), fromZone.getCoord()));
		CommonMATSimUtilities.completePlanToHwh(plan, toZone.getCoord(), new ActivityFacilitiesImpl("centroid_zone_"+toZone.getId()).createFacility(toZone.getId(), toZone.getCoord()));
		
		PersonPrepareForSim pps = new PersonPrepareForSim( controler.createRoutingAlgorithm() , network);
		pps.run(dummyPerson);
		
		if( plan.getPlanElements().size() >= 2 ){
			
			// get first leg. this contains the route from "fromZone" to "toZone"
			PlanElement pe = plan.getPlanElements().get(1);
			if (pe instanceof LegImpl) {
				LegImpl l = (LegImpl) pe;
				
				LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) l.getRoute();

				if(route.getLinkIds().size()  > 0){
					NetworkRoute nr = RouteUtils.createNetworkRoute(route.getLinkIds(), network);
					distance = RouteUtils.calcDistance(nr, network);
				}
			}
		}
		return distance;
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

//			if( ((PersonImpl)person).isEmployed())
//				System.out.println("isEmployed");
			
			boolean isFirstPlanActivity = true;
			String lastZoneId = null;
			
			if(plan.getPlanElements().size() <= 1)
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
					
					
//					Map<Id, ActivityFacility> allFacilities = ((ScenarioImpl)sc).getActivityFacilities().getFacilities();
//					
//					ActivityFacility fac = allFacilities.get(id); // tnicolai: fac == null !!!
////					ActivityFacility fac = ((ScenarioImpl)sc).getActivityFacilities().getFacilities().get( act.getFacilityId() ) ;
//					if(fac == null)
//						continue;
//					
//					String zone_ID = ((Id) fac.getCustomAttributes().get(Constants.ZONE_ID)).toString() ;
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
					lastZoneId = zone_ID ;
				}
			}
				
		}
		// tnicolai: for debugging
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
	 * initialize betas for logsum cost function 
	 * 
	 * @param scenario
	 */
	private void initCostfunctionParameter(Scenario scenario){
		
		try{
			beta = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM, Constants.BETA) );
			betaTravelTimes = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM, Constants.BETA_TRAVEL_TIMES) );
			betaLnTravelTimes = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM, Constants.BETA_LN_TRAVEL_TIMES) );
			betaPowerTravelTimes = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM, Constants.BETA_POWER_TRAVEL_TIMES) );
			betaTravelCosts = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM, Constants.BETA_TRAVEL_COSTS) );
			betaLnTravelCosts = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM, Constants.BETA_LN_TRAVEL_COSTS) );
			betaPowerTravelCosts = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM, Constants.BETA_POWER_TRAVEL_COSTS) );
			betaTravelDistance = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM, Constants.BETA_TRAVEL_DISTANCE) );
			betaLnTravelDistance = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM, Constants.BETA_LN_TRAVEL_DISTANCE) );
			betaPowerTravelDistance = Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM, Constants.BETA_POWER_TRAVEL_DISTANCE) );
		}
		catch(NumberFormatException e){
			e.printStackTrace();
			System.exit(-1);
		}
		
	}

}
