/* *********************************************************************** *
 * project: org.matsim.*
 * FollowNextLinkAnalyzer.java
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

import playground.gregor.withinday_evac.Beliefs;
import playground.gregor.withinday_evac.communication.InformationEntity;
import playground.gregor.withinday_evac.communication.NextLinkMessage;
import playground.gregor.withinday_evac.communication.InformationEntity.MSG_TYPE;

public class FollowHerdAnalyzer implements Analyzer {

	private final Beliefs beliefs;
	private double coef;

	public FollowHerdAnalyzer(final Beliefs beliefs) {
		this.beliefs = beliefs;
		this.coef = 1;
	}
//	public Option getAction(final double now) {
//		final ArrayList<InformationEntity> ies = this.beliefs.getInfos().get(MSG_TYPE.MY_NEXT_LINK);
//		if (ies == null) {
//			return new NextLinkOption(null,0);
//		}
//		final HashMap<Link,Counter> counts = new HashMap<Link,Counter>();
//		for (final InformationEntity ie : ies) {
//			final NextLinkMessage m = (NextLinkMessage) ie.getMsg();
//			final Counter c = counts.get(m.getLink());
//			if (c != null) {
//				c.value++;
//				
//			} else {
//				counts.put(m.getLink(), new Counter(1));
//			}
//			
//		}
//		
//		int max_val = 0;
//		Link link = null;
//		for (final Entry<Link, Counter> e : counts.entrySet()) {
//			if (e.getValue().value > max_val) {
//				max_val = e.getValue().value;
//				link = e.getKey();
//			}
//		}
//		
//		
//		return new NextLinkOption(link,1);
//	}
	
	public ArrayList<NextLinkOption> getActions(final double now) {
		final ArrayList<InformationEntity> ies = this.beliefs.getInfos().get(MSG_TYPE.MY_NEXT_LINK);
		if (ies == null) {
			return null;
		}
		final HashMap<Link,Counter> counts = new HashMap<Link,Counter>();
		int allCount = 0;
		for (final InformationEntity ie : ies) {
			final NextLinkMessage m = (NextLinkMessage) ie.getMsg();
			final Counter c = counts.get(m.getLink());
			allCount++;
			if (c != null) {
				c.value++;
				
			} else {
				counts.put(m.getLink(), new Counter(1));
			}
			
		}
		
		ArrayList<NextLinkOption> ret = new ArrayList<NextLinkOption>();
		for (Entry<Link, Counter> e : counts.entrySet()) {
			ret.add(new NextLinkOption(e.getKey(),this.coef * e.getValue().value / allCount));
		}
		
		return ret;
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



	public void setCoefficient(final double coef) {
		this.coef = coef;
		
	}





}
