/* *********************************************************************** *
 * project: org.matsim.*
 * ComparingDistanceStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.telaviv.analysis;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Based on distance stats method from LocationChoice contrib. It introduces
 * two changes:
 * - travel distances are taken from the previous route instead of using the crow fly distance
 * - results are compared to provided reference values
 * 
 * @author cdobler
 */
public class ComparingDistanceStats implements IterationEndsListener {	

	private double analysisBinSize;
	private double analysisBoundary;
	private ComparingBins bins;
	private String bestOrSelected = "selected";
	private String type = null;
	private ActTypeConverter actTypeConverter;
	private String mode;
	private String idExclusion;

	public ComparingDistanceStats(double analysisBinSize, double analysisBoundary, String idExclusion, String bestOrSelected, String type, ActTypeConverter actTypeConverter, String mode, double[] referenceShares) {
		this.analysisBinSize = analysisBinSize;
		this.analysisBoundary = analysisBoundary;
		this.idExclusion = idExclusion;
		this.bestOrSelected = bestOrSelected;
		this.type = type;
		this.actTypeConverter = actTypeConverter;
		this.mode = mode;
		this.bins = new ComparingBins(this.analysisBinSize, this.analysisBoundary, type + " " + mode + " distance distribution", referenceShares);
		
	}
	
	public void notifyIterationEnds(final IterationEndsEvent event) {	
		this.bins.clear();

        for (Person p : event.getServices().getScenario().getPopulation().getPersons().values()) {
			
			// continue if person is in the analysis population or if the id is not numeric
			if (!this.isInteger(p.getId().toString()) ||
					Integer.parseInt(p.getId().toString()) > Integer.parseInt(this.idExclusion)) continue;
					
			PlanImpl plan = (PlanImpl) p.getSelectedPlan();
			
			if (bestOrSelected.equals("best")) {
				double bestPlanScore = -999.0;
				int bestIndex = 0;
				int cnt = 0;
				for (Plan planTmp : p.getPlans()) {
					if (planTmp.getScore() > bestPlanScore){
						bestPlanScore = planTmp.getScore();
						bestIndex = cnt;
					}
					cnt++;
				}
				plan = (PlanImpl) p.getPlans().get(bestIndex);	
				//log.info(plan.getScore() + "\t" + bestIndex + "\tperson " + p.getId());
			}		
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					if (this.actTypeConverter.convertType(((Activity) pe).getType()).equals(this.actTypeConverter.convertType(type)) &&
							plan.getPreviousLeg((Activity)pe).getMode().equals(this.mode)) {
						double distance = -1.0;
						Leg previousLeg = plan.getPreviousLeg((Activity)pe);
						Route route = previousLeg.getRoute();
						if (route instanceof NetworkRoute) {
							if (route.getDistance() != Double.NaN) distance = route.getDistance();
							else distance = RouteUtils.calcDistanceExcludingStartEndLink((NetworkRoute) route, event.getServices().getScenario().getNetwork());
						} else {
							if (route.getDistance() != Double.NaN) distance = route.getDistance();
							else distance = CoordUtils.calcEuclideanDistance(((Activity) pe).getCoord(), plan.getPreviousActivity(plan.getPreviousLeg((Activity)pe)).getCoord());
						}
						this.bins.addVal(distance, 1.0);
					}	
				}
			}
		}
		
		String path = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "comparing,plan=" + this.bestOrSelected +
				",analysisBinSize=" + this.analysisBinSize + ",analysisBoundary=" + this.analysisBoundary + ",activityType=" + type + 
				",mode=" + mode);
		this.bins.plotBinnedDistribution(path, "#", "m");
	}
	
	private boolean isInteger(String str) {
	    try {
	        Integer.parseInt(str);
	        return true;
	    } catch (NumberFormatException nfe) {}
	    return false;
	}
}
