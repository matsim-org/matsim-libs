/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkKmlStyleFactory
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
package org.matsim.vis.kml;

import java.io.IOException;

import org.matsim.core.api.internal.MatsimFactory;

import net.opengis.kml.v_2_2_0.StyleType;

public interface NetworkKmlStyleFactory extends MatsimFactory {

	public abstract StyleType createDefaultNetworkNodeStyle() throws IOException;

	public abstract StyleType createDefaultNetworkLinkStyle() throws IOException;

}