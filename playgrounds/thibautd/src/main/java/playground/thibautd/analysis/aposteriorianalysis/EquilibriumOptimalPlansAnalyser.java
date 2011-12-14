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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;

import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
import playground.thibautd.jointtripsoptimizer.population.PopulationWithCliques;
import playground.thibautd.utils.charts.WrapperChartUtil;

/**
 * Generates aggregated resutls from the plans files exported by
 * {@link GenerateEquilibriumOptimalPlans}
 * @author thibautd
 */
public class EquilibriumOptimalPlansAnalyser {
	private final Map<Id, ComparativePlan> plans = new HashMap<Id, ComparativePlan>();
	private final ScoringFunctionFactory scoringFunctionFactory;

	// /////////////////////////////////////////////////////////////////////////
	// construction
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * @param untoggledPopulation
	 * @param toggledPopulation
	 * @param individualPopulation
	 * @param scoringFunctionFactory 
	 */
	public EquilibriumOptimalPlansAnalyser(
			final PopulationWithCliques untoggledPopulation,
			final PopulationWithCliques toggledPopulation,
			final PopulationWithCliques individualPopulation,
			final ScoringFunctionFactory scoringFunctionFactory) {
		this.scoringFunctionFactory = scoringFunctionFactory;
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
			plan.setUntoggledScore( getScore( untoggledPerson ) );
		}
		for(List<Leg> trip : extractLegsWithJointTrips( toggledPerson )) {
			plan.addToggledLeg( trip );
			plan.setToggledScore( getScore( toggledPerson ) );
		}
		for(Leg leg : extractLegsWithoutJointTrips( individualPerson )) {
			plan.addIndividualLeg( leg );
			plan.setIndividualScore( getScore( individualPerson ) );
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

	private double getScore(final Person person) {
		Plan plan = person.getSelectedPlan();
		ScoringFunction fitnessFunction;
		Activity currentActivity;
		Leg currentLeg;
		//double now;

		fitnessFunction =
			this.scoringFunctionFactory.createNewScoringFunction(plan);
		//now = 0d;

		// step through plan and score it
		List<PlanElement> elements = plan.getPlanElements();
		Activity lastActivity = (Activity) elements.get(elements.size() - 1);
		for (PlanElement pe : elements) {
			if (pe instanceof Activity) {
				currentActivity = (Activity) pe;

				// Quick and dirty fix to have everithing working with the
				// changed ScoringFunction interface: if last activity has an
				// end time defined, the last activity is counted twice
				// ---------------------------------------------------------
				if ( currentActivity == lastActivity ) {
					currentActivity.setEndTime( Time.UNDEFINED_TIME );
				}
				fitnessFunction.handleActivity( currentActivity );
			}
			else if (pe instanceof Leg) {
				currentLeg = (Leg) pe;
				fitnessFunction.handleLeg( currentLeg );
			}
			else {
				throw new IllegalArgumentException("unrecognized plan element type");
			}
		}

		fitnessFunction.finish();
		plan.setScore(fitnessFunction.getScore());
		return plan.getScore();
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
				"travel time improvements implied by passenger trips",
				"",
				"relative improvement (%)",
				dataset,
				true);
		formatCategoryChart( chart );
		return new WrapperChartUtil( chart );
	}

	/**
	 * Creates a box and whisker chart of score improvements.
	 * The score improvements are computed between the toggled and the
	 * individual plans, when the toggled plan has at least one passenger trip
	 * and no driver trip.
	 *
	 * @return the chart
	 */
	public ChartUtil getScoreAbsoluteImprovementsChart() {
		DefaultBoxAndWhiskerCategoryDataset dataset =
			new DefaultBoxAndWhiskerCategoryDataset();

		List<Double> improvements = new ArrayList<Double>();
		planLoop:
		for (ComparativePlan plan : plans.values()) {
			double improvement = Double.NaN;
			boolean isPassenger = false;
			for (ComparativeLeg leg : plan) {
				if (leg.isToggledJoint()) {
					if (leg.isToggledPassenger()) {
						double jointScore = plan.getToggledScore();
						double individualScore = plan.getIndividualScore();
						improvement = jointScore - individualScore;
						isPassenger = true;
					}
					else {
						// there is a driver leg: score improvements cannot
						// be interpreted easily anymore
						continue planLoop;
					}
				}
			}
			if (isPassenger) improvements.add( improvement );
		}

		dataset.add(improvements, "", "");
		JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
				"score improvements implied by passenger trips",
				"",
				"improvement",
				dataset,
				true);
		formatCategoryChart( chart );
		return new WrapperChartUtil( chart );
	}

	private static void formatCategoryChart(final JFreeChart chart) {
		((NumberAxis) chart.getCategoryPlot().getRangeAxis()).setAutoRangeIncludesZero( true );
		// limit the width of the bar to 15% of the available space
		((BoxAndWhiskerRenderer) chart.getCategoryPlot().getRenderer()).setMaximumBarWidth( 0.15 );
		// due to the ChartUtil formating procedure, we have to put the
		// legend at creation, but here it is just empty
		((BoxAndWhiskerRenderer) chart.getCategoryPlot().getRenderer()).setBaseSeriesVisibleInLegend( false );
		// draw a line at the 0.
		chart.getCategoryPlot().setRangeZeroBaselineVisible( true );
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

	private double untoggledScore = Double.NaN;
	private double toggledScore = Double.NaN;
	private double individualScore = Double.NaN;

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

	public void setUntoggledScore( final double score ) {
		untoggledScore = score;
	}

	public void setToggledScore( final double score ) {
		toggledScore = score;
	}

	public void setIndividualScore( final double score ) {
		individualScore = score;
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

	@Override
	public Iterator<ComparativeLeg> iterator() {
		lock();
		return new LegIterator();
	}

	public Person getPerson() {
		return person;
	}

	public double getUntoggledScore() {
		return untoggledScore;
	}

	public double getToggledScore() {
		return toggledScore;
	}

	public double getIndividualScore() {
		return individualScore;
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
