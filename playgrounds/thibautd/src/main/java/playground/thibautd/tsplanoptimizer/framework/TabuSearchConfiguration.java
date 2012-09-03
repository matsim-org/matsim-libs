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
public class TabuSearchConfiguration {
	private List<AppliedMoveListener> appliedMoveListeners = new ArrayList<AppliedMoveListener>();
	private List<StartListener> startListeners = new ArrayList<StartListener>();
	private List<EndListener> endListeners = new ArrayList<EndListener>();
	private Random random = null;
	private MoveGenerator moveGenerator = null;
	private TabuChecker tabuChecker = null;
	private FitnessFunction fitnessFunction = null;
	private Solution initialSolution = null;
	private EvolutionMonitor evolutionMonitor = null;

	private boolean locked = false;

	public Random getRandom() {
		lock();
		return random;
	}

	public void setRandom(final Random random) {
		checkLock();
		this.random = random;
	}

	public MoveGenerator getMoveGenerator() {
		lock();
		return moveGenerator;
	}

	public void setMoveGenerator(final MoveGenerator moveGenerator) {
		checkLock();
		this.moveGenerator = moveGenerator;
	}

	public TabuChecker getTabuChecker() {
		lock();
		return tabuChecker;
	}

	public void setTabuChecker(final TabuChecker tabuChecker) {
		checkLock();
		this.tabuChecker = tabuChecker;
	}

	public FitnessFunction getFitnessFunction() {
		lock();
		return fitnessFunction;
	}

	public void setFitnessFunction(final FitnessFunction fitnessFunction) {
		checkLock();
		this.fitnessFunction = fitnessFunction;
	}

	public void setInitialSolution(final Solution initialSolution) {
		checkLock();
		this.initialSolution = initialSolution;
	}

	public Solution getInitialSolution() {
		lock();
		return initialSolution.createClone();
	}

	/**
	 * Adds a listener. The object will be added to the lists corresponding
	 * to the listener interfaces it implements.
	 * Nothing will be done if it does not implements a listener interface.
	 * This method is called automatically on all fields at locking.
	 * @param listener
	 */
	public void addListener(final Object listener) {
		checkLock();
		if (listener instanceof AppliedMoveListener) {
			appliedMoveListeners.add( (AppliedMoveListener) listener );
		}
		if (listener instanceof StartListener) {
			startListeners.add( (StartListener) listener );
		}
		if (listener instanceof EndListener) {
			endListeners.add( (EndListener) listener );
		}
	}

	public List<AppliedMoveListener> getAppliedMoveListeners() {
		lock();
		return appliedMoveListeners;
	}

	public List<StartListener> getStartListeners() {
		lock();
		return startListeners;
	}

	public List<EndListener> getEndListeners() {
		lock();
		return endListeners;
	}

	public void setEvolutionMonitor(
			final EvolutionMonitor evolutionMonitor) {
		checkLock();
		this.evolutionMonitor = evolutionMonitor;
	}

	public EvolutionMonitor getEvolutionMonitor() {
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
			addListener( random );
			addListener( tabuChecker );
			addListener( moveGenerator );
			addListener( fitnessFunction );
			addListener( initialSolution );
			addListener( evolutionMonitor );

			locked = true;
		}
	}

	private void checkLock() {
		if (locked) throw new IllegalStateException( "cannot modify "+getClass()+" after it was locked" );
	}
}

