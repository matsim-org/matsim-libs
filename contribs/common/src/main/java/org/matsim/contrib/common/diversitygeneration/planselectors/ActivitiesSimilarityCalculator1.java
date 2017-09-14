package org.matsim.contrib.common.diversitygeneration.planselectors;

import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;

/* package */ class ActivitiesSimilarityCalculator1 implements ActivitiesSimilarityCalculator {
	static class Builder {
		private double actTypeWeight = 1. ;
		private double locationWeight = 1. ;
		private double actTimeWeight = 1. ;
		// yyyy no idea if the above are good defaults!  kai, sep'17
		public void setActTypeWeight(double actTypeWeight) { this.actTypeWeight = actTypeWeight; }
		public void setLocationWeight(double locationWeight) { this.locationWeight = locationWeight; }
		public void setActTimeWeight(double actTimeWeight) { this.actTimeWeight = actTimeWeight; }
		public ActivitiesSimilarityCalculator build() {
			return new ActivitiesSimilarityCalculator1( actTypeWeight, locationWeight, actTimeWeight ) ;
		}
	}
	private final double actTypeWeight;
	private final double locationWeight;
	private final double actTimeWeight;
	private ActivitiesSimilarityCalculator1( double actTypeWeight, double locationWeight, double actTimeWeight ) {
		this.actTypeWeight = actTypeWeight;
		this.locationWeight = locationWeight;
		this.actTimeWeight = actTimeWeight;
	}
	@Override public double calculateSimilarity(List<Activity> activities1, List<Activity> activities2) {
		double simil = 0. ;
		Iterator<Activity> it1 = activities1.iterator() ;
		Iterator<Activity> it2 = activities2.iterator() ;
		for ( ; it1.hasNext() && it2.hasNext() ; ) {
			Activity act1 = it1.next() ;
			Activity act2 = it2.next() ;
			
			// activity type
			if ( act1.getType().equals(act2.getType() ) ) {
				simil += actTypeWeight ;
			}
			
			// activity location
			if ( act1.getCoord().equals( act2.getCoord() ) ){ 
				simil += locationWeight ;
			}
			
			// activity end times
			if ( Double.isInfinite( act1.getEndTime() ) && Double.isInfinite( act2.getEndTime() ) ){
				// both activities have no end time, no need to compute a similarity penalty
			} else {
				// both activities have an end time, comparing the end times
				
				// 300/ln(2) means a penalty of 0.5 for 300 sec difference
				double delta = Math.abs(act1.getEndTime() - act2.getEndTime()) ;
				simil += actTimeWeight * Math.exp( - delta/(300/Math.log(2)) ) ;
			}
			
		}
		
		// a positive value is interpreted as a penalty
		return simil ;
	}
}