/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelFitnessFunction.java
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

package playground.anhorni.locationchoice.planomatLocationChoice.costestimators;

import java.util.ArrayList;
import java.util.TreeMap;

import org.jgap.FitnessFunction;
import org.jgap.IChromosome;
import org.jgap.impl.IntegerGene;
import org.matsim.basic.v01.BasicAct;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.BasicPlanImpl;
import org.matsim.basic.v01.Id;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Plan;
import org.matsim.router.PlansCalcRouteDijkstra;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.scoring.ScoringFunction;

public class CharyparNagelFitnessFunction extends FitnessFunction {

	public static final double FITNESS_OFFSET = 10000.0;
	private static final long serialVersionUID = 1L;

	private final  ScoringFunction sf;
	private final Plan plan;
	private  NetworkLayer network = null;
	private  TravelCostI travelCostCalculator = null;
	private  TravelTimeI travelTimeCalculator = null;

	final Facilities facilities = (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);
	final TreeMap<Id,Facility> shop_facilities=new TreeMap<Id,Facility>();
	final TreeMap<Id,Facility> leisure_facilities=new TreeMap<Id,Facility>();

	public CharyparNagelFitnessFunction(
			final ScoringFunction sf,
			final Plan plan,
			final NetworkLayer network,
			final TravelCostI travelCostCalculator,
			final TravelTimeI travelTimeCalculator) {
		super();
		this.sf = sf;
		this.plan = plan;
		this.network=network;
		this.travelCostCalculator=travelCostCalculator;
		this.travelTimeCalculator=travelTimeCalculator;


		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_gt2500sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get1000sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get400sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get100sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_other"));

		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_gastro"));
		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_culture"));
		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_sports"));

	}

	public Plan evaluateLast(final IChromosome a_subject, final Plan plan) {
		this.evaluate(a_subject);
		return this.plan;
	}

	@Override
	protected double evaluate(final IChromosome a_subject) {
		double planScore = 0.0;

		// get now routes and times ----------------------------------------------
		// change locations:
		 final Id [] shop_array = this.shop_facilities.keySet().toArray(
		    		new Id[this.shop_facilities.keySet().size()]);

		int gene_nr=0;
		final BasicPlanImpl.ActIterator iter_act = this.plan.getIteratorAct();
		while(iter_act.hasNext()) {
			final BasicAct act = iter_act.next();
			if (act.getType().startsWith("s")) {

				final int shop_index=((IntegerGene)a_subject.getGene(gene_nr)).intValue();
				final Facility facility=this.shop_facilities.get(
						shop_array[shop_index]);

				this.exchangeFacility("s",facility, this.plan);
			}
			gene_nr++;
		}

		// calculate new route --------------

		final PlansCalcRouteDijkstra router=new PlansCalcRouteDijkstra(
				this.network, this.travelCostCalculator, this.travelTimeCalculator);
		router.run(this.plan);

		// do the scoring
		this.sf.reset();
		final BasicPlanImpl.LegIterator iter_leg = this.plan.getIteratorLeg();
		while(iter_leg.hasNext()) {
			final BasicLeg leg = iter_leg.next();
			this.sf.startLeg(leg.getDepTime(), null);
			this.sf.endLeg(leg.getDepTime()+leg.getTravTime());
		}

		this.sf.finish();
		planScore = this.sf.getScore();
		// JGAP accepts only fitness values >= 0. bad plans often have negative scores. So we have to
		// - make sure a fitness value will be >= 0, but
		// - see that the fitness landscape will not be distorted too much by this, so we will add an offset (this s**ks, but works)
		// - could become a problem if some calculation in the GA is based on score ratio (e.g. the calculation of a selection probability)
		return Math.max(0.0, planScore + CharyparNagelFitnessFunction.FITNESS_OFFSET);
	}


	public void exchangeFacility(final String type, final Facility facility, final Plan plan) {
		// modify plan by randomly exchanging a link (facility) in the plan
		final ArrayList<?> actslegs = plan.getActsLegs();
		for (int j = 0; j < actslegs.size(); j=j+2) {
			final Act act = (Act)actslegs.get(j);
			if (act.getType().startsWith(type)) {
				// plans: link, coords
				// facilities: coords
				// => use coords
				act.setCoord(facility.getCenter());
			}
		}

		// loop over all <leg>s, remove route-information
		for (int j = 1; j < actslegs.size(); j=j+2) {
			final Leg leg = (Leg)actslegs.get(j);
			leg.setRoute(null);
		}
	}
}









