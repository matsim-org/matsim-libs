/* *********************************************************************** *
 * project: org.matsim.*
 * PrimlocModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.fabrice.primloc;

/**
 * This Module adapts the MATSIM-independent Primary Location Choice Model (PrimlocEngine),
 * to the MATSIM Plans, Population, Persons, Layers, etc.
 *
 * This Module modifies the Person by adding know Facilities to the Knowledge of the Person
 *
 * It solves primary location choice with capacity constraints (limited number of facilities)
 *
 * The method is described in : F. Marchal (2005), "A trip generation method for time-dependent
 * large-scale simulations of transport and land-use", Networks and Spatial Economics 5(2),
 * Kluwer Academic Publishers, 179-192.
 *
 * The CumulativeDistribution is in the same unit as the O-D Travel penalties
 * so if the input is travel time in minutes -> trip distribution as a function of time in minutes.
 *
 * There are some config files and tests with some verbosity in
 * test/input/org/matsim/demandmodeling/primloc
 *
 * you can use the module without any distribution but then you cannot
 * calibrate it automatically against anything. If you need , say a rough start, you can :
 * - use free flow travel times between O-D as a proxy for travel costs
 * - put a "mu" value of the same order of magnitude as the average travel cost
 *
 * ... this makes sure that exp(-cost / mu) doesnt go over the edge
 *
 * ... eventually you get a trip matrix which might make sense or not^^
 *
 * I think (not sure), Waddell/de Palma/Picard have even generalised the problem and
 * there maybe even a python code in urbansim to do that also ...
 *
 * @author Fabrice Marchal
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.world.Layer;
import org.matsim.world.World;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

import Jama.Matrix;

public class PrimlocModule extends AbstractPersonAlgorithm {

	private final static String module_name = "primary location choice";

	private PrimlocCore core = new PrimlocCore();  // Core of the module, independent of MATSIM plans
	private String primaryActivityName; // activity to be considered in trips enumeration

	// Agreggation layer
	private Zone[] zones;
	private Layer zoneLayer;
	private HashMap<Zone,Integer> zoneids = new HashMap<Zone,Integer>();
	private HashMap<Zone, ArrayList<ActivityFacilityImpl>> primActFacilitiesPerZone =
		new HashMap<Zone, ArrayList<ActivityFacilityImpl>>();

	// Class responsible for the computation of Travel_cost(Zone #i, Zone #j)
	private PrimlocTravelCostAggregator travelCostAggregator;

	// Class responsible for the computation of the calibration error/fitness
	// i.e. comparison with real external data
	private PrimlocCalibrationError errorCalibrationClass;

	// Class containing the external trip distribution against
	// which the calibration is performed
	CumulativeDistribution externalTripDist;

	// Options
	private boolean overwriteKnowledge; // toggle knowledge creation/modification
	private boolean calibration; // toggle calibration/simple simulation
	private boolean unspecifiedMu; // if starting mu is not specified

	// Utility classes
	private final static Logger log = Logger.getLogger(PrimlocModule.class);
	private Random random;

	private final Config cfg;

	private Knowledges knowledges;

	public PrimlocModule (Config config, Knowledges knowledges) {
		this.cfg = config;
		this.knowledges = knowledges;
	}

	@Override
	public void run(Person guy){

		// Modify the plans of the agents accordingly to the Primloc model
		// that was run in setup()
		//
		// For each agent performing the primary activity at least once:
		// increase the knowledge of the agent by one Facility

		if( !agentHasPrimaryActivityInPlan( guy ) )
			return;

		KnowledgeImpl knowledge = this.knowledges.getKnowledgesByPersonId().get(guy.getId());
		ActivityFacilityImpl home = knowledge.getActivities("home").get(0).getFacility();
		Zone homezone = (Zone) zoneLayer.getNearestLocations( home.getCoord(), null).get(0);
		if( homezone == null )
			log.warn("Homeless person (poor guy)" );
		else{
			int homeZoneID = zoneids.get(homezone);

			// Compute a random work zone using the trip matrix as a
			// probability distribution, with home being conditional
			double epsilon = MatsimRandom.getRandom().nextDouble();
			double cumul=0.0;
			int workZoneID = 0;
			while( (cumul < epsilon) && (workZoneID<core.numZ-1) ){
				cumul += (core.trips.get(homeZoneID, workZoneID))/core.P[homeZoneID];
				workZoneID++;
			}

			// Assign a link location corresponding to the workplace
			ArrayList<ActivityFacilityImpl> workplaces = primActFacilitiesPerZone.get( zones[workZoneID] );
			while( workplaces.size() == 0 ){
				// This can happen if a person has a job in a zone without
				// any job facility because of the hack in normalizeJobHomeVectors().
				// Hack: it is then reassigned to a random zone
				int zid = (int)(random.nextDouble()*core.numZ);
				workplaces = primActFacilitiesPerZone.get( zones[zid] );
			}
			int wid = (int)(random.nextDouble()*workplaces.size());
			ActivityFacilityImpl workplace = workplaces.get(wid);

			// Change the knowledge of the person
			if( overwriteKnowledge )
				knowledge.removeActivities( primaryActivityName );
			knowledge.addActivityOption(new ActivityOptionImpl(primaryActivityName, workplace), true);
		}
	}

	public void setup( World world, Population population, ActivityFacilities facilities ){

		random = new Random(this.cfg.global().getRandomSeed());

		setupParameters();

		setupAggregationLayer(world);

		setupTravelCosts();

		setupNumberHomesPerZone( population );

		setupNumberJobsPerZone(facilities);

		normalizeJobHomeVectors();

		setupCalibrationData();

		// Run the core location choice model
		if( calibration )
			core.runCalibrationProcess();
		else
			core.runModel();

	}

	public void setAggregationLayer( Layer zoneLayer ){
		if( ! (zoneLayer instanceof ZoneLayer) )
			Gbl.errorMsg( new Exception("PrimLocChoice_MATSIM needs a Zone Layer") );
		this.zoneLayer = zoneLayer;
	}

	public void setTravelCost( PrimlocTravelCostAggregator travelCostAggregator ){
		this.travelCostAggregator = travelCostAggregator;
	}

	public void setExternalTripDistribution( CumulativeDistribution tripDist ){
		externalTripDist = tripDist;
	}

	private void setupParameters(){
		// Fetch parameters from the config file

		primaryActivityName = cfg.getParam( module_name, "primary activity");
		overwriteKnowledge = Boolean.parseBoolean( cfg.getParam(module_name, "overwrite knowledge"));
		calibration = Boolean.parseBoolean( cfg.getParam(module_name, "calibration"));

		String muString = cfg.findParam(module_name, "mu");
		unspecifiedMu = ( muString == null );
		if( !unspecifiedMu )
			core.mu = Double.parseDouble( muString );
		core.theta = Double.parseDouble( cfg.getParam(module_name, "theta") );
		core.threshold1 = Double.parseDouble( cfg.getParam(module_name, "threshold1") );
		core.threshold2 = Double.parseDouble( cfg.getParam(module_name, "threshold2") );
		core.threshold3 = Double.parseDouble( cfg.getParam(module_name, "threshold3") );
		core.maxiter = Integer.parseInt( cfg.getParam(module_name, "maxiter") );
		core.verbose = Boolean.parseBoolean(cfg.getParam(module_name, "verbose"));

	}

	private void setupAggregationLayer(World world){
		// Check / load the aggregation layer
		if( zoneLayer == null ){
			String layerName = cfg.findParam( module_name, "aggregation layer");
			if( layerName == null )
				Gbl.errorMsg( new Exception("PrimLocChoice_MATSIM needs an aggregation layer" ) );
			zoneLayer = world.getLayer( layerName );
			if( ! (zoneLayer instanceof ZoneLayer) )
				Gbl.errorMsg( new Exception("PrimLocChoice_MATSIM needs a Zone Layer") );
		}

		// We store the zones in a given way so that
		// we do not rely on the order in the collection
		Collection<? extends BasicLocation> listloc = zoneLayer.getLocations().values();
		int internalID=0;
		core.numZ = listloc.size();
		zones = new Zone[ core.numZ ];
		for(  Object obj : listloc ){
			Zone zone = (Zone) obj;
			zoneids.put( zone, internalID );
			zones[ internalID ] = zone;
			primActFacilitiesPerZone.put( zone, new ArrayList<ActivityFacilityImpl>() );
			internalID++;
		}
	}

	private void setupTravelCosts(){
		String distParam = cfg.findParam(module_name, "euclidean distance costs");
		if( distParam != null ){
			if( Boolean.parseBoolean( distParam ) )
				setEuclideanDistanceImpedances();
			else if( travelCostAggregator == null )
				Gbl.errorMsg( new Exception("PrimLocChoice_MATSIM needs a Travel costs aggregator or euclidean distance costs enabled") );
		}
		else if( travelCostAggregator == null )
				Gbl.errorMsg( new Exception("PrimLocChoice_MATSIM needs a Travel costs aggregator or euclidean distance costs enabled") );


		// The following is optional but will allow to calibrate against
		// a given trip distribution if needed
		core.setupCostStatistics();

		if( unspecifiedMu ){
			core.mu = core.avgCost;
			if( core.verbose )
				System.out.println("Setting mu = <cost> = "+core.avgCost);
		}

	}

	private void setupNumberHomesPerZone( Population population ){
		// Setup the number of originating trips.
		// In this case it corresponds to the number of Persons which will
		// make at least one trip with activity type = primaryActivityName
		// We assume that agents have plans that contains at least the agenda of
		// their activities

		core.P = new double[ core.numZ ];
		// Determine how many employed persons live in each zone
		for (Person guy : population.getPersons().values())
			if( agentHasPrimaryActivityInPlan( guy ) ){
				ActivityFacilityImpl homeOfGuy = this.knowledges.getKnowledgesByPersonId().get(guy.getId()).getActivities("home").get(0).getFacility();
				ArrayList<? extends BasicLocation> list = zoneLayer.getNearestLocations(homeOfGuy.getCoord(), null);
				Zone homezone = (Zone) list.get(0);
				if( homezone == null )
					log.warn("Homeless employed person (poor guy)" );
				else
					core.P[ zoneids.get(homezone) ]++;
			}
	}

	private boolean agentHasPrimaryActivityInPlan(Person guy) {
		for (Plan plan : guy.getPlans()) {
			for (PlanElement pe : plan.getPlanElements()) {
				if ((pe instanceof Activity) && (((Activity) pe).getType().equals(primaryActivityName))) {
					return true;
				}
			}
		}
		return false;
	}

	private void setupNumberJobsPerZone(ActivityFacilities facilities){
		// Setup the number of available facilities at the destination of the trips
		// In this case we take the capacities of the existing Facilities
		// and maintain a list of Facilities per Zone
		core.J = new double[ core.numZ ];
		for( ActivityFacility facility : facilities.getFacilities().values() ){
			ActivityOptionImpl act = (ActivityOptionImpl) facility.getActivityOptions().get(primaryActivityName);
			if( act != null ){
				ArrayList<? extends BasicLocation> list = zoneLayer.getNearestLocations( facility.getCoord(), null);
				Zone zone = (Zone) list.get(0);
				core.J[ zoneids.get(zone) ] += act.getCapacity();
				primActFacilitiesPerZone.get( zone ).add( (ActivityFacilityImpl) facility );
			}
		}
	}

	private void normalizeJobHomeVectors(){
		// Hack to ensure that no element is zero (singular matrix)
		core.N = 0.0;
		for( int i=0; i<core.numZ; i++){
			core.P[i] = Math.max(1.0, core.P[i]);
			core.J[i] = Math.max(1.0, core.J[i]);
			core.N += core.P[i];
		}
		System.out.println("# employed "+core.N);
		core.normalizeJobVector();
		System.out.println("Zone attribute vector:");
		for( int i=0; i<core.numZ; i++){
			System.out.println( "Zone #"+zones[i].getId()+
					"\t#residents: "+core.df.format(core.P[i]) +
					"\t#"+primaryActivityName+": "+core.df.format(core.J[i]));
		}
	}



	private void setEuclideanDistanceImpedances(){
		// Compute a simple Travel Cost matrix
		// based on the euclidean distance between centroids
		core.cij = new Matrix( core.numZ, core.numZ );

		for( int i=0; i<core.numZ; i++){
			for( int j=0; j<core.numZ; j++)
				core.cij.set( i, j, zones[i].calcDistance( zones[j].getCoord() ) );

			// The method requires cii > 0 therefore we set
			// cii = length of the diagonal of the bounding box of Zone #i

			core.cij.set(i, i, (CoordUtils.calcDistance(zones[i].getMax(), zones[i].getMin()))/2 );

			if( core.cij.get( i, i ) == 0.0 )
				Gbl.errorMsg( new Exception("PrimLocChoice_core requires Cii>0 for intrazonal travel costs"));

		}
	}

	private void setupCalibrationData(){
		// This example simply illustrate how to calibrate against
		// a given trip distribution

		if( errorCalibrationClass == null ){
			if( externalTripDist == null )
				return;
			else{
				errorCalibrationClass = new PrimlocTripDistributionError( externalTripDist, core.cij );
			}
		}
		core.setCalibrationError( errorCalibrationClass );
	}

}
