/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.controler;

import jakarta.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.utils.misc.ClassUtils;

import javax.swing.event.EventListenerList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Class encapsulating all behavior concerning the ControllerEvents/Listeners
 *
 * @author dgrether
 */
public final class ControllerListenerManagerImpl implements ControllerListenerManager {

	private final static Logger log = LogManager.getLogger(ControllerListenerManagerImpl.class);

	private MatsimServices controller = null;

	void setControler(MatsimServices controller) {
        this.controller = controller;
    }

	/** The swing event listener list to manage ControllerListeners efficiently. First list manages core listeners
	 * which are called first when a ControllerEvent is thrown. I.e. this list contains the listeners that are
	 * always running in a predefined order to ensure correctness.
	 * The second list manages the other listeners, which can be added by calling addControllerListener(...).
	 * A normal ControllerListener is not allowed to depend on the execution of other ControllerListeners.
	 */
	private final EventListenerList coreListenerList = new EventListenerList();
	private final EventListenerList listenerList = new EventListenerList();


	/**
	 * Add a core ControllerListener to the Controller instance
	 */
	@SuppressWarnings("unchecked")
	protected void addCoreControllerListener(final ControllerListener l) {
		for (Class type : ClassUtils.getAllTypes(l.getClass())) {
			if (type.isInterface() && ControllerListener.class.isAssignableFrom(type)) {
				this.coreListenerList.add(type, l);
			}
		}
	}

	/**
	 * Add a ControllerListener to the Controller instance
	 *
	 */
	@SuppressWarnings("unchecked")
	public void addControllerListener(final ControllerListener l) {
		for (Class type : ClassUtils.getAllTypes(l.getClass())) {
			if (ControllerListener.class.isAssignableFrom(type)) {
				this.listenerList.add(type, l);
			}
		}
	}

	/**
	 * Removes a ControllerListener from the Controller instance
	 *
	 */
	@SuppressWarnings("unchecked")
	public void removeControllerListener(final ControllerListener l) {
		Class[] interfaces = l.getClass().getInterfaces();
        for (Class anInterface : interfaces) {
            if (ControllerListener.class.isAssignableFrom(anInterface)) {
                this.listenerList.remove(anInterface, l);
            }
        }
	}

	@Deprecated(since="2025-07-19")
	public void removeControlerListener(final ControllerListener l) {
		this.removeControllerListener(l);
	}

	@Deprecated(since="2025-07-19")
	public void fireControlerStartupEvent() {
		this.fireControllerStartupEvent();
	}

	/**
	 * Notifies all ControllerListeners
	 */
	public void fireControllerStartupEvent() {
		StartupEvent event = new StartupEvent(this.controller);
		StartupListener[] listener = this.coreListenerList.getListeners(StartupListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControllerListener::priority).reversed());
        for (StartupListener aListener : listener) {
            log.info("calling notifyStartup on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyStartup(event);
        }
		listener = this.listenerList.getListeners(StartupListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControllerListener::priority).reversed());
        for (StartupListener aListener : listener) {
            log.info("calling notifyStartup on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyStartup(event);
        }
		log.info("all ControllerStartupListeners called." );
	}

	@Deprecated(since="2025-07-19")
	public void fireControlerShutdownEvent(final boolean unexpected, int iteration) {
		this.fireControllerShutdownEvent(unexpected, iteration);
	}

	/**
	 * Notifies all ControllerListeners
	 * @param unexpected Whether the shutdown is unexpected or not.
	 */
	public void fireControllerShutdownEvent(final boolean unexpected, int iteration) {
		fireControllerShutdownEvent(unexpected, iteration, null);
	}

	@Deprecated(since="2025-07-19")
	public void fireControlerShutdownEvent(final boolean unexpected, int iteration, @Nullable Throwable exception) {
		this.fireControllerShutdownEvent(unexpected, iteration, exception);
	}

	/**
	 * Notifies all ControllerListeners
	 * @param unexpected Whether the shutdown is unexpected or not.
	 */
	public void fireControllerShutdownEvent(final boolean unexpected, int iteration, @Nullable Throwable exception) {
		ShutdownEvent event = new ShutdownEvent(this.controller, unexpected, iteration, exception);
        ShutdownListener[] listener = this.coreListenerList.getListeners(ShutdownListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControllerListener::priority).reversed());

        for (ShutdownListener aListener : listener) {
            log.info("calling notifyShutdown on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyShutdown(event);
        }
        listener = this.listenerList.getListeners(ShutdownListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControllerListener::priority).reversed());

        for (ShutdownListener aListener : listener) {
            log.info("calling notifyShutdown on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyShutdown(event);
        }
        log.info("all ControlerShutdownListeners called.");
	}

	@Deprecated(since="2025-07-19")
	public void fireControlerIterationStartsEvent(final int iteration, boolean isLastIteration) {
		this.fireControllerIterationStartsEvent(iteration, isLastIteration);
	}

	/**
	 * Notifies all ControllerSetupIterationStartsListeners
     *
	 */
	public void fireControllerIterationStartsEvent(final int iteration, boolean isLastIteration) {
		IterationStartsEvent event = new IterationStartsEvent(this.controller, iteration, isLastIteration);
		IterationStartsListener[] listener = this.coreListenerList.getListeners(IterationStartsListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControllerListener::priority).reversed());
        for (IterationStartsListener aListener : listener) {
            log.info("calling notifyIterationStarts on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyIterationStarts(event);
        }
		listener = this.listenerList.getListeners(IterationStartsListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControllerListener::priority).reversed());
        for (IterationStartsListener aListener : listener) {
            log.info("calling notifyIterationStarts on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyIterationStarts(event);
        }
		log.info("[it." + iteration + "] all ControllerIterationStartsListeners called.");
	}

	@Deprecated(since="2025-07-19")
	public void fireControlerIterationEndsEvent(final int iteration, boolean isLastIteration) {
		this.fireControllerIterationEndsEvent(iteration, isLastIteration);
	}

	/**
	 * Notifies all ControllerIterationEndsListeners
	 *
	 */
	public void fireControllerIterationEndsEvent(final int iteration, boolean isLastIteration) {
		IterationEndsEvent event = new IterationEndsEvent(this.controller, iteration, isLastIteration);
		{
			IterationEndsListener[] listener = this.coreListenerList.getListeners(IterationEndsListener.class);
			Arrays.sort(listener, Comparator.comparingDouble(ControllerListener::priority).reversed());
			for (IterationEndsListener aListener : listener) {
                log.info("calling notifyIterationEnds on " + aListener.getClass().getName() + " with priority " + aListener.priority());
                aListener.notifyIterationEnds(event);
            }
		}
		{
			IterationEndsListener[] listener = this.listenerList.getListeners(IterationEndsListener.class);
			Arrays.sort(listener, Comparator.comparingDouble(ControllerListener::priority).reversed());
            for (IterationEndsListener aListener : listener) {
                log.info("calling notifyIterationEnds on " + aListener.getClass().getName() + " with priority " + aListener.priority());
                aListener.notifyIterationEnds(event);
            }
		}
		log.info("[it." + iteration + "] all ControllerIterationEndsListeners called.");
	}

	@Deprecated(since="2025-07-19")
	public void fireControlerScoringEvent(final int iteration, boolean isLastIteration) {
		this.fireControllerScoringEvent(iteration, isLastIteration);
	}

	/**
	 * Notifies all ControllerScoringListeners
	 */
	public void fireControllerScoringEvent(final int iteration, boolean isLastIteration) {
		ScoringEvent event = new ScoringEvent(this.controller, iteration, isLastIteration);
		{
			ScoringListener[] listener = this.coreListenerList.getListeners(ScoringListener.class);
			Arrays.sort(listener, Comparator.comparingDouble(ControllerListener::priority).reversed());
            for (ScoringListener aListener : listener) {
                log.info("calling notifyScoring on " + aListener.getClass().getName() + " with priority " + aListener.priority());
                aListener.notifyScoring(event);
            }
		}
		{
			ScoringListener[] listener = this.listenerList.getListeners(ScoringListener.class);
			Arrays.sort(listener, Comparator.comparingDouble(ControllerListener::priority).reversed());
            for (ScoringListener aListener : listener) {
                log.info("calling notifyScoring on " + aListener.getClass().getName() + " with priority " + aListener.priority());
                aListener.notifyScoring(event);
            }
		}
		log.info("[it." + iteration + "] all ControllerScoringListeners called.");
	}

	@Deprecated(since="2025-07-19")
	public void fireControlerReplanningEvent(final int iteration, boolean isLastIteration) {
		this.fireControllerReplanningEvent(iteration, isLastIteration);
	}

	/**
	 * Notifies all ControlerReplanningListeners
	 *
	 */
	public void fireControllerReplanningEvent(final int iteration, boolean isLastIteration) {
		ReplanningEvent event = new ReplanningEvent(this.controller, iteration, isLastIteration);
		ReplanningListener[] listener = this.coreListenerList.getListeners(ReplanningListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControllerListener::priority).reversed());
        for (ReplanningListener aListener : listener) {
            log.info("calling notifyReplanning on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyReplanning(event);
        }
		listener = this.listenerList.getListeners(ReplanningListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControllerListener::priority).reversed());
        for (ReplanningListener aListener : listener) {
            log.info("calling notifyReplanning on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyReplanning(event);
        }
		log.info("[it." + iteration + "] all ControllerReplanningListeners called.");
	}

	@Deprecated(since="2025-07-19")
	public void fireControlerBeforeMobsimEvent(final int iteration, boolean isLastIteration) {
		this.fireControllerBeforeMobsimEvent(iteration, isLastIteration);
	}

	/**
	 * Notifies all ControllerBeforeMobsimListeners
	 *
	 */
	public void fireControllerBeforeMobsimEvent(final int iteration, boolean isLastIteration) {
		BeforeMobsimEvent event = new BeforeMobsimEvent(this.controller, iteration, isLastIteration);
		BeforeMobsimListener[] listener = this.coreListenerList.getListeners(BeforeMobsimListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControllerListener::priority).reversed());
        for (BeforeMobsimListener aListener : listener) {
            log.info("calling notifyBeforeMobsim on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyBeforeMobsim(event);
        }
		listener = this.listenerList.getListeners(BeforeMobsimListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControllerListener::priority).reversed());
        for (BeforeMobsimListener aListener : listener) {
            log.info("calling notifyBeforeMobsim on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyBeforeMobsim(event);
        }
		log.info("[it." + iteration + "] all ControlerBeforeMobsimListeners called.");
	}

	@Deprecated(since="2025-07-19")
	public void fireControlerAfterMobsimEvent(final int iteration, boolean isLastIteration) {
		this.fireControllerAfterMobsimEvent(iteration, isLastIteration);
	}

	/**
	 * Notifies all ControllerAfterMobsimListeners
	 */
	public void fireControllerAfterMobsimEvent(final int iteration, boolean isLastIteration) {
		AfterMobsimEvent event = new AfterMobsimEvent(this.controller, iteration, isLastIteration);
		AfterMobsimListener[] listener = this.coreListenerList.getListeners(AfterMobsimListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControllerListener::priority).reversed());
        for (AfterMobsimListener aListener : listener) {
            log.info("calling notifyAfterMobsim on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyAfterMobsim(event);
        }
		listener = this.listenerList.getListeners(AfterMobsimListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControllerListener::priority).reversed());
        for (AfterMobsimListener aListener : listener) {
            log.info("calling notifyAfterMobsim on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyAfterMobsim(event);
        }
		log.info("[it." + iteration + "] all ControllerAfterMobsimListeners called.");
	}

}
