/* *********************************************************************** *
 * project: org.matsim.*
 * CompressedRouteTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.marcel;

import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.population.routes.CarRoute;
import org.matsim.population.routes.AbstractCarRouteTest;

import playground.yu.compressRoute.Subsequent;

/**
 * @author mrieser
 */
public class CompressedCarRouteTest extends AbstractCarRouteTest {

	@Override
	public CarRoute getCarRouteInstance() {

		NetworkLayer network = (NetworkLayer) Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE);
		Subsequent subsequent = new Subsequent(network);
		return new CompressedCarRoute(subsequent.getSubsequentLinks());
	}

}
