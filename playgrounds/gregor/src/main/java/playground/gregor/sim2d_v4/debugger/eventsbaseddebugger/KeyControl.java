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

import org.gicentre.utils.move.ZoomPan;

import processing.core.PVector;

public class KeyControl implements KeyListener {
	private double speedup = 1;
	private boolean pause = false;
	private final CyclicBarrier pauseBarrier = new CyclicBarrier(2);
	private final ZoomPan zoomer;
	
	private final Map<Integer,Zoom> zooms = new HashMap<Integer,Zoom>();
	
	public KeyControl(ZoomPan zoomer) {
		this.zoomer = zoomer;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		System.out.println(e.getModifiers() + " " + e.getKeyCode());
		if (e.getKeyChar() == '+') {
			if (this.speedup >= 128) {
				return;
			}
			this.speedup *= 2;
			System.out.println("speedup:" + this.speedup);
		} else if (e.getKeyChar() == '-') {
			if (this.speedup <= 0.125) {
				return;
			}
			this.speedup  /= 2;
			
			System.out.println("speedup:" + this.speedup);
		} else if (e.getKeyChar() == 'p') {
			if (this.pause) {
				awaitPause();
			}
			this.pause = !this.pause;
		}
		
		//this works with Mac
		if (e.getModifiers() == 4 &&  e.getKeyCode() >= 48 && e.getKeyCode() <= 57) {
			Zoom z = new Zoom();
			z.z = this.zoomer.getZoomScale();
			z.o = this.zoomer.getPanOffset();
			this.zooms.put(e.getKeyCode(), z);
		} else if (e.getKeyCode() >= 48 && e.getKeyCode() <= 57){
			Zoom z = this.zooms.get(e.getKeyCode());
			if (z != null) {
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
}
