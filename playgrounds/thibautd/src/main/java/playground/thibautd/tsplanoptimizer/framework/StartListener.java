/* *********************************************************************** *
 * project: org.matsim.*
 * StartListener.java
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

/**
 * An interface for objects needing to be notified of the start of the iterations.
 * @author thibautd
 */
public interface StartListener<T> extends Listener<T> {
	/**
	 * called before the search process starts.
	 * @param startSolution the initial solution
	 * @param startScore the fitness of the initial solution
	 */
	public void notifyStart( final Solution<? extends T> startSolution , final double startScore );
}

