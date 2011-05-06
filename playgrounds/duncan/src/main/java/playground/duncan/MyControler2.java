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

package playground.duncan;
/*
 * $Id: MyControler1.java,v 1.1 2007/11/14 12:00:28 nagel Exp $
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureIterator;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


public class MyControler2 {
	private static final Logger log = Logger.getLogger(MyControler2.class);

	private static Population createPlansFromShp(final FeatureSource n, final Population population) {
		List<Coord> workPlaces = new ArrayList<Coord>() ;

		int popCnt = 0 ;

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
//				continue;
			}
			final Polygon polygon = (Polygon) multiPolygon.getGeometryN(0);
			Point center = polygon.getCentroid();
//			Coord coord = new CoordImpl ( center.getY()/100000.-180.+2.15 , center.getX()/10000. ) ;
			Coord coord = new CoordImpl ( center.getY() , center.getX() ) ;
			// (FIXME: should check if this really produces useful coordinates)

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
				PersonImpl newPerson = new PersonImpl( id ) ;
				population.addPerson( newPerson ) ;
				PlanImpl plan = newPerson.createAndAddPlan(true);
				playground.kai.urbansim.Utils.makeHomePlan(plan, coord) ;
			}

			// store workplace coordinates in temporary data structure
			for ( int ii=0 ; ii<nJobs ; ii++ ) {
				workPlaces.add( coord ) ;
			}
		}

		for ( Person pp : population.getPersons().values() ) {
			Plan plan = pp.getSelectedPlan();
			int idx = (int)( Math.random()*workPlaces.size() ) ; // TODO: replace by matsim rnd generator
			Coord workCoord = workPlaces.get( idx ) ;
//			workPlaces.remove( idx ) ;
			// (with replacement.  W/o replacement, make sure that there are enough workplaces!)
			playground.kai.urbansim.Utils.completePlanToHwh((PlanImpl) plan, workCoord) ;
		}

		return population ;
	}

	public static void main(final String[] args) throws IOException {

		Config config;
		// parse the config arguments so we have a config.  generate scenario data from this
		if ( args.length==0 ) {
			config = ConfigUtils.loadConfig("./src/playground/duncan/myconfig1.xml");
		} else {
			config = ConfigUtils.loadConfig(args[0]);
		}
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		loader.loadNetwork();
		ScenarioImpl scenarioData = loader.getScenario();

		// create population
		final String shpFile = "/Users/nagel/shared-svn/studies/north-america/ca/vancouver/facilities/shp/landuse.shp";

		createPlansFromShp( ShapeFileReader.readDataFile(shpFile), scenarioData.getPopulation() );

		// write the population for debugging purposes
		new PopulationWriter(scenarioData.getPopulation(), scenarioData.getNetwork()).write("pop.xml.gz") ;

		log.info("### DONE with demand generation from urbansim ###") ;


		// get the network.  Always cleaning it seems a good idea since someone may have modified the input files manually in
		// order to implement policy measures.
		NetworkImpl network = scenarioData.getNetwork() ;
		log.info("") ; 	log.info("cleaning network ...");
		NetworkCleaner nwCleaner = new NetworkCleaner() ;
		nwCleaner.run( network ) ;
		log.info("... finished cleaning network.") ; log.info("") ;

		// start the control(l)er with the network and plans as defined above
		Controler controler = new Controler(scenarioData) ;

		// this means existing files will be over-written.  Be careful!
		controler.setOverwriteFiles(true);

		// start the matsim iterations (configured by the config file)
		controler.run();
	}

}
