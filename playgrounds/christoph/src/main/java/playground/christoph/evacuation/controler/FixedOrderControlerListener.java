/* *********************************************************************** *
 * project: org.matsim.*
 * FixedOrderControlerListener.java
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

package playground.christoph.evacuation.controler;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

/**
 * The services calls registered services listeners in reverse order,
 * which is confusing at some points. Therefore, this class can be used
 * to call listeners in a pre-defined order. 
 */
public class FixedOrderControlerListener implements StartupListener, IterationStartsListener, BeforeMobsimListener, 
		AfterMobsimListener, ScoringListener, ReplanningListener, IterationEndsListener, ShutdownListener {

	private final static Logger log = Logger.getLogger(FixedOrderControlerListener.class);
	
	private List<StartupListener> startUpListeners;
	private List<IterationStartsListener> iterationStartsListeners;
	private List<BeforeMobsimListener> beforeMobsimListeners;
	private List<AfterMobsimListener> afterMobsimListeners;
	private List<ScoringListener> scoringListeners;
	private List<ReplanningListener> replanningListeners;
	private List<IterationEndsListener> iterationEndsListeners;
	private List<ShutdownListener> shutDownListeners;

	public FixedOrderControlerListener() {
		this.startUpListeners = new ArrayList<StartupListener>();
		this.iterationStartsListeners = new ArrayList<IterationStartsListener>();
		this.beforeMobsimListeners = new ArrayList<BeforeMobsimListener>();
		this.afterMobsimListeners = new ArrayList<AfterMobsimListener>();
		this.scoringListeners = new ArrayList<ScoringListener>();
		this.replanningListeners = new ArrayList<ReplanningListener>();
		this.iterationEndsListeners = new ArrayList<IterationEndsListener>();
		this.shutDownListeners = new ArrayList<ShutdownListener>();
	}
	
	public void addControlerListener(ControlerListener listener) {
		if (listener instanceof StartupListener) {
			this.startUpListeners.add((StartupListener) listener);
		}
		if (listener instanceof IterationStartsListener) {
			this.iterationStartsListeners.add((IterationStartsListener) listener);
		}
		if (listener instanceof BeforeMobsimListener) {
			this.beforeMobsimListeners.add((BeforeMobsimListener) listener);
		}
		if (listener instanceof AfterMobsimListener) {
			this.afterMobsimListeners.add((AfterMobsimListener) listener);
		}
		if (listener instanceof ScoringListener) {
			this.scoringListeners.add((ScoringListener) listener);
		}
		if (listener instanceof ReplanningListener) {
			this.replanningListeners.add((ReplanningListener) listener);
		}
		if (listener instanceof IterationEndsListener) {
			this.iterationEndsListeners.add((IterationEndsListener) listener);
		}
		if (listener instanceof ShutdownListener) {
			this.shutDownListeners.add((ShutdownListener) listener);
		}
	}
	
	public void removeControlerListener(ControlerListener listener) {
		if (listener instanceof StartupListener) {
			this.startUpListeners.remove((StartupListener) listener);
		}
		if (listener instanceof IterationStartsListener) {
			this.iterationStartsListeners.remove((IterationStartsListener) listener);
		}
		if (listener instanceof BeforeMobsimListener) {
			this.beforeMobsimListeners.remove((BeforeMobsimListener) listener);
		}
		if (listener instanceof AfterMobsimListener) {
			this.afterMobsimListeners.remove((AfterMobsimListener) listener);
		}
		if (listener instanceof ScoringListener) {
			this.scoringListeners.remove((ScoringListener) listener);
		}
		if (listener instanceof ReplanningListener) {
			this.replanningListeners.remove((ReplanningListener) listener);
		}
		if (listener instanceof IterationEndsListener) {
			this.iterationEndsListeners.remove((IterationEndsListener) listener);
		}
		if (listener instanceof ShutdownListener) {
			this.shutDownListeners.remove((ShutdownListener) listener);
		}
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		for (StartupListener listener : this.startUpListeners) {
			log.info("\t" + "calling notifyStartup on " + listener.getClass().getCanonicalName());
			listener.notifyStartup(event);
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		for (IterationStartsListener listener : this.iterationStartsListeners) {
			log.info("\t" + "calling notifyIterationStarts on " + listener.getClass().getCanonicalName());
			listener.notifyIterationStarts(event);
		}
	}
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		for (BeforeMobsimListener listener : this.beforeMobsimListeners) {
			log.info("\t" + "calling notifyBeforeMobsim on " + listener.getClass().getCanonicalName());
			listener.notifyBeforeMobsim(event);
		}
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		for (AfterMobsimListener listener : this.afterMobsimListeners) {
			log.info("\t" + "calling notifyAfterMobsim on " + listener.getClass().getCanonicalName());
			listener.notifyAfterMobsim(event);
		}
	}

	@Override
	public void notifyScoring(ScoringEvent event) {
		for (ScoringListener listener : this.scoringListeners) {
			log.info("\t" + "calling notifyScoring on " + listener.getClass().getCanonicalName());
			listener.notifyScoring(event);
		}
	}

	@Override
	public void notifyReplanning(ReplanningEvent event) {
		for (ReplanningListener listener : this.replanningListeners) {
			log.info("\t" + "calling notifyReplanning on " + listener.getClass().getCanonicalName());
			listener.notifyReplanning(event);
		}
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		for (IterationEndsListener listener : this.iterationEndsListeners) {
			log.info("\t" + "calling notifyIterationEnds on " + listener.getClass().getCanonicalName());
			listener.notifyIterationEnds(event);
		}
	}
	
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		for (ShutdownListener listener : this.shutDownListeners) {
			log.info("\t" + "calling notifyShutdown on " + listener.getClass().getCanonicalName());
			listener.notifyShutdown(event);
		}
	}

}