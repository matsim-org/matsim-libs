/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.commercialTrafficApplications.jointDemand;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.freight.carriers.Carriers;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;

public interface CommercialJobGenerator extends BeforeMobsimListener, AfterMobsimListener {

	String COMMERCIALJOB_ACTIVITYTYPE_PREFIX = "commercialJob";
	String CUSTOMER_ATTRIBUTE_NAME = "customer";
	String SERVICEID_ATTRIBUTE_NAME = "serviceId";
	String EXPECTED_ARRIVALTIME_NAME = "eta";
	String SERVICE_DURATION_NAME = "duration";

	/**
	 * Converts Jsprit tours to MATSim freight agents and inserts them into the population
	 */
	default void createAndAddFreightAgents(Carriers carriers, Population population) {}

	/**
	 * generates the services (out of the person population) and assigns them to the carriers
	 */
	default void generateIterationServices(Carriers carriers, Population population) {}

	/**
	 * removes freight agents and their vehicles from the scenario
	 * @param scenario
	 */
	default void removeFreightAgents(Scenario scenario) {}

	}
