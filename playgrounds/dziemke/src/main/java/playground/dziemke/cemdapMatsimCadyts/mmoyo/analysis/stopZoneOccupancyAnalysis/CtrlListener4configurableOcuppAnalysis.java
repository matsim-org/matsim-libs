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

package playground.dziemke.cemdapMatsimCadyts.mmoyo.analysis.stopZoneOccupancyAnalysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.pt.CadytsPtOccupancyAnalyzerI;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.pt.transitSchedule.api.TransitLine;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Controler listener for stop zone analysis
 */
public class CtrlListener4configurableOcuppAnalysis implements IterationEndsListener, BeforeMobsimListener {
	private Controler controler;//gt

	ConfigurableOccupancyAnalyzer configurableOccupAnalyzer;
	KMZPtCountSimComparisonWriter kmzPtCountSimComparisonWritter;
	boolean stopZoneConversion;

	public CtrlListener4configurableOcuppAnalysis(final Controler controler){
		this.controler = controler;//gt

		//create occupancy analyzer based on CadytsPtConfigGroup();
		if (!(controler.getConfig().getModule(CadytsConfigGroup.GROUP_NAME) instanceof CadytsConfigGroup)){
			CadytsConfigGroup ccc = new CadytsConfigGroup() ;
			controler.getConfig().addModule(ccc) ;
		}
		CadytsConfigGroup cptcg = (CadytsConfigGroup) controler.getConfig().getModule(CadytsConfigGroup.GROUP_NAME);
		configurableOccupAnalyzer = new ConfigurableOccupancyAnalyzer( toTransitLineIdSet(cptcg.getCalibratedItems()) ,  cptcg.getTimeBinSize());
		controler.getEvents().addHandler(configurableOccupAnalyzer);

		kmzPtCountSimComparisonWritter = new KMZPtCountSimComparisonWriter(controler);
	}
	
	public CadytsPtOccupancyAnalyzerI getAnalyzer() {
		return this.configurableOccupAnalyzer ;
	}

	@Override
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
		configurableOccupAnalyzer.reset(event.getIteration());
		configurableOccupAnalyzer.setStopZoneConversion(stopZoneConversion);
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		int it = event.getIteration();
//		Controler controler = event.getControler();
		if (isActiveInThisIteration(it,  controler)) {
			kmzPtCountSimComparisonWritter.write( configurableOccupAnalyzer.getOccuAnalyzer(), it, stopZoneConversion);
		}
	}

	private static boolean isActiveInThisIteration(final int iter, final Controler controler) {
		final int ptCountsInterval = controler.getConfig().ptCounts().getPtCountsInterval();
		if ( ptCountsInterval==0 ) {
			return false ;
		}
		return (iter % ptCountsInterval == 0);
	}

	public void setStopZoneConversion(boolean stopZoneConversion){
		this.stopZoneConversion = stopZoneConversion;
	}

	private static Set<Id<TransitLine>> toTransitLineIdSet(Set<String> list) {
		Set<Id<TransitLine>> converted = new LinkedHashSet<>();

		for (String linkId : list) {
			Id<Link> id = Id.create(linkId, Link.class);
			converted.add(Id.create(id, TransitLine.class));
		}

		return converted;
	}
}
