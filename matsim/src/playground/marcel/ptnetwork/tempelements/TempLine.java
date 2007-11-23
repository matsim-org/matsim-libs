/* *********************************************************************** *
 * project: org.matsim.*
 * TempLine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.marcel.ptnetwork.tempelements;

import java.util.ArrayList;

import playground.marcel.ptnetwork.PtNetworkReader;


public class TempLine {

	private String name;
	private String vType;

	public ArrayList<TempHP> hps = new ArrayList<TempHP>();
	public ArrayList<TempLink> links = new ArrayList<TempLink>();
	public ArrayList<TempRoute> routes = new ArrayList<TempRoute>();

	public void setName(String name) {
		this.name=name;
	}

	public void setVType(String vType) {
		this.vType=vType;
	}

	public String getName() {
		return this.name;
	}

	public String getVType() {
		return this.vType;
	}

	public TempLine(PtNetworkReader reader) {
		this.name = reader.getLineName();
		this.vType = reader.getVType();
		this.hps.clear();
		this.links.clear();
		this.routes.clear();
		this.hps = reader.hps;
		this.links = reader.links;
		this.routes = reader.routes;
	}

	public TempLine() {
		this.name = null;
		this.vType = null;
	}

	public TempLine(String name, String vType) {
		this.name = name;
		this.vType = vType;
	}

	public TempLink getLink(String fromNodeID, String toNodeID) {
		TempHP fromNode = getHP(fromNodeID);
		TempHP toNode = getHP(toNodeID);
		return getLink(fromNode, toNode);
	}

	public TempLink getLink(TempHP fromNode, TempHP toNode){
		for (TempLink link : fromNode.outLinks) {
			if (link.toNode.equals(toNode)) {
				return link;
			}
		}
		return null;
	}

	public TempHP getHP (String ID){
		for (TempHP temphp : this.hps) {
			if (temphp.getHp_Id().equals(ID)){
				return temphp;
			}
		}
		return null;
	}

	public TempRoute getTempRoute (String id) {
		for (TempRoute route : this.routes) {
			if(route.id.equals(id)){
				return route;
			}
		}
		return null;
	}

}
