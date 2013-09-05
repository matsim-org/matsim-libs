/* *********************************************************************** *
 * project: org.matsim.*
 * DgCreateCottbusCityNetwork
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.events.DgNetShrinkImproved;
import playground.dgrether.koehlerstrehlersignal.network.DgNetworkUtils;

import com.vividsolutions.jts.geom.Envelope;


/**
 * @author dgrether
 *
 */
public class DgCreateCottbusCityNetwork {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String boundingBoxShape = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/shape_files/signal_systems/cottbus_city_bounding_box.shp";
		ShapeFileReader shapeReader = new ShapeFileReader();
		shapeReader.readFileAndInitialize(boundingBoxShape);
		Envelope env = shapeReader.getBounds();
		
		String fullNetwork = DgPaths.REPOS  + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_wgs84_utm33n.xml.gz";
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader netReader = new MatsimNetworkReader(sc);
		netReader.readFile(fullNetwork);

		
		DgNetShrinkImproved netShrink = new DgNetShrinkImproved();
		Network cottbusCityNet = netShrink.createSmallNetwork(sc.getNetwork(), env);
	
		String cityNetwork = DgPaths.REPOS  + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/cottbus_city_network/network_city_wgs84_utm33n";
		NetworkWriter netWriter = new NetworkWriter(cottbusCityNet);
		netWriter.write(cityNetwork + ".xml.gz");
		
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
		DgNetworkUtils.writeNetwork2Shape(cottbusCityNet, crs, cityNetwork);

	}

}
