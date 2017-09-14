package org.matsim.contrib.common.diversitygeneration.planselectors;

import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;

/* package */ class LegsSimilarityCalculator1 implements LegsSimilarityCalculator {
	static class Builder {
		private final Network network ;
		private double sameModeReward = 1. ;
		private double sameRouteReward = 1 ;
		// yyyy no idea if these are good defaults.  kai, sep'17
		Builder( Network network ) { this.network = network ; }
		public void setSameModeReward(double sameModeReward) { this.sameModeReward = sameModeReward; }
		public void setSameRouteReward(double sameRouteReward) { this.sameRouteReward = sameRouteReward; }
		public LegsSimilarityCalculator build() {
			return new LegsSimilarityCalculator1( network, sameModeReward, sameRouteReward ) ;
		}
	}
	private final Network network ;
	private final double sameRouteReward;
	private final double sameModeReward;
	private LegsSimilarityCalculator1( Network network, double sameModeReward, double sameRouteReward ) {
		this.network = network;
		this.sameModeReward = sameModeReward;
		this.sameRouteReward = sameRouteReward;
	}
	@Override public double calculateSimilarity(List<Leg> legs1, List<Leg> legs2) {
		double simil = 0. ;
		Iterator<Leg> it1 = legs1.iterator();
		Iterator<Leg> it2 = legs2.iterator();
		for ( ; it1.hasNext() && it2.hasNext(); ) {
			Leg leg1 = it1.next() ;
			Leg leg2 = it2.next() ;
			if ( leg1.getMode().equals( leg2.getMode() ) ) {
				simil += sameModeReward ;
			}
			// the easy way for the route is to not go along the links but just check for overlap.
			Route route1 = leg1.getRoute() ;
			Route route2 = leg2.getRoute() ;
			// currently only for network routes:
			NetworkRoute nr1, nr2 ;
			if ( route1 instanceof NetworkRoute ) {
				nr1 = (NetworkRoute) route1 ;
			} else {
				continue ; // next leg
			}
			if ( route2 instanceof NetworkRoute ) {
				nr2 = (NetworkRoute) route2 ;
			} else {
				continue ; // next leg
			}
			simil += sameRouteReward * RouteUtils.calculateCoverage(nr1, nr2, network) ;
		}
		return simil ;
	}
}