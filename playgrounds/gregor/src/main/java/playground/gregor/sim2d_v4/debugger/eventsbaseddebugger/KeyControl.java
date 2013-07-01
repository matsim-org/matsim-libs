/* *********************************************************************** *
 * project: org.matsim.*
 * KeyControl.java
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

package playground.gregor.sim2d_v4.debugger.eventsbaseddebugger;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;
import org.gicentre.utils.move.ZoomPan;

import processing.core.PVector;

public class KeyControl implements KeyListener {
	
	private static final Logger log = Logger.getLogger(KeyControl.class);
	private double speedup = 1;
	private boolean pause = false;
	private final CyclicBarrier pauseBarrier = new CyclicBarrier(2);
	private final CyclicBarrier screenshotBarrier = new CyclicBarrier(2);
	private final ZoomPan zoomer;
	
	private final Map<Integer,Zoom> zooms = new HashMap<Integer,Zoom>();
	private boolean makeScreenshot = false;
	
	//static zoom
	//32505.22:15830.94x76.88606195154944
	//70185.26:29303.623x80.73036504912677 
	//108827.0:45366.152x125.2392931869562 -- sections
	//169010.61:70300.016x194.28724927013116 -- agents
	private static final Zoom z = new Zoom();
	static {
		z.z = 194.28724927013116;
		z.o = new PVector(169010.61f,70300.016f);
	}
	
	public KeyControl(ZoomPan zoomer) {
		this.zoomer = zoomer;
		int c = '0';
		this.zooms.put(c, z);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyChar() == '+') {
			if (this.speedup >= 512) {
				log.info("acceleration already at maximum (512).");
				return;
			}
			this.speedup *= 2;
//			System.out.println("speedup:" + this.speedup);
			log.info("setting accelaration to:" + this.speedup);
		} else if (e.getKeyChar() == '-') {
			if (this.speedup <= 0.125) {
				log.info("acceleration already at minimum (0.125).");
				return;
			}
			this.speedup  /= 2;
			log.info("setting accelaration to:" + this.speedup);
		} else if (e.getKeyChar() == 'p') {
			log.info("toggle pause");
			if (this.pause) {
				awaitPause();
			}
			this.pause = !this.pause;
		} else if (e.getKeyChar() == 's'){
			log.info("screenshot requested");
			this.makeScreenshot  = true;
		}
		
		//this works with Mac
		if (e.getModifiers() == 4 &&  e.getKeyCode() >= 48 && e.getKeyCode() <= 57) {
			Zoom z = new Zoom();
			z.z = this.zoomer.getZoomScale();
			z.o = this.zoomer.getPanOffset();
			this.zooms.put(e.getKeyCode(), z);
			log.info("storing zoom:" + z.o.x + ":" + z.o.y + "x" + z.z + " at key "  + e.getKeyChar());
		} else if (e.getKeyCode() >= 48 && e.getKeyCode() <= 57){
			Zoom z = this.zooms.get(e.getKeyCode());
			if (z != null) {
				log.info("loading zoom:" + z.o.x + ":" + z.o.y + "x" + z.z);
				this.zoomer.setZoomScale(z.z);
				this.zoomer.setPanOffset(z.o.x, z.o.y);
//				this.zoomer.transform();
			}
		}

	}

	public void awaitPause() {
		if(!this.pause) {
			return;
		}
		try {
			this.pauseBarrier.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
		
	}
	
	
	public void awaitScreenshot() {
		if (!this.makeScreenshot) {
			return;
		}
		try {
			this.screenshotBarrier.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
	}

	public boolean isScreenshotRequested() {
		return this.makeScreenshot;
	}
	public void informScreenshotPerformed() {
		this.makeScreenshot = false;
	}
	
	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

	/*package*/ double getSpeedup() {
		return this.speedup;
	}
	
	private static final class Zoom {

		public PVector o;
		public double z;
		
	}

	public boolean isOneObjectWaitingAtScreenshotBarrier() {
		return this.screenshotBarrier.getNumberWaiting() == 1;
	}
}
