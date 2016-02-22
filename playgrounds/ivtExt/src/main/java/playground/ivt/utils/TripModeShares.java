/* *********************************************************************** *
 * project: org.matsim.*
 * TripModeShares.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ivt.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author thibautd
 */
@Singleton
public class TripModeShares implements IterationEndsListener, ShutdownListener {
	private final int graphWriteInterval;
	private final String pngFileName;
	private final Scenario scenario;
	private final MainModeIdentifier mainModeIdentifier;
	private final StageActivityTypes stageActivityTypes;

	private final BufferedWriter writer;
	private final History history = new History();

	@Inject
	public TripModeShares(
			final OutputDirectoryHierarchy outputDirectoryHierarchy,
			final Scenario scenario,
			final TripRouter tripRouter) {
		this( 10 , outputDirectoryHierarchy , scenario , tripRouter.getMainModeIdentifier() , tripRouter.getStageActivityTypes() );
	}

	public TripModeShares(
			final int graphWriteInterval,
			final OutputDirectoryHierarchy outputDirectoryHierarchy,
			final Scenario scenario,
			final MainModeIdentifier mainModeIdentifier,
			final StageActivityTypes stageActivityTypes) {
		this.graphWriteInterval = graphWriteInterval;
		this.scenario = scenario;
		this.mainModeIdentifier = mainModeIdentifier;
		this.stageActivityTypes = stageActivityTypes;

		this.pngFileName = outputDirectoryHierarchy.getOutputFilename(
					"tripModeShares.png" );
		this.writer = IOUtils.getBufferedWriter(
				outputDirectoryHierarchy.getOutputFilename(
					"tripModeShares.dat" ) );
		try {
			this.writer.write( "iter\tmode\tnTrips" );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		recordHistory( event.getIteration() );
		writeDataFile( event.getIteration() );
		if ( graphWriteInterval < 1 || event.getIteration() % graphWriteInterval == 0 ) {
			writePng( );
		}
	}

	private void writePng() {
		final DefaultTableXYDataset dataset = new DefaultTableXYDataset();

		for (ModeHistory h : history.getHistories()) {
			final XYSeries series = new XYSeries( h.mode , false , false );

			for (int i : history.getIterations()) {
				series.add(i, h.getCount( i ));
			}

			dataset.addSeries(series);
		}

		final ChartUtil chart =
			new WrapperChartUtil(
					ChartFactory.createStackedXYAreaChart(
						"number of trips per mode",
						"iteration",
						"n trips",
						dataset,
						PlotOrientation.VERTICAL,
						true,
						false,
						false));
		chart.addMatsimLogo();
		chart.saveAsPng(
				this.pngFileName,
				800,
				600);

	}

	private void writeDataFile(final int iteration) {
		for (ModeHistory h : history.getHistories()) {
			if ( h.getLastIteration() != iteration ) continue;

			try {
				writer.newLine();
				writer.write(
						iteration+"\t"+
						h.mode+"\t"+
						h.getLastCount() );
			}
			catch (IOException e) {
				throw new UncheckedIOException( e );
			}
		}
		try {
			writer.flush();
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	@Override
	public void notifyShutdown(final ShutdownEvent event) {
		writePng();
		try {
			writer.close();
		} catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	private void recordHistory(final int iteration) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			final Plan plan = person.getSelectedPlan();
			final List<Trip> trips = TripStructureUtils.getTrips( plan , stageActivityTypes );

			for (Trip t : trips) {
				history.increment(
						iteration,
						mainModeIdentifier.identifyMainMode(
							t.getTripElements() ));
			}
		}
	}

	private static class History {
		private final Map<String, ModeHistory> histories = new TreeMap<String, ModeHistory>();
		private final Set<Integer> iterations = new TreeSet<Integer>();

		public Iterable<ModeHistory> getHistories() {
			return histories.values();
		}

		public void increment(final int iter, final String mode) {
			iterations.add( iter );
			get( mode ).increment( iter );
		}

		public Iterable<Integer> getIterations() {
			return iterations;
		}

		private ModeHistory get(final String mode) {
			ModeHistory h = histories.get( mode );

			if ( h == null ) {
				h = new ModeHistory( mode );
				histories.put( mode , h );
			}

			return h;
		}
	}

	private static class ModeHistory {
		public final String mode;
		private final List<Integer> iterations = new ArrayList<Integer>();
		private int[] countPerIteration = new int[0];

		public ModeHistory(final String mode) {
			this.mode = mode;
		}

		private void increment(final int iteration) {
			notifyIteration( iteration );
			countPerIteration[ countPerIteration.length - 1 ]++;
		}

		private void notifyIteration(final int iteration) {
			if (iterations.contains( iteration )) {
				if ( iterations.indexOf( iteration ) != iterations.size() - 1 ) {
					throw new IllegalArgumentException(
							iteration+" is not the last element of "+iterations );
				}
				return;
			}

			iterations.add( iteration );
			countPerIteration = Arrays.copyOf( countPerIteration , countPerIteration.length + 1 );
		}

		public int getLastIteration() {
			return iterations.get( iterations.size() - 1 );
		}

		public int getLastCount() {
			return countPerIteration[ countPerIteration.length - 1 ];
		}

		//public double[] getIterations() {
		//	final double[] array = new double[ iterations.size() ];

		//	int i = 0;
		//	for (int iter : iterations) {
		//		array[ i ] = iter;
		//		i++;
		//	}

		//	return array;
		//}

		//public double[] getCounts() {
		//	final double[] array = new double[ countPerIteration.length ];

		//	for (int i = 0; i < countPerIteration.length; i++) {
		//		array[ i ] = countPerIteration[ i ];
		//	}

		//	return array;
		//}

		public int getCount(final int iteration) {
			int c = 0;
			for (int iter : iterations) {
				if (iter == iteration) return countPerIteration[ c ];
				c++;
			}
			return 0;
		}
	}
}

