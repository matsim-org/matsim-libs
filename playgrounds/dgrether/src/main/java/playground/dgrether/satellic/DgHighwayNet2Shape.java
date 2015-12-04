/* *********************************************************************** *
 * project: org.matsim.*
 * DgHighwayNet2Shape
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
package playground.dgrether.satellic;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.utils.DgNet2Shape;


/**
 * @author dgrether
 *
 */
public class DgHighwayNet2Shape {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String net = "/media/data/work/repos/shared-svn/projects/satellicHsUlm2010/network_cleaned_wgs84.xml.gz";
		String out = "/media/data/work/repos/shared-svn/projects/satellicHsUlm2010/network_cleaned_wgs84.shp";

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().network().setInputFile(net);
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(net);
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84);
		new DgNet2Shape().write(network, out, crs);
	}

}
