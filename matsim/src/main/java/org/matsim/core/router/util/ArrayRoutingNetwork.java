/* *********************************************************************** *
 * project: org.matsim.*
 * ArrayRoutingNetwork.java
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

package org.matsim.core.router.util;

import org.matsim.api.core.v01.network.Network;

public class ArrayRoutingNetwork extends AbstractRoutingNetwork {
		
	public ArrayRoutingNetwork(Network network) {
		super(network);
	}
	
	@Override
	public final void initialize() {
		// nothing to do here
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}


}