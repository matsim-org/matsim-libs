/* *********************************************************************** *
 * project: org.matsim.*
 * QSimEngineRunner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.agrona.ErrorHandler;
import org.agrona.concurrent.Agent;
import org.matsim.core.gbl.Gbl;

/**
 * Split up the old {@code QNetsimEngineRunner} which was implementing
 * 2 different approaches.
 * 
 * @author droeder @ Senozon Deutschland GmbH
 */
final class QNetsimEngineRunnerForThreadpool extends AbstractQNetsimEngineRunner implements Agent, ErrorHandler {
// Note that this agent interface has nothing to do with MATSim

	private volatile State state;
	private volatile Throwable t;

	QNetsimEngineRunnerForThreadpool() {
	}

	@Override
	public int doWork() throws Exception {

		if (state == State.FINISHED || state == State.IDLE || state == State.ERROR) {
			return 0;
		}

		else if (state == State.MOVE_NODES) {
			moveNodes();
		} else if (state == State.MOVE_LINKS){
			moveLinks();
		}

		state = State.IDLE;

		return 1;
	}

	@Override
	public String roleName() {
		return "QNetsimEngineRunner";
	}

	public final void afterSim() {
		this.state  = State.FINISHED;
	}

	public final void setState(State state) {
		this.state = state;
	}

	public State getState() {
		return state;
	}

	@Override
	public void onError(Throwable throwable) {
		this.state = State.ERROR;
		this.t = throwable;
	}

	public Throwable getError() {
		return t;
	}

	public static enum State {

		MOVE_NODES,
		MOVE_LINKS,
		IDLE,
		FINISHED,

		ERROR,

	}
}