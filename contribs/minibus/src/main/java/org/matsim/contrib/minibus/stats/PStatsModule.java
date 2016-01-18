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

package org.matsim.contrib.minibus.stats;

import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.contrib.minibus.operator.Operators;
import org.matsim.contrib.minibus.stats.abtractPAnalysisModules.PAnalysisManager;
import org.matsim.contrib.minibus.stats.abtractPAnalysisModules.PtMode2LineSetter;
import org.matsim.contrib.minibus.stats.operatorLogger.POperatorLogger;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import java.io.File;

/**
 * 
 * Registers all stats modules with MATSim
 * 
 * @author aneumann
 *
 */
public final class PStatsModule {

    public static void configureControler(MatsimServices controler, PConfigGroup pConfig, Operators pBox, PtMode2LineSetter lineSetter) {
        controler.addControlerListener(new PStatsOverview(pBox, pConfig));
        controler.addControlerListener(new POperatorLogger(pBox, pConfig));
        controler.addControlerListener(new GexfPStat(pConfig, false));
        controler.addControlerListener(new GexfPStatLight(pConfig));
        controler.addControlerListener(new Line2GexfPStat(pConfig));
        
        if (pConfig.getWriteMetrics()) {controler.addControlerListener(new PAnalysisManager(pConfig, lineSetter));}
        
        controler.addControlerListener(new ActivityLocationsParatransitUser(pConfig));
        controler.addControlerListener(new StartupListener() {
            @Override
            public void notifyStartup(StartupEvent event) {
                String outFilename = event.getServices().getControlerIO().getOutputPath() + PConstants.statsOutputFolder;
                new File(outFilename).mkdir();
            }
        });
    }

}
