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

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.gregor.casim.simulation.physics.CAEvent.CAEventType;

public class CAMultiLaneLinkTest extends MatsimTestCase {

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
	public void testCAMultiLaneLinkLaneChangingLR() {
		CAMultiLaneLink l = createCAMultiLaneLink();
		CAMoveableEntity[] l0 = l.getParticles(0);
		CAMoveableEntity[] l1 = l.getParticles(1);
		CAMoveableEntity[] l2 = l.getParticles(2);
		CASimpleDynamicAgent a = new CASimpleDynamicAgent(null, -1, null, l);
		a.materialize(1, 1, 1);
		l1[1] = a;
		l1[4] = new CASimpleDynamicAgent(null, -1, null, null);
		l1[5] = new CASimpleDynamicAgent(null, -1, null, null);
		l1[6] = new CASimpleDynamicAgent(null, -1, null, null);
		l1[7] = new CASimpleDynamicAgent(null, -1, null, null);
		l1[8] = new CASimpleDynamicAgent(null, -1, null, null);

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
			if (i < 4 || i > 8) {
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
		int cells = l.getSize();
		CAMoveableEntity[] l0 = l.getParticles(0);
		CAMoveableEntity[] l1 = l.getParticles(1);
		CAMoveableEntity[] l2 = l.getParticles(2);
		CASimpleDynamicAgent a = new CASimpleDynamicAgent(null, -1, null, l);
		a.materialize(cells - 2, -1, 1);
		l1[cells - 2] = a;
		l1[cells - 4] = new CASimpleDynamicAgent(null, -1, null, null);
		l1[cells - 5] = new CASimpleDynamicAgent(null, -1, null, null);
		l1[cells - 6] = new CASimpleDynamicAgent(null, -1, null, null);
		l1[cells - 7] = new CASimpleDynamicAgent(null, -1, null, null);
		l1[cells - 8] = new CASimpleDynamicAgent(null, -1, null, null);

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
			if (i < cells - 8 || i > cells - 4) {
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
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkFactory fac = sc.getNetwork().getFactory();
		Node n0 = fac.createNode(Id.createNodeId("n0"), new CoordImpl(0, 0));
		Node n1 = fac.createNode(Id.createNodeId("n1"), new CoordImpl(10, 0));
		Link l0 = fac.createLink(Id.createLinkId("l0"), n0, n1);
		Link l0rev = fac.createLink(Id.createLinkId("l0rev"), n1, n0);
		l0.setLength(10);
		l0rev.setLength(10);
		l0.setCapacity(3 * AbstractCANetwork.PED_WIDTH);
		l0rev.setCapacity(3 * AbstractCANetwork.PED_WIDTH);
		AbstractCANetwork caNet = new DummyNetwork();
		CAMultiLaneLink caLink = new CAMultiLaneLink(l0, l0rev, null, null,
				caNet, new CAMultiLaneDensityEstimatorSPH(null));
		return caLink;
	}

	private static final class DummyNetwork extends AbstractCANetwork {

		public DummyNetwork() {
			super(null, null, null, null);
		}

		@Override
		public void pushEvent(CAEvent event) {
			// nothing to be done here
		}

	}
}
