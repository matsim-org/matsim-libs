/* *********************************************************************** *
 * project: org.matsim.*
 * SubMatrixCreator.java
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

package playground.qiuhan.sa;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;

public class SubMatrixCreator {
	private Matrix m;
	private DailyTrafficLoadCurve d;

	public SubMatrixCreator(final Matrix m, final DailyTrafficLoadCurve d) {
		this.m = m;
		this.d = d;
	}

	public Map<String, Matrix> createMatrixes() {
		Map<String, Matrix> map = new HashMap<String, Matrix>();
		for (Integer i : this.d.getTrafficLoad().keySet()) {

			String time = i.toString();
			Double share = this.d.getTrafficShare(i);

			Matrix smallMatrix = this.createMatrix(time, share);

			map.put(time, smallMatrix);
		}

		return map;

	}

	public Matrix createMatrix(String time, Double share) {
		Matrix matrix = new Matrix(time, "with traffic share " + share);
		for (Id from : this.m.getFromLocations().keySet()) {
			for (Entry entry : this.m.getFromLocEntries(from)) {
				matrix.createEntry(from, entry.getToLocation(),
						entry.getValue() * share / 100.0);
			}
		}
		return matrix;
	}
}
