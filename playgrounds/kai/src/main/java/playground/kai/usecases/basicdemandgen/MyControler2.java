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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;



public class MyControler2 {
	private static final Logger log = Logger.getLogger(MyControler2.class);

	@SuppressWarnings("unchecked")
	private static Population createPlansFromShp(final FeatureSource n) {
		Scenario sc = new ScenarioImpl() ;
		
		Population population = sc.getPopulation() ;
		PopulationFactory pb = population.getFactory() ;

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

				Person person = pb.createPerson(id); 
				population.addPerson(person);
				
				Plan plan = pb.createPlan() ;
				person.addPlan(plan) ;
				
				plan.setSelected(true) ; // will also work without

				Activity act = pb.createActivityFromCoord("home",coord) ;
				plan.getPlanElements().add(act) ;
			}

			// store workplace coordinates in temporary data structure
			for ( int ii=0 ; ii<nJobs ; ii++ ) {
				workPlaces.add( coord ) ;
			}
		}

		for ( Person pp : population.getPersons().values() ) {
			Plan plan = pp.getPlans().get(0) ;
			
			int idx = (int)( Math.random()*workPlaces.size() ) ; // TODO: replace by matsim rnd generator
			Coord workCoord = workPlaces.get( idx ) ;
//			workPlaces.remove( idx ) ;
			// (with replacement.  W/o replacement, make sure that there are enough workplaces!)
			
			Leg legH2W = pb.createLeg(TransportMode.car) ;
			plan.getPlanElements().add(legH2W) ;
			
			Activity workAct = pb.createActivityFromCoord("work", workCoord ) ;
			plan.getPlanElements().add(workAct) ;
			
			Leg legW2H = pb.createLeg(TransportMode.bike) ;
			plan.getPlanElements().add(legW2H) ;
			
			Activity homeAct1 = (Activity) plan.getPlanElements().get(0) ;
			Coord homeCoord = homeAct1.getCoord() ;

			Activity homeAct2 = pb.createActivityFromCoord("home", homeCoord ) ;
			plan.getPlanElements().add(homeAct2) ;
			
		}

		return population ;
	}

	public static void main(final String[] args) {

		final String shpFile = "/Users/nagel/shared-svn/studies/countries/ca/vancouver/facilities/shp/landuse.shp";

		Population plans=null ;
		try {
			plans = createPlansFromShp( ShapeFileReader.readDataFile(shpFile) );
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// BasicPopulationWriter.write needs a parameter from the config that I don't know how else to create. :-(  Kai
		//dg yes but the used constructor from PopulationWriter doesn't need the config parameter
		// so we don't need any Gbl or config 
		// write the population for debugging purposes
		PopulationWriter popWriter = new PopulationWriter(plans) ;
		popWriter.write("pop.xml.gz") ;

		log.info("### DONE with demand generation  ### at " + new File("pop.xml.gz").getAbsolutePath()) ;
	}

}
