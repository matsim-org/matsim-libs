/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.utils.ana.countVehOnLinks;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.utils.gis.matsim2esri.network.CapacityBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class PackageMain extends Links2ESRIShape{

	private static Logger log = Logger.getLogger(Links2ESRIShape.class);

	public PackageMain(Network network, String filename) {
		super(network, filename, "DHDN_GK4");
	}

	public static void main(String[] args) {
		String netfile = null ;
		String outputFileLs = null ;
		String outputFileP = null ;

		if ( args.length == 0 ) {
			netfile = "d:\\Berlin\\berlin-fggeoinfo\\30_Run_20_percent\\20101005_run777_778\\network_modified_20100806_added_BBI_AS_cl.xml.gz";
//		String netfile = "./test/scenarios/berlin/network.xml.gz";
			EventsCompareConfig.eventsFileOne = "E:/run778/output/ITERS/it.900/run778.900.events.txt";
			EventsCompareConfig.eventsFileTwo = "E:/run777/output/ITERS/it.900/run777.900.events.txt";

			outputFileLs = "e:\\temp\\networkLs_abs.shp";
			outputFileP = "e:\\temp\\networkP_abs.shp";
		} else if ( args.length == 3 ) {
			netfile = args[0] ;
			outputFileLs = args[1] ;
			outputFileP  = args[2] ;
		} else {
			log.error("Arguments cannot be interpreted.  Aborting ...") ;
			System.exit(-1) ;
		}

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().global().setCoordinateSystem("DHDN_GK4");

		log.info("loading network from " + netfile);
		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netfile);
		log.info("done.");

		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, "DHDN_GK4");
//		builder.setFeatureGeneratorPrototype(CountVehOnLinksStringBasedFeatureGenerator.class);
		builder.setFeatureGeneratorPrototype(CountVehOnLinksStringBasedFeatureGenerator.class);
		builder.setWidthCoefficient(0.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		new Links2ESRIShape(network,outputFileLs, builder).write();

		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		builder.setWidthCoefficient(0.01);
//		builder.setFeatureGeneratorPrototype(CountVehOnLinksPolygonBasedFeatureGenerator.class);
		builder.setFeatureGeneratorPrototype(CountVehOnLinksPolygonBasedFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Links2ESRIShape(network,outputFileP, builder).write();

	}

}
