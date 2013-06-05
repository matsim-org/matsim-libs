/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractGuidance
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsptelematics.ha2;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;

public abstract class AbstractGuidance implements Guidance {

	protected Id id2 = new IdImpl("2");
	protected Id id3 = new IdImpl("3");
	protected Id id4 = new IdImpl("4");
	protected Id id5 = new IdImpl("5");
	protected double ttFs1;
	protected double ttFs2;
	protected Id guidance = id2;
	protected Network network;
	protected String outputFilename;

	public AbstractGuidance(Network network, String filename) {
		this.network = network;
		this.calcFreespeedTTs();
		this.outputFilename = filename;
	}

	private void calcFreespeedTTs() {
		this.ttFs1 = 0.0;
		ttFs1 += this.network.getLinks().get(id2).getFreespeed();
		ttFs1 += this.network.getLinks().get(id4).getFreespeed();
		this.ttFs2 = 0.0;
		ttFs2 += this.network.getLinks().get(id3).getFreespeed();
		ttFs2 += this.network.getLinks().get(id5).getFreespeed();
	}
	

	
	
}