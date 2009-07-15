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
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.transitSchedule.api.TransitSchedule;

import playground.mmoyo.PTRouter.PTTimeTable;
import playground.mmoyo.TransitSimulation.SimplifyPtLegs;
import playground.mmoyo.TransitSimulation.TransitRouteFinder;
import playground.mmoyo.input.transitconverters.TransitScheduleToPTTimeTableConverter;
import playground.mmoyo.input.PTNetworkFactory;

public class PlansCalcPtRoute extends PlansCalcRoute {

	private final SimplifyPtLegs planSimplifier;
	private final TransitRouteFinder ptRouter;

	private PlanImpl currentPlan = null;
	private final List<Tuple<LegImpl, List<LegImpl>>> legReplacements = new LinkedList<Tuple<LegImpl, List<LegImpl>>>();

	public PlansCalcPtRoute(final PlansCalcRouteConfigGroup config, final NetworkLayer network,
			final TravelCost costCalculator, final TravelTime timeCalculator,
			final LeastCostPathCalculatorFactory factory, final TransitSchedule schedule) {
		super(config, network, costCalculator, timeCalculator, factory);
		this.planSimplifier = new SimplifyPtLegs();

//		String timeTableFilename = "../shared-svn/studies/schweiz-ivtch/pt-experimental/inptnetfile.xml";
//		String ptNewLinesFilename = "../shared-svn/studies/schweiz-ivtch/pt-experimental/TestCase/InPTDIVA.xml";
		String transitScheduleFilename = "test/input/playground/marcel/pt/transitSchedule.xml";
		
		TransitScheduleToPTTimeTableConverter transitScheduleToPTTimeTableConverter = new TransitScheduleToPTTimeTableConverter();
		PTTimeTable timeTable = transitScheduleToPTTimeTableConverter.getPTTimeTable(transitScheduleFilename, network);
//		new PTLineAggregator(ptNewLinesFilename, net, timeTable).addLines();

		PTNetworkFactory ptNetFactory = new PTNetworkFactory(); // this looks like it could be an abstract Utility class.
		// internal setup of things should happen internally somewhere, not be called externally...
//		ptNetFactory.createTransferLinks(net, timeTable); // this looks like it could be a static helper method
//		ptNetFactory.createDetachedTransfers(net, 300); // this also.
//
//		this.ptRouter = new TransitRouteFinder(new PTRouter2(net, timeTable)); // should work with transitSchedule only
		
		this.ptRouter = new TransitRouteFinder(schedule);
	}

	@Override
	public void handlePlan(final PlanImpl plan) {
		this.planSimplifier.run(plan);
		this.currentPlan = plan;
		this.legReplacements.clear();
		super.handlePlan(plan);
		this.replaceLegs();
		this.currentPlan = null;

	}

	@Override
	public double handleLeg(final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {
		if (TransportMode.pt.equals(leg.getMode())) {
			return this.handlePtPlan(leg, fromAct, toAct, depTime);
		}
		return super.handleLeg(leg, fromAct, toAct, depTime);
	}

	private double handlePtPlan(final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {
		List<LegImpl> legs= this.ptRouter.calculateRoute(fromAct, toAct, this.currentPlan.getPerson());
		this.legReplacements.add(new Tuple<LegImpl, List<LegImpl>>(leg, legs));

		double travelTime = 0.0;
		for (LegImpl leg2 : legs) {
			travelTime += leg2.getTravelTime();
		}
		return travelTime; // this is currently only the time traveling, not including the time waiting at stops etc.
	}

	private void replaceLegs() {
		Iterator<Tuple<LegImpl, List<LegImpl>>> replacementIterator = this.legReplacements.iterator();
		if (!replacementIterator.hasNext()) {
			return;
		}
		List<PlanElement> planElements = this.currentPlan.getPlanElements();
		Tuple<LegImpl, List<LegImpl>> currentTuple = replacementIterator.next();
		for (int i = 0; i < this.currentPlan.getPlanElements().size(); i++) {
			PlanElement pe = planElements.get(i);
			if (pe instanceof LegImpl) {
				LegImpl leg = (LegImpl) pe;
				if (leg == currentTuple.getFirst()) {
					// do the replacement
					boolean isFirstLeg = true;
					for (LegImpl leg2 : currentTuple.getSecond()) {
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
