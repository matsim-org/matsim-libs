/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.tnicolai.matsim4opus.matsim4urbansim;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
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
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelWalkTimeCostCalculator;
import playground.tnicolai.matsim4opus.utils.ProgressBar;
import playground.tnicolai.matsim4opus.utils.UtilityCollection;
import playground.tnicolai.matsim4opus.utils.helperObjects.ZoneObject;

/**
 * This controller version is designed for the sustaincity mile stone (Month 18).
 * 
 * @author nagel
 * @author thomas
 *
 */
public class Zone2ZoneImpedancesControlerListener implements ShutdownListener {
	private static final Logger log = Logger.getLogger(Zone2ZoneImpedancesControlerListener.class);

	private ActivityFacilitiesImpl zones;
	private ActivityFacilitiesImpl parcels;
	private String travelDataPath;

	/**
	 * constructor	
	 * @param zones 
	 * @param parcels
	 */
	Zone2ZoneImpedancesControlerListener( final ActivityFacilitiesImpl zones, ActivityFacilitiesImpl parcels) {
		this.zones = zones;
		this.parcels = parcels;
		this.travelDataPath = Constants.MATSIM_4_OPUS_TEMP + Constants.TRAVEL_DATA_FILE_CSV;
	}
	
	/**
	 *	calculating and dumping the following outcomes:
	 *  - zone2zone impedances including travel times, travel costs and am walk time
	 *  - logsum computation of workplace accessibility
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		log.info("Entering notifyShutdown ..." ) ;

		// get the controller and scenario
		Controler controler = event.getControler();
		Scenario sc = controler.getScenario();

		// init least cost path tree in order to calculate travel times and travel costs
		TravelTime ttc = controler.getTravelTimeCalculator();
		LeastCostPathTree lcptTravelTime = new LeastCostPathTree(ttc,new TravelTimeDistanceCostCalculator(ttc, controler.getConfig().planCalcScore()));
		// tnicolai: calculate distance -> add "single_vehicle_to_work_travel_distance.lf4" to header
		LeastCostPathTree lcptTravelDistance = new LeastCostPathTree(ttc, new TravelWalkTimeCostCalculator()); // tnicolai: check with kai
		
		NetworkImpl network = (NetworkImpl) controler.getNetwork() ;
		double depatureTime = 8.*3600 ;	// tnicolai: make configurable
		
		// od-trip matrix (zonal based)
		Matrix originDestinationMatrix = new Matrix("tripMatrix", "Zone to Zone origin destination trip matrix");
		
		try {
			BufferedWriter travelDataWriter = initZone2ZoneImpedaceWriter(); // creating zone-to-zone impedance matrix with header
			
			computeZoneToZoneTrips(sc, originDestinationMatrix);

			log.info("Computing and writing zone2zone impedance matrix ..." );

			// init array with zone informations
			ZoneObject[] zones = UtilityCollection.assertZoneCentroid2NearestNode(this.zones, network);
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
				lcptTravelTime.calculate(network, fromNode, depatureTime);
				lcptTravelDistance.calculate(network, fromNode, depatureTime);
				
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
					
					// get travel cost (marginal cost of time * travel time)
					double travelCost = lcptTravelTime.getTree().get( toNode.getId() ).getCost();
					// get travel distance (link lengths in meter)
					double distance_meter = lcptTravelDistance.getTree().get( toNode.getId() ).getCost();
					
					// query trips in OD Matrix
					double trips = 0.0;
					Entry e = originDestinationMatrix.getEntry( originZoneID, destinationZoneID );
					if(e != null)
						trips = e.getValue();
					
					// IMPORTANT: Do adapt the travel_data header in "initZone2ZoneImpedaceWriter"
					// 			  when changing anything at this call.
					travelDataWriter.write ( originZoneID.toString()			//origin zone id
										+ "," + destinationZoneID.toString()	//destination zone id
										+ "," + travelCost //travelCost 		//tcost
										+ "," + travelTime_min 					//ttimes
										+ "," + travelTime_min*10.				//walk ttimes
										+ "," + trips							//vehicle trips
										+ "," + distance_meter					// distance
					);		
					travelDataWriter.newLine();
				}
			}

			// finish progress bar
			System.out.println();
			// flush and close writers
			travelDataWriter.flush();
			travelDataWriter.close();
			log.info("... done with writing travel_data.csv" );
			
			System.out.println(" ... done");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.info("... done with notifyShutdown.") ;
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
		// tnicolai: add single_vehicle_to_work_travel_distance:f4 for distance output
		travelDataWriter.write ( "from_zone_id:i4,to_zone_id:i4,single_vehicle_to_work_travel_cost:f4,am_single_vehicle_to_work_travel_time:f4,am_walk_time_in_minutes:f4,am_pk_period_drive_alone_vehicle_trips:f4:single_vehicle_to_work_travel_distance:f4" ) ; 
		
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
					
					Map<Id, ActivityFacility> allFacilities = parcels.getFacilities();
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
}
