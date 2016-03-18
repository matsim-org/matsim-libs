/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertOsmToMatsim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.southafrica.utilities.network;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.utils.gis.matsim2esri.network.CapacityBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;

import playground.southafrica.utilities.Header;

/**
 * Class to convert an OpenStreetMap *.osm file into a MATSim network.
 * The highway conversion defaults have been set to best represent the 
 * South African road configuration.
 * 
 * @see <a href=http://wiki.openstreetmap.org/wiki/South_African_Tagging_Guidelines>South African tagging guidelines on <i>OpenStreetMap</i></a>
 * 
 * @author jwjoubert
 */
public class ConvertOsmToMatsim {
	final private static Logger LOG = Logger.getLogger(ConvertOsmToMatsim.class);
	
	/**
	 * Implementation of converting an <i>OpenStreeMap</i> file (with file type
	 * <code>*.osm</code>) into a MATSim network.
	 *  
	 * @param args The following arguments are all required:
	 * <ol>
	 * 		<li> <b>inputFile</b> - the path to the <code>*.osm</code> file;
	 * 		<li> <b>outputFile</b> - path to the MATSim network file, either
	 * 			 <code>*.xml</code> or <code>*.xml.gz</code>;
	 * 		<li> <b>shapefileLinks</b> - path to the file where the network 
	 * 			will be written as ESRI shapefile (this may be '<code>null</code>'
	 * 			in which case the network will not be written as a shapefile).
	 * 			Even though it may be '<code>null</code>', the argument must be 
	 * 			passed.
	 * 		<li> <b>fullNetwork</b> - a boolean value to indicate if the full
	 * 			or cleaned up network must be generated. The value '<code>true</code>'
	 * 			will result in a full network, while '<code>false</code>' 
	 * 			results in the cleaned up network.
	 * 		<li> <b>CRS</b> - the projected <i>coordinate reference system</i> 
	 * 			for the	final network. In the case of South Africa, this should 
	 * 			typically be 'WGS84_SA_Albers'. This is the standard Albers 
	 * 			projection with standard parallels 18S and 32S, and the central 
	 * 			meridian at 24E. See <a href=http://www.spatialreference.org/ref/sr-org/7490/>
	 * 			SpatialReference.org</a>.
	 * </ol>
	 */
	public static void main(String[] args) {
		Header.printHeader(ConvertOsmToMatsim.class.toString(), args);
		
		String inputFile = null;
		String outputFile = null;
		String shapefileLinks = null;
		boolean fullNetwork = true;
		String CRS = null;
		
		if(args.length != 5){
			throw new IllegalArgumentException("Must have five arguments: and osm-file; network-file; shapefile; boolean indicating full or cleaned network; and the final coordinate reference system.");
		} else{
			inputFile = args[0];
			outputFile = args[1];
			shapefileLinks = args[2].equalsIgnoreCase("null") ? null : args[2];	
			fullNetwork = Boolean.parseBoolean(args[3]);
			CRS = args[4];
		}

		Scenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network nw = sc.getNetwork();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, CRS);
		OsmNetworkReader onr = new OsmNetworkReader(nw, ct, true);
		onr.setKeepPaths(fullNetwork);
		
		/*
		 * Configure the highway classification.
		 */
		LOG.info("Overwriting some highway defaults...");
		onr.setHighwayDefaults(1, "trunk", 1, 120/3.6, 1, 2000);
		onr.setHighwayDefaults(1, "primary", 2, 80/3.6, 1, 1500);
		onr.setHighwayDefaults(1, "secondary", 2, 80/3.6, 1, 1000);
		onr.setHighwayDefaults(1, "tertiary", 1, 60/3.6, 1, 1000);
		onr.setHighwayDefaults(1, "unclassified", 1, 60/3.6, 1, 800);
		onr.setHighwayDefaults(1, "residential", 1, 45/3.6, 1, 600);
		onr.setHighwayDefaults(1, "service", 1, 60/3.6, 1, 600);
		
		LOG.info("Parsing the OSM file...");
		onr.parse(inputFile);
		
		NetworkCleaner nc = new NetworkCleaner();
		nc.run(nw);		
		
		new NetworkWriter(nw).writeFileV1(outputFile);
		
		sc.getConfig().global().setCoordinateSystem(CRS);
		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(nw, CRS);
		builder.setWidthCoefficient(-0.01);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		
		if(shapefileLinks != null){
			new Links2ESRIShape(nw, shapefileLinks, builder).write();
		}
		Header.printFooter();
	}

}
