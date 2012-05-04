/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DAgentFactory.java
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

package playground.gregor.sim2d_v3.simulation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import playground.gregor.sim2d_v3.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v3.simulation.floor.Agent2D;
import playground.gregor.sim2d_v3.simulation.floor.PhysicalAgentRepresentation;
import playground.gregor.sim2d_v3.simulation.floor.VelocityCalculator;
import playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.LinkSwitcher;
import playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.MentalLinkSwitcher;

import com.vividsolutions.jts.geom.Coordinate;

public class Sim2DAgentFactory implements AgentFactory {

	protected final Netsim simulation;
	private final DefaultAgentFactory defaultAgentFactory;
	private final Scenario sc;
	private final VelocityCalculator velocityCalculator;
	private final LinkSwitcher mlsw;

	public Sim2DAgentFactory(final Netsim simulation, Scenario sc) {
		this.simulation = simulation;
		this.defaultAgentFactory = new DefaultAgentFactory(simulation);
		this.sc = sc;
		this.velocityCalculator = new VelocityCalculator(sc.getConfig().plansCalcRoute());
		Sim2DConfigGroup s2d = (Sim2DConfigGroup) sc.getConfig().getModule("sim2d");
		
		if (s2d.isEnableMentalLinkSwitch()){
			this.mlsw = new MentalLinkSwitcher(sc);
		} else {
			this.mlsw = new LinkSwitcher() {
				@Override
				public void checkForMentalLinkSwitch(Id curr, Id next, Agent2D agent) {
					// nothing to do here
				}
			};
		}
	}

	public Agent2D createAgent2DFromMobsimAgent(MobsimDriverAgent mobsimDriverAgent) {
		
//		PhysicalAgentRepresentation par = new VelocityDependentEllipse();
		PhysicalAgentRepresentation par = new PhysicalAgentRepresentation() {
			@Override
			public void update(double v, double alpha, Coordinate pos) {
				// TODO Auto-generated method stub
			}
			@Override
			public void translate(Coordinate pos) {
				// TODO Auto-generated method stub
			}
		};
		
		Agent2D agent = new Agent2D(mobsimDriverAgent, this.sc, this.velocityCalculator, this.mlsw, par);
		return agent;
	}
	
	@Override
	public MobsimAgent createMobsimAgentFromPerson(Person p) {
		return this.defaultAgentFactory.createMobsimAgentFromPerson(p);
	}

}
