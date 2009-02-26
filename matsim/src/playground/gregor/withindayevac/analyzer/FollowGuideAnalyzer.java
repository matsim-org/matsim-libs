/* *********************************************************************** *
 * project: org.matsim.*
 * FollowGuideAnalyzer.java
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Link;

import playground.gregor.withindayevac.Beliefs;
import playground.gregor.withindayevac.communication.FollowGuideMessage;
import playground.gregor.withindayevac.communication.InformationEntity;
import playground.gregor.withindayevac.communication.InformationEntity.MSG_TYPE;

public class FollowGuideAnalyzer implements Analyzer {

	private final Beliefs beliefs;
	private double coef;

	public FollowGuideAnalyzer(final Beliefs beliefs) {
		this.beliefs = beliefs;
	}
	
	public NextLinkOption getAction(final double now) {
		Id nodeId = this.beliefs.getCurrentLink().getToNode().getId();
		Collection<InformationEntity> ies = this.beliefs.getInfos(now, MSG_TYPE.FOLLOW_ME, nodeId);
//		final ArrayList<InformationEntity> ies = this.beliefs.getInfos().get(MSG_TYPE.FOLLOW_ME);
		if (ies.size() == 0) {
			return new NextLinkOption(null,0);
		}
		final HashMap<Link,Counter> counts = new HashMap<Link,Counter>();
		for (final InformationEntity ie : ies) {
			final FollowGuideMessage m = (FollowGuideMessage) ie.getMsg();
			final Counter c = counts.get(m.getLink().getId());
			if (c != null) {
				c.value++;
				
			} else {
				counts.put(m.getLink(), new Counter(1));
			}
			
		}
		
		int max_val = 0;
		Link link = null;
		
		for (final Entry<Link, Counter> e : counts.entrySet()) {
			if (e.getValue().value > max_val) {
				max_val = e.getValue().value;
				link = e.getKey();
			}
		}
		
		
		return new NextLinkOption(link,max_val * this.coef);
	}
	
	
	
	
	private class Counter {
		int value;
		public Counter(final int i) {
			this.value = i;
		}
		
		@Override
		public String toString(){
			return this.value + "";
		}
		
	}




	public void setCoefficient(final double coef) {
		this.coef = coef;
		
	}
	
}
