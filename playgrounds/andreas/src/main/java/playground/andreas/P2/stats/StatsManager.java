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

package playground.andreas.P2.stats;

import org.matsim.core.controler.Controler;

import playground.andreas.P2.ana.ActivityLocationsParatransitUser;
import playground.andreas.P2.ana.PAnalysisManager;
import playground.andreas.P2.ana.helper.PtMode2LineSetter;
import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PConstants;
import playground.andreas.P2.pbox.PBox;
import playground.andreas.P2.stats.gexfPStats.GexfPStat;
import playground.andreas.P2.stats.gexfPStats.GexfPStatLight;
import playground.andreas.P2.stats.gexfPStats.Line2GexfPStat;
import playground.andreas.P2.stats.overview.PStatsOverview;

/**
 * 
 * Registers all stats modules with MATSim
 * 
 * @author aneumann
 *
 */
public class StatsManager {
	
	public StatsManager(Controler controler, PConfigGroup pConfig, PBox pBox, PtMode2LineSetter lineSetter){
		controler.addControlerListener(new PStatsOverview(pBox, pConfig));
		controler.addControlerListener(new PCoopLogger(pBox, pConfig));
		controler.addControlerListener(new GexfPStat(pConfig, false));
//		controler.addControlerListener(new GexfPStat(pConfig, true));
		controler.addControlerListener(new GexfPStatLight(pConfig));
		controler.addControlerListener(new Line2GexfPStat(pConfig));
		
		controler.addControlerListener(new PAnalysisManager(pConfig, PConstants.ptDriverPrefix, lineSetter));
		
		controler.addControlerListener(new ActivityLocationsParatransitUser(pConfig, 100.0));
	}
}
