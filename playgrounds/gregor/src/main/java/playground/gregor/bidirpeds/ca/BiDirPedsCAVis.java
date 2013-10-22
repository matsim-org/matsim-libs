/* *********************************************************************** *
 * project: org.matsim.*
 * BiDirPedsCAVis.java
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

package playground.gregor.bidirpeds.ca;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;

import org.matsim.core.utils.misc.Time;

import playground.gregor.bidirpeds.ca.BiDirPedsCA.Cell;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.FrameSaver;
import processing.core.PApplet;

public class BiDirPedsCAVis extends PApplet{

	private final JFrame fr;
	private final int nrCells;

	private final Cell [] cells;
	private float cy;
	private float incrX;
	private float r;
	private double time;
	private final double sleep = 0;
	private double oldTime;
	private long last;

	
	private final FrameSaver fs = new FrameSaver("/Users/laemmel/tmp/ca/mvi", "png", 0);
	public BiDirPedsCAVis(int cells) {
		this.fr = new JFrame();
		this.fr.setSize(1024, 54);
		JPanel compositePanel = new JPanel();
		compositePanel.setLayout(new OverlayLayout(compositePanel));

		this.fr.add(compositePanel,BorderLayout.CENTER);

		compositePanel.add(this);
		compositePanel.setEnabled(true);
		compositePanel.setVisible(true);

		//		size(1024, 768);
		this.fr.setVisible(true);
		this.nrCells = cells;
		this.cells =  new Cell[cells];
		for (int i = 0; i < cells; i++) {
			this.cells[i] = new Cell();
		}
		this.init();
		frameRate(60);
	}

	@Override
	public void setup() {
		//				size(1024,768);
		size(1024,34);
		this.cy = 34/2;
		this.incrX = 1024/this.nrCells;
		this.r = this.incrX/2.f;
		background(0);
	}

	@Override
	public void draw() {
		
		background(0);
		float x =0;
		rectMode(CORNER);
		strokeWeight(1.5f);
		stroke(128);
		for (int i = 0; i <= this.nrCells; i++) {
//			if (i%2 == 0) {
//				fill(192,255);
//			} else {
//				fill(224,255);
//			}
			fill(255,255);
			rect(x,0,this.incrX,64);
			x += this.incrX;
		}
		
		synchronized(this.cells) {
			x = this.incrX/2;
			ellipseMode(RADIUS);
			fill(255);
			for (int i = 0; i < this.nrCells; i++) {
				Cell cell = this.cells[i];
				if (cell.ped != null) {
					int pr = cell.ped.hashCode()%128;
					if (cell.ped.dir == 1) {
//						fill(0,255-pr,pr,192);
						stroke(255,255);
						fill(0,0,0,255);
					} else {
						stroke(0,255);
						fill(255,255,255,255);
//						fill(pr,0,255-pr,192);
					}
					ellipse(x, this.cy, this.r, this.r);
				}
				x += this.incrX;
			}
		}
		if (this.fs != null) {
			this.fs.saveFrame(this, Time.writeTime(this.time*100, Time.TIMEFORMAT_HHMMSSDOTSS));
		}
	}

	public void setCells(Cell [] cells, double time) {
		if (this.fs != null) {
			this.fs.await();
		}
		synchronized (this.cells) {
			this.time = time;
			for (int i = 1; i <= this.nrCells; i++) {
				this.cells[i-1] = cells[i];
			}

		}
		long current = System.currentTimeMillis();
		double diff = (time-this.oldTime)*1000;
		double rTDiff = current-this.last;
		if (rTDiff < diff) {
			double sleep = diff-rTDiff;
			try {
				Thread.sleep((long) sleep);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.oldTime = time;
		this.last = System.currentTimeMillis();
	}

}
