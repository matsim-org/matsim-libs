/* *********************************************************************** *
 * project: org.matsim.*
 * ActivatableContainer
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
package playground.dgrether;




/**
 * @author dgrether
 *
 */
public class ActivatableContainer<T> implements Comparable<ActivatableContainer<T>>{
	private final double time;
	private final T activatableObject;


	public ActivatableContainer(final double time, final T activatableObject){
		this.time = time;
		this.activatableObject = activatableObject;
	}
	
	public int compareTo(final ActivatableContainer<T> o) {
		if (this.time < o.time) return -1;
		if (this.time > o.time) return +1;
		return 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(final Object obj) {
		if (obj == null) return false;
		if (!(obj instanceof ActivatableContainer)) return false;
		ActivatableContainer<T> la = (ActivatableContainer<T>)obj;
		return (this.time == la.time) && (this.activatableObject.equals(la.activatableObject));
	}

	@Override
	public int hashCode() {
		return this.activatableObject.hashCode();
	}
	
	public T getActivatableObject(){
		return this.activatableObject;
	}

	public double getTime() {
		return this.time;
	}

}
