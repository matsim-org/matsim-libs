/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.crossborder;

import java.util.ArrayList;

import org.matsim.core.network.NetworkImpl;

abstract class Parser {
	
	
	protected ArrayList<Relation> relations;
	protected String file;
	protected NetworkImpl network;

	public Parser() {}
	
	public Parser(NetworkImpl network, String file) {
		this.relations=new ArrayList<Relation>();
		this.file=file;
		this.network=network;
	}
	
	public ArrayList<Relation> getRelations() {
		return relations;
	}

	public abstract int parse(String type, int startTime, int actPersonNumber);
}
