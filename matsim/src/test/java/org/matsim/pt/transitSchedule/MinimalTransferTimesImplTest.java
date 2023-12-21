/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.pt.transitSchedule;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.MinimalTransferTimes;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.NoSuchElementException;

/**
 * @author mrieser / SBB
 */
public class MinimalTransferTimesImplTest {

	private final Id<TransitStopFacility> stopId1 = Id.create(1, TransitStopFacility.class);
	private final Id<TransitStopFacility> stopId2 = Id.create(2, TransitStopFacility.class);
	private final Id<TransitStopFacility> stopId3 = Id.create(3, TransitStopFacility.class);
	private final Id<TransitStopFacility> stopId4 = Id.create(4, TransitStopFacility.class);

	@Test
	void testSetGet() {
		MinimalTransferTimes mtt = new MinimalTransferTimesImpl();
		Assertions.assertEquals(Double.NaN, mtt.get(this.stopId1, this.stopId2), 0.0);
		mtt.set(this.stopId1, this.stopId2, 180.0);
		mtt.set(this.stopId1, this.stopId3, 240.0);

		Assertions.assertEquals(180.0, mtt.get(this.stopId1, this.stopId2), 0.0);
		Assertions.assertEquals(240.0, mtt.get(this.stopId1, this.stopId3), 0.0);

		// overwrite a value
		mtt.set(this.stopId1, this.stopId2, 300.0);
		Assertions.assertEquals(300.0, mtt.get(this.stopId1, this.stopId2), 0.0);

		Assertions.assertEquals(Double.NaN, mtt.get(this.stopId1, this.stopId4), 0.0);
	}

	@Test
	void testGetNonSetConnection() {
		MinimalTransferTimes mtt = new MinimalTransferTimesImpl();
		mtt.set(this.stopId1, this.stopId1, 300.0);
		mtt.set(this.stopId3, this.stopId3, 240.0);

		Assertions.assertEquals(300.0, mtt.get(this.stopId1, this.stopId1), 0.0);
		Assertions.assertEquals(240.0, mtt.get(this.stopId3, this.stopId3), 0.0);

		Assertions.assertEquals(300.0, mtt.get(this.stopId1, this.stopId3), 0.0);

	}

	@Test
	void testGetWithDefault() {
		MinimalTransferTimes mtt = new MinimalTransferTimesImpl();
		double defaultSeconds = 60.0;
		Assertions.assertEquals(Double.NaN, mtt.get(this.stopId1, this.stopId2), 0.0);
		Assertions.assertEquals(60.0,  mtt.get(this.stopId1, this.stopId2, defaultSeconds), 0.0);
		mtt.set(this.stopId1, this.stopId2, 180.0);
		mtt.set(this.stopId1, this.stopId3, 240.0);

		Assertions.assertEquals(180.0, mtt.get(this.stopId1, this.stopId2, defaultSeconds), 0.0);
		Assertions.assertEquals(240.0, mtt.get(this.stopId1, this.stopId3, defaultSeconds), 0.0);

		Assertions.assertEquals(defaultSeconds, mtt.get(this.stopId1, this.stopId4, defaultSeconds), 0.0);
	}

	@Test
	void testRemove() {
		MinimalTransferTimes mtt = new MinimalTransferTimesImpl();
		Assertions.assertEquals(Double.NaN, mtt.get(this.stopId1, this.stopId2), 0.0);
		mtt.set(this.stopId1, this.stopId2, 180.0);
		mtt.set(this.stopId1, this.stopId3, 240.0);

		Assertions.assertEquals(180.0, mtt.get(this.stopId1, this.stopId2), 0.0);
		Assertions.assertEquals(240.0, mtt.get(this.stopId1, this.stopId3), 0.0);

		Assertions.assertEquals(180.0, mtt.remove(this.stopId1, this.stopId2), 0.0);
		Assertions.assertEquals(Double.NaN, mtt.get(this.stopId1, this.stopId2), 0.0);
		Assertions.assertEquals(240.0, mtt.get(this.stopId1, this.stopId3), 0.0);

		Assertions.assertEquals(Double.NaN, mtt.remove(this.stopId1, this.stopId4), 0.0); // we never set it, let's not throw an exception
		Assertions.assertEquals(Double.NaN, mtt.get(this.stopId1, this.stopId4), 0.0);
	}

	@Test
	void testNotBidirection() {
		MinimalTransferTimes mtt = new MinimalTransferTimesImpl();
		mtt.set(this.stopId1, this.stopId2, 180.0);
		mtt.set(this.stopId1, this.stopId3, 240.0);

		Assertions.assertEquals(180.0, mtt.get(this.stopId1, this.stopId2), 0.0);
		Assertions.assertEquals(Double.NaN, mtt.get(this.stopId2, this.stopId1), 0.0);
		Assertions.assertEquals(240.0, mtt.get(this.stopId1, this.stopId3), 0.0);
		Assertions.assertEquals(Double.NaN, mtt.get(this.stopId3, this.stopId1), 0.0);

		mtt.set(this.stopId3, this.stopId1, 120.0);
		Assertions.assertEquals(120.0, mtt.get(this.stopId3, this.stopId1), 0.0);
		Assertions.assertEquals(240.0, mtt.get(this.stopId1, this.stopId3), 0.0);
	}

	@Test
	void testNotTransitive() {
		MinimalTransferTimes mtt = new MinimalTransferTimesImpl();
		mtt.set(this.stopId1, this.stopId2, 180.0);
		mtt.set(this.stopId2, this.stopId3, 240.0);

		Assertions.assertEquals(180.0, mtt.get(this.stopId1, this.stopId2), 0.0);
		Assertions.assertEquals(240.0, mtt.get(this.stopId2, this.stopId3), 0.0);
		Assertions.assertEquals(Double.NaN, mtt.get(this.stopId1, this.stopId3), 0.0);
		Assertions.assertEquals(Double.NaN, mtt.get(this.stopId3, this.stopId1), 0.0);

		mtt.set(this.stopId1, this.stopId3, 480.0);
		Assertions.assertEquals(480.0, mtt.get(this.stopId1, this.stopId3), 0.0);
	}

	@Test
	void testIterator_empty() {
		MinimalTransferTimes mtt = new MinimalTransferTimesImpl();

		MinimalTransferTimes.MinimalTransferTimesIterator iter = mtt.iterator();

		Assertions.assertFalse(iter.hasNext());
		try {
			iter.getFromStopId();
			Assertions.fail("expected Exception, got none.");
		} catch (NoSuchElementException expected) {}

		try {
			iter.getToStopId();
			Assertions.fail("expected Exception, got none.");
		} catch (NoSuchElementException expected) {}

		try {
			iter.getSeconds();
			Assertions.fail("expected Exception, got none.");
		} catch (NoSuchElementException expected) {}

		try {
			iter.next();
			Assertions.fail("expected Exception");
		} catch (NoSuchElementException expected) {}

		try {
			iter.getFromStopId(); // there should still be an exception after calling next()
			Assertions.fail("expected Exception, got none.");
		} catch (NoSuchElementException expected) {}

	}

	@Test
	void testIterator() {
		MinimalTransferTimes mtt = new MinimalTransferTimesImpl();

		MinimalTransferTimes.MinimalTransferTimesIterator iter = mtt.iterator();
		Assertions.assertFalse(iter.hasNext());

		mtt.set(this.stopId1, this.stopId2, 120.0);

		iter = mtt.iterator();
		Assertions.assertTrue(iter.hasNext());
		try {
			iter.getFromStopId();
			Assertions.fail("expected Exception, got none.");
		} catch (NoSuchElementException expected) {}

		iter.next();
		Assertions.assertEquals(this.stopId1, iter.getFromStopId());
		Assertions.assertEquals(this.stopId2, iter.getToStopId());
		Assertions.assertEquals(120, iter.getSeconds(), 0.0);

		Assertions.assertFalse(iter.hasNext());

		try {
			iter.next();
			Assertions.fail("expected Exception, got none.");
		} catch (NoSuchElementException expected) {}

		try {
			iter.getFromStopId();
			Assertions.fail("expected Exception, got none.");
		} catch (NoSuchElementException expected) {}

		mtt.set(this.stopId1, this.stopId3, 180);
		mtt.set(this.stopId2, this.stopId3, 240);
		mtt.set(this.stopId1, this.stopId2, 300); // overwrite an existing entry

		// there should be a total of 3 entries now
		iter = mtt.iterator();
		int count = 0;
		boolean found1to2 = false;
		boolean found1to3 = false;
		boolean found2to3 = false;
		while (iter.hasNext()) {
			count++;
			iter.next();
			Id<TransitStopFacility> fromStopId = iter.getFromStopId();
			Id<TransitStopFacility> toStopId = iter.getToStopId();
			double seconds = iter.getSeconds();
			if (fromStopId == this.stopId1 && toStopId == this.stopId2 && seconds == 300) {
				found1to2 = true;
			} else if (fromStopId == this.stopId1 && toStopId == this.stopId3 && seconds == 180) {
				found1to3 = true;
			} else if (fromStopId == this.stopId2 && toStopId == this.stopId3 && seconds == 240) {
				found2to3 = true;
			} else {
				Assertions.fail("found unexcpected minimal transfer time: " + fromStopId + " / " + toStopId + " / " + seconds);
			}
			if (count > 3) {
				Assertions.fail("too many elements in iterator.");
			}
		}
		Assertions.assertTrue(found1to2);
		Assertions.assertTrue(found1to3);
		Assertions.assertTrue(found2to3);


	}

	@Test
	void testIterator_withRemove() {
		MinimalTransferTimes mtt = new MinimalTransferTimesImpl();
		mtt.set(this.stopId1, this.stopId2, 180);
		mtt.set(this.stopId2, this.stopId3, 240);
		mtt.set(this.stopId3, this.stopId1, 300);

		MinimalTransferTimes.MinimalTransferTimesIterator iter = mtt.iterator();
		int count = 0;
		while (iter.hasNext()) {
			count++;
			iter.next();
		}
		Assertions.assertEquals(3, count);

		mtt.remove(this.stopId2, this.stopId3);
		count = 0;
		iter = mtt.iterator();
		while (iter.hasNext()) {
			count++;
			iter.next();
		}
		Assertions.assertEquals(2, count);

		mtt.remove(this.stopId1, this.stopId2);
		count = 0;
		iter = mtt.iterator();
		while (iter.hasNext()) {
			count++;
			iter.next();
		}
		Assertions.assertEquals(1, count);

		mtt.remove(this.stopId3, this.stopId1);
		iter = mtt.iterator();
		Assertions.assertFalse(iter.hasNext());
	}
}
