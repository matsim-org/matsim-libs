/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ${file_name}
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) ${year} by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 *
 * ${filecomment}
 * ${package_declaration}
 *
 * ${typecomment}
 * ${type_declaration}
 */

package playground.mzilske.city2000w;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.mobsim.CarrierAgentTracker;
import org.matsim.contrib.freight.mobsim.FreightAgentSource;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;

/**
 * Created by IntelliJ IDEA.
 * User: zilske
 * Date: 10/31/11
 * Time: 4:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class City2000WQSimFactory implements MobsimFactory {

	private CarrierAgentTracker carrierAgentTracker;

	public City2000WQSimFactory(CarrierAgentTracker carrierAgentTracker) {
		this.carrierAgentTracker = carrierAgentTracker;
	}

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		final QSim sim = (QSim) new QSimFactory().createMobsim(sc, eventsManager);
		Collection<Plan> plans = carrierAgentTracker.createPlans();
		sim.addAgentSource(new FreightAgentSource(plans, new DefaultAgentFactory(sim), sim));
		return sim;
	}

}
