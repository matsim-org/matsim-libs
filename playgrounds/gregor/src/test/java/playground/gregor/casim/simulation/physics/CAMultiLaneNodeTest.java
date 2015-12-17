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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.testcases.MatsimTestCase;

import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;

public class CAMultiLaneNodeTest extends MatsimTestCase {

	@Test
	public void testDynamicsRL() {
		Triple tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
		CAConstantDensityEstimator.RHO = 2;
		double d = AbstractCANetwork.ALPHA
				+ AbstractCANetwork.BETA
				* Math.pow(CAConstantDensityEstimator.RHO
						* AbstractCANetwork.PED_WIDTH, AbstractCANetwork.GAMMA);
		double z = 1 / (AbstractCANetwork.RHO_HAT + AbstractCANetwork.V_HAT)
				+ d;
		double tFree = tr.l0.getTFree();
		{
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "0,0,|<|,>");
			CAMoveableEntity a = agents.get(0);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.TTA);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "0,<,|0|,>");
			assertEquals(null, agents.get(1).getCurrentEvent());
			assertEquals(CAEventType.TTA, agents.get(0).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(0).getCurrentEvent()
					.getEventExcexutionTime());

		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "<,0,|<|,>");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.TTA);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "<,<,|0|,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(1).getCurrentEvent());
			assertEquals(null, agents.get(2).getCurrentEvent());

		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "0,0,|<|,<");
			CAMoveableEntity a = agents.get(0);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.TTA);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "0,<,|0|,<");
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + z, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.TTA, agents.get(0).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(0).getCurrentEvent()
					.getEventExcexutionTime());

		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "<,0,|<|,<");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.TTA);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "<,<,|0|,<");
			assertEquals(CAEventType.TTA, agents.get(2).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + z, agents.get(2).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(1).getCurrentEvent());
			assertEquals(null, agents.get(0).getCurrentEvent());

		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, ">,0,|<|,>");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.TTA);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, ">,<,|0|,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(t + d + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.SWAP, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(null, agents.get(2).getCurrentEvent());

		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, ">,0,|<|,<");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.TTA);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, ">,<,|0|,<");
			assertEquals(CAEventType.TTA, agents.get(2).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + z, agents.get(2).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(t + d + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.SWAP, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(null, agents.get(0).getCurrentEvent());

		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z / 2;
			List<CAMoveableEntity> agents = setConfiguration(tr, "|0|,<");
			CAMoveableEntity a = agents.get(0);
			CAEvent e = new CAEvent(t, a, tr.l1, CAEventType.TTA);
			tr.l1.handleEvent(e);

			checkPostConfiguration(tr, "|0|,<");
			assertEquals(CAEventType.TTA, agents.get(0).getCurrentEvent()
					.getCAEventType());
			assertEquals(z, agents.get(0).getCurrentEvent()
					.getEventExcexutionTime());
		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = 0;
			List<CAMoveableEntity> agents = setConfiguration(tr, "|>|,<");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.l1, CAEventType.TTA);
			tr.l1.handleEvent(e);

			checkPostConfiguration(tr, "|>|,<");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(1).getCurrentEvent());
		}

		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "0,>,|<|,0");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "0,<,|>|,0");
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
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "0,>,|<|,>");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "0,<,|>|,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(2).getCurrentEvent());

		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "<,>,|<|,0");
			CAMoveableEntity a = agents.get(2);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "<,<,|>|,0");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());

			assertEquals(null, agents.get(2).getCurrentEvent());
		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "<,>,|<|,>");
			CAMoveableEntity a = agents.get(2);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "<,<,|>|,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(1).getCurrentEvent());
			assertEquals(null, agents.get(2).getCurrentEvent());
			assertEquals(null, agents.get(3).getCurrentEvent());
		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, ">,>,|<|,0");
			CAMoveableEntity a = agents.get(2);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, ">,<,|>|,0");
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
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, ">,>,|<|,<");
			CAMoveableEntity a = agents.get(2);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, ">,<,|>|,<");
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
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "0,>,|<|,<");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "0,<,|>|,<");
			assertEquals(null, agents.get(2).getCurrentEvent());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.SWAP, agents.get(0).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + d + tFree, agents.get(0).getCurrentEvent()
					.getEventExcexutionTime());
		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "<,>,|<|,<");
			CAMoveableEntity a = agents.get(2);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "<,<,|>|,<");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(3).getCurrentEvent());
			assertEquals(CAEventType.SWAP, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + d + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(2).getCurrentEvent());
		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, ">,>,|<|,>");
			CAMoveableEntity a = agents.get(2);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, ">,<,|>|,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(CAEventType.SWAP, agents.get(2).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + d + tFree, agents.get(2).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(3).getCurrentEvent());
		}
	}

	@Test
	public void testDynamicsLR() {
		Triple tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
		CAConstantDensityEstimator.RHO = 2;
		double d = AbstractCANetwork.ALPHA
				+ AbstractCANetwork.BETA
				* Math.pow(CAConstantDensityEstimator.RHO
						* AbstractCANetwork.PED_WIDTH, AbstractCANetwork.GAMMA);
		double z = 1 / (AbstractCANetwork.RHO_HAT + AbstractCANetwork.V_HAT)
				+ d;
		double tFree = tr.l0.getTFree();
		{
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "<,|>|,0,0");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.TTA);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "<,|0|,>,0");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());

		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "<,|>|,0,>");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.TTA);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "<,|0|,>,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(1).getCurrentEvent());
			assertEquals(null, agents.get(2).getCurrentEvent());

		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, ">,|>|,0,0");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.TTA);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, ">,|0|,>,0");
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
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, ">,|>|,0,>");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.TTA);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, ">,|0|,>,>");
			assertEquals(CAEventType.TTA, agents.get(0).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + z, agents.get(0).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(1).getCurrentEvent());
			assertEquals(null, agents.get(2).getCurrentEvent());

		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "<,|>|,0,<");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.TTA);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "<,|0|,>,<");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(t + d + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(CAEventType.SWAP, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(null, agents.get(2).getCurrentEvent());

		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, ">,|>|,0,<");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.TTA);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, ">,|0|,>,<");
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
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z / 2;
			List<CAMoveableEntity> agents = setConfiguration(tr, ">,|0|");
			CAMoveableEntity a = agents.get(0);
			CAEvent e = new CAEvent(t, a, tr.l0, CAEventType.TTA);
			tr.l0.handleEvent(e);

			checkPostConfiguration(tr, ">,|0|");
			assertEquals(CAEventType.TTA, agents.get(0).getCurrentEvent()
					.getCAEventType());
			assertEquals(z, agents.get(0).getCurrentEvent()
					.getEventExcexutionTime());
		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = 0;
			List<CAMoveableEntity> agents = setConfiguration(tr, ">,|<|");
			CAMoveableEntity a = agents.get(0);
			CAEvent e = new CAEvent(t, a, tr.l0, CAEventType.TTA);
			tr.l0.handleEvent(e);

			checkPostConfiguration(tr, ">,|<|");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(1).getCurrentEvent());
		}

		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "0,|>|,<,0");
			CAMoveableEntity a = agents.get(0);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "0,|<|,>,0");
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
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "<,|>|,<,0");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "<,|<|,>,0");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(2).getCurrentEvent());

		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "0,|>|,<,>");
			CAMoveableEntity a = agents.get(0);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "0,|<|,>,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(CAEventType.TTA, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());

			assertEquals(null, agents.get(2).getCurrentEvent());
		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "<,|>|,<,>");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "<,|<|,>,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(1).getCurrentEvent());
			assertEquals(null, agents.get(2).getCurrentEvent());
			assertEquals(null, agents.get(3).getCurrentEvent());
		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "0,|>|,<,<");
			CAMoveableEntity a = agents.get(0);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "0,|<|,>,<");
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
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, ">,|>|,<,<");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, ">,|<|,>,<");
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
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, ">,|>|,<,0");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, ">,|<|,>,0");
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
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, ">,|>|,<,>");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, ">,|<|,>,>");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(null, agents.get(1).getCurrentEvent());
			assertEquals(CAEventType.SWAP, agents.get(2).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + d + tFree, agents.get(2).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(3).getCurrentEvent());
		}
		{
			tr = createCAMultiLaneNet(AbstractCANetwork.PED_WIDTH);
			double t = z;
			List<CAMoveableEntity> agents = setConfiguration(tr, "<,|>|,<,<");
			CAMoveableEntity a = agents.get(1);
			CAEvent e = new CAEvent(t, a, tr.n, CAEventType.SWAP);
			tr.n.handleEvent(e);

			checkPostConfiguration(tr, "<,|<|,>,<");
			assertEquals(null, agents.get(0).getCurrentEvent());
			assertEquals(CAEventType.SWAP, agents.get(1).getCurrentEvent()
					.getCAEventType());
			assertEquals(t + d + tFree, agents.get(1).getCurrentEvent()
					.getEventExcexutionTime());
			assertEquals(null, agents.get(2).getCurrentEvent());
			assertEquals(null, agents.get(3).getCurrentEvent());
		}
	}

	private void checkPostConfiguration(Triple tr, String string) {
		String[] conf = StringUtils.explode(string, ',');

		int nIdx = 0;
		for (int i = 0; i < conf.length; i++) {
			if (conf[i].startsWith("|")) {
				nIdx = i;
				break;
			}
		}

		int from = tr.l0.getSize() - nIdx;
		int idx = 0;
		for (int i = from; i < tr.l0.getSize(); i++) {
			CAMoveableEntity obj0 = tr.l0.getParticles(0)[i];
			if (conf[idx].equals("<")) {
				assertEquals(-1, obj0.getDir());
			} else if (conf[idx].equals(">")) {
				assertEquals(1, obj0.getDir());
			} else {
				assertEquals(null, obj0);
			}
			idx++;
		}

		CAMoveableEntity obj1 = tr.n.peekForAgentInSlot(0);
		if (conf[idx].equals("|0|")) {
			assertEquals(null, obj1);
		} else {
			assertEquals(true, obj1 != null ? true : false);
		}

		idx++;
		int pIdx = 0;
		for (int i = idx; i < conf.length; i++) {
			CAMoveableEntity obj = tr.l1.getParticles(0)[pIdx];
			if (conf[i].equals("<")) {
				assertEquals(-1, obj.getDir());
			} else if (conf[i].equals(">")) {
				assertEquals(1, obj.getDir());
			} else {
				assertEquals(null, obj);
			}
			idx++;
			pIdx++;
		}
	}

	private List<CAMoveableEntity> setConfiguration(Triple tr, String string) {
		List<CAMoveableEntity> ret = new ArrayList<>();
		String[] conf = StringUtils.explode(string, ',');

		int nIdx = 0;
		for (int i = 0; i < conf.length; i++) {
			if (conf[i].startsWith("|")) {
				nIdx = i;
				break;
			}
		}

		int from = tr.l0.getSize() - nIdx;

		CAMoveableEntity[] lane0 = tr.l0.getParticles(0);
		CAMultiLaneNode n = tr.n;
		CAMoveableEntity[] lane1 = tr.l1.getParticles(0);

		List<Link> lr = new ArrayList<>();
		lr.add(tr.l0.getDownstreamLink());
		lr.add(tr.l1.getDownstreamLink());
		List<Link> rl = new ArrayList<>();
		rl.add(tr.l1.getUpstreamLink());
		rl.add(tr.l0.getUpstreamLink());

		int idx = 0;
		for (int i = from; i < tr.l0.getSize(); i++) {
			if (conf[idx].equals("<")) {
				CASimpleDynamicAgent a = new CASimpleDynamicAgent(rl, -1, Id.create(idx, CASimpleDynamicAgent.class),
						tr.l0);
				a.materialize(i, -1, 0);
				lane0[i] = a;
				ret.add(a);
			} else if (conf[idx].equals(">")) {
				CASimpleDynamicAgent a = new CASimpleDynamicAgent(lr, 1, Id.create(idx, CASimpleDynamicAgent.class),
						tr.l0);
				a.materialize(i, 1, 0);
				lane0[i] = a;
				ret.add(a);
			}

			idx++;
		}

		if (conf[idx].equals("|<|")) {
			CASimpleDynamicAgent a = new CASimpleDynamicAgent(rl, 1, Id.create(idx, CASimpleDynamicAgent.class),
					tr.l0);
			a.materialize(-1, -1, 0);
			n.putAgentInSlot(0, a);
			ret.add(a);
		} else if (conf[idx].equals("|>|")) {
			CASimpleDynamicAgent a = new CASimpleDynamicAgent(lr, 1, Id.create(idx, CASimpleDynamicAgent.class),
					tr.l0);
			a.materialize(-1, 1, 0);
			n.putAgentInSlot(0, a);
			ret.add(a);
		}

		idx++;
		int pIdx = 0;
		for (int i = idx; i < conf.length; i++) {
			if (conf[i].equals("<")) {
				CASimpleDynamicAgent a = new CASimpleDynamicAgent(rl, 1, Id.create(idx, CASimpleDynamicAgent.class),
						tr.l1);
				a.materialize(pIdx, -1, 0);
				lane1[pIdx] = a;
				ret.add(a);
			} else if (conf[i].equals(">")) {
				CASimpleDynamicAgent a = new CASimpleDynamicAgent(rl, -1, Id.create(idx, CASimpleDynamicAgent.class),
						tr.l1);
				a.materialize(pIdx, 1, 0);
				lane1[pIdx] = a;
				ret.add(a);
			}

			idx++;
			pIdx++;
		}
		return ret;
	}

	private Triple createCAMultiLaneNet(double width) {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();
		NetworkFactory fac = sc.getNetwork().getFactory();
		Node n0 = fac.createNode(Id.createNodeId("n0"), new Coord((double) 0, (double) 0));
		Node n1 = fac.createNode(Id.createNodeId("n1"), new Coord((double) 10, (double) 0));
		Node n2 = fac.createNode(Id.createNodeId("n2"), new Coord((double) 20, (double) 0));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);

		Link l0 = fac.createLink(Id.createLinkId("l0"), n0, n1);
		Link l0rev = fac.createLink(Id.createLinkId("l0rev"), n1, n0);
		l0.setLength(10);
		l0rev.setLength(10);
		l0.setCapacity(width);
		l0rev.setCapacity(width);
		net.addLink(l0);
		net.addLink(l0rev);

		Link l1 = fac.createLink(Id.createLinkId("l1"), n1, n2);
		Link l1rev = fac.createLink(Id.createLinkId("l1rev"), n2, n1);
		l1.setLength(10);
		l1rev.setLength(10);
		l1.setCapacity(width);
		l1rev.setCapacity(width);
		net.addLink(l1);
		net.addLink(l1rev);

		DummyNetwork caNet = new DummyNetwork(net);
		Triple tr = new Triple();
		tr.l0 = (CAMultiLaneLink) caNet.getCALink(l0.getId());
		tr.l1 = (CAMultiLaneLink) caNet.getCALink(l1.getId());
		tr.n = (CAMultiLaneNode) caNet.getNodes().get(n1.getId());

		return tr;
	}

	private static final class Triple {
		CAMultiLaneLink l0;
		CAMultiLaneNode n;
		CAMultiLaneLink l1;
	}

	private static final class DummyNetwork extends AbstractCANetwork {

		private CAConstantDensityEstimator k = new CAConstantDensityEstimator();
		private EventsManager em = new EventsManager() {

			@Override
			public void resetHandlers(int iteration) {
				// TODO Auto-generated method stub

			}

			@Override
			public void removeHandler(EventHandler handler) {
				// TODO Auto-generated method stub

			}

			@Override
			public void processEvent(Event event) {
				// TODO Auto-generated method stub

			}

			@Override
			public void initProcessing() {
				// TODO Auto-generated method stub

			}

			@Override
			public void finishProcessing() {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterSimStep(double time) {
				// TODO Auto-generated method stub

			}

			@Override
			public void addHandler(EventHandler handler) {
				// TODO Auto-generated method stub

			}
		};

		public DummyNetwork(Network net) {
			super(net, null, null, null);
			k.RHO = 2;
			init();
		}

		private void init() {
			for (Node n : this.net.getNodes().values()) {

				CAMultiLaneNode caNode = new CAMultiLaneNode(n, this);
				this.caNodes.put(n.getId(), caNode);
				if (caNode.getTFree() < this.tFreeMin) {
					this.tFreeMin = caNode.getTFree();
				}
			}

			for (Link l : this.net.getLinks().values()) {
				CAMultiLaneNode us = (CAMultiLaneNode) this.caNodes.get(l
						.getFromNode().getId());
				CAMultiLaneNode ds = (CAMultiLaneNode) this.caNodes.get(l
						.getToNode().getId());
				Link rev = null;
				for (Link ll : l.getToNode().getOutLinks().values()) {
					if (ll.getToNode() == l.getFromNode()) {
						rev = ll;
					}
				}
				if (rev != null) {

					CALink revCA = this.caLinks.get(rev.getId());
					if (revCA != null) {
						this.caLinks.put(l.getId(), revCA);
						continue;
					}
				}
				CAMultiLaneLink caL = new CAMultiLaneLink(l, rev, ds, us, this,
						this.k);

				if (caL.getTFree() < tFreeMin) {
					tFreeMin = caL.getTFree();
				}

				us.addLink(caL);
				ds.addLink(caL);
				this.caLinks.put(l.getId(), caL);
			}
		}

		@Override
		public void pushEvent(CAEvent event) {
			event.getCAAgent().setCurrentEvent(event);
		}

		@Override
		public EventsManager getEventsManager() {
			return this.em;
		}
	}
}
