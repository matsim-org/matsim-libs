/* *********************************************************************** *
 * project: org.matsim.*
 * VisLinkImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.dgrether.xvis.vismodel;

import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.vis.snapshotwriters.VisData;
import org.matsim.vis.snapshotwriters.VisLink;
import org.matsim.vis.snapshotwriters.VisVehicle;


/**
 * 
 * @author dgrether
 */
public class VisLinkImpl implements VisLink {

	private Link link;
	
	public VisLinkImpl(Link link) {
		this.link = link;
	}
	
	@Override
	public Link getLink() {
		return link;
	}

	@Override
	public Collection<? extends VisVehicle> getAllVehicles() {
		throw new UnsupportedOperationException();
	}

	@Override
	public VisData getVisData() {
		return null;
	}

}
