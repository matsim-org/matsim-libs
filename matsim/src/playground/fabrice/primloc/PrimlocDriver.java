/* *********************************************************************** *
 * project: org.matsim.*
 * PrimlocDriver.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PlansAlgorithm;
import org.matsim.utils.identifiers.IdI;
import org.matsim.world.Layer;
import org.matsim.world.Location;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

import Jama.Matrix;

public class PrimlocDriver  extends PlansAlgorithm {

	final static String module_name = "primary location choice";
	
	private final static Logger log = Logger.getLogger(PrimlocDriver.class);

	PrimlocEngine core = new PrimlocEngine();
	Zone[] zones;
	Layer zonelayer;
	HashMap<Zone,Integer> zoneids = new HashMap<Zone,Integer>();
	HashMap<Zone, ArrayList<Facility>> primActFacilitiesPerZone =
		new HashMap<Zone, ArrayList<Facility>>();
	String primaryActivityName;

	@Override
	public void run(Plans plans) {

		// Run the core location choice model
		if( core.calibration )
			core.calibrationProcess();
		else
			core.runModel();

		// Modify the plans of the agents accordingly
		// For each agent: replace the location of the primary
		// activity in the selected plan

		modifyPlans( plans );
	}

	void modifyPlans( Plans plans ){
		String primaryActivityName = Gbl.getConfig().getParam( module_name, "primary activity");
		for( Person guy : plans.getPersons().values() ){
		// old code:
//			ActivityFacilities actfac = guy.getKnowledge().getActivityFacilities().get("home");
//			Facility facility = actfac.getFacilities().get(actfac.getFacilities().firstKey());
		// new code:
			Facility facility = guy.getKnowledge().getActivities("home").get(0).getFacility();
			Zone homezone = (Zone) zonelayer.getNearestLocations( facility.getCenter(), null).get(0);
			if( homezone == null )
				log.warn("Homeless employed person (poor guy)" );
			else{
				int homeZoneID = zoneids.get(homezone);
				double epsilon = Math.random();
				double cumul=0.0;
				int workZoneID = 0;
				while( (cumul < epsilon) && (workZoneID<core.numZ-1) ){
					cumul += (core.trips.get(homeZoneID, workZoneID))/core.P[homeZoneID];
					workZoneID++;
				}
				// We found the zone where he works
				// assign a link location for that workplace
				ArrayList<Facility> list2 = primActFacilitiesPerZone.get( zones[workZoneID] );
				while( list2.size() == 0 ){
					// This can happen if a person has a job in a zone without
					// any job facility because of the hack.
					// Hack: it is then reassigned to a random zone
					list2 = primActFacilitiesPerZone.get( zones[Gbl.random.nextInt(core.numZ)] );
				}
				facility = list2.get(Gbl.random.nextInt(list2.size()));
				// Change the knowledge of the person too
				
				// old code:
//				TreeMap<String,ActivityFacilities> tm = guy.getKnowledge().getActivityFacilities();
//				actfac = tm.get(primaryActivityName);
//				if( actfac != null)
//					// Delete current work location
//					tm.remove(primaryActivityName);
//				actfac = new ActivityFacilities(primaryActivityName);
//				actfac.addFacility(facility);
//				tm.put(primaryActivityName, actfac);
				
				// new code:
				ArrayList<Activity> primActs = guy.getKnowledge().getActivities(primaryActivityName);
				if (primActs.size() > 0) {
					guy.getKnowledge().removeActivity(primActs.get(0)); // delete current work location
				}
				guy.getKnowledge().addActivity(new Activity(primaryActivityName, facility));
			}
		}
	}


	void setup( Plans plans ){

		getZonesAndParams();

		getTravelImpedances();

		getNumberHomesPerZone( plans );

		getNumberJobsPerZone();

		hack();

		if( core.calibration )
			loadCalibrationMatrix();
	}

	void getZonesAndParams(){
		Config cfg = Gbl.getConfig();
		primaryActivityName = Gbl.getConfig().getParam( module_name, "primary activity");
		String layerName = cfg.getParam( module_name, "aggregation layer");
		if( layerName == null )
			Gbl.errorMsg( new Exception("PrimLocChoice_MATSIM needs an aggregation layer" ) );

		zonelayer = Gbl.getWorld().getLayer( layerName );

		if( ! (zonelayer instanceof ZoneLayer) )
			Gbl.errorMsg( new Exception("PrimLocChoice_MATSIM needs a Zone Layer") );

		// Fetch parameters from the config file
		core.mu = Double.parseDouble( cfg.getParam(module_name, "mu") );
		if( cfg.getParam( module_name, "calibration matrix" ) != null ){
			log.warn("Matrix importation unsupported - calibration matrix ignored");
			// core.calibration = true
		}

		core.theta = Double.parseDouble( cfg.getParam(module_name, "theta") );
		core.threshold1 = Double.parseDouble( cfg.getParam(module_name, "threshold1") );
		core.threshold2 = Double.parseDouble( cfg.getParam(module_name, "threshold2") );
		core.threshold3 = Double.parseDouble( cfg.getParam(module_name, "threshold3") );
		core.maxiter = Integer.parseInt( cfg.getParam(module_name, "maxiter") );
		core.verbose = Boolean.parseBoolean(cfg.getParam(module_name, "verbose"));

		// We store the zones in a given way so that
		// we do not rely on the order in the collection
		Collection<? extends Location> listloc = zonelayer.getLocations().values();
		int internalID=0;
		core.numZ = listloc.size();
		zones = new Zone[ core.numZ ];
		for(  Object obj : listloc ){
			Zone zone = (Zone) obj;
			zoneids.put( zone, internalID );
			zones[ internalID ] = zone;
			primActFacilitiesPerZone.put( zone, new ArrayList<Facility>() );
			internalID++;
		}
	}

	void loadCalibrationMatrix(){
		// read the matric from the following URL: cfg.getParam( module_name, "calibration matrix" ) );
		// core.calib = ...
	}

	void getTravelImpedances(){
		// Compute a simple Travel impedance matrix
		// with euclidean distance
		core.cij = new Matrix( core.numZ, core.numZ );

		for( int i=0; i<core.numZ; i++){
			for( int j=0; j<core.numZ; j++){
				core.cij.set( i, j, zones[i].calcDistance( zones[j].getCenter() ) );
				// The method requires cii > 0
			}
			core.cij.set(i, i, (zones[i].getMax().calcDistance(zones[i].getMin()))/2 );
			if( core.cij.get( i, i ) == 0.0 ){
				Gbl.errorMsg( new Exception("PrimLocChoice_core requires Cii>0 for intrazonal travel costs"));
			}
		}
		core.setupECij();
	}

	void getNumberHomesPerZone( Plans plans ){
		core.P = new double[ core.numZ ];
		// Determine how many employed persons live in each zone
		Map<IdI, Person> agents = plans.getPersons();
		for (Person guy : agents.values()) {
			Facility facility = guy.getKnowledge().getActivities("home").get(0).getFacility();
			ArrayList<Location> list = zonelayer.getNearestLocations(facility.getCenter(), null);
			Zone homezone = (Zone) list.get(0);
			if( homezone == null )
				log.warn("Homeless employed person (poor guy)" );
			else
				core.P[ zoneids.get(homezone) ]++;
		}
	}

	void getNumberJobsPerZone(){
		core.J = new double[ core.numZ ];
		// Determine the number of jobs per zone
		// and remember the work facilities that belong to given zones
		Collection<? extends Facility> facilities = ((Facilities) Gbl.getWorld().getLayer(Facilities.LAYER_TYPE)).getFacilities().values();
		for( Facility facility : facilities ){
			Activity act = facility.getActivity( primaryActivityName );
			if( act != null ){
				ArrayList<Location> list = zonelayer.getNearestLocations( facility.getCenter(), null);
				Zone zone = (Zone) list.get(0);
				core.J[ zoneids.get(zone) ] += act.getCapacity();
				primActFacilitiesPerZone.get( zone ).add( facility );
			}
		}
	}

	void hack(){
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
}
