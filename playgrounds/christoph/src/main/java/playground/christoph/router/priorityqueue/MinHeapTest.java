/* *********************************************************************** *
 * project: org.matsim.*
 * MinHeapTest.java
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

package playground.christoph.router.priorityqueue;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

/**
 * @author cdobler
 */
public class MinHeapTest extends TestCase {
	
	protected static final Logger log = Logger.getLogger(MinHeapTest.class);
	
	protected static class DummyHeapEntry implements HeapEntry {
		
		final int index;
		
		public DummyHeapEntry(int index) {
			this.index = index;
		}
		
		@Override
		public int getArrayIndex() {
			return index;
		}
		
		@Override
		public String toString() {
			return String.valueOf(this.index);
		}
	}

	protected void assertEqualsHE(DummyHeapEntry e1, DummyHeapEntry e2) {
		assertEquals(e1.index, e2.index);
		assertEquals(e1, e2);
	}
}


