/* *********************************************************************** *
 * project: org.matsim.*
 * PedVis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.gregor.pedvis;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 * 
 */
public class PedVis {

	private static final double PI_HALF = Math.PI / 2;
	private static final double TWO_PI = 2 * Math.PI;

	public static void main(String[] args) {
		Importer imp = new Importer();
		try {
			imp.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Set<Id> init = new HashSet<Id>();

		List<Ped> peds = imp.getPeds();
		List<Double> timeSteps = imp.getTimeSteps();
		System.err.println(peds.size());

		PeekABotClient pc = new PeekABotClient();
		pc.initII();

		double step = 1000. / 25.;
		long oldTime = System.currentTimeMillis();

		while (true) {
			for (int i = 0; i < timeSteps.size(); i++) {
				double time = timeSteps.get(i);
				for (Ped ped : peds) {
					if (time >= ped.depart && time < ped.arrived) {

						Coordinate c = ped.coords.get(time);
						float a = 0;
						if (i > 0) {
							double preTime = timeSteps.get(i - 1);
							Coordinate preC = ped.coords.get(preTime);
							if (preC != null) {
								a = (float) getPhaseAngle(c.x - preC.x, c.y - preC.y);
							}

						}

						float x = (float) c.x;
						float y = (float) c.y;
						float z = (float) c.z;
						if (!init.contains(ped.id)) {
							init.add(ped.id);
							pc.addBotII(Integer.parseInt(ped.id.toString()), x, y, z);
							if (ped.color.equals("red")) {
								pc.setBotColorII(Integer.parseInt(ped.id.toString()), 1.f, 0, MatsimRandom.getRandom().nextFloat());
							} else {
								pc.setBotColorII(Integer.parseInt(ped.id.toString()), 0, 1.f, MatsimRandom.getRandom().nextFloat());
							}
						} else {
							pc.setBotPositionII(Integer.parseInt(ped.id.toString()), x, y, z, a);
						}
					}
					if (time >= ped.arrived) {
						pc.setBotPositionII(Integer.parseInt(ped.id.toString()), 20, 20, 0, 0.f);
					}
				}

				long currTime = System.currentTimeMillis();
				long diff = (long) (step - (currTime - oldTime));
				if (diff > 0) {

					try {
						Thread.sleep(diff);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				oldTime = System.currentTimeMillis();
			}

		}
	}

	private static double getPhaseAngle(double dX, double dY) {
		double alpha = 0.0;
		if (dX > 0) {
			alpha = Math.atan(dY / dX);
		} else if (dX < 0) {
			alpha = Math.PI + Math.atan(dY / dX);
		} else { // i.e. DX==0
			if (dY > 0) {
				alpha = PI_HALF;
			} else {
				alpha = -PI_HALF;
			}
		}
		if (alpha < 0.0)
			alpha += TWO_PI;
		return alpha;
	}

}
