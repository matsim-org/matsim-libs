/* *********************************************************************** *
 * project: org.matsim.*
 * IntervalSimService.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.services;

/**
 * @author illenberger
 *
 */
public class IntervalSimService<T> implements SimService<T> {

	private final SimService<T> delegate;
	
	private final long interval;
	
	private final boolean execute;
	
	private long iteration;
	
	public IntervalSimService(SimService<T> delegate, long interval, boolean execute) {
		this.delegate = delegate;
		this.interval = interval;
		this.execute = execute;
	}
	
	@Override
	public void init() {
		delegate.init();
	}

	@Override
	public void run() {
		if(execute) {
			if(iteration % interval == 0) {
				delegate.run();
			}
		} else {
			if(!(iteration % interval == 0)) {
				delegate.run();
			}
		}
		iteration++;
	}

	@Override
	public T get() {
		return delegate.get();
	}

	@Override
	public void terminate() {
		delegate.terminate();
	}

}
