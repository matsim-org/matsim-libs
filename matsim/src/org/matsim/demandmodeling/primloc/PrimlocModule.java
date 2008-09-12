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

package org.matsim.demandmodeling.primloc;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.config.Config;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.population.Knowledge;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.world.Layer;
import org.matsim.world.Location;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

import Jama.Matrix;


//TODO:
//Separate test case from core
//Driver -> rename as a "Module" 
//Engine -> rename to Core
//put things under demandmodeling.primloc
//Should read HOMES from KNowledge not from plans
//Should read Work from Facilities
//Determine IF the person is performing the prim activity from scanning the existing plans
//Expose the layer / customized

public class PrimlocModule  implements PersonAlgorithm {

	final static String module_name = "primary location choice";

	private final static Logger log = Logger.getLogger(PrimlocModule.class);

	PrimlocCore core = new PrimlocCore();
	
	Zone[] zones;
	Layer zoneLayer;
	PrimlocTravelCostAggregator travelCostAggregator;
	
	HashMap<Zone,Integer> zoneids = new HashMap<Zone,Integer>();
	HashMap<Zone, ArrayList<Facility>> primActFacilitiesPerZone =
		new HashMap<Zone, ArrayList<Facility>>();
	String primaryActivityName;
	
	Random random;
	
	
	public void run(Person guy){

		// Modify the plans of the agents accordingly
		// For each agent: replace the location of the primary
		// activity in the selected plan
		
		Knowledge know = guy.getKnowledge();
		Facility home = know.getActivities("home").get(0).getFacility();
		Zone homezone = (Zone) zoneLayer.getNearestLocations( home.getCenter(), null).get(0);
		if( homezone == null )
			log.warn("Homeless employed person (poor guy)" );
		else{			

//			// We check if the person is performing primary activity or not
//			if( guy.getKnowledge().getActivities(primaryActivityName).size() == 0 )
//				return;			
			
			int homeZoneID = this.zoneids.get(homezone);
			double epsilon = Math.random();
			double cumul=0.0;
			int workZoneID = 0;
			while( (cumul < epsilon) && (workZoneID<this.core.numZ-1) ){
				cumul += (this.core.trips.get(homeZoneID, workZoneID))/this.core.P[homeZoneID];
				workZoneID++;
			}
			// We assigned a zone where he works now
			// assign a link location for that workplace
			ArrayList<Facility> workplaces = this.primActFacilitiesPerZone.get( this.zones[workZoneID] );
			while( workplaces.size() == 0 ){
				// This can happen if a person has a job in a zone without
				// any job facility because of the hack.
				// Hack: it is then reassigned to a random zone
				int zid = (int)(random.nextDouble()*this.core.numZ);
				workplaces = this.primActFacilitiesPerZone.get( this.zones[zid] );
			}
			int wid = (int)(random.nextDouble()*workplaces.size());
			Facility workplace = workplaces.get(wid);
			// Change the knowledge of the person too
			ArrayList<Activity> primActs = guy.getKnowledge().getActivities(primaryActivityName);
			if (primActs.size() > 0) {
				guy.getKnowledge().removeActivity(primActs.get(0)); // delete current work location
			}
			guy.getKnowledge().addActivity(new Activity(primaryActivityName, workplace));
		}
	}



	public void setup( Population population ){

		random = new Random(Gbl.getConfig().global().getRandomSeed());
		
		setupParameters();
		
		setupAggregationLayer();

		setupTravelCosts();
	
		
		getNumberHomesPerZone( population );

		getNumberJobsPerZone();

		normalizeJobHomevectors();

		if( this.core.calibration )
			loadCalibrationMatrix();
		
		// Run the core location choice model
		if( this.core.calibration )
			this.core.runCalibrationProcess();
		else
			this.core.runModel();

	}
	
	public void setAggregationLayer( Layer zoneLayer ){
		if( ! (zoneLayer instanceof ZoneLayer) )
			Gbl.errorMsg( new Exception("PrimLocChoice_MATSIM needs a Zone Layer") );
		this.zoneLayer = zoneLayer;
	}
	
	public void setTravelCost( PrimlocTravelCostAggregator travelCostAggregator ){
		this.travelCostAggregator = travelCostAggregator;
	}
	
	void setupParameters(){
		Config cfg = Gbl.getConfig();
		
		// Fetch parameters from the config file
		
		primaryActivityName = cfg.getParam( module_name, "destination facility type");
				
		core.mu = Double.parseDouble( cfg.getParam(module_name, "mu") );
		core.theta = Double.parseDouble( cfg.getParam(module_name, "theta") );
		core.threshold1 = Double.parseDouble( cfg.getParam(module_name, "threshold1") );
		core.threshold2 = Double.parseDouble( cfg.getParam(module_name, "threshold2") );
		core.threshold3 = Double.parseDouble( cfg.getParam(module_name, "threshold3") );
		core.maxiter = Integer.parseInt( cfg.getParam(module_name, "maxiter") );
		core.verbose = Boolean.parseBoolean(cfg.getParam(module_name, "verbose"));
	}
	
	void setupAggregationLayer(){
		Config cfg = Gbl.getConfig();
		
		// Check / load the aggregation layer
		if( zoneLayer == null ){		
			String layerName = cfg.getParam( module_name, "aggregation layer");
			if( layerName == null )
				Gbl.errorMsg( new Exception("PrimLocChoice_MATSIM needs an aggregation layer" ) );
			zoneLayer = Gbl.getWorld().getLayer( layerName );
			if( ! (zoneLayer instanceof ZoneLayer) )
				Gbl.errorMsg( new Exception("PrimLocChoice_MATSIM needs a Zone Layer") );
		}

		// We store the zones in a given way so that
		// we do not rely on the order in the collection
		Collection<? extends Location> listloc = zoneLayer.getLocations().values();
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
	
	void setupTravelCosts(){
		Config cfg = Gbl.getConfig();
		String distParam = cfg.getParam(module_name, "euclidean distance costs");
		if( distParam != null ){
			if( Boolean.parseBoolean( distParam) )
				setEuclideanDistanceImpedances();
			else
				getTravelImpedancesfromFile( Gbl.getConfig().getParam( module_name, "impedances matrix file") );
		}
	}


	void loadCalibrationMatrix(){
		// read the matric from the following URL: cfg.getParam( module_name, "calibration matrix" ) );
		// core.calib = ...
	}



	void getNumberHomesPerZone( Population population ){
		this.core.P = new double[ this.core.numZ ];
		// Determine how many employed persons live in each zone
		Map<Id, Person> agents = population.getPersons();
		for (Person guy : agents.values()) {
			Facility facility = guy.getKnowledge().getActivities("home").get(0).getFacility();
			ArrayList<Location> list = this.zoneLayer.getNearestLocations(facility.getCenter(), null);
			Zone homezone = (Zone) list.get(0);
			if( homezone == null )
				log.warn("Homeless employed person (poor guy)" );
			else
				this.core.P[ this.zoneids.get(homezone) ]++;
		}
	}

	void getNumberJobsPerZone(){
		this.core.J = new double[ this.core.numZ ];
		// Determine the number of jobs per zone
		// and remember the work facilities that belong to given zones
		Collection<? extends Facility> facilities = ((Facilities) Gbl.getWorld().getLayer(Facilities.LAYER_TYPE)).getFacilities().values();
		for( Facility facility : facilities ){
			Activity act = facility.getActivity( this.primaryActivityName );
			if( act != null ){
				ArrayList<Location> list = this.zoneLayer.getNearestLocations( facility.getCenter(), null);
				Zone zone = (Zone) list.get(0);
				this.core.J[ this.zoneids.get(zone) ] += act.getCapacity();
				this.primActFacilitiesPerZone.get( zone ).add( facility );
			}
		}
	}

	void normalizeJobHomevectors(){
		// Hack to ensure that no element is zero (singular matrix)
		this.core.N = 0.0;
		for( int i=0; i<this.core.numZ; i++){
			this.core.P[i] = Math.max(1.0, this.core.P[i]);
			this.core.J[i] = Math.max(1.0, this.core.J[i]);
			this.core.N += this.core.P[i];
		}
		System.out.println("# employed "+this.core.N);
		this.core.normalizeJobVector();
		System.out.println("Zone attribute vector:");
		for( int i=0; i<this.core.numZ; i++){
			System.out.println( "Zone #"+this.zones[i].getId()+
					"\t#residents: "+this.core.df.format(this.core.P[i]) +
					"\t#"+this.primaryActivityName+": "+this.core.df.format(this.core.J[i]));
		}
	}

	
	void setEuclideanDistanceImpedances(){
		// Compute a simple Travel impedance matrix
		// with euclidean distance
		this.core.cij = new Matrix( this.core.numZ, this.core.numZ );

		for( int i=0; i<this.core.numZ; i++){
			for( int j=0; j<this.core.numZ; j++){
				this.core.cij.set( i, j, this.zones[i].calcDistance( this.zones[j].getCenter() ) );
				// The method requires cii > 0
			}
			this.core.cij.set(i, i, (this.zones[i].getMax().calcDistance(this.zones[i].getMin()))/2 );
			if( this.core.cij.get( i, i ) == 0.0 ){
				Gbl.errorMsg( new Exception("PrimLocChoice_core requires Cii>0 for intrazonal travel costs"));
			}
		}
		this.core.setupECij();
	}

	void getTravelImpedancesfromFile( String costFileName ){

//		this.core.cij = new Matrix( this.core.numZ, this.core.numZ );
//		System.out.println("Reading travel impedances from " + costFileName );
//		this.core.cij = SomeIO.readODMatrix( costFileName, this.core.numZ, this.core.numZ);
//		for( int i=0; i<this.core.numZ; i++ ){
//			double mincij = Double.POSITIVE_INFINITY;
//			for( int j=0; j<this.core.numZ; j++ ){
//				double v=this.core.cij.get(i, j);
//				if( (v < mincij) && (v>0.0) )
//					mincij = v;
//			}
//			if( this.core.cij.get(i, i) == 0.0 )
//				this.core.cij.set(i, i, mincij );
//		}
//		this.core.setupECij();
	}
	
}
