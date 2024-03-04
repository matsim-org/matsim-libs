/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.core.utils.geometry.transformations;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;

/**
 * @author boescpa
 */
public class CH1903LV03PlustofromCH1903LV03Test {

	@Test
	void testCH1903LV03PlustoCH1903LV03() {
		CH1903LV03PlustoCH1903LV03 converter = new CH1903LV03PlustoCH1903LV03();
		Coord n = converter.transform(new Coord((double) 2700000, (double) 1100000));
		Assertions.assertEquals(700000, n.getX(), 0.0);
		Assertions.assertEquals(100000, n.getY(), 0.0);
	}

	@Test
	void testCH1903LV03toCH1903LV03Plus() {
		CH1903LV03toCH1903LV03Plus converter = new CH1903LV03toCH1903LV03Plus();
		Coord n = converter.transform(new Coord((double) 700000, (double) 100000));
		Assertions.assertEquals(2700000, n.getX(), 0.0);
		Assertions.assertEquals(1100000, n.getY(), 0.0);
	}
}
