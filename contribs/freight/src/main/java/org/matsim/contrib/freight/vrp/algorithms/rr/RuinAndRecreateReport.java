package org.matsim.contrib.freight.vrp.algorithms.rr;

import java.util.Collection;

import org.matsim.contrib.freight.vrp.basics.Tour;

public class RuinAndRecreateReport implements RuinAndRecreateListener{

	private int nOfIteration = 0;
	
	private double bestResult;
	
	private Collection<Tour> bestSolution;
	
	@Override
	public void inform(RuinAndRecreateEvent event) {
		nOfIteration++;
		bestResult = event.getCurrentResult();
		bestSolution = event.getCurrentSolution();
	}

	@Override
	public void finish() {
		System.out.println("totalCosts="+Math.round(bestResult));
		System.out.println("#vehicles="+getActiveTours());
		System.out.println("distance=" + getDistance());
		System.out.println("time=" + getTime());
//		for(Tour t : bestSolution){
//			if(t.getActivities().size() <= 2){
//				continue;
//			}
//			System.out.println(t);
//			System.out.println("tpCosts=" + round(t.costs.generalizedCosts));
//			System.out.println("tpDistance=" + round(t.costs.distance));
//			System.out.println("tpTime=" + round(t.costs.time));
//		}
	}

	private double getTime() {
		double time = 0.0;
		for(Tour t : bestSolution){
			if(t.getActivities().size()>2){
				time+=t.costs.time;
			}
		}
		return time;
	}

	private double getDistance() {
		double dist = 0.0;
		for(Tour t : bestSolution){
			if(t.getActivities().size()>2){
				dist+=t.costs.distance;
			}
		}
		return dist;
	}

	private int getActiveTours() {
		int nOfTours = 0;
		for(Tour t : bestSolution){
			if(t.getActivities().size()>2){
				nOfTours++;
			}
		}
		return nOfTours;
	}

	private Long round(double time) {
		return Math.round(time);
	}
	
	

}
