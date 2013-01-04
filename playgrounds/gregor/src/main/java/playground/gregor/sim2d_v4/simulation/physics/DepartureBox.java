/* *********************************************************************** *
 * project: org.matsim.*
 * DepartureBox.java
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

package playground.gregor.sim2d_v4.simulation.physics;

import java.util.Iterator;

import org.matsim.api.core.v01.Id;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;

public class DepartureBox extends PhysicalSim2DSection {

	public DepartureBox(Section sec, Sim2DScenario sim2dsc, double offsetX,
			double offsetY, PhysicalSim2DEnvironment penv) {
		super(sec, sim2dsc, offsetX, offsetY, penv);
	}

	@Override
	public void moveAgents() {
		Iterator<Sim2DAgent> it = this.agents.iterator();
		while (it.hasNext()) {
			Sim2DAgent agent = it.next();
			float [] v = agent.getVelocity();
			float dx = v[0] * this.timeStepSize;
			float dy = v[1] * this.timeStepSize;
			Id currentLinkId = agent.getCurrentLinkId();
			LinkInfo li = this.linkInfos.get(currentLinkId);
			float [] oldPos = agent.getPos();
			float newXPosX = oldPos[0] + dx;
			float newXPosY = oldPos[1] + dy;
			float lefOfFinishLine = CGAL.isLeftOfLine(newXPosX, newXPosY, li.finishLine.x0, li.finishLine.y0, li.finishLine.x1, li.finishLine.y1);
			if (lefOfFinishLine >= 0) { //agent has reached the end of link
				it.remove(); //removing the agent from the agents list
				PhysicalSim2DSection nextSection = this.penv.getPhysicalSim2DSectionAssociatedWithLinkId(agent.getCurrentLinkId());
				nextSection.addAgentToInBuffer(agent);
			}
			agent.move(dx, dy);
		}
	}

}
