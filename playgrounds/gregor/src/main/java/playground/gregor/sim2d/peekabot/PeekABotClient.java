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
package playground.gregor.sim2d.peekabot;

import org.matsim.core.gbl.MatsimRandom;

public class PeekABotClient {
	
	public native void addBot(int id, float  x, float  y, float  z);
	
	public native void setBotPosition(int id, float  x, float  y, float  z, float azimuth);
	
	public native void setBotColor(int id, float r , float g, float b);
	
	public native void removeBot(int id);
	
	public native void initPolygon(int id, int numCoords, float r, float g, float b, float height);
	public native void addPolygonCoord(int id,float x, float y, float z);
	
	public native void init();
	
	public native void restAgents();
	
    static {
        System.loadLibrary("peekabotclient");
    }



    public static void main(String args[]) {
    	PeekABotClient pc = new PeekABotClient();
    	pc.initPolygon(0, 4, .5f, 1, 0, 0);
    	pc.addPolygonCoord(0,10, 10, 0);
    	pc.addPolygonCoord(0,10, 0, 0);
    	pc.addPolygonCoord(0,0, 0, 0);
    	pc.addPolygonCoord(0,0, 15, 0);
    	
    	
    	pc.initPolygon(1, 4, 1, .5f, 0, 0);
    	pc.addPolygonCoord(1,10, 10, 3);
    	pc.addPolygonCoord(1,10, 0, 3);
    	pc.addPolygonCoord(1,0, 0, 3);
    	pc.addPolygonCoord(1,0, 15, 3);
    	pc.init();
    	for (int i = 0; i < 10; i++) {
    		float x = MatsimRandom.getRandom().nextFloat() * 10;
    		float y = MatsimRandom.getRandom().nextFloat() * 10;
    		pc.addBot(i, x,y ,0.f );
    	}
    	int count = 0;
    	while (true) {
        	try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        	for (int i = 0; i < 10; i++) {
        		float x = MatsimRandom.getRandom().nextFloat() * 10;
        		float y = MatsimRandom.getRandom().nextFloat() * 10;
        		float vx;
        		float vy;
        		double rnd = MatsimRandom.getRandom().nextDouble();
        		double alpha = (Math.PI * 2) * rnd;
        		
        		vx = (float) Math.cos(alpha);
        		vy = (float) Math.sin(alpha);
        		pc.setBotPosition(i, x,y ,0.f,(float) alpha );
        		if (count > 165 && i >= 86) {
//        			System.out.println(i + " vx:" + vx + " vy:" + vy + "  " + (vx*vx + vy*vy));
        		}
        	}
        	System.out.println(count++);
        	
    	}
    	
//    	try {
//			Thread.sleep(10000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//    	pc.addBot(100, 0., 0.);
    }



}
