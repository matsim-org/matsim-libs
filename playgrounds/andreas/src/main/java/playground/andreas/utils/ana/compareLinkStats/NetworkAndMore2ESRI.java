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

package playground.andreas.utils.ana.compareLinkStats;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.utils.gis.matsim2esri.network.CapacityBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.LanesBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class NetworkAndMore2ESRI extends Links2ESRIShape{

	private static Logger log = Logger.getLogger(Links2ESRIShape.class);

	public NetworkAndMore2ESRI(Network network, String filename) {
		super(network, filename, "DHDN_GK4");
	}

	public static void main(String[] args) {
		String netfile = null ;
		String outputFileLs = null ;
		String outputFileP = null ;

		if ( args.length == 0 ) {
			netfile = "d:\\Berlin\\berlin-fggeoinfo\\30_Run_20_percent\\20101005_run777_778\\network_modified_20100806_added_BBI_AS_cl.xml.gz";
//			netfile = "f:/server/run793/counts_network_merged.xml_cl.xml";
			LinkStatsCompareConfig.linkStatsFileOne = "e:/run793/output/ITERS/it.600/run793.600.linkstats.txt";
			LinkStatsCompareConfig.linkStatsFileTwo = "e:/run794/output/ITERS/it.600/run794.600.linkstats.txt";
//			LinkStatsCompareConfig.linkStatsFileOne = "E:/run778/output/ITERS/it.900/run778.900.linkstats.txt";
//			LinkStatsCompareConfig.linkStatsFileTwo = "E:/run777/output/ITERS/it.900/run777.900.linkstats.txt";

			outputFileLs = "f:\\temp\\networkLs.shp";
			outputFileP = "f:\\temp\\networkP.shp";
		} else if ( args.length == 3 ) {
			netfile = args[0] ;
			outputFileLs = args[1] ;
			outputFileP  = args[2] ;
		} else {
			log.error("Arguments cannot be interpreted.  Aborting ...") ;
			System.exit(-1) ;
		}

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().global().setCoordinateSystem("DHDN_GK4");

		log.info("loading network from " + netfile);
		final Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netfile);
		log.info("done.");

		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(network, "DHDN_GK4");
//		builder.setFeatureGeneratorPrototype(CountVehOnLinksStringBasedFeatureGenerator.class);
		builder.setFeatureGeneratorPrototype(LinkstatsStringBasedFeatureGenerator.class);
		builder.setWidthCoefficient(0.5);
		builder.setWidthCalculatorPrototype(LanesBasedWidthCalculator.class);
		new Links2ESRIShape(network,outputFileLs, builder).write();

		CoordinateReferenceSystem crs = MGC.getCRS("DHDN_GK4");
		builder.setWidthCoefficient(0.01);
//		builder.setFeatureGeneratorPrototype(CountVehOnLinksPolygonBasedFeatureGenerator.class);
		builder.setFeatureGeneratorPrototype(LinksstatsPolygonBasedFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		builder.setCoordinateReferenceSystem(crs);
		new Links2ESRIShape(network,outputFileP, builder).write();

	}

}
