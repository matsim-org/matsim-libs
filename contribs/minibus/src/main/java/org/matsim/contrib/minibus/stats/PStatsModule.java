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

import java.io.File;

import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.contrib.minibus.stats.abtractPAnalysisModules.PAnalysisManager;
import org.matsim.contrib.minibus.stats.operatorLogger.POperatorLogger;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

/**
 *
 * Registers all stats modules with MATSim
 *
 * @author aneumann
 *
 */
public final class PStatsModule extends AbstractModule {

	@Override
	public void install() {
		PConfigGroup pConfig = ConfigUtils.addOrGetModule(this.getConfig(), PConfigGroup.class ) ;

		this.addControllerListenerBinding().to(PStatsOverview.class);
		this.addControllerListenerBinding().toInstance(new POperatorLogger());
		this.addControllerListenerBinding().toInstance(new GexfPStat(false, pConfig));
		this.addControllerListenerBinding().to(GexfPStatLight.class);
		this.addControllerListenerBinding().to(Line2GexfPStat.class);

		if (pConfig.getWriteMetrics()) {
			this.addControllerListenerBinding().toInstance(new PAnalysisManager(pConfig));
		}

		this.addControllerListenerBinding().to(ActivityLocationsParatransitUser.class);

		this.addControllerListenerBinding().toInstance(new StartupListener() {
			@Override public void notifyStartup(StartupEvent event) {
				String outFilename = event.getServices().getControllerIO().getOutputPath() + PConstants.statsOutputFolder;
				new File(outFilename).mkdir();
			}
		});
	}

}
