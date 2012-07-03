/* *********************************************************************** *
 * project: org.matsim.*
 * BinaryMinHeapTest.java
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

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;

/**
 * @author cdobler
 */
public class BinaryMinHeapTest extends TestCase {
	protected static final Logger log = Logger.getLogger(BinaryMinHeapTest.class);
	
	protected static class DummyHeapEntry implements HeapEntry {
		int index = 0;
		
		public DummyHeapEntry(int index) {
			this.index = index;
		}
		
		@Override
		public int getArrayIndex() {
			return index;
		}
	}

	protected void assertEqualsHE(DummyHeapEntry e1, DummyHeapEntry e2) {
		assertEquals(e1.index, e2.index);
		assertEquals(e1, e2);
	}
}


