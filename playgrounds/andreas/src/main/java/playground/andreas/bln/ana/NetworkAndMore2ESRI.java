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

package playground.andreas.bln.ana;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
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
			netfile = "d:\\Berlin\\FG Geoinformation\\Scenario\\Ausgangsdaten\\20100809_verwendet\\network_modified_20100806_added_BBI_AS_cl.xml.gz";
//		String netfile = "./test/scenarios/berlin/network.xml.gz";

			outputFileLs = "e:\\temp\\qgis\\networkLs.shp";
			outputFileP = "e:\\temp\\qgis\\networkP.shp";
		} else if ( args.length == 3 ) {
			netfile = args[0] ;
			outputFileLs = args[1] ;
			outputFileP  = args[2] ;
		} else {
			log.error("Arguments cannot be interpreted.  Aborting ...") ;
			System.exit(-1) ;
		}

		ScenarioImpl scenario = new ScenarioImpl();
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
