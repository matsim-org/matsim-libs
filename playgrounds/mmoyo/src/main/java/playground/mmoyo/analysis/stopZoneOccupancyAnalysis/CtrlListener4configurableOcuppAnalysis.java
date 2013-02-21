/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mmoyo.analysis.stopZoneOccupancyAnalysis;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.cadyts.pt.CadytsPtConfigGroup;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.collections.CollectionUtils;

/**
 * Controler listener for stop zone analysis
 */
public class CtrlListener4configurableOcuppAnalysis implements IterationEndsListener, BeforeMobsimListener {
	ConfigurableOccupancyAnalyzer configurableOccupAnalyzer;
	KMZPtCountSimComparisonWriter kmzPtCountSimComparisonWritter;
	private final Set<Id> calibratedLines = new HashSet<Id>();
	boolean stopZoneConversion;
	
	public CtrlListener4configurableOcuppAnalysis(final Controler controler){
		
		//create occupancy analyzer based on CadytsPtConfigGroup();
		String strCalLibes = controler.getConfig().getParam(CadytsPtConfigGroup.GROUP_NAME, "calibratedLines");
		String strTimeBinSize = controler.getConfig().getParam(CadytsPtConfigGroup.GROUP_NAME, "timeBinSize");
		
		for (String lineId : CollectionUtils.stringToArray(strCalLibes)) {
			this.calibratedLines.add(new IdImpl(lineId));
		}
		int timeBinSize = Integer.parseInt(strTimeBinSize);
		configurableOccupAnalyzer = new ConfigurableOccupancyAnalyzer(this.calibratedLines, timeBinSize);
		controler.getEvents().addHandler(configurableOccupAnalyzer);
			
		kmzPtCountSimComparisonWritter = new KMZPtCountSimComparisonWriter(controler);
	}
	
	@Override
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
		configurableOccupAnalyzer.reset(event.getIteration());
		configurableOccupAnalyzer.setStopZoneConversion(stopZoneConversion);
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		int it = event.getIteration();
		Controler controler = event.getControler();
		if (isActiveInThisIteration(it,  controler)) {
			kmzPtCountSimComparisonWritter.write( configurableOccupAnalyzer.getOccuAnalyzer(), it);
		}
	}
	
	private boolean isActiveInThisIteration(final int iter, final Controler controler) {
		return (iter % controler.getConfig().ptCounts().getPtCountsInterval() == 0);
	}
	
	public void setStopZoneConversion(boolean stopZoneConversion){
		this.stopZoneConversion = stopZoneConversion;
	}
}
