/* *********************************************************************** *
 * project: org.matsim.*
 * DecisionMaker.java
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
import java.util.HashMap;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.Link;

import playground.gregor.withinday_evac.analyzer.Analyzer;
import playground.gregor.withinday_evac.analyzer.BlockedLinksAnalyzer;
import playground.gregor.withinday_evac.analyzer.FollowGuideAnalyzer;
import playground.gregor.withinday_evac.analyzer.FollowHerdAnalyzer;
import playground.gregor.withinday_evac.analyzer.FollowPlanAnalyzer;
import playground.gregor.withinday_evac.analyzer.NextLinkOption;
import playground.gregor.withinday_evac.analyzer.Option;
import playground.gregor.withinday_evac.analyzer.ReRouteAnalyzer;

public class DecisionMaker {

	private final HashMap<String, Analyzer> analyzers;

	public DecisionMaker(final HashMap<String, Analyzer> analyzers) {
		this.analyzers = analyzers;
	}

	public Link chooseNextLink(final double now, final Id nodeId, final boolean isGuide) {

		//		if (this.analyzers.get("DestinationReachedAnalyzer").getAction(now) != null) {
		//			System.out.println("DestinationReached");
		//			return null;
		//		}
		if (isGuide) {
			return nextLinkGuide(now, nodeId);
		} else {
			return nextLink(now,nodeId);
		}




	}

	private Link nextLink(final double now, final Id nodeId) {

		ArrayList<NextLinkOption> options = getOptions(now);
        NextLinkOption o = drawOption(options);
		return o.getNextLink();		

	}

	private NextLinkOption drawOption(final ArrayList<NextLinkOption> options) {
		
		double expSum = 0;
		for (NextLinkOption o : options) {
			o.setConfidence(Math.exp(o.getConfidence()));
			expSum += o.getConfidence();
		}
		
		double selnum = expSum*MatsimRandom.random.nextDouble();
		for (NextLinkOption o : options) {
			selnum -= o.getConfidence();
			if (selnum <= 0) {
				return o;
			}
		}		
		return null;
	}

	private ArrayList<NextLinkOption> getOptions(final double now) {
		ArrayList<NextLinkOption> options = new ArrayList<NextLinkOption>();
		//			final BlockedLinksAnalyzer ba = ((BlockedLinksAnalyzer)this.analyzers.get("BlockedLinksAnalyzer"));
		//			ba.update(now);


		//			final FollowGuideAnalyzer a = (FollowGuideAnalyzer) this.analyzers.get("FollowGuideAnalyzer");
		//			Option ac = a.getAction(now);

		//			if (((NextLinkOption)ac).getNextLink() == null) {
		final FollowHerdAnalyzer b = (FollowHerdAnalyzer) this.analyzers.get("HerdAnalyzer");
		ArrayList<NextLinkOption> os = b.getActions(now);  
		if (os != null) {
			options.addAll(os);
		}
		//			} 
		//			
		final FollowPlanAnalyzer c = (FollowPlanAnalyzer) this.analyzers.get("FollowPlanAnalyzer");
		NextLinkOption o = (NextLinkOption) c.getAction(now);
		if (o != null) {
			options.add(o);
		}


		if (options.size() == 0) {
			final ReRouteAnalyzer d = (ReRouteAnalyzer) this.analyzers.get("ReRouteAnalyzer");
			options.add((NextLinkOption) d.getAction(now));
		}

		//			if (((NextLinkOption)ac).getNextLink() == null || ba.isLinkBlocked(((NextLinkOption)ac).getNextLink())) {
		//				final ReRouteAnalyzer c = (ReRouteAnalyzer) this.analyzers.get("ReRouteAnalyzer");
		//				ac = c.getAction(now);
		//			}
		//			
		//			if (((NextLinkOption)ac).getNextLink() == null) {
		//				throw new RuntimeException("this should not happen!!!");
		//			}
		//			
		//			return ((NextLinkOption)ac).getNextLink();


		return options;
	}

	private Link nextLinkGuide(final double now, final Id nodeId) {
		final BlockedLinksAnalyzer ba = ((BlockedLinksAnalyzer)this.analyzers.get("BlockedLinksAnalyzer"));
		ba.update(now);
		final FollowGuideAnalyzer a = (FollowGuideAnalyzer) this.analyzers.get("FollowGuideAnalyzer");
		Option ac = a.getAction(now);
		if (((NextLinkOption)ac).getNextLink() == null || ac.getConfidence() < 2 ) {
			return null;			
		}
		return ((NextLinkOption)ac).getNextLink();
	}



}
