/* *********************************************************************** *
 * project: org.matsim.*
 * TestTestMain
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.lanes.MixedLaneTestFixture;
import org.matsim.vis.otfvis.OTFVisConfigGroup;


/**
 * @author dgrether
 *
 */
public class TestTestMain {

	public static void main(String[] args) {
		MixedLaneTestFixture fixture = new MixedLaneTestFixture();
		fixture.create2PersonPopulation();
		Scenario scenario = fixture.sc;
		ConfigUtils.addOrGetModule(scenario.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setScaleQuadTreeRect(true);
		scenario.getConfig().qsim().setNodeOffset(10.0);
		scenario.getConfig().qsim().setSnapshotStyle( SnapshotStyle.queue ) ;;
		OTFVis.playScenario(scenario);
	}

}
