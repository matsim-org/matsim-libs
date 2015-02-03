/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.gregor.casim.simulation.physics;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.testcases.MatsimTestCase;

import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;

public class CAMultiLaneLinkTest extends MatsimTestCase {

	@Test
	public void testDynamicsRL() {
		CAMultiLaneLink l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
		CAConstantDensityEstimator.RHO = 2;
		double d = AbstractCANetwork.ALPHA
				+ AbstractCANetwork.BETA
				* Math.pow(CAConstantDensityEstimator.RHO
						* AbstractCANetwork.PED_WIDTH, AbstractCANetwork.GAMMA);
		double z = 1 / (AbstractCANetwork.RHO_HAT + AbstractCANetwork.V_HAT)
				+ d;
		double tFree = l.getTFree();
		{
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "0,0,<,>,");
			CAMoveableEntity a = agents.get(0);
			CAEvent e = new CAEvent(t, a, l, CAEventType.TTA);
			l.handleEvent(e);

			checkPostConfiguration(l, "0,<,0,>");
			assertEquals(CAEventType.TTA, agents.get(0).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(0).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(1).getCurrentEvent());

		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "<,0,<,>");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.TTA);
			l.handleEvent(e);

			checkPostConfiguration(l, "<,<,0,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(1).getCurrentEvent());
			assertEquals(null, agents.get(2).getCurrentEvent());

		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "0,0,<,<");
			CAMoveableEntity a = agents.get(0);
			CAEvent e = new CAEvent(t, a, l, CAEventType.TTA);
			l.handleEvent(e);

			checkPostConfiguration(l, "0,<,0,<");
			assertEquals(CAEventType.TTA, agents.get(0).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(0).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + z, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());

		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "<,0,<,<");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.TTA);
			l.handleEvent(e);

			checkPostConfiguration(l, "<,<,0,<");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(1).getCurrentEvent());
			assertEquals(CAEventType.TTA, agents.get(2).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + z, agents.get(2).getCurrentEvent()
					.getEventExcexutionTime());

		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, ">,0,<,>");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.TTA);
			l.handleEvent(e);

			checkPostConfiguration(l, ">,<,0,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(t + d + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.SWAP, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(null, agents.get(2).getCurrentEvent());

		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, ">,0,<,<");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.TTA);
			l.handleEvent(e);

			checkPostConfiguration(l, ">,<,0,<");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(CAEventType.TTA, agents.get(2).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + z, agents.get(2).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(t + d + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.SWAP, agents.get(1).getCurrentEvent()
					.getCAEventType());

		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z / 2;
			List<CAMoveableEntity> agents = setConfiguration(l, "0,<");
			CAMoveableEntity a = agents.get(0);
			CAEvent e = new CAEvent(t, a, l, CAEventType.TTA);
			l.handleEvent(e);

			checkPostConfiguration(l, "0,<");
			assertEquals(CAEventType.TTA, agents.get(0).getCurrentEvent()
					.getCAEventType());
			assertEquals(z, agents.get(0).getCurrentEvent()
					.getEventExcexutionTime());
		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = 0;
			List<CAMoveableEntity> agents = setConfiguration(l, ">,<");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.TTA);
			l.handleEvent(e);

			checkPostConfiguration(l, ">,<");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(1).getCurrentEvent());
		}

		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "0,>,<,0");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, "0,<,>,0");
			assertEquals(CAEventType.TTA, agents.get(0).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(0).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());

		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "0,>,<,>");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, "0,<,>,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(2).getCurrentEvent());

		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "<,>,<,0");
			CAMoveableEntity a = agents.get(2);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, "<,<,>,0");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());

			assertEquals(null, agents.get(2).getCurrentEvent());
		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "<,>,<,>");
			CAMoveableEntity a = agents.get(2);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, "<,<,>,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(1).getCurrentEvent());
			assertEquals(null, agents.get(2).getCurrentEvent());
			assertEquals(null, agents.get(3).getCurrentEvent());
		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, ">,>,<,0");
			CAMoveableEntity a = agents.get(2);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, ">,<,>,0");
			assertEquals(CAEventType.SWAP, agents.get(2).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + d + tFree, agents.get(2).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(0).getCurrentEvent());
		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, ">,>,<,<");
			CAMoveableEntity a = agents.get(2);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, ">,<,>,<");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(CAEventType.SWAP, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + d + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.SWAP, agents.get(2).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + d + tFree, agents.get(2).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(3).getCurrentEvent());
		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "0,>,<,<");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, "0,<,>,<");
			assertEquals(CAEventType.SWAP, agents.get(0).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + d + tFree, agents.get(0).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(2).getCurrentEvent());
		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "<,>,<,<");
			CAMoveableEntity a = agents.get(2);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, "<,<,>,<");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(2).getCurrentEvent());
			assertEquals(CAEventType.SWAP, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + d + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(3).getCurrentEvent());
		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, ">,>,<,>");
			CAMoveableEntity a = agents.get(2);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, ">,<,>,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(1).getCurrentEvent());
			assertEquals(CAEventType.SWAP, agents.get(2).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + d + tFree, agents.get(2).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(3).getCurrentEvent());
		}
	}

	@Test
	public void testDynamicsLR() {
		CAMultiLaneLink l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
		CAConstantDensityEstimator.RHO = 2;
		double d = AbstractCANetwork.ALPHA
				+ AbstractCANetwork.BETA
				* Math.pow(CAConstantDensityEstimator.RHO
						* AbstractCANetwork.PED_WIDTH, AbstractCANetwork.GAMMA);
		double z = 1 / (AbstractCANetwork.RHO_HAT + AbstractCANetwork.V_HAT)
				+ d;
		double tFree = l.getTFree();
		{
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "<,>,0,0");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.TTA);
			l.handleEvent(e);

			checkPostConfiguration(l, "<,0,>,0");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());

		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "<,>,0,>");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.TTA);
			l.handleEvent(e);

			checkPostConfiguration(l, "<,0,>,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(1).getCurrentEvent());
			assertEquals(null, agents.get(2).getCurrentEvent());

		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, ">,>,0,0");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.TTA);
			l.handleEvent(e);

			checkPostConfiguration(l, ">,0,>,0");
			assertEquals(CAEventType.TTA, agents.get(0).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + z, agents.get(0).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());

		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, ">,>,0,>");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.TTA);
			l.handleEvent(e);

			checkPostConfiguration(l, ">,0,>,>");
			assertEquals(CAEventType.TTA, agents.get(0).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + z, agents.get(0).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(1).getCurrentEvent());
			assertEquals(null, agents.get(2).getCurrentEvent());

		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "<,>,0,<");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.TTA);
			l.handleEvent(e);

			checkPostConfiguration(l, "<,0,>,<");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(t + d + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.SWAP, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(null, agents.get(2).getCurrentEvent());

		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, ">,>,0,<");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.TTA);
			l.handleEvent(e);

			checkPostConfiguration(l, ">,0,>,<");
			assertEquals(CAEventType.TTA, agents.get(0).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + z, agents.get(0).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(t + d + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.SWAP, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(null, agents.get(2).getCurrentEvent());

		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z / 2;
			List<CAMoveableEntity> agents = setConfiguration(l, ">,0");
			CAMoveableEntity a = agents.get(0);
			CAEvent e = new CAEvent(t, a, l, CAEventType.TTA);
			l.handleEvent(e);

			checkPostConfiguration(l, ">,0");
			assertEquals(CAEventType.TTA, agents.get(0).getCurrentEvent()
					.getCAEventType());
			assertEquals(z, agents.get(0).getCurrentEvent()
					.getEventExcexutionTime());
		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = 0;
			List<CAMoveableEntity> agents = setConfiguration(l, ">,<");
			CAMoveableEntity a = agents.get(0);
			CAEvent e = new CAEvent(t, a, l, CAEventType.TTA);
			l.handleEvent(e);

			checkPostConfiguration(l, ">,<");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(1).getCurrentEvent());
		}

		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "0,>,<,0");
			CAMoveableEntity a = agents.get(0);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, "0,<,>,0");
			assertEquals(CAEventType.TTA, agents.get(0).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(0).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());

		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "<,>,<,0");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, "<,<,>,0");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(2).getCurrentEvent());

		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "0,>,<,>");
			CAMoveableEntity a = agents.get(0);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, "0,<,>,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());

			assertEquals(null, agents.get(2).getCurrentEvent());
		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "<,>,<,>");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, "<,<,>,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(1).getCurrentEvent());
			assertEquals(null, agents.get(2).getCurrentEvent());
			assertEquals(null, agents.get(3).getCurrentEvent());
		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "0,>,<,<");
			CAMoveableEntity a = agents.get(0);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, "0,<,>,<");
			assertEquals(CAEventType.SWAP, agents.get(0).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + d + tFree, agents.get(0).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(2).getCurrentEvent());
		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, ">,>,<,<");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, ">,<,>,<");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(CAEventType.SWAP, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + d + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.SWAP, agents.get(2).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + d + tFree, agents.get(2).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(3).getCurrentEvent());
		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, ">,>,<,0");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, ">,<,>,0");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.SWAP, agents.get(2).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + d + tFree, agents.get(2).getCurrentEvent()
					.getEventExcexutionTime());
		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, ">,>,<,>");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, ">,<,>,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(1).getCurrentEvent());
			assertEquals(CAEventType.SWAP, agents.get(2).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + d + tFree, agents.get(2).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(3).getCurrentEvent());
		}
		{
			l = createCAMultiLaneLink(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(l, "<,>,<,<");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, l, CAEventType.SWAP);
			l.handleEvent(e);

			checkPostConfiguration(l, "<,<,>,<");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(CAEventType.SWAP, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + d + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(2).getCurrentEvent());
			assertEquals(null, agents.get(3).getCurrentEvent());
		}
	}

	private void checkPostConfiguration(CAMultiLaneLink l, String string) {
		String[] conf = StringUtils.explode(string, ',');
		int idx = 0;
		CAMoveableEntity[] lane = l.getParticles(0);
		for (int i = 0; i < conf.length; i++) {
			if (conf[i].equals("0")) {
				assertEquals(null, lane[idx]);
			} else if (conf[i].equals("<")) {
				assertEquals(-1, lane[idx].getDir());
			} else if (conf[i].equals(">")) {
				assertEquals(1, lane[idx].getDir());
			}

			idx++;
		}
	}

	private List<CAMoveableEntity> setConfiguration(CAMultiLaneLink l,
			String string) {
		List<CAMoveableEntity> ret = new ArrayList<>();
		String[] conf = StringUtils.explode(string, ',');
		int idx = 0;
		CAMoveableEntity[] lane = l.getParticles(0);
		for (int i = 0; i < conf.length; i++) {
			if (conf[i].equals("<")) {
				CASimpleDynamicAgent a = new CASimpleDynamicAgent(null, -1,
						null, l);
				a.materialize(idx, -1, 0);
				lane[idx] = a;
				ret.add(a);
			} else if (conf[i].equals(">")) {
				CASimpleDynamicAgent a = new CASimpleDynamicAgent(null, -1,
						null, l);
				a.materialize(idx, 1, 0);
				lane[idx] = a;
				ret.add(a);
			}

			idx++;
		}
		return ret;

	}

	@Test
	public void testCAMultiLaneLinkParameters() {
		CAMultiLaneLink l = createCAMultiLaneLink();

		// width equals 3 * PED_WIDTH --> 3 lanes
		assertEquals(3, l.getNrLanes());

		// nr of cells per lane
		// lanes width exactly PED_WIDTH -->
		double cellLength = 1 / (AbstractCANetwork.RHO_HAT * AbstractCANetwork.PED_WIDTH);
		int nrCells = (int) Math.round(l.getLink().getLength() / cellLength);
		assertEquals(nrCells, l.getSize());

		double spd = cellLength / l.getTFree();
		assertEquals(AbstractCANetwork.V_HAT, spd);
	}

	@Test
	public void testConflicDelayImplementationLR() {
		CAMultiLaneLink l = createCAMultiLaneLink();
		CAConstantDensityEstimator.RHO = 2;
		double d = AbstractCANetwork.ALPHA
				+ AbstractCANetwork.BETA
				* Math.pow(CAConstantDensityEstimator.RHO
						* AbstractCANetwork.PED_WIDTH, AbstractCANetwork.GAMMA);
		double z = 1 / (AbstractCANetwork.RHO_HAT + AbstractCANetwork.V_HAT)
				+ d;
		double tFree = l.getTFree();
		CAMoveableEntity[] l0 = l.getParticles(0);
		CAMoveableEntity[] l1 = l.getParticles(1);
		CAMoveableEntity[] l2 = l.getParticles(2);
		CASimpleDynamicAgent a = new CASimpleDynamicAgent(null, -1, null, l);
		a.materialize(1, 1, 1);
		l1[1] = a;
		CASimpleDynamicAgent b = new CASimpleDynamicAgent(null, -1, null, l);
		b.materialize(3, -1, 1);
		l1[3] = b;
		l0[2] = new CASimpleDynamicAgent(null, -1, null, l);
		l2[2] = new CASimpleDynamicAgent(null, -1, null, l);
		CAEvent e = new CAEvent(z, a, l, CAEventType.TTA);
		l.handleEvent(e);
		assertEquals(z + d + tFree, a.getCurrentEvent()
				.getEventExcexutionTime());
		assertEquals(CAEventType.SWAP, a.getCurrentEvent().getCAEventType());
	}

	@Test
	public void testConflicDelayImplementationRL() {
		CAMultiLaneLink l = createCAMultiLaneLink();
		CAConstantDensityEstimator.RHO = 2;
		double d = AbstractCANetwork.ALPHA
				+ AbstractCANetwork.BETA
				* Math.pow(CAConstantDensityEstimator.RHO
						* AbstractCANetwork.PED_WIDTH, AbstractCANetwork.GAMMA);
		double z = 1 / (AbstractCANetwork.RHO_HAT + AbstractCANetwork.V_HAT)
				+ d;
		double tFree = l.getTFree();
		CAMoveableEntity[] l0 = l.getParticles(0);
		CAMoveableEntity[] l1 = l.getParticles(1);
		CAMoveableEntity[] l2 = l.getParticles(2);
		CASimpleDynamicAgent a = new CASimpleDynamicAgent(null, -1, null, l);
		a.materialize(1, 1, 1);
		l1[1] = a;
		CASimpleDynamicAgent b = new CASimpleDynamicAgent(null, -1, null, l);
		b.materialize(3, -1, 1);
		l1[3] = b;
		l0[2] = new CASimpleDynamicAgent(null, -1, null, l);
		l2[2] = new CASimpleDynamicAgent(null, -1, null, l);
		CAEvent e = new CAEvent(z, b, l, CAEventType.TTA);
		l.handleEvent(e);
		assertEquals(z + d + tFree, b.getCurrentEvent()
				.getEventExcexutionTime());
		assertEquals(CAEventType.SWAP, b.getCurrentEvent().getCAEventType());
	}

	@Test
	public void testTimeGapImplementationLR() {
		CAMultiLaneLink l = createCAMultiLaneLink();
		CAConstantDensityEstimator.RHO = 2;
		double d = AbstractCANetwork.ALPHA
				+ AbstractCANetwork.BETA
				* Math.pow(CAConstantDensityEstimator.RHO
						* AbstractCANetwork.PED_WIDTH, AbstractCANetwork.GAMMA);
		double z = 1 / (AbstractCANetwork.RHO_HAT + AbstractCANetwork.V_HAT)
				+ d;
		double tFree = l.getTFree();
		{
			CAMoveableEntity[] l1 = l.getParticles(1);
			CASimpleDynamicAgent a = new CASimpleDynamicAgent(null, -1, null, l);
			a.materialize(1, 1, 1);
			l1[1] = a;
			CASimpleDynamicAgent b = new CASimpleDynamicAgent(null, -1, null, l);
			b.materialize(0, 1, 1);
			l1[0] = b;
			CAEvent e = new CAEvent(z / 2, a, l, CAEventType.TTA);
			l.handleEvent(e);
			assertEquals(z, a.getCurrentEvent().getEventExcexutionTime());
			assertEquals(CAEventType.TTA, a.getCurrentEvent().getCAEventType());
			assertEquals(null, b.getCurrentEvent());
		}
		{
			CAMoveableEntity[] l0 = l.getParticles(0);
			CASimpleDynamicAgent a = new CASimpleDynamicAgent(null, -1, null, l);
			a.materialize(1, 1, 0);
			l0[1] = a;
			CASimpleDynamicAgent b = new CASimpleDynamicAgent(null, -1, null, l);
			b.materialize(0, 1, 0);
			l0[0] = b;

			CAEvent e = new CAEvent(z, a, l, CAEventType.TTA);
			l.handleEvent(e);
			assertEquals(z + tFree, a.getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.TTA, a.getCurrentEvent().getCAEventType());
			assertEquals(CAEventType.TTA, b.getCurrentEvent().getCAEventType());
			assertEquals(z + z, b.getCurrentEvent().getEventExcexutionTime());
		}

	}

	@Test
	public void testTimeGapImplementationRL() {
		CAMultiLaneLink l = createCAMultiLaneLink();
		int cells = l.getSize();
		CAConstantDensityEstimator.RHO = 2;
		double d = AbstractCANetwork.ALPHA
				+ AbstractCANetwork.BETA
				* Math.pow(CAConstantDensityEstimator.RHO
						* AbstractCANetwork.PED_WIDTH, AbstractCANetwork.GAMMA);
		double z = 1 / (AbstractCANetwork.RHO_HAT + AbstractCANetwork.V_HAT)
				+ d;
		double tFree = l.getTFree();
		{
			CAMoveableEntity[] l1 = l.getParticles(1);
			CASimpleDynamicAgent a = new CASimpleDynamicAgent(null, -1, null, l);
			a.materialize(cells - 2, -1, 1);
			l1[cells - 2] = a;
			CASimpleDynamicAgent b = new CASimpleDynamicAgent(null, -1, null, l);
			b.materialize(cells - 1, -1, 1);
			l1[cells - 1] = b;
			CAEvent e = new CAEvent(z / 2, a, l, CAEventType.TTA);
			l.handleEvent(e);
			assertEquals(z, a.getCurrentEvent().getEventExcexutionTime());
			assertEquals(CAEventType.TTA, a.getCurrentEvent().getCAEventType());
			assertEquals(null, b.getCurrentEvent());
		}
		{
			CAMoveableEntity[] l0 = l.getParticles(0);
			CASimpleDynamicAgent a = new CASimpleDynamicAgent(null, -1, null, l);
			a.materialize(cells - 2, -1, 0);
			l0[cells - 2] = a;
			CASimpleDynamicAgent b = new CASimpleDynamicAgent(null, -1, null, l);
			b.materialize(cells - 1, -1, 1);
			l0[cells - 1] = b;
			CAEvent e = new CAEvent(z, a, l, CAEventType.TTA);
			l.handleEvent(e);
			assertEquals(z + tFree, a.getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.TTA, a.getCurrentEvent().getCAEventType());
			assertEquals(z + z, b.getCurrentEvent().getEventExcexutionTime());
		}

	}

	@Test
	public void testCAMultiLaneLinkLaneChangingLR() {
		CAMultiLaneLink l = createCAMultiLaneLink();
		CAConstantDensityEstimator.RHO = 2;

		CAMoveableEntity[] l0 = l.getParticles(0);
		CAMoveableEntity[] l1 = l.getParticles(1);
		CAMoveableEntity[] l2 = l.getParticles(2);
		CASimpleDynamicAgent a = new CASimpleDynamicAgent(null, -1, null, l);
		a.materialize(1, 1, 1);
		l1[1] = a;
		l1[4] = new CASimpleDynamicAgent(null, -1, null, null);

		l0[2] = new CASimpleDynamicAgent(null, -1, null, null);
		l2[8] = new CASimpleDynamicAgent(null, -1, null, null);
		CAEvent e = new CAEvent(10, a, l, CAEventType.TTA);
		l.handleEvent(e);

		assertEquals(2, a.getLane());
		assertEquals(2, a.getPos());
		assertEquals(1, a.getDir());

		int cells = l.getSize();
		for (int i = 0; i < cells; i++) {
			if (i != 2) {
				assertEquals(null, l0[i]);
			} else {
				assertEquals(true, l0[i] != null ? true : false);
				assertEquals(a, l2[i]);
			}
			if (i != 4) {
				assertEquals(null, l1[i]);
			} else {
				assertEquals(true, l1[i] != null ? true : false);
			}
			if (i == 8 || i == 2) {
				assertEquals(true, l2[i] != null ? true : false);
			} else {
				assertEquals(null, l2[i]);
			}
		}
	}

	@Test
	public void testCAMultiLaneLinkLaneChangingRL() {
		CAMultiLaneLink l = createCAMultiLaneLink();
		CAConstantDensityEstimator.RHO = 2;
		int cells = l.getSize();
		CAMoveableEntity[] l0 = l.getParticles(0);
		CAMoveableEntity[] l1 = l.getParticles(1);
		CAMoveableEntity[] l2 = l.getParticles(2);
		CASimpleDynamicAgent a = new CASimpleDynamicAgent(null, -1, null, l);
		a.materialize(cells - 2, -1, 1);
		l1[cells - 2] = a;
		l1[cells - 4] = new CASimpleDynamicAgent(null, -1, null, null);

		l0[cells - 3] = new CASimpleDynamicAgent(null, -1, null, null);
		l2[cells - 8] = new CASimpleDynamicAgent(null, -1, null, null);
		CAEvent e = new CAEvent(10, a, l, CAEventType.TTA);
		l.handleEvent(e);

		assertEquals(2, a.getLane());
		assertEquals(cells - 3, a.getPos());
		assertEquals(-1, a.getDir());

		for (int i = 0; i < cells; i++) {
			if (i != cells - 3) {
				assertEquals(null, l0[i]);
			} else {
				assertEquals(true, l0[i] != null ? true : false);
				assertEquals(a, l2[i]);
			}
			if (i != cells - 4) {
				assertEquals(null, l1[i]);
			} else {
				assertEquals(true, l1[i] != null ? true : false);
			}
			if (i == cells - 8 || i == cells - 3) {
				assertEquals(true, l2[i] != null ? true : false);
			} else {
				assertEquals(null, l2[i]);
			}
		}
	}

	private CAMultiLaneLink createCAMultiLaneLink() {
		DummyNetwork caNet = new DummyNetwork();
		return createCAMultiLaneLink(caNet, 3 * AbstractCANetwork.PED_WIDTH);
	}

	private CAMultiLaneLink createCAMultiLaneLink(double width) {
		DummyNetwork caNet = new DummyNetwork();
		return createCAMultiLaneLink(caNet, width);
	}

	private CAMultiLaneLink createCAMultiLaneLink(DummyNetwork caNet,
			double width) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkFactory fac = sc.getNetwork().getFactory();
		Node n0 = fac.createNode(Id.createNodeId("n0"), new CoordImpl(0, 0));
		Node n1 = fac.createNode(Id.createNodeId("n1"), new CoordImpl(10, 0));
		Link l0 = fac.createLink(Id.createLinkId("l0"), n0, n1);
		Link l0rev = fac.createLink(Id.createLinkId("l0rev"), n1, n0);
		l0.setLength(10);
		l0rev.setLength(10);
		l0.setCapacity(width);
		l0rev.setCapacity(width);
		CAMultiLaneLink caLink = new CAMultiLaneLink(l0, l0rev, null, null,
				caNet, new CAConstantDensityEstimator());
		return caLink;
	}

	private static final class DummyNetwork extends AbstractCANetwork {

		public DummyNetwork() {
			super(null, null, null, null);
		}

		@Override
		public void pushEvent(CAEvent event) {
			event.getCAAgent().setCurrentEvent(event);
		}

	}
}
