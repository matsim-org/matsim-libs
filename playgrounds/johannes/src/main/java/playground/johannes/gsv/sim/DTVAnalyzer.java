/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.sim;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;

/**
 * @author johannes
 *
 */
public class DTVAnalyzer implements IterationEndsListener, IterationStartsListener {

	private final Counts obsCounts;
	
	private VolumesAnalyzer simCounts;
	
	public DTVAnalyzer(Network network, Controler controler, EventsManager events, String countsfile) {
		obsCounts = new Counts();
		CountsReaderMatsimV1 reader = new CountsReaderMatsimV1(obsCounts);
		reader.parse(countsfile);
	}
	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.IterationStartsListener#notifyIterationStarts(org.matsim.core.controler.events.IterationStartsEvent)
	 */
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		simCounts = new VolumesAnalyzer(30*60*60, 30*60*60, event.getControler().getNetwork());
		event.getControler().getEvents().addHandler(simCounts);
	}
	/* (non-Javadoc)
	 * @see org.matsim.core.controler.listener.IterationEndsListener#notifyIterationEnds(org.matsim.core.controler.events.IterationEndsEvent)
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String outdir = event.getControler().getControlerIO().getIterationPath(event.getIteration());
//		simCounts = event.getControler().getVolumes();
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outdir + "/counts.txt"));

			writer.write("obs\tsim");
			writer.newLine();
			
			for (Count count : obsCounts.getCounts().values()) {
				Id linkId = count.getLocId();
				int[] simVols = simCounts.getVolumesForLink(linkId);
				int simVol = 0;
				if(simVols != null) {
					for(int i = 0; i < simVols.length; i++)
						simVol += simVols[i];
				}
				double obsVol = count.getVolume(1).getValue();

				writer.write(String.valueOf(obsVol));
				writer.write("\t");
				writer.write(String.valueOf(simVol));
				writer.newLine();
			}

			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
