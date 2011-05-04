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
package playground.dgrether.signalsystems.cottbus.commuterdemand;

import java.io.IOException;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.gis.DgPopulation2ShapeWriter;

/**
 * @author jbischoff
 * @author dgrether
 */
public class CommuterGenerator {

	public static void main(String[] args) throws IOException {
		
		String networkFile = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network.xml.gz";
		String populationOutputDirectory = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cb_spn_gemeinde_nachfrage/";
		String populationOutputFile = populationOutputDirectory + "commuter_population_wgs84_utm33n.xml.gz";
		Config config1 = ConfigUtils.createConfig();
		config1.network().setInputFile(networkFile);
		Scenario sc = ScenarioUtils.loadScenario(config1);
		
		CommuterDataReader cdr = new CommuterDataReader();
		cdr.addFilterRange(12071000);
		cdr.addFilter("12052000"); //12052000 == cottbus stadt
		cdr.readFile(DgPaths.REPOS + "shared-svn/studies/countries/de/pendler_nach_gemeinden/brandenburg_einpendler.csv");
		
		String gemeindenBrandenburgShapeFile = DgPaths.REPOS + "shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/gemeinden/dlm_gemeinden.shp";
		ShapeFileReader shapeReader = new ShapeFileReader();
		Set<Feature> gemeindenFeatures = shapeReader.readFileAndInitialize(gemeindenBrandenburgShapeFile);
		
		CommuterDemandWriter cdw = new CommuterDemandWriter(sc, gemeindenFeatures, shapeReader.getCoordinateSystem(), 
				cdr.getCommuterRelations(), MGC.getCRS(TransformationFactory.WGS84_UTM33N));
		cdw.setScalefactor(0.55);//1.0 is default already
		cdw.writeDemand(populationOutputFile);

		
		//write some test output
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationOutputFile);
		Scenario baseScenario = ScenarioUtils.loadScenario(config);
		
		String shapeFilename = populationOutputDirectory + "commuter_population.shp";
		new DgPopulation2ShapeWriter(baseScenario.getPopulation(),	MGC.getCRS(TransformationFactory.WGS84_UTM33N)).write(shapeFilename, MGC.getCRS(TransformationFactory.WGS84_UTM33N));

	}

}
