/* *********************************************************************** *
 * project: org.matsim.*
 * LaneLayoutTestShowNetwork
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.lanes.laneLayoutTest;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;


/**
 * @author dgrether
 *
 */
public class LaneLayoutTestShowNetwork {

	public static void main(String[] args) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().network().setInputFile(LaneLayoutTestFileNames.NETWORK);
		sc.getConfig().plans().setInputFile(LaneLayoutTestFileNames.POPULATION);
		ConfigUtils.addOrGetModule(sc.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setDrawLinkIds(true);
		ConfigUtils.addOrGetModule(sc.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setDrawNonMovingItems(true);
		sc.getConfig().qsim().setSnapshotStyle( SnapshotStyle.queue ) ;;
		
		ScenarioUtils.loadScenario(sc);
		EventsManager events = EventsUtils.createEventsManager();

		QSim otfVisQSim = QSimUtils.createDefaultQSim(sc, events);
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, events, otfVisQSim);
		OTFClientLive.run(sc.getConfig(), server);
		otfVisQSim.run();
	}

}
