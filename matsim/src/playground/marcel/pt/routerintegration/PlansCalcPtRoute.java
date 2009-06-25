/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcPtRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel.pt.routerintegration;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;

import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.mmoyo.PTRouter.PTTimeTable2;
import playground.mmoyo.TransitSimulation.SimplifyPtLegs;
import playground.mmoyo.TransitSimulation.TransitRouteFinder;
import playground.mmoyo.TransitSimulation.TransitScheduleToPTTimeTableConverter;
import playground.mmoyo.input.PTNetworkFactory2;

public class PlansCalcPtRoute extends PlansCalcRoute {

	private final SimplifyPtLegs planSimplifier;
	private final TransitRouteFinder ptRouter;

	private Plan currentPlan = null;
	private final List<Tuple<Leg, List<Leg>>> legReplacements = new LinkedList<Tuple<Leg, List<Leg>>>();

	public PlansCalcPtRoute(final PlansCalcRouteConfigGroup config, final Network network,
			final TravelCost costCalculator, final TravelTime timeCalculator,
			final LeastCostPathCalculatorFactory factory, final TransitSchedule schedule) {
		super(config, network, costCalculator, timeCalculator, factory);
		this.planSimplifier = new SimplifyPtLegs();

//		String timeTableFilename = "../shared-svn/studies/schweiz-ivtch/pt-experimental/inptnetfile.xml";
//		String ptNewLinesFilename = "../shared-svn/studies/schweiz-ivtch/pt-experimental/TestCase/InPTDIVA.xml";
		String transitScheduleFilename = "test/input/playground/marcel/pt/transitSchedule.xml";
		
		TransitScheduleToPTTimeTableConverter transitScheduleToPTTimeTableConverter = new TransitScheduleToPTTimeTableConverter();
		PTTimeTable2 timeTable = transitScheduleToPTTimeTableConverter.getPTTimeTable(transitScheduleFilename, network);
//		new PTLineAggregator(ptNewLinesFilename, net, timeTable).addLines();

		PTNetworkFactory2 ptNetFactory = new PTNetworkFactory2(); // this looks like it could be an abstract Utility class.
		// internal setup of things should happen internally somewhere, not be called externally...
//		ptNetFactory.createTransferLinks(net, timeTable); // this looks like it could be a static helper method
//		ptNetFactory.createDetachedTransfers(net, 300); // this also.
//
//		this.ptRouter = new TransitRouteFinder(new PTRouter2(net, timeTable)); // should work with transitSchedule only
		
		this.ptRouter = new TransitRouteFinder(schedule);
	}

	@Override
	public void handlePlan(final Plan plan) {
		this.planSimplifier.run(plan);
		this.currentPlan = plan;
		this.legReplacements.clear();
		super.handlePlan(plan);
		this.replaceLegs();
		this.currentPlan = null;

	}

	@Override
	public double handleLeg(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		if (TransportMode.pt.equals(leg.getMode())) {
			return this.handlePtPlan(leg, fromAct, toAct, depTime);
		}
		return super.handleLeg(leg, fromAct, toAct, depTime);
	}

	private double handlePtPlan(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		List<Leg> legs= this.ptRouter.calculateRoute(fromAct, toAct, this.currentPlan.getPerson());
		this.legReplacements.add(new Tuple<Leg, List<Leg>>(leg, legs));

		double travelTime = 0.0;
		for (Leg leg2 : legs) {
			travelTime += leg2.getTravelTime();
		}
		return travelTime; // this is currently only the time traveling, not including the time waiting at stops etc.
	}

	private void replaceLegs() {
		Iterator<Tuple<Leg, List<Leg>>> replacementIterator = this.legReplacements.iterator();
		if (!replacementIterator.hasNext()) {
			return;
		}
		List<PlanElement> planElements = this.currentPlan.getPlanElements();
		Tuple<Leg, List<Leg>> currentTuple = replacementIterator.next();
		for (int i = 0; i < this.currentPlan.getPlanElements().size(); i++) {
			PlanElement pe = planElements.get(i);
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				if (leg == currentTuple.getFirst()) {
					// do the replacement
					boolean isFirstLeg = true;
					for (Leg leg2 : currentTuple.getSecond()) {
						if (isFirstLeg) {
							planElements.set(i, leg2);
						} else {
							i++;
							planElements.add(i, new ActivityImpl("pt interaction", leg2.getRoute().getStartLink()));
							i++;
							planElements.add(i, leg2);
						}
					}
					if (!replacementIterator.hasNext()) {
						return;
					}
					currentTuple = replacementIterator.next();
				}
			}
		}

	}

}
