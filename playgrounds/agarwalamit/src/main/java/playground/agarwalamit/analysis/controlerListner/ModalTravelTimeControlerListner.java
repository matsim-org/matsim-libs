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

package playground.agarwalamit.analysis.controlerListner;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;

import playground.agarwalamit.analysis.travelTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.utils.ListUtils;

/**
 * @author amit
 */

public class ModalTravelTimeControlerListner implements StartupListener, IterationEndsListener{

	private int firstIteration = 0;
	private int numberOfIterations = 0;
	private SortedMap<String, double []> mode2AvgTripTimes = new TreeMap<>();

	@Inject
	private ModalTripTravelTimeHandler travelTimeHandler;

	@Inject
	private EventsManager events;

	@Override
	public void notifyStartup(StartupEvent event) {
		this.firstIteration = event.getServices().getConfig().controler().getFirstIteration();
		this.numberOfIterations = event.getServices().getConfig().controler().getLastIteration() - this.firstIteration + 1;

		this.events.addHandler(this.travelTimeHandler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		String outputDir = event.getServices().getConfig().controler().getOutputDirectory();

		SortedMap<String, Double > mode2AvgTripTime = modalAvgTime();
		int itNrIndex = event.getIteration() - this.firstIteration;

		if(itNrIndex == 0) {
			for(String mode : mode2AvgTripTime.keySet()) {
				if ( ! mode2AvgTripTimes.containsKey(mode)){ // initialize
					double [] avgTimes = new double [this.numberOfIterations];
					avgTimes[0] = mode2AvgTripTime.get(mode);
					mode2AvgTripTimes.put(mode, avgTimes);
				}
			}
			return; // only one data points... so no plotting
		}

		//storeData 
		for(String mode : mode2AvgTripTime.keySet()) {
			double [] avgTimes = this.mode2AvgTripTimes.get(mode);
			avgTimes[itNrIndex] = mode2AvgTripTime.get(mode);
		}

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