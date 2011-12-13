/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyseEquilibriumOptimalPlans.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.analysis.aposteriorianalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
import playground.thibautd.jointtripsoptimizer.population.PopulationWithCliques;
import playground.thibautd.utils.charts.WrapperChartUtil;

/**
 * Generates aggregated resutls from the plans files exported by
 * {@link GenerateEquilibriumOptimalPlans}
 * @author thibautd
 */
public class EquilibriumOptimalPlansAnalyser {
	private Map<Id, ComparativePlan> plans = new HashMap<Id, ComparativePlan>();

	// /////////////////////////////////////////////////////////////////////////
	// construction
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * @param untoggledPopulation
	 * @param toggledPopulation
	 * @param individualPopulation
	 */
	public EquilibriumOptimalPlansAnalyser(
			final PopulationWithCliques untoggledPopulation,
			final PopulationWithCliques toggledPopulation,
			final PopulationWithCliques individualPopulation) {
		run(untoggledPopulation,
			toggledPopulation,
			individualPopulation);
	}

	// /////////////////////////////////////////////////////////////////////////
	// run data extraction
	// /////////////////////////////////////////////////////////////////////////
	private void run(
			final PopulationWithCliques untoggledPopulation,
			final PopulationWithCliques toggledPopulation,
			final PopulationWithCliques individualPopulation) {
		Map<Id, ? extends Person> toggledPersons = new HashMap<Id, Person>(toggledPopulation.getPersons());
		Map<Id, ? extends Person> individualPersons = new HashMap<Id, Person>(individualPopulation.getPersons());

		Counter counter = new Counter( this.getClass().getSimpleName()+": processing info for agent # " );
		for (Map.Entry<Id, ? extends Person> untoggledPerson :
				untoggledPopulation.getPersons().entrySet()) {
			counter.incCounter();
			Person toggledPerson = toggledPersons.remove( untoggledPerson.getKey() );
			Person individualPerson = individualPersons.remove( untoggledPerson.getKey() );

			plans.put(
					untoggledPerson.getKey(),
					createComparativePlan(
						untoggledPerson.getValue(),
						toggledPerson,
						individualPerson) );

		}
		counter.printCounter();
	}

	private ComparativePlan createComparativePlan(
			final Person untoggledPerson,
			final Person toggledPerson,
			final Person individualPerson) {
		ComparativePlan plan = new ComparativePlan( untoggledPerson );

		for(List<Leg> trip : extractLegsWithJointTrips( untoggledPerson )) {
			plan.addUntoggledLeg( trip );
		}
		for(List<Leg> trip : extractLegsWithJointTrips( toggledPerson )) {
			plan.addToggledLeg( trip );
		}
		for(Leg leg : extractLegsWithoutJointTrips( individualPerson )) {
			plan.addIndividualLeg( leg );
		}

		plan.lock();
		return plan;
	}

	private List<Leg> extractLegsWithoutJointTrips(final Person person) {
		List<Leg> out = new ArrayList<Leg>();

		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Leg) {
				out.add((Leg) pe);
			}
		}

		return out;
	}

	private List<List<Leg>> extractLegsWithJointTrips(final Person person) {
		List<List<Leg>> out = new ArrayList<List<Leg>>();

		List< Leg > currentTrip = null;
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Activity) {
				// if we are on a non-pu/do activity, start a new trip
				String type = ((Activity) pe).getType();
				if (!type.equals( JointActingTypes.PICK_UP ) &&
						!type.equals( JointActingTypes.DROP_OFF )) {
					if (currentTrip != null) out.add( currentTrip );
					currentTrip = new ArrayList<Leg>();
				}
			}
			else if (pe instanceof Leg) {
				currentTrip.add((Leg) pe);
			}
			else {
				throw new RuntimeException( "unknown PlanElement type "+
						pe.getClass().getSimpleName()+": neither a Leg nor an Activity!" );
			}
		}

		return out ;
	}

	// /////////////////////////////////////////////////////////////////////////
	// analysis elements getters
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Creates a box and whisker chart representing the improvements
	 * in travel time permited by the joint trips, for the passengers.
	 * the improvement is defined as
	 * <tt>
	 * (tt_indiv - tt_joint) / tt_indiv
	 * </tt>
	 *
	 * @return a {@link ChartUtil} containing a BoxAndWhisker chart
	 */
	public ChartUtil getTravelTimeRelativeImprovementsChart() {
		DefaultBoxAndWhiskerCategoryDataset dataset =
			new DefaultBoxAndWhiskerCategoryDataset();

		List<Double> improvements = new ArrayList<Double>();
		for (ComparativePlan plan : plans.values()) {
			for (ComparativeLeg leg : plan) {
				if (leg.isToggledPassenger()) {
					double jointTravelTime = leg.getToggledTravelTime();
					double individualTravelTime = leg.getIndividualTravelTime();
					double improvement = 100d *
						(-jointTravelTime + individualTravelTime) / individualTravelTime;
					improvements.add( improvement );
				}
			}
		}

		dataset.add(improvements, "", "");
		JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
				"travel time improvements implied by joint trips",
				"",
				"relative improvement (%)",
				dataset,
				true);
		((NumberAxis) chart.getCategoryPlot().getRangeAxis()).setAutoRangeIncludesZero( true );
		// limit the width of the bar to 15% of the available space
		((BoxAndWhiskerRenderer) chart.getCategoryPlot().getRenderer()).setMaximumBarWidth( 0.15 );
		// due to the ChartUtil formating procedure, we have to put the
		// legend at creation, but here it is just empty
		((BoxAndWhiskerRenderer) chart.getCategoryPlot().getRenderer()).setBaseSeriesVisibleInLegend( false );
		return new WrapperChartUtil( chart );
	}
}

class ComparativePlan implements Iterable<ComparativeLeg> {
	private final Person person;
	private final List< List<Leg> > untoggledLegs =
		new ArrayList< List<Leg> >();
	private final List< List<Leg> > toggledLegs =
		new ArrayList< List<Leg> >();
	private final List< Leg > individualLegs =
		new ArrayList< Leg >();

	private boolean isLocked = false;

	// /////////////////////////////////////////////////////////////////////////
	// construction
	// /////////////////////////////////////////////////////////////////////////
	public ComparativePlan(
			final Person person) {
		this.person = person;
	}

	// /////////////////////////////////////////////////////////////////////////
	// add methods
	// /////////////////////////////////////////////////////////////////////////
	public void addUntoggledLeg(
			final List<Leg> leg) {
		if (isLocked) throw new IllegalStateException();
		untoggledLegs.add( leg );
	}

	public void addToggledLeg(
			final List<Leg> leg) {
		if (isLocked) throw new IllegalStateException();
		toggledLegs.add( leg );
	}

	public void addIndividualLeg(
			final Leg leg) {
		if (isLocked) throw new IllegalStateException();
		individualLegs.add( leg );
	}

	// /////////////////////////////////////////////////////////////////////////
	// processing methods
	// /////////////////////////////////////////////////////////////////////////
	public void lock() {
		if (!isLocked) {
			isLocked = true;

			int nUntoggledLegs = untoggledLegs.size();
			int nToggledLegs = toggledLegs.size();
			int nIndividualLegs = individualLegs.size();

			if (nUntoggledLegs != nToggledLegs || nToggledLegs != nIndividualLegs) {
				throw new IllegalStateException( "cannot lock a "+this.getClass().getSimpleName()
						+" with inconsistent number of legs:"
						+" untoggled="+nUntoggledLegs
						+" toggled="+nToggledLegs
						+" indiv="+nIndividualLegs
						+" for person "+person.getId());
			}
		}
	}

	public Iterator<ComparativeLeg> iterator() {
		lock();
		return new LegIterator();
	}

	public Person getPerson() {
		return person;
	}

	// /////////////////////////////////////////////////////////////////////////
	// classes
	// /////////////////////////////////////////////////////////////////////////
	private class LegIterator implements Iterator<ComparativeLeg> {
		private final Iterator<List<Leg>> untoggledIterator =
			untoggledLegs.iterator();
		private final Iterator<List<Leg>> toggledIterator =
			toggledLegs.iterator();
		private final Iterator<Leg> individualIterator =
			individualLegs.iterator();

		@Override
		public boolean hasNext() {
			return untoggledIterator.hasNext();
		}

		@Override
		public ComparativeLeg next() {
			return new ComparativeLeg(
					untoggledIterator.next(),
					toggledIterator.next(),
					individualIterator.next());
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}

class ComparativeLeg {
	private final List< Leg > untoggledLeg;
	private final List< Leg > toggledLeg;
	private final Leg individualLeg;

	// /////////////////////////////////////////////////////////////////////////
	// construction
	// /////////////////////////////////////////////////////////////////////////
	public ComparativeLeg(
			final List< Leg > untoggledLeg,
			final List< Leg > toggledLeg,
			final Leg individualLeg) {
		this.untoggledLeg = untoggledLeg;
		this.toggledLeg = toggledLeg;
		this.individualLeg = individualLeg;
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters
	// /////////////////////////////////////////////////////////////////////////
	public boolean isUntoggledJoint() {
		return untoggledLeg.size() > 1;
	}

	public boolean isToggledJoint() {
		return toggledLeg.size() > 1;
	}

	public boolean isUntoggledPassenger() {
		return isUntoggledJoint() &&
			untoggledLeg.get(1).getMode().equals( JointActingTypes.PASSENGER );
	}

	public boolean isToggledPassenger() {
		return isToggledJoint() &&
			toggledLeg.get(1).getMode().equals( JointActingTypes.PASSENGER );
	}

	public String getIndividualMode() {
		return individualLeg.getMode();
	}

	public double getUntoggledTravelTime() {
		double departure = untoggledLeg.get(0).getDepartureTime();
		Leg lastLeg = untoggledLeg.get( untoggledLeg.size() - 1 );
		double arrival = lastLeg.getDepartureTime() + lastLeg.getTravelTime();
		return arrival - departure;
	}

	public double getToggledTravelTime() {
		double departure = toggledLeg.get(0).getDepartureTime();
		Leg lastLeg = toggledLeg.get( toggledLeg.size() - 1 );
		double arrival = lastLeg.getDepartureTime() + lastLeg.getTravelTime();
		return arrival - departure;
	}

	public double getIndividualTravelTime() {
		return individualLeg.getTravelTime();
	}

}
