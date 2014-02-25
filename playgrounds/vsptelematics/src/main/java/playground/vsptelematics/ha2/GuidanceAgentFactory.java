/* *********************************************************************** *
 * project: org.matsim.*
 * ReactiveGuidanceFactory
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
package playground.vsptelematics.ha2;

import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.PopulationUtils;


/**
 * @author dgrether
 *
 */
public class GuidanceAgentFactory implements AgentFactory {

	private final Netsim simulation;
	private double equipmentFraction;
	private Random random;
	private Guidance guidance;
	private GuidanceRouteTTObserver ttObserver;

	public GuidanceAgentFactory(final Netsim simulation, double equipmentFraction, Guidance guidance, GuidanceRouteTTObserver ttObserver) {
		this.simulation = simulation;
		this.equipmentFraction = equipmentFraction;
		this.random = MatsimRandom.getLocalInstance();
		this.guidance = guidance;
		this.ttObserver = ttObserver;
	}
	
	@Override
	public MobsimAgent createMobsimAgentFromPerson(Person p) {
		double r = random.nextDouble();
		MobsimDriverAgent agent = null;
		if (r < equipmentFraction){
			agent = new GuidanceWithindayAgent(p, this.simulation, this.guidance);
			this.ttObserver.addGuidedAgentId(p.getId());
		}
		else {
			agent = new PersonDriverAgentImpl(PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), this.simulation); 
			this.ttObserver.addUnGuidedAgentId(p.getId());
		}
		return agent;
	}

}
