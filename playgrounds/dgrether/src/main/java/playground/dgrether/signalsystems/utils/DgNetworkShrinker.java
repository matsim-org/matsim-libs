/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusSmallNetworkGenerator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.utils;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.analysis.FeatureNetworkLinkStartEndCoordFilter;


public class DgNetworkShrinker {

	public Network createSmallNetwork(Network net, SimpleFeature boundingBoxFeature, CoordinateReferenceSystem networkCrs) {
		NetworkFilterManager filterManager = new NetworkFilterManager(net);
		filterManager.addLinkFilter(new FeatureNetworkLinkStartEndCoordFilter(networkCrs, boundingBoxFeature, networkCrs));
		Network newNetwork = filterManager.applyFilters();
		return newNetwork;		
	}

}

