/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ConcurrentListSPSC.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.wrashid.tryouts.performance;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * This implementation of a concurrent list is optimized for the single producer - single consumer threading.
 * It can be used by multiple producers, but it is not optimized for that case.
 *
 * @author rashid_waraich
 */
public class ConcurrentListSPSC<T> {
	private LinkedList<T> inputBuffer = new LinkedList<T>();
	private LinkedList<T> outputBuffer = new LinkedList<T>();

	public void add(T element) {
		synchronized (inputBuffer) {
			inputBuffer.add(element);
		}
	}

	// the input list will be emptied
	public void add(ArrayList<T> list) {
		synchronized (inputBuffer) {
			inputBuffer.addAll(list);
		}
	}

	// returns null, if empty, else the first element
	public T remove() {
		if (outputBuffer.size() > 0) {
			return outputBuffer.poll();
		}
		if (inputBuffer.size() > 0) {
			synchronized (inputBuffer) {
				// swap buffers
				LinkedList<T> tempList = null;
				tempList = inputBuffer;
				inputBuffer = outputBuffer;
				outputBuffer = tempList;
			}
			return outputBuffer.poll();
		}
		return null;
	}
}
