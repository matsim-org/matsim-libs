package org.matsim.pt.withinday;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.pt.PtConstants;

public class PlanSegments {

	public static List<PlanSegment> segmentize(List<PlanElement> elements) {
		return segmentize(elements, act -> !act.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE));
	}
	
	public static List<PlanSegment> segmentize(List<PlanElement> elements, Predicate<Activity> nonDummy) {
		List<PlanSegment> result = new ArrayList<>();
		
		List<PlanElement> segmentList = new ArrayList<>();
		for (PlanElement pe : elements) {
			if (segmentList.size() % 2 == 0) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					if (nonDummy.test(act) && segmentList.size() > 2) {
						segmentList.add(pe);
						PlanSegment segment = new PlanSegment(segmentList);
						result.add(segment);
						segmentList.clear();
					}
					segmentList.add(pe);
				}
				else {
					throw new IllegalArgumentException("PlanElements at even positions must be of type Activity");
				}
			}
			else {
				if (pe instanceof Leg) {
					segmentList.add(pe);
				}
				else {
					throw new IllegalArgumentException("PlanElements at odd positions must be of type Leg");
				}
			}
		}
		
		return result;
	}
	
	
	public static class PlanSegment {
		private List<Activity> activities;
		private List<Leg> legs;
		
		public PlanSegment(List<PlanElement> elements) {
			if (elements.size() < 3 || elements.size() % 2 != 1) {
				throw new IllegalArgumentException("A PlanSegment must have an odd number of and at least three PlanElements");
			}
			
			activities = new ArrayList<>(elements.size()/2+1);
			legs = new ArrayList<>(elements.size()/2);
			
			for (int t=0; t+1 < elements.size(); t+=2) {
				PlanElement actEl = elements.get(t);
				PlanElement legEl = elements.get(t+1);
				if (actEl instanceof Activity) {
					activities.add((Activity)actEl);
				}
				else {
					throw new IllegalArgumentException("Even positions should contain activities");
				}
				if (legEl instanceof Leg) {
					legs.add((Leg)legEl);
				}
				else {
					throw new IllegalArgumentException("Odd positions should contain legs");
				}
			}
			PlanElement last = elements.get(elements.size()-1);
			if (last instanceof Activity) {
				activities.add((Activity)last);
			}
			else {
				throw new IllegalArgumentException("Even Positions should contain activities");
			}
				
		}
		
		public Activity getStart() {
			return activities.get(0);
		}
		
		public Activity getEnd() {
			return activities.get(activities.size()-1);
		}
		
		@Override
		public String toString() {
			return getStart() + " ... " + getEnd();
		}
	}
	
}
