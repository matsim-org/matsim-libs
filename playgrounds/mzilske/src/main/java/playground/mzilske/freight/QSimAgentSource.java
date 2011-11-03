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

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.ptproject.qsim.agents.AgentFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: zilske
 * Date: 10/31/11
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class QSimAgentSource implements AgentSource {

    private Collection<Plan> plans;

    private AgentFactory agentFactory;

    public QSimAgentSource(Collection<Plan> plans, AgentFactory agentFactory) {
        this.plans = plans;
        this.agentFactory = agentFactory;
    }

    @Override
    public List<MobsimAgent> insertAgentsIntoMobsim() {
        List<MobsimAgent> agents = new ArrayList<MobsimAgent>();
        for (Plan plan : plans) {
            MobsimAgent agent = this.agentFactory.createMobsimAgentFromPersonAndInsert(plan.getPerson());
            agents.add(agent);
        }
        return agents;
    }

}
