/* *********************************************************************** *
 * project: org.matsim.*
 * CompareSharedRidePtTravelTimes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.analysis.aposteriorianalysis;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Simple executable class which parses a population and comparesthe (expected) shared ride
 * travel times with the estimated teleportation pt travel times.
 *
 * @author thibautd
 */
public class CompareSharedRidePtTravelTimes {
	public void main(final String[] args) {
		Config config = ConfigUtils.loadConfig( args[ 0 ] );
		Scenario scen = ScenarioUtils.loadScenario( config );

		Data data = new Data();

		// TODO parse population and output (form to define)
	}
}

class Data {
	private final List< Item > items = new ArrayList<Item>();

	public void addData(
			final double carPoolingTt,
			final double ptTt) {
		items.add( new Item( carPoolingTt , ptTt ) );
	}

	public static class Item {
		private final double cpTt;
		private final double ptTt;

		private Item(
				final double cpTt,
				final double ptTt) {
			this.cpTt = cpTt;
			this.ptTt = ptTt;
		}
	}
}
