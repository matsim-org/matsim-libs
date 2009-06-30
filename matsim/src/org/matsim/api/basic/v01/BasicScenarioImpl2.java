/* *********************************************************************** *
 * project: org.matsim.*
 * BasicScenarioImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.api.basic.v01;

import org.matsim.core.api.Scenario;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.basic.v01.vehicles.BasicVehicles;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.households.Households;
import org.matsim.knowledges.Knowledges;
import org.matsim.lanes.basic.BasicLaneDefinitions;
import org.matsim.signalsystems.basic.BasicSignalSystems;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurations;

/** @deprecated this is a proposal; do not use */
public class BasicScenarioImpl2 implements Scenario {

	private final Config config;
	private final Network network;
	private final Population population;
	
	private BasicScenarioImpl2() {
		this(new Config());
		this.config.addCoreModules();
	}
	
	private BasicScenarioImpl2(Config config) {
		this.config = config;

		this.network = new NetworkLayer();  
		
		//never use the next line in new matsim code
		Gbl.getWorld().setNetworkLayer((NetworkLayer)this.network);

		this.population = new PopulationImpl( this );
	}

	public Network getNetwork() {
		return this.network;
	}

	public Population getPopulation() {
		return this.population;
	}

	public Config getConfig() {
		return this.config;
	}

	public Id createId(String string) {
		return new IdImpl(string);
	}

	public Coord createCoord(double x, double y) {
		return new CoordImpl(x, y);
	}

	public ActivityFacilities getActivityFacilities() {
		throw new UnsupportedOperationException("not on basic level") ;
	}

	public Households getHouseholds() {
		throw new UnsupportedOperationException("not on basic level") ;
	}

	public Knowledges getKnowledges() {
		throw new UnsupportedOperationException("not on basic level") ;
	}

	public BasicLaneDefinitions getLaneDefinitions() {
		throw new UnsupportedOperationException("not on basic level") ;
	}

	public BasicSignalSystemConfigurations getSignalSystemConfigurations() {
		throw new UnsupportedOperationException("not on basic level") ;
	}

	public BasicSignalSystems getSignalSystems() {
		throw new UnsupportedOperationException("not on basic level") ;
	}

	public BasicVehicles getVehicles() {
		throw new UnsupportedOperationException("not on basic level") ;
	}

}
