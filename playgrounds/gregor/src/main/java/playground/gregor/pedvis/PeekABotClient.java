/* *********************************************************************** *
 * project: org.matsim.*
 * PeekABotClient.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import org.matsim.core.gbl.MatsimRandom;

public class PeekABotClient {


	public static final int CAR = 0;

	public static final int WALK_2_D = 1;
	// public native void addBot(int id, float x, float y, float z);

	// public native void setBotPosition(int id, float x, float y, float z,
	// float azimuth);

	// public native void setBotColor(int id, float r, float g, float b);

	// public native void removeBot(int id);

	// public native void initPolygon(int id, int numCoords, float r, float g,
	// float b, float height);
	//
	// public native void addPolygonCoord(int id, float x, float y, float z);
	//
	// public native void init();

	// public native void restAgents();

	// public native void addArrow(int arrowId, int agentId, float r, float g,
	// float b, float fromX, float fromY, float fromZ, float toX, float toY,
	// float toZ);
	//
	// public native void removeArrow(int arrowId, int agentId);

	public native void initII();

	public native void addBotII(int id, float x, float y, float z, float scale);

	public native void setBotPositionII(int id, float x, float y, float z, float az, float scale);

	public native void removeBotII(int id);

	public native void removeAllBotsII();

	public native void setBotColorII(int id, float r, float g, float b);

	public native void initPolygonII(int id, int numCoords, float r, float g, float b, float height);

	public native void addPolygonCoordII(int id, float x, float y, float z);

	public native void drawLink(int id, int fromId, int toId, float fromX, float fromY, float toX, float toY);

	//	public native void setBotShapeII(int id, int shape);

	/**
	 * @param arrowId
	 * @param agentId
	 * @param r
	 * @param g
	 * @param b
	 * @param fromX
	 * @param fromY
	 * @param fromZ
	 * @param toX
	 * @param toY
	 * @param toZ
	 */
	public native void drawArrowII(int arrowId, int agentId, float r, float g, float b, float fromX, float fromY, float fromZ, float toX, float toY, float toZ);

	static {
		System.loadLibrary("peekabotclient");
	}

	public static void main(String args[]) {
		PeekABotClient pc = new PeekABotClient();
		pc.initII();

		for (int i = 0; i < 2500; i++) {
			float x = MatsimRandom.getRandom().nextFloat() * 50;
			float y = MatsimRandom.getRandom().nextFloat() * 50;
			pc.addBotII(i, x, y, 0,1);
		}

		for (int i = 0; i < 50; i++) {

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			float x = MatsimRandom.getRandom().nextFloat();
			float y = MatsimRandom.getRandom().nextFloat();
			for (int j = 0; j < 2500; j++) {
				pc.setBotPositionII(j, x, y, 0.4f, 1,1);
				x += 5 * (MatsimRandom.getRandom().nextFloat() - .5f);
				y += 5 * (MatsimRandom.getRandom().nextFloat() - .5f);

				float r = MatsimRandom.getRandom().nextFloat();
				float g = MatsimRandom.getRandom().nextFloat();
				float b = MatsimRandom.getRandom().nextFloat();
				pc.setBotColorII(j, r, g, b);
			}

		}
		pc.removeAllBotsII();

	}

}
