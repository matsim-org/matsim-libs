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

package playground.gregor.withindayevac;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.Link;

import playground.gregor.withindayevac.communication.InformationEntity;
import playground.gregor.withindayevac.communication.InformationExchanger;
import playground.gregor.withindayevac.communication.InformationStorage;
import playground.gregor.withindayevac.communication.InformationEntity.MSG_TYPE;

public class Beliefs {
	
	
	HashMap<MSG_TYPE,ArrayList<InformationEntity>> infos = new HashMap<MSG_TYPE,ArrayList<InformationEntity>>(); 
	private Link currentLink;
//	public Beliefs() {
//		
//	}
	private final InformationExchanger informationExchanger;

	

	public Beliefs(final InformationExchanger informationExchanger) {
		this.informationExchanger = informationExchanger;
	}

//	public void update(final Collection<InformationEntity> information) {
//		this.infos.clear();
//		
//		for (final InformationEntity ie : information){
//			addIE(ie);
//		}
//		
//	}
//	
//	public void addIE(final InformationEntity ie) {
//		final MSG_TYPE type = ie.getMsgType();
//
//		ArrayList<InformationEntity> info = this.infos.get(type);
//		if (info == null) {
//			info = new ArrayList<InformationEntity>();
//			this.infos.put(type, info);
//		}
//		info.add(ie);
//	}
//	
//	public HashMap<MSG_TYPE,ArrayList<InformationEntity>> getInfos() {
//		return this.infos;
//	}

	public Collection<InformationEntity> getInfos(final double now, final MSG_TYPE type, final Id id) {
		Collection<InformationEntity> ret = new ArrayList<InformationEntity>();
		InformationStorage is = this.informationExchanger.getInformationStorage(id);
		for (InformationEntity ie : is.getInformation(now)) {
			if (ie.getMsgType() == type) {
				ret.add(ie);
			}
				
		}
		return ret;
		
	}
	
	
	public void setCurrentLink(final Link currentLink) {
		this.currentLink = currentLink;
	}

	public Link getCurrentLink() {
		return this.currentLink;
	}

}
