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

package playground.gregor.withinday_evac.analyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.matsim.network.Link;

import playground.gregor.withinday_evac.beliefs.Beliefs;
import playground.gregor.withinday_evac.communication.FollowGuideMessage;
import playground.gregor.withinday_evac.communication.InformationEntity;
import playground.gregor.withinday_evac.communication.InformationEntity.MSG_TYPE;

public class FollowGuideAnalyzer implements Analyzer {

	private final Beliefs beliefs;

	public FollowGuideAnalyzer(final Beliefs beliefs) {
		this.beliefs = beliefs;
	}
	
	public Action getAction(final double now) {
		final ArrayList<InformationEntity> ies = this.beliefs.getInfos().get(MSG_TYPE.FOLLOW_ME);
		if (ies == null) {
			return new NextLinkAction(null,0);
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
		
		
		return new NextLinkAction(link,1);
	}
	
	
	
	
	private static class Counter {
		int value;
		public Counter(final int i) {
			this.value = i;
		}
		
		@Override
		public String toString(){
			return this.value + "";
		}
		
	}
	
}
