package playground.gregor.ctsim.physics;
/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.matsim.testcases.MatsimTestCase;
import playground.gregor.ctsim.simulation.physics.CTCell;
import playground.gregor.ctsim.simulation.physics.CTCellFace;
import playground.gregor.ctsim.simulation.physics.CTLinkCell;

import java.util.List;

/**
 * Created by laemmel on 15/10/15.
 */
public class CTCellTest extends MatsimTestCase {

	private playground.gregor.ctsim.simulation.physics.CTPed CTPed;

	public void testAddGetFace() throws Exception {
		CTCell c = getCTLinkCell();
		CTCellFace face = new CTCellFace(0, 0, 1, 1, null, 1);
		c.addFace(face);
		List<CTCellFace> faces = c.getFaces();
		assertEquals("number of faces", 1, faces.size());
		assertEquals("same face", face, faces.iterator().next());
	}

	private CTLinkCell getCTLinkCell() {
		CTLinkCell cell = new CTLinkCell(42.0, 24.0, null, null, 1, 1);

		return cell;
	}

	public void testAddNeighbor() throws Exception {
		CTCell c = getCTLinkCell();
		CTCell nb = getCTLinkCell();
		c.addNeighbor(nb);
		List<CTCell> nbs = c.getNeighbors();
		assertEquals("number of neighbors", 1, nbs.size());
		assertEquals("same neighbor", nb, nbs.iterator().next());
	}

	public void testSetArea() throws Exception {
		CTCell c = getCTLinkCell();
		c.setArea(1234);
		double a = c.getAlpha();
		assertEquals("correct area", 1234d, a);
//		c.getA
	}


//	public void testGetFHHi() throws Exception {
//		CTCell c = getCTLinkCell();
////		h = c.getFHHi();
//	}

	public void testGetXY() throws Exception {
		CTCell c = getCTLinkCell();
		double x = c.getX();
		double y = c.getY();
		assertEquals("x-coordinate", 42d, x);
		assertEquals("y-coordinate", 24d, y);
	}

//	public void testGetJ() throws Exception {
//
//
//		CTCell c = getCTLinkCell();
//		c.setRho(1.2);
//		//demand: Math.min(Q, V_0 * this.getRho())
//		// Q = (V_0 * RHO_M) / (V_0 / GAMMA + 1);
//		//   = (1.5 * 6.667) / (1.5 / 0.3 + 1);
//		//   = 1.66675
//		//	V_0 * this.getRho() = 1.5 * 1.2 = 1.8
//		// demand = 1.66675
//		CTCell nb = getCTLinkCell();
//		nb.setRho(2.5);
//		//supply: Math.min(Q, GAMMA * (RHO_M - this.getRho())
//		// Q = 1.66675
//		// GAMMA* (RHO_M - this.getRho) = 0.3 * (6.667 - 2.5) = 1.2501
//		// supply = 1.2501
//		CTPed ped = new CTPed(c,null);
//		double j = c.getJ(nb);
//		//j = 1.5*width * Math.min(demand, supply);
//		//  = 1.5 * min(1.66675,1.2501)
//		//	= 1.87515
//
//		assertEquals("correct flow", 1.87515, j);
//	}


}
