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

package playground.gregor.withindayevac.analyzer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.Link;

import playground.gregor.withindayevac.Beliefs;
import playground.gregor.withindayevac.communication.InformationEntity;
import playground.gregor.withindayevac.communication.NextLinkMessage;
import playground.gregor.withindayevac.communication.InformationEntity.MSG_TYPE;

public class FollowHerdAnalyzer implements Analyzer {

	private final Beliefs beliefs;
	private double coef;

	public FollowHerdAnalyzer(final Beliefs beliefs) {
		this.beliefs = beliefs;
		this.coef = 1;
	}
	public NextLinkOption getAction(final double now) {
		Id nodeId = this.beliefs.getCurrentLink().getToNode().getId();
		Collection<InformationEntity> ies = this.beliefs.getInfos(now, MSG_TYPE.MY_NEXT_LINK, nodeId);
//		final ArrayList<InformationEntity> ies = this.beliefs.getInfos().get(MSG_TYPE.MY_NEXT_LINK);
		if (ies.size() == 0) {
			return null;
		}
		final HashMap<Link,Counter> counts = new HashMap<Link,Counter>();
		ArrayList<Link> indices = new ArrayList<Link>();
		for (final InformationEntity ie : ies) {
			final NextLinkMessage m = (NextLinkMessage) ie.getMsg();
			final Counter c = counts.get(m.getLink());
			if (c != null) {
				c.value += 1.0;
			} else {
				counts.put(m.getLink(), new Counter(1));
				indices.add(m.getLink());
				
			}
			
		}
				
			
		


				
		
//		return getNextLinkLogit(indices, counts);
		return getNextLinkMaxCount(counts);
	}
	
	private NextLinkOption getNextLinkLogit(final ArrayList<Link> indices,
			final HashMap<Link, Counter> counts) {
		double weightSum = 0;
		for (final Link l : indices) {
			Counter c = counts.get(l);
			c.value = Math.exp(c.value);
			weightSum += c.value; 
		}
		
		double selNum = weightSum * MatsimRandom.random.nextDouble();
		for (final Link l : indices) {
			Counter c = counts.get(l);
			selNum -= c.value;
			if (selNum <= 0) {
				return new NextLinkOption(l,1 * this.coef);
			}
		}
		return null;
	}
	private NextLinkOption getNextLinkMaxCount(final HashMap<Link, Counter> counts) {
		double maxCount = 0;
		Link ret = null;
		for (Map.Entry<Link, Counter> e : counts.entrySet()) {
			if (e.getValue().value > maxCount) {
				maxCount = e.getValue().value;
				ret = e.getKey();
			}
			
		}
		
		if (ret == null) {
			return null;
		}
		return new NextLinkOption(ret,1 * this.coef);
	}
	public ArrayList<NextLinkOption> getActions(final double now) {
		throw new RuntimeException("remove this method from class!!!! don't use this anymore!!!!");
//		final ArrayList<InformationEntity> ies = this.beliefs.getInfos().get(MSG_TYPE.MY_NEXT_LINK);
//		if (ies == null) {
//			return null;
//		}
//		final HashMap<Link,Counter> counts = new HashMap<Link,Counter>();
//		int allCount = 0;
//		for (final InformationEntity ie : ies) {
//			final NextLinkMessage m = (NextLinkMessage) ie.getMsg();
//			final Counter c = counts.get(m.getLink());
//			allCount++;
//			if (c != null) {
//				c.value++;
//				
//			} else {
//				counts.put(m.getLink(), new Counter(1));
//			}
//			
//		}
//		
//		ArrayList<NextLinkOption> ret = new ArrayList<NextLinkOption>();
//		for (Entry<Link, Counter> e : counts.entrySet()) {
//			ret.add(new NextLinkOption(e.getKey(),this.coef * e.getValue().value / allCount));
//		}
//		
//		return ret;
	}
	
	
	
	private static class Counter {
		double value;
		public Counter(final double i) {
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
