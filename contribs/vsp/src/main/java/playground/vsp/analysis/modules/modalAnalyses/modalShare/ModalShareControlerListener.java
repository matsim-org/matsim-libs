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

package playground.vsp.analysis.modules.modalAnalyses.modalShare;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.inject.Inject;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.charts.XYLineChart;

/**
 * @author amit
 */

public class ModalShareControlerListener implements StartupListener, IterationEndsListener{

	private int firstIteration = 0;
	private final SortedMap<String, double []> mode2numberofLegs = new TreeMap<>();

	// following is required to fix if at some intermediate iteration, one of the mode type vanishes, the arrayCopy wont work
	private final Set<String> modeHistory = new HashSet<>();

	@Inject
	private ModalShareEventHandler modalShareHandler;

	@Inject
	private EventsManager events;

	@Override
	public void notifyStartup(StartupEvent event) {
		this.firstIteration = event.getServices().getConfig().controler().getFirstIteration();
		this.events.addHandler(this.modalShareHandler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String outputDir = event.getServices().getConfig().controler().getOutputDirectory();
		
		SortedMap<String, Integer > mode2legs = this.modalShareHandler.getMode2numberOflegs();
		modeHistory.addAll(mode2legs.keySet());
		modeHistory.stream().filter(e -> ! mode2legs.containsKey(e)).forEach(e -> mode2legs.put(e, 0));

		int itNrIndex = event.getIteration() - this.firstIteration;

		for(String mode : mode2legs.keySet()) {
			if ( ! mode2numberofLegs.containsKey(mode)){ // initialize
				double [] legs = new double [itNrIndex+1];
				legs[itNrIndex] = mode2legs.get(mode);
				mode2numberofLegs.put(mode, legs);
			} else {
				double [] legsSoFar = mode2numberofLegs.get(mode);
				double [] legsNew = new double[legsSoFar.length+1];
				System.arraycopy(legsSoFar,0,legsNew,0,legsSoFar.length);
				legsNew[itNrIndex] = mode2legs.get(mode);
				mode2numberofLegs.put(mode,legsNew);
			}
		}

		if(itNrIndex == 0) return;

		//plot data here...
		XYLineChart chart = new XYLineChart("Modal Share", "iteration", "Number of legs");

		// x-series
		double[] iterations = new double[itNrIndex + 1];
		for (int i = 0; i <= itNrIndex; i++) {
			iterations[i] = i + this.firstIteration;
		}
		
		//y series
		for(String mode : this.mode2numberofLegs.keySet()){
			double [] values = new double [itNrIndex+1]; // array of only available data
			System.arraycopy(this.mode2numberofLegs.get(mode), 0, values, 0, itNrIndex + 1);
			chart.addSeries(mode, iterations, values);
		}
		
		// others--
		chart.addMatsimLogo();
        chart.saveAsPng(outputDir+"/modalShare.png", 800, 600);
	}
}


