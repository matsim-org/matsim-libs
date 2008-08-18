/* *********************************************************************** *
 * project: org.matsim.*
 * NextLinkBlockedAnalyzer.java
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

package playground.gregor.withinday_evac.analyzer;

import java.util.HashSet;

import org.matsim.network.Link;
import org.matsim.network.TimeVariantLinkImpl;

import playground.gregor.withinday_evac.Beliefs;
import playground.gregor.withinday_evac.communication.InformationEntity;
import playground.gregor.withinday_evac.communication.LinkBlockedMessage;
import playground.gregor.withinday_evac.communication.InformationEntity.MSG_TYPE;

public class BlockedLinksAnalyzer implements Analyzer {

	private final Beliefs beliefs;
	
	private final HashSet<Link> blockedLinks = new HashSet<Link>();

	private double coef;
	
	public BlockedLinksAnalyzer(final Beliefs beliefs) {
		this.beliefs = beliefs;
	}
	
	public void update(final double now) {
		this.blockedLinks.clear();
		for (final Link link : this.beliefs.getCurrentLink().getToNode().getOutLinks().values()) {
			if (((TimeVariantLinkImpl)link).getFreespeed(now) <= 0.){
				final InformationEntity ie = new InformationEntity(now,MSG_TYPE.LINK_BLOCKED,new LinkBlockedMessage(link));
				ie.setResend(true);
				this.beliefs.addIE(ie);
				this.blockedLinks.add(link);
			}
		}
	}

	public boolean isLinkBlocked(final Link link) {
		return this.blockedLinks.contains(link);
	}

	

	public void setCoefficient(final double coef) {
		this.coef = coef;
		
	}
}
