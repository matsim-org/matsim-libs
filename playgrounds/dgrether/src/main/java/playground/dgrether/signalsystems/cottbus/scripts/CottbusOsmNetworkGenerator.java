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

package playground.dgrether.signalsystems.cottbus.scripts;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.run.NetworkCleaner;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.utils.DgNet2Shape;


public class CottbusOsmNetworkGenerator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Network network = NetworkImpl.createNetwork();
		OsmNetworkReader osmReader = new OsmNetworkReader(network,
				TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,TransformationFactory.WGS84_UTM33N),  false);
		osmReader.setKeepPaths(false);
		osmReader.setScaleMaxSpeed(true);
		String input = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/brandenburg_tagged.osm.gz";
		String output = DgPaths.REPOS +  "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network2";

//		 set osmReader useHighwayDefaults false
//		 Autobahn
		osmReader.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
		osmReader.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
//		 Bundesstrasse?
		osmReader.setHighwayDefaults(1, "trunk",         1,  80.0/3.6, 1.0, 2000);
		osmReader.setHighwayDefaults(1, "trunk_link",    1,  50.0/3.6, 1.0, 1500);
//		 Durchgangsstrassen
		osmReader.setHighwayDefaults(1, "primary",       1,  80.0/3.6, 1.0, 1500);
		osmReader.setHighwayDefaults(1, "primary_link",  1,  60.0/3.6, 1.0, 1500);
//		 Hauptstrassen
		osmReader.setHighwayDefaults(1, "secondary",     1,  60.0/3.6, 1.0, 1000);
//		 mehr Hauptstrassen
		osmReader.setHighwayDefaults(1, "tertiary",      1,  45.0/3.6, 1.0,  600);
//		 Nebenstrassen
		osmReader.setHighwayDefaults(2, "minor",         1,  45.0/3.6, 1.0,  600);
//		 diverse
		osmReader.setHighwayDefaults(2, "unclassified",  1,  45.0/3.6, 1.0,  600);
//		 Wohngebiete
		osmReader.setHighwayDefaults(2, "residential",   1,  30.0/3.6, 1.0,  600);
//		 Spielstrassen irrelevant, since only tiny percentile
//		 osmReader.setHighwayDefaults(6, "living_street", 1,  15.0/3.6, 1.0,  300);
//		More Brandenburg:		
//		osmReader.setHierarchyLayer(52.04382,13.499222, 51.258248,14.887619, 1);
		
//		used for BA:		
		//spree neisse
		osmReader.setHierarchyLayer( 52.045199,14.115944, 51.551772,14.817009, 1);
		//cottbus innenstadt
		osmReader.setHierarchyLayer(51.820578,14.247866, 51.684789,14.507332, 2);

		osmReader.parse(input);

		// Write network to file
		new NetworkWriter(network).write(output + ".xml.gz");
		System.out.println("Done! Unprocessed MATSim Network saved as " + output + ".xml.gz");
		// Clean network
		new NetworkCleaner().run(new String[] {output + ".xml.gz", output + "_cl.xml.gz"});
		System.out.println("NetworkCleaner done! Network saved as " + output + "_cl.xml.gz");

		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
		new DgNet2Shape().write(network, output + ".shp", crs);


	}

}
