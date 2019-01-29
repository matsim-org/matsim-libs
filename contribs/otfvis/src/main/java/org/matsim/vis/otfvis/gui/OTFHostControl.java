/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.vis.otfvis.gui;

import java.awt.*;
import java.util.Collection;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.util.Animator;
import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.interfaces.OTFLiveServer;
import org.matsim.vis.otfvis.interfaces.OTFServer;

public class OTFHostControl implements GLEventListener {

	private static Logger log = Logger.getLogger(OTFHostControl.class);
	private final Component canvas;

	private final BoundedRangeModel simTime;

	private volatile int loopStart = 0;

	private volatile int loopEnd = Integer.MAX_VALUE;

	private final Animator animator;

	private final OTFServer server;
	private volatile boolean playing = false;
	private volatile boolean syncronizedPlay = true;

	public OTFHostControl(OTFServer aServer, Component aCanvas) {
		server = aServer;
		canvas = aCanvas;
		Collection<Double> steps = getTimeStepsdrawer();
		if (steps != null) {
			// Movie mode with timesteps
			Double[] dsteps = steps.toArray(new Double[steps.size()]);
			int min = dsteps[0].intValue();
			int max = dsteps[dsteps.length-1].intValue();
			simTime = new DefaultBoundedRangeModel(min, 0 /* extent */, min, max);
		} else {
			// Live mode without timesteps
			simTime = new DefaultBoundedRangeModel(0 /* value */, 0 /* extent */, 0 /* value */, Integer.MAX_VALUE /* max */);
		}
		simTime.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				canvas.repaint();
			}
		});
		animator = new Animator();
		animator.add(((GLAutoDrawable) canvas));
		((GLAutoDrawable) canvas).addGLEventListener(this);
		simTime.setValue(server.getLocalTime());
	}

	public void toStart() {
		requestTimeStep(loopStart);
		log.debug("To start...");
	}


	void requestTimeStep(int newTime) {
		server.requestNewTime(newTime);
		simTime.setValue(server.getLocalTime());
	}

	public boolean isLive() {
		return server.isLive();
	}

	public int getSimTime() {
		return simTime.getValue();
	}

	public BoundedRangeModel getSimTimeModel() {
		return simTime;
	}

	public void play(boolean synchronizedPlay) {
		log.debug("Pressed PLAY, creating animator.");
		playing = true;
		syncronizedPlay = synchronizedPlay;
		new Thread(new Runnable() {
			@Override
			public void run() {
				animator.start();
			}
		}).start();
		if (!synchronizedPlay) {
			((OTFLiveServer) server).play();
		}
	}

	public void pause() {
		if (server.isLive()) {
			((OTFLiveServer) server).pause();
		}
		animator.stop();
		playing = false;
	}


	public Collection<Double> getTimeStepsdrawer() {
		return server.getTimeSteps();
	}


	/**
	 *  sets the loop that the movieplayer should loop
	 * @param min either sec for startloop or -1 for leave unchanged default =0
	 * @param max either sec for endloop or -1 for leave unchanged default = Integer.MAX_VALUE
	 */
	public void setLoopBounds(int min, int max) {
		if (min != -1) {
			loopStart = min;
		}
		if (max != -1) {
			loopEnd = max;
		}
	}

	@Override
	public void init(GLAutoDrawable glAutoDrawable) {

	}

	@Override
	public void dispose(GLAutoDrawable glAutoDrawable) {

	}

	@Override
	public void display(GLAutoDrawable glAutoDrawable) {
		int delay = OTFClientControl.getInstance().getOTFVisConfig().getDelay_ms();
		if (playing) {
			if (syncronizedPlay) {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				server.requestNewTime(getSimTime() + 1);
				if (simTime.getValue() >= loopEnd) {
					server.requestNewTime(loopStart);
					simTime.setValue(server.getLocalTime());
				}
			}
			simTime.setValue(server.getLocalTime());
			if (server.isFinished()) {
				animator.stop();
			}
		}
	}

	@Override
	public void reshape(GLAutoDrawable glAutoDrawable, int i, int i1, int i2, int i3) {

	}

}
