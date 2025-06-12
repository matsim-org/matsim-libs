/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerListenerManager
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
 * Class encapsulating all behavior concerning the ControlerEvents/Listeners
 *
 * @author dgrether
 */
public final class ControlerListenerManagerImpl implements ControlerListenerManager {

	private final static Logger log = LogManager.getLogger(ControlerListenerManagerImpl.class);

    private MatsimServices controler = null;

	void setControler(MatsimServices controler) {
        this.controler = controler;
    }

	/** The swing event listener list to manage ControlerListeners efficiently. First list manages core listeners
	 * which are called first when a ControlerEvent is thrown. I.e. this list contains the listeners that are
	 * always running in a predefined order to ensure correctness.
	 * The second list manages the other listeners, which can be added by calling addControlerListener(...).
	 * A normal ControlerListener is not allowed to depend on the execution of other ControlerListeners.
	 */
	private final EventListenerList coreListenerList = new EventListenerList();
	private final EventListenerList listenerList = new EventListenerList();


	/**
	 * Add a core ControlerListener to the Controler instance
	 *
	 */
	@SuppressWarnings("unchecked")
	protected void addCoreControlerListener(final ControlerListener l) {
		for (Class type : ClassUtils.getAllTypes(l.getClass())) {
			if (type.isInterface() && ControlerListener.class.isAssignableFrom(type)) {
				this.coreListenerList.add(type, l);
			}
		}
	}

	/**
	 * Add a ControlerListener to the Controler instance
	 *
	 */
	@SuppressWarnings("unchecked")
	public void addControlerListener(final ControlerListener l) {
		for (Class type : ClassUtils.getAllTypes(l.getClass())) {
			if (ControlerListener.class.isAssignableFrom(type)) {
				this.listenerList.add(type, l);
			}
		}
	}

	/**
	 * Removes a ControlerListener from the Controler instance
	 *
	 */
	@SuppressWarnings("unchecked")
	public void removeControlerListener(final ControlerListener l) {
		Class[] interfaces = l.getClass().getInterfaces();
        for (Class anInterface : interfaces) {
            if (ControlerListener.class.isAssignableFrom(anInterface)) {
                this.listenerList.remove(anInterface, l);
            }
        }
	}

	/**
	 * Notifies all ControlerListeners
	 */
	public void fireControlerStartupEvent() {
		StartupEvent event = new StartupEvent(this.controler);
		StartupListener[] listener = this.coreListenerList.getListeners(StartupListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControlerListener::priority).reversed());
        for (StartupListener aListener : listener) {
            log.info("calling notifyStartup on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyStartup(event);
        }
		listener = this.listenerList.getListeners(StartupListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControlerListener::priority).reversed());
        for (StartupListener aListener : listener) {
            log.info("calling notifyStartup on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyStartup(event);
        }
		log.info("all ControlerStartupListeners called." );
	}

	/**
	 * Notifies all ControlerListeners
	 * @param unexpected Whether the shutdown is unexpected or not.
	 */
	public void fireControlerShutdownEvent(final boolean unexpected, int iteration) {
		fireControlerShutdownEvent(unexpected, iteration, null);
	}

	/**
	 * Notifies all ControlerListeners
	 * @param unexpected Whether the shutdown is unexpected or not.
	 */
	public void fireControlerShutdownEvent(final boolean unexpected, int iteration, @Nullable Throwable exception) {
		ShutdownEvent event = new ShutdownEvent(this.controler, unexpected, iteration, exception);
        ShutdownListener[] listener = this.coreListenerList.getListeners(ShutdownListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControlerListener::priority).reversed());

        for (ShutdownListener aListener : listener) {
            log.info("calling notifyShutdown on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyShutdown(event);
        }
        listener = this.listenerList.getListeners(ShutdownListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControlerListener::priority).reversed());

        for (ShutdownListener aListener : listener) {
            log.info("calling notifyShutdown on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyShutdown(event);
        }
        log.info("all ControlerShutdownListeners called.");
	}

	/**
	 * Notifies all ControlerSetupIterationStartsListeners
     *
	 */
	public void fireControlerIterationStartsEvent(final int iteration, boolean isLastIteration) {
		IterationStartsEvent event = new IterationStartsEvent(this.controler, iteration, isLastIteration);
		IterationStartsListener[] listener = this.coreListenerList.getListeners(IterationStartsListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControlerListener::priority).reversed());
        for (IterationStartsListener aListener : listener) {
            log.info("calling notifyIterationStarts on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyIterationStarts(event);
        }
		listener = this.listenerList.getListeners(IterationStartsListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControlerListener::priority).reversed());
        for (IterationStartsListener aListener : listener) {
            log.info("calling notifyIterationStarts on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyIterationStarts(event);
        }
		log.info("[it." + iteration + "] all ControlerIterationStartsListeners called.");
	}

	/**
	 * Notifies all ControlerIterationEndsListeners
	 *
	 */
	public void fireControlerIterationEndsEvent(final int iteration, boolean isLastIteration) {
		IterationEndsEvent event = new IterationEndsEvent(this.controler, iteration, isLastIteration);
		{
			IterationEndsListener[] listener = this.coreListenerList.getListeners(IterationEndsListener.class);
			Arrays.sort(listener, Comparator.comparingDouble(ControlerListener::priority).reversed());
			for (IterationEndsListener aListener : listener) {
                log.info("calling notifyIterationEnds on " + aListener.getClass().getName() + " with priority " + aListener.priority());
                aListener.notifyIterationEnds(event);
            }
		}
		{
			IterationEndsListener[] listener = this.listenerList.getListeners(IterationEndsListener.class);
			Arrays.sort(listener, Comparator.comparingDouble(ControlerListener::priority).reversed());
            for (IterationEndsListener aListener : listener) {
                log.info("calling notifyIterationEnds on " + aListener.getClass().getName() + " with priority " + aListener.priority());
                aListener.notifyIterationEnds(event);
            }
		}
		log.info("[it." + iteration + "] all ControlerIterationEndsListeners called.");
	}

	/**
	 * Notifies all ControlerScoringListeners
	 *
	 */
	public void fireControlerScoringEvent(final int iteration, boolean isLastIteration) {
		ScoringEvent event = new ScoringEvent(this.controler, iteration, isLastIteration);
		{
			ScoringListener[] listener = this.coreListenerList.getListeners(ScoringListener.class);
			Arrays.sort(listener, Comparator.comparingDouble(ControlerListener::priority).reversed());
            for (ScoringListener aListener : listener) {
                log.info("calling notifyScoring on " + aListener.getClass().getName() + " with priority " + aListener.priority());
                aListener.notifyScoring(event);
            }
		}
		{
			ScoringListener[] listener = this.listenerList.getListeners(ScoringListener.class);
			Arrays.sort(listener, Comparator.comparingDouble(ControlerListener::priority).reversed());
            for (ScoringListener aListener : listener) {
                log.info("calling notifyScoring on " + aListener.getClass().getName() + " with priority " + aListener.priority());
                aListener.notifyScoring(event);
            }
		}
		log.info("[it." + iteration + "] all ControlerScoringListeners called.");
	}

	/**
	 * Notifies all ControlerReplanningListeners
	 *
	 */
	public void fireControlerReplanningEvent(final int iteration, boolean isLastIteration) {
		ReplanningEvent event = new ReplanningEvent(this.controler, iteration, isLastIteration);
		ReplanningListener[] listener = this.coreListenerList.getListeners(ReplanningListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControlerListener::priority).reversed());
        for (ReplanningListener aListener : listener) {
            log.info("calling notifyReplanning on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyReplanning(event);
        }
		listener = this.listenerList.getListeners(ReplanningListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControlerListener::priority).reversed());
        for (ReplanningListener aListener : listener) {
            log.info("calling notifyReplanning on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyReplanning(event);
        }
		log.info("[it." + iteration + "] all ControlerReplanningListeners called.");
	}

	/**
	 * Notifies all ControlerBeforeMobsimListeners
	 *
	 */
	public void fireControlerBeforeMobsimEvent(final int iteration, boolean isLastIteration) {
		BeforeMobsimEvent event = new BeforeMobsimEvent(this.controler, iteration, isLastIteration);
		BeforeMobsimListener[] listener = this.coreListenerList.getListeners(BeforeMobsimListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControlerListener::priority).reversed());
        for (BeforeMobsimListener aListener : listener) {
            log.info("calling notifyBeforeMobsim on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyBeforeMobsim(event);
        }
		listener = this.listenerList.getListeners(BeforeMobsimListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControlerListener::priority).reversed());
        for (BeforeMobsimListener aListener : listener) {
            log.info("calling notifyBeforeMobsim on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyBeforeMobsim(event);
        }
		log.info("[it." + iteration + "] all ControlerBeforeMobsimListeners called.");
	}

	/**
	 * Notifies all ControlerAfterMobsimListeners
	 *
	 */
	public void fireControlerAfterMobsimEvent(final int iteration, boolean isLastIteration) {
		AfterMobsimEvent event = new AfterMobsimEvent(this.controler, iteration, isLastIteration);
		AfterMobsimListener[] listener = this.coreListenerList.getListeners(AfterMobsimListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControlerListener::priority).reversed());
        for (AfterMobsimListener aListener : listener) {
            log.info("calling notifyAfterMobsim on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyAfterMobsim(event);
        }
		listener = this.listenerList.getListeners(AfterMobsimListener.class);
		Arrays.sort(listener, Comparator.comparingDouble(ControlerListener::priority).reversed());
        for (AfterMobsimListener aListener : listener) {
            log.info("calling notifyAfterMobsim on " + aListener.getClass().getName() + " with priority " + aListener.priority());
            aListener.notifyAfterMobsim(event);
        }
		log.info("[it." + iteration + "] all ControlerAfterMobsimListeners called.");
	}

}
