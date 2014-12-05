/* *********************************************************************** *
 * project: org.matsim.*
 * FrameSaver.java
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

package matsimConnector.visualizer.debugger.eventsbaseddebugger;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import processing.core.PApplet;

public class FrameSaver {
	
	private final CyclicBarrier barrier = new CyclicBarrier(2);
	private final String path;
	private final String extension;
	private int frameSkip;
	private int skiped;
	private int frameNumber;
	private int nZeros;

	public FrameSaver(String path, String extension, int frameSkip) {
		this.path = path;
		this.extension = extension;
		this.frameSkip = frameSkip;
		this.skiped = frameSkip;
		frameNumber=0;
		nZeros = 5;
	}
	
//	public boolean wouldskipNext() {
//		if (this.skiped == this.frameSkip) {
//			return true;
//		}
//		this.skiped++;
//		return false;
//	}
	public void saveFrame(PApplet p) {
		String identifier = "img";
		for(int i=nZeros-1; i>0 && Math.pow(10, i) > frameNumber; i--)
			identifier += 0;
		identifier += frameNumber;
		saveFrame(p,identifier);
		frameNumber++;
	}
	
	
	public void saveFrame(PApplet p, String identifier) {
		if (this.skiped < this.frameSkip) {
			return;
		}
//		this.await();
		this.skiped = 0;
		StringBuffer bf = new StringBuffer();
		bf.append(this.path);
		bf.append("/");
		bf.append(identifier);
		bf.append(".");
		bf.append(this.extension);
		p.saveFrame(bf.toString());
		this.await();
	}
	
	public boolean incrSkipped() {
		this.skiped++;
		return this.skiped >= this.frameSkip;
	}

	
	
	public void await() {
		try {
			this.barrier.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
	}
	
	public void setSkip(int round) {
		this.frameSkip = round;
		
	}
	

}
