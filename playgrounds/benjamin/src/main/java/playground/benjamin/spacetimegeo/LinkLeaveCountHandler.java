/* *********************************************************************** *
 * project: org.matsim.*
 * LinkEnterCountHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.spacetimegeo;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

/**
 * @author benjamin
 *
 */
public class LinkLeaveCountHandler implements IterationStartsListener, LinkLeaveEventHandler, PersonMoneyEventHandler {
	private static Logger logger = Logger.getLogger(LinkLeaveCountHandler.class);
	
	int iterationNo;

	double link3Counter = 0.0;
	double link9Counter = 0.0;
	double link11Counter = 0.0;
	
	double tollPaid = 0.0;


	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.iterationNo = event.getIteration();
	}
	
	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Link> linkId = event.getLinkId();
		
		if(linkId.equals(Id.create("3", Link.class))){
			logger.info("The agent is chosing route 3 in iteration " + this.iterationNo);
			link3Counter++;
		} else if(linkId.equals(Id.create("9", Link.class))){
			logger.info("The agent is chosing route 9 in iteration " + this.iterationNo);
			link9Counter++;
		} else if(linkId.equals(Id.create("11", Link.class))){
			logger.info("The agent is chosing route 11 in iteration " + this.iterationNo);
			link11Counter++;
		} else {
			// do nothing
		}
	}

	protected double getLink3Counter() {
		return link3Counter;
	}

	protected double getLink9Counter() {
		return link9Counter;
	}

	protected double getLink11Counter() {
		return link11Counter;
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		tollPaid += event.getAmount();
		logger.info("The agent is paying " + event.getAmount());
	}

	protected double getTollPaid() {
		return tollPaid;
	}
}
