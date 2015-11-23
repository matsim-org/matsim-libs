/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideTripsAnalizer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.parknride.analysis;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.pt.PtConstants;
import playground.ivt.utils.WrapperChartUtil;
import playground.thibautd.parknride.ParkAndRideConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides methods to create plots relative to various caracteristics of
 * park and ride trips.
 * @author thibautd
 */
public class ParkAndRideTripsAnalyzer {
	private final List< List<PlanElement> > trips;

	private static final int N_BINS = 10;

	public ParkAndRideTripsAnalyzer(final Population population) {
		trips = extractTrips( population );
	}

	private static List<List<PlanElement>> extractTrips(final Population population) {
		List<List<PlanElement>> trips = new ArrayList<List<PlanElement>>();

		for (Person person : population.getPersons().values()) {
			List<PlanElement> currentTrip = new ArrayList<PlanElement>();

			Plan plan = person.getSelectedPlan();

			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					currentTrip.add( pe );
				}
				else {
					if (((Activity) pe).getType().equals( PtConstants.TRANSIT_ACTIVITY_TYPE )) {
						currentTrip.add( pe );
					}
					else if (((Activity) pe).getType().equals( ParkAndRideConstants.PARKING_ACT )) {
						currentTrip.add( pe );
						trips.add( currentTrip );
					}
					else {
						currentTrip = new ArrayList<PlanElement>();
					}
				}
			}
		}

		return trips;
	}

	public ChartUtil getPtDistanceProportionHistogram() {
		throw new UnsupportedOperationException( "TODO" );
	}

	public ChartUtil getPtTimeProportionHistogram() {
		double[] proportions = new double[ trips.size() ];

		int i = 0;
		for (List<PlanElement> trip : trips) {
			double ptTime = 0;
			double totalTime = 0;

			for (PlanElement pe : trip) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					String mode = leg.getMode();
					double tt = leg.getTravelTime();

					if ( mode.equals( TransportMode.pt ) || mode.equals( TransportMode.transit_walk ) ) {
						ptTime += tt;
					}
					totalTime += tt;
				}
			}

			proportions[ i ] = ptTime / totalTime;
			i++;
		}

		return createProportionHistogram(
				"Part of the Transit travel time (including walk) in the total PNR travel time",
				"TimePt / TotalTripTime",
				"Number of Trips",
				proportions,
				N_BINS,
				0,
				1);
	}

	public ChartUtil getNumberOfPtLegsHistogram() {
		double[] numberOfPtLegs = new double[ trips.size() ];

		int i = 0;
		int maxCount = -1;
		for (List<PlanElement> trip : trips) {
			int count = 0;

			for (PlanElement pe : trip) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					String mode = leg.getMode();

					if ( mode.equals( TransportMode.pt ) ) {
						count++;
					}
				}
			}

			numberOfPtLegs[ i ] = count;
			maxCount = count > maxCount ? count : maxCount;
			i++;
		}

		return createProportionHistogram(
				"Number of transit legs (walk excluded) per PNR Trip",
				"Number of Pt Legs",
				"Number of Trips",
				numberOfPtLegs,
				// one bin per count, including 0
				maxCount + 1,
				-0.5,
				maxCount + 0.5);
	}

	private static ChartUtil createProportionHistogram(
			final String title,
			final String xName,
			final String yName,
			final double[] values,
			final int nBins,
			final double min,
			final double max) {
		HistogramDataset dataset = new HistogramDataset();
		dataset.addSeries( xName , values , nBins , min , max );

		JFreeChart chart =
			ChartFactory.createHistogram(
					title,
					xName,
					yName,
					dataset,
					PlotOrientation.VERTICAL,
					false,
					false,
					false);

		tuneHistogramAppearence( chart );

		return new WrapperChartUtil( chart );
	}

	private static void tuneHistogramAppearence(final JFreeChart chart) {
		XYBarRenderer renderer = (XYBarRenderer) chart.getXYPlot().getRenderer();

		renderer.setBarPainter( new StandardXYBarPainter() );
		renderer.setShadowVisible( false );
		renderer.setDrawBarOutline( true );
	}
}

