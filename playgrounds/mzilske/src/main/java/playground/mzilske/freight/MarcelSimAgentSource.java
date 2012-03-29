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

package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.mobsim.CarrierAgentTracker;

import playground.mrieser.core.mobsim.api.AgentSource;
import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.impl.DefaultPlanAgent;

public class MarcelSimAgentSource implements AgentSource {

	private List<PlanAgent> agents = new ArrayList<PlanAgent>();

	private final double weight = 1;

    public MarcelSimAgentSource(CarrierAgentTracker carrierAgentTracker) {
        Collection<Plan> plans = carrierAgentTracker.createPlans();
        for (Plan plan : plans) {
            PlanAgent planAgent = new DefaultPlanAgent(plan, weight);
            agents.add(planAgent);
        }
    }

    @Override
    public List<PlanAgent> getAgents() {
        return agents;
    }

}
