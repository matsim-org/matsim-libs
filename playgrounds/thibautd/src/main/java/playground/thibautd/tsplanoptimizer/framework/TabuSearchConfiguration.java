/* *********************************************************************** *
 * project: org.matsim.*
 * TabuSearchConfiguration.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.tsplanoptimizer.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Defines various settings for the Tabu Search process.
 * See documentation of the interfaces of the components for details:
 * this class is just a container.
 * @author thibautd
 */
public final class TabuSearchConfiguration<T> {
	private List<AppliedMoveListener<T>> appliedMoveListeners = new ArrayList<AppliedMoveListener<T>>();
	private List<StartListener<T>> startListeners = new ArrayList<StartListener<T>>();
	private List<EndListener<T>> endListeners = new ArrayList<EndListener<T>>();
	private Random random = null;
	private MoveGenerator moveGenerator = null;
	private TabuChecker<T> tabuChecker = null;
	private FitnessFunction<T> fitnessFunction = null;
	private EvolutionMonitor<T> evolutionMonitor = null;

	private boolean locked = false;

	public Random getRandom() {
		lock();
		return random;
	}

	public Random setRandom(final Random random) {
		checkLock();
		Random old = this.random;
		this.random = random;
		return old;
	}

	public MoveGenerator getMoveGenerator() {
		lock();
		return moveGenerator;
	}

	public MoveGenerator setMoveGenerator(final MoveGenerator moveGenerator) {
		checkLock();
		MoveGenerator old = this.moveGenerator;
		this.moveGenerator = moveGenerator;
		return old;
	}

	public TabuChecker<T> getTabuChecker() {
		lock();
		return tabuChecker;
	}

	public TabuChecker<T> setTabuChecker(final TabuChecker<T> tabuChecker) {
		checkLock();
		TabuChecker<T> old = this.tabuChecker;
		this.tabuChecker = tabuChecker;
		return old;
	}

	public FitnessFunction<T> getFitnessFunction() {
		lock();
		return fitnessFunction;
	}

	public FitnessFunction<T> setFitnessFunction(final FitnessFunction<T> fitnessFunction) {
		checkLock();
		FitnessFunction<T> old = this.fitnessFunction;
		this.fitnessFunction = fitnessFunction;
		return old;
	}

	/**
	 * Adds a listener. The object will be added to the lists corresponding
	 * to the listener interfaces it implements.
	 * Nothing will be done if it does not implements a listener interface.
	 * This method is called automatically on all fields at locking.
	 * @param listener
	 */
	public void addListener(final Listener<T> listener) {
		checkLock();
		if (listener instanceof AppliedMoveListener) {
			appliedMoveListeners.add( (AppliedMoveListener<T>) listener );
		}
		if (listener instanceof StartListener) {
			startListeners.add( (StartListener<T>) listener );
		}
		if (listener instanceof EndListener) {
			endListeners.add( (EndListener<T>) listener );
		}
	}

	public List<AppliedMoveListener<T>> getAppliedMoveListeners() {
		lock();
		return appliedMoveListeners;
	}

	public List<StartListener<T>> getStartListeners() {
		lock();
		return startListeners;
	}

	public List<EndListener<T>> getEndListeners() {
		lock();
		return endListeners;
	}

	public EvolutionMonitor<T> setEvolutionMonitor(
			final EvolutionMonitor<T> evolutionMonitor) {
		checkLock();
		EvolutionMonitor<T> old = this.evolutionMonitor;
		this.evolutionMonitor = evolutionMonitor;
		return old;
	}

	public EvolutionMonitor<T> getEvolutionMonitor() {
		lock();
		return evolutionMonitor;
	}

	/**
	 * After this method is called, calls to the setters results in an IllegalStateException.
	 * It is called at the first getter called.
	 */
	public void lock() {
		if (!locked) {
			// add all registered field to the listener list.
			// this is safe, as they cannot be modified from now on.
			//addListener( random );
			addListener( tabuChecker );
			//addListener( moveGenerator );
			addListener( fitnessFunction );
			//addListener( initialSolution );
			addListener( evolutionMonitor );

			locked = true;
		}
	}

	private void checkLock() {
		if (locked) throw new IllegalStateException( "cannot modify "+getClass()+" after it was locked" );
	}
}

