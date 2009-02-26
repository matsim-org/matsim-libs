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

package playground.gregor.withindayevac.analyzer;

import java.util.HashSet;

import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.TimeVariantLinkImpl;

import playground.gregor.withindayevac.Beliefs;
import playground.gregor.withindayevac.communication.InformationEntity;
import playground.gregor.withindayevac.communication.LinkBlockedMessage;
import playground.gregor.withindayevac.communication.InformationEntity.MSG_TYPE;

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
				//FIXME
//				this.beliefs.addIE(ie);
				this.blockedLinks.add(link);
				throw new RuntimeException("something to fix here!!!");
			}
		}
	}

	public boolean isLinkBlocked(final Link link) {
		return this.blockedLinks.contains(link);
	}

	

	public void setCoefficient(final double coef) {
		this.coef = coef;
		
	}

	public NextLinkOption getAction(final double now) {
		// TODO Auto-generated method stub
		return null;
	}
}
