/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.kai.usecases.basicdemandgen;
/*
 * $Id: MyControler1.java,v 1.1 2007/11/14 12:00:28 nagel Exp $
 */

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.*;

import com.vividsolutions.jts.geom.*;

import org.matsim.api.basic.v01.*;
import org.matsim.api.basic.v01.population.*;
import org.matsim.core.utils.gis.ShapeFileReader;



public class MyControler2 {
	private static final Logger log = Logger.getLogger(MyControler2.class);

	@SuppressWarnings("unchecked")
	private static BasicPopulation createPlansFromShp(final FeatureSource n) {
		BasicScenario sc = new BasicScenarioImpl() ;
		
		BasicPopulation population = sc.getPopulation() ;
		BasicPopulationBuilder pb = population.getPopulationBuilder() ;

		// get all features from the shp file:
		FeatureIterator it = null; try {
			it = n.getFeatures().features();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		long popCnt = 0 ;
		List<Coord> workPlaces = new ArrayList<Coord>() ;
		
		// iterate through the features:
		while (it.hasNext()) {
			final Feature feature = it.next();

			double area = (Double) feature.getAttribute("AREA") ;

			final MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
			if (multiPolygon.getNumGeometries() > 1) {
				log.warn("MultiPolygons with more then 1 Geometry ignored!");
			}
			final Polygon polygon = (Polygon) multiPolygon.getGeometryN(0);
			Point center = polygon.getCentroid();
			Coord coord = sc.createCoord( center.getY() , center.getX() ) ;

			int nPersons = 0 ;
			int nJobs = 0 ;
			String lu_code = (String) feature.getAttribute("LU_CODE") ;
			if ( lu_code.equals("S210") ) {
				// define number of home/workplaces for zone:
				nPersons = (int) (area/1000.) ;
				nJobs = (int) (area/2000.) ;
			}

			// generate correct number of persons:
			for ( int ii=0 ; ii<nPersons ; ii++ ) {
				Id id = sc.createId( Long.toString( popCnt ) ); popCnt++ ;

				BasicPerson person = pb.createPerson(id); 
				population.getPersons().put( id, person ) ;
				
				BasicPlan plan = pb.createPlan(person) ;
				person.getPlans().add(plan) ;
				
				plan.setSelected(true) ; // will also work without

				BasicActivity act = pb.createActivityFromCoord("home",coord) ;
				plan.getPlanElements().add(act) ;
			}

			// store workplace coordinates in temporary data structure
			for ( int ii=0 ; ii<nJobs ; ii++ ) {
				workPlaces.add( coord ) ;
			}
		}

		for ( Object oo : population.getPersons().values() ) {
			BasicPerson pp = (BasicPerson) oo ;
			BasicPlan plan = (BasicPlan) pp.getPlans().get(0) ;
			
			int idx = (int)( Math.random()*workPlaces.size() ) ; // TODO: replace by matsim rnd generator
			Coord workCoord = workPlaces.get( idx ) ;
//			workPlaces.remove( idx ) ;
			// (with replacement.  W/o replacement, make sure that there are enough workplaces!)
			
			BasicLeg legH2W = pb.createLeg(TransportMode.car) ;
			plan.getPlanElements().add(legH2W) ;
			
			BasicActivity workAct = pb.createActivityFromCoord("work", workCoord ) ;
			plan.getPlanElements().add(workAct) ;
			
			BasicLeg legW2H = pb.createLeg(TransportMode.bike) ;
			plan.getPlanElements().add(legW2H) ;
			
			BasicActivity homeAct1 = (BasicActivity) plan.getPlanElements().get(0) ;
			Coord homeCoord = homeAct1.getCoord() ;

			BasicActivity homeAct2 = pb.createActivityFromCoord("home", homeCoord ) ;
			plan.getPlanElements().add(homeAct2) ;
			
		}

		return population ;
	}

	public static void main(final String[] args) {

		final String shpFile = "/Users/nagel/shared-svn/studies/countries/ca/vancouver/facilities/shp/landuse.shp";

		BasicPopulation plans=null ;
		try {
			plans = createPlansFromShp( ShapeFileReader.readDataFile(shpFile) );
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// BasicPopulationWriter.write needs a parameter from the config that I don't know how else to create. :-(  Kai
		//dg yes but the used constructor from PopulationWriter doesn't need the config parameter
		// so we don't need any Gbl or config 
		// write the population for debugging purposes
		BasicPopulationWriter popWriter = new BasicPopulationWriter(plans) ;
		popWriter.write("pop.xml.gz") ;

		log.info("### DONE with demand generation  ### at " + new File("pop.xml.gz").getAbsolutePath()) ;
	}

}
