/* *********************************************************************** *
 * project: org.matsim.*
 * Beliefs.java
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

package playground.gregor.withinday_evac;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.matsim.network.Link;

import playground.gregor.withinday_evac.communication.InformationEntity;
import playground.gregor.withinday_evac.communication.InformationEntity.MSG_TYPE;

public class Beliefs {
	
	
	HashMap<MSG_TYPE,ArrayList<InformationEntity>> infos = new HashMap<MSG_TYPE,ArrayList<InformationEntity>>(); 
	private Link currentLink;
//	public Beliefs() {
//		
//	}

	public void update(final Collection<InformationEntity> information) {
		this.infos.clear();
		
		for (final InformationEntity ie : information){
			addIE(ie);
		}
		
	}
	
	public void addIE(final InformationEntity ie) {
		final MSG_TYPE type = ie.getMsgType();

		ArrayList<InformationEntity> info = this.infos.get(type);
		if (info == null) {
			info = new ArrayList<InformationEntity>();
			this.infos.put(type, info);
		}
		info.add(ie);
	}
	
	public HashMap<MSG_TYPE,ArrayList<InformationEntity>> getInfos() {
		return this.infos;
	}

	public void setCurrentLink(final Link currentLink) {
		this.currentLink = currentLink;
	}

	public Link getCurrentLink() {
		return this.currentLink;
	}

}
