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

import java.io.IOException;

import java.util.*;

import org.apache.log4j.Logger;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.BasicActivity;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPopulation;
import org.matsim.api.basic.v01.population.BasicPopulationBuilder;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationWriter;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.gis.ShapeFileReader;



public class MyControler2 {
	private static final Logger log = Logger.getLogger(MyControler2.class);

	private static BasicPopulation createPlansFromShp(final FeatureSource n) {
		List<Coord> workPlaces = new ArrayList<Coord>() ;

//		BasicPopulation<? extends BasicPerson<? extends BasicPlan>> population = new PopulationImpl(PopulationImpl.NO_STREAMING) ;
		BasicPopulation population = new PopulationImpl(PopulationImpl.NO_STREAMING) ;
		// FIXME: select specific implementation here.  Makes sense, but is it what we want?  (Could also be empty population
		// taken from controler.)
		// TODO: The generics approach, as of now, is awful.
		int popCnt = 0 ;
		
		BasicPopulationBuilder pb = population.getPopulationBuilder() ;

		FeatureIterator it = null; try {
			it = n.getFeatures().features();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		while (it.hasNext()) {
			final Feature feature = it.next();

			double area = (Double) feature.getAttribute("AREA") ;

			final MultiPolygon multiPolygon = (MultiPolygon) feature.getDefaultGeometry();
			if (multiPolygon.getNumGeometries() > 1) {
				log.warn("MultiPolygons with more then 1 Geometry ignored!");
			}
			final Polygon polygon = (Polygon) multiPolygon.getGeometryN(0);
			Point center = polygon.getCentroid();
			Coord coord = new CoordImpl ( center.getY() , center.getX() ) ;

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
				Id id = new IdImpl( popCnt ) ; popCnt++ ;

				BasicPerson person = null;
				try {
					person = pb.createPerson(id); 
				} catch (Exception e) {
					e.printStackTrace();
				} 
				population.getPersons().put( id, person ) ;
				
				BasicPlan plan = pb.createPlan(person) ;
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
//			playground.kai.urbansim.Utils.completePlanToHwh(plan, workCoord) ; // FIXME once createAct methods are there
			
			BasicLeg leg = pb.createLeg(BasicLeg.Mode.bike) ;
			plan.getPlanElements().add(leg) ;
			
		}

		return population ;
	}

	public static void main(final String[] args) {

		final String shpFile = "/Users/nagel/shared-svn/studies/north-america/ca/vancouver/facilities/shp/landuse.shp";

		BasicPopulation plans=null ;
		try {
			plans = createPlansFromShp( ShapeFileReader.readDataFile(shpFile) );
		} catch (IOException e) {
			e.printStackTrace();
		}

		// write the population for debugging purposes
		PopulationWriter popWriter = new PopulationWriter( (Population) plans,"pop.xml.gz","v4",1) ;
		popWriter.write();

		log.info("### DONE with demand generation from urbansim ###") ;

		// parse the config arguments so we have a config.  generate scenario data from this
		if ( args.length==0 ) {
			Gbl.createConfig(new String[] {"./src/playground/duncan/myconfig1.xml"});
		} else {
			Gbl.createConfig(args) ;
		}
		ScenarioImpl scenarioData = new ScenarioImpl( Gbl.getConfig() ) ;

		// get the network.  Always cleaning it seems a good idea since someone may have modified the input files manually in
		// order to implement policy measures.
		NetworkLayer network = scenarioData.getNetwork() ;
		log.info("") ; 	log.info("cleaning network ...");
		NetworkCleaner nwCleaner = new NetworkCleaner() ;
		nwCleaner.run( network ) ;
		log.info("... finished cleaning network.") ; log.info("") ;

		// start the control(l)er with the network and plans as defined above
		Controler controler = new Controler(Gbl.getConfig(),network,(Population) plans) ;

		// this means existing files will be over-written.  Be careful!
		controler.setOverwriteFiles(true);

		// start the matsim iterations (configured by the config file)
		controler.run();
	}

}
