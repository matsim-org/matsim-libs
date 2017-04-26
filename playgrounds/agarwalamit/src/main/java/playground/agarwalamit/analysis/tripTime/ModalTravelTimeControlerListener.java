/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis.tripTime;

import java.util.*;
import javax.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;
import playground.agarwalamit.utils.ListUtils;

/**
 * @author amit
 */

public class ModalTravelTimeControlerListener implements StartupListener, IterationEndsListener{

	private int firstIteration = 0;
	private final SortedMap<String, double []> mode2AvgTripTimes = new TreeMap<>();

	// following is required to fix if at some intermediate iteration, one of the mode type vanishes, the arrayCopy wont work
	private final Set<String> modeHistory = new HashSet<>();

	@Inject
	private ModalTripTravelTimeHandler travelTimeHandler;

	@Inject
	private EventsManager events;

	@Override
	public void notifyStartup(StartupEvent event) {
		this.firstIteration = event.getServices().getConfig().controler().getFirstIteration();
		this.events.addHandler(this.travelTimeHandler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		String outputDir = event.getServices().getConfig().controler().getOutputDirectory();

		SortedMap<String, Double > mode2AvgTripTime = modalAvgTime();
		modeHistory.addAll(mode2AvgTripTime.keySet());
		modeHistory.stream().filter(e -> ! mode2AvgTripTime.containsKey(e)).forEach(e -> mode2AvgTripTime.put(e, 0.));


		int itNrIndex = event.getIteration() - this.firstIteration;

		for(String mode : mode2AvgTripTime.keySet()) {
			if ( ! mode2AvgTripTimes.containsKey(mode)){
				double [] avgTimes = new double [itNrIndex+1];
				avgTimes[itNrIndex] = mode2AvgTripTime.get(mode);
				mode2AvgTripTimes.put(mode, avgTimes);
			} else {
				double [] avgTimesSoFar = mode2AvgTripTimes.get(mode);
				double [] avgTimesNew = new double [avgTimesSoFar.length+1];
				System.arraycopy(avgTimesSoFar, 0, avgTimesNew, 0, avgTimesSoFar.length);
				avgTimesNew[itNrIndex] = mode2AvgTripTime.get(mode);
				mode2AvgTripTimes.put(mode, avgTimesNew);
			}
		}

		if(itNrIndex == 0) return;

		//plot data here...
		XYLineChart chart = new XYLineChart("Modal Travel Time", "iteration", "travel time [sec]");

		// x-series
		double[] iterations = new double[itNrIndex + 1];
		for (int i = 0; i <= itNrIndex; i++) {
			iterations[i] = i + this.firstIteration;
		}

		//y series
		for(String mode : this.mode2AvgTripTimes.keySet()){
			double [] values = new double [itNrIndex+1]; // array of only available data
			System.arraycopy(this.mode2AvgTripTimes.get(mode), 0, values, 0, itNrIndex + 1);
			chart.addSeries(mode, iterations, values);
		}

		// others--
		chart.addMatsimLogo();
		chart.saveAsPng(outputDir+"/modalTravelTime.png", 800, 600);
	}

	private SortedMap<String, Double > modalAvgTime() {
		SortedMap<String, Double > mode2AvgTripTime = new TreeMap<>();
		SortedMap<String, Map<Id<Person>, List<Double>>> times = travelTimeHandler.getLegMode2PesonId2TripTimes();
		for(String mode :times.keySet()){
			double tripTimes =0;
				int count = 0;
				for(Id<Person> id : times.get(mode).keySet()){
					tripTimes += ListUtils.doubleSum(times.get(mode).get(id));
					count += times.get(mode).get(id).size();
				}
			mode2AvgTripTime.put(mode, tripTimes/count);
		}
		return mode2AvgTripTime;
	}
}