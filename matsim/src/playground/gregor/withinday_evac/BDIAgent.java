/* *********************************************************************** *
 * project: org.matsim.*
 * BDIAgent.java
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

import java.util.Collection;

import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.plans.Person;

import playground.gregor.withinday_evac.information.InformationEntity;
import playground.gregor.withinday_evac.information.InformationExchanger;
import playground.gregor.withinday_evac.information.InformationStorage;
import playground.gregor.withinday_evac.information.Message;
import playground.gregor.withinday_evac.information.NextLinkMessage;
import playground.gregor.withinday_evac.mobsim.OccupiedVehicle;


public class BDIAgent {

	private final Person person;
	private final OccupiedVehicle vehicle;
	private final InformationExchanger informationExchanger;

	public BDIAgent(final Person person, final OccupiedVehicle v, final InformationExchanger informationExchanger){
		this.person = person;
		this.vehicle = v;
		this.vehicle.setAgent(this);
		this.informationExchanger = informationExchanger;
		
	}

	public Link replan(final double now, final Id nodeId) {
		final InformationStorage infos = this.informationExchanger.getInformationStorage(nodeId);
		final Link nextLink = null;
		updateBeliefs(infos.getInformation(now));
		
		
		final Message msg = new NextLinkMessage(nextLink);
		final InformationEntity ie = new InformationEntity(now,InformationEntity.MSG_TYPE.MY_NEXT_LINK,msg);
		infos.addInformationEntity(ie);
		return nextLink;
		
	}

	private void updateBeliefs(final Collection<InformationEntity> information) {
		// TODO Auto-generated method stub
		
	}
}
