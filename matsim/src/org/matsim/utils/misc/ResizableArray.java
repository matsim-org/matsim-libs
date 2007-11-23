/* *********************************************************************** *
 * project: org.matsim.*
 * ResizableArray.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.misc;

public class ResizableArray<T> {
	/* It is not possible to create arrays of generic types, e.g. T[] array = new T[5];
	 * for more details see chpt 7.3. on http://java.sun.com/j2se/1.5/pdf/generics-tutorial.pdf
	 * thus use an Object array, but ensure types in the function parameters, thus we can safely cast ourself
	 */
	private Object[] array = null;
	private int size = -1;

	public ResizableArray(final int size) {
		this.size = size;
		this.array = new Object[size];
	}

	public final void set(final int idx, final T o) throws ArrayIndexOutOfBoundsException {
		this.array[idx] = o;
	}

	@SuppressWarnings("unchecked")
	public final T get(final int idx) throws ArrayIndexOutOfBoundsException {
		return (T)this.array[idx];
	}

	public final int size() {
		return this.size;
	}

	public final void resize(final int newsize) {
		Object[] newArray = new Object[newsize];
		System.arraycopy(this.array, 0, newArray, 0, Math.min(this.size, newsize));
		this.array = newArray;
		this.size = newsize;
	}

}
