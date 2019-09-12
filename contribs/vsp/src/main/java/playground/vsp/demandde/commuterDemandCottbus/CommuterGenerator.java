/* *********************************************************************** *
 * project: org.matsim.*
 * CommuterGenerator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.vsp.demandde.commuterDemandCottbus;

import java.io.File;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author jbischoff
 * @author dgrether
 */
public class CommuterGenerator {
	
	private static final Logger log = Logger.getLogger(CommuterGenerator.class);
	
	public static void main(String[] args) throws Exception {
		
		String baseDir = "../../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/";
		String networkFile = baseDir + "network_wgs84_utm33n.xml.gz";
		boolean useLanduse = true;
		String populationOutputDirectory = baseDir + "cb_spn_gemeinde_nachfrage_landuse_ohneTagebau/";

//		String shapesOutputDirectory = populationOutputDirectory + "shapes_all_modes/";
		String shapesOutputDirectory = populationOutputDirectory + "shapes/";
		
//		String populationOutputFile = populationOutputDirectory + "commuter_population_wgs84_utm33n.xml.gz";
		String populationOutputFile = populationOutputDirectory + "commuter_population_wgs84_utm33n_car_only.xml.gz";
		OutputDirectoryLogging.initLoggingWithOutputDirectory(populationOutputDirectory);

		File shapes = new File(shapesOutputDirectory);
		if (! shapes.exists() ) {
			shapes.mkdir();
		}
		
		Config config1 = ConfigUtils.createConfig();
		config1.network().setInputFile(networkFile);
		Scenario sc = ScenarioUtils.loadScenario(config1);
		
		CommuterDataReader cdr = new CommuterDataReader();
		cdr.addFilterRange(12071000);
		cdr.addFilter("12052000"); //12052000 == cottbus stadt
		cdr.readFile("../../../shared-svn/studies/countries/de/pendler_nach_gemeinden/brandenburg_einpendler.csv");
//		cdr.getCommuterRelations().add(new CommuterDataElement("12052000", "12052000", 1000));
		
		String gemeindenBrandenburgShapeFile = "../../../shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/gemeinden/dlm_gemeinden.shp";
		ShapeFileReader gemeindenReader = new ShapeFileReader();
		Collection<SimpleFeature> gemeindenFeatures = gemeindenReader.readFileAndInitialize(gemeindenBrandenburgShapeFile);
		
		
		
		CommuterDemandWriter cdw = new CommuterDemandWriter(gemeindenFeatures, gemeindenReader.getCoordinateSystem(), 
				cdr.getCommuterRelations(), MGC.getCRS(TransformationFactory.WGS84_UTM33N));
		//landuse
		if (useLanduse){
			DgLanduseReader landuseReader = new DgLanduseReader();
			Tuple<Collection<SimpleFeature>,CoordinateReferenceSystem> homeLanduse = landuseReader.readLanduseDataHome();
			Tuple<Collection<SimpleFeature>,CoordinateReferenceSystem> workLanduse = landuseReader.readLanduseDataWork();
			cdw.addLanduse("home", homeLanduse);
			cdw.addLanduse("work", workLanduse);
		}
//		
//		cdw.setScalefactor(1.0); // all modes
		cdw.setScalefactor(0.55); //car mode share
//		cdw.setScalefactor(0.1); //testing
		
		cdw.computeDemand(sc);
		PopulationWriter populationWriter = new PopulationWriter(sc.getPopulation(),
				sc.getNetwork());
		populationWriter.write(populationOutputFile);
		log.info("population written to " + populationOutputFile);

		
		//write some test output
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationOutputFile);
		Scenario baseScenario = ScenarioUtils.loadScenario(config);
		
		String shapeFilename = shapesOutputDirectory + "commuter_population_home.shp";
		new DgPopulation2ShapeWriter(baseScenario.getPopulation(),	MGC.getCRS(TransformationFactory.WGS84_UTM33N))
			.write("home", shapeFilename, MGC.getCRS(TransformationFactory.WGS84_UTM33N));
		shapeFilename = shapesOutputDirectory + "commuter_population_work.shp";
		new DgPopulation2ShapeWriter(baseScenario.getPopulation(),	MGC.getCRS(TransformationFactory.WGS84_UTM33N))
		.write("work", shapeFilename, MGC.getCRS(TransformationFactory.WGS84_UTM33N));
		
		log.info("done!");
		OutputDirectoryLogging.closeOutputDirLogging();
	}

}
