/* *********************************************************************** *
 * project: org.matsim.*
 * ODMatrixBezirke.java
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

package playground.marcel;

import java.util.TreeMap;

import org.matsim.gbl.Gbl;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.utils.identifiers.IdI;
import org.matsim.world.Location;
import org.matsim.world.World;
import org.matsim.world.ZoneLayer;

public class ODMatrixBezirke extends PersonAlgorithm implements PlanAlgorithmI {

	private final ZoneLayer tvzLayer;
	private final Matrix<Integer> matrix;
	private final World world;

	public ODMatrixBezirke(final String matrixId) {
		this.world = Gbl.getWorld();
		this.tvzLayer = (ZoneLayer)this.world.getLayer("tvz");
		this.matrix = Matrices.getSingleton().<Integer>createMatrix(matrixId, this.world.getLayer("bezirke").getType().toString(), "od for miv, 0-24h, 23 bezirke in berlin, 123 outside berlin");
	}

	@Override
	public void run(final Person person) {
		for (Plan plan : person.getPlans()) {
			if (plan.isSelected()) run(plan);
		}
	}

	public final void run(final Plan plan) {
		Location fromLoc = null;
		Location toLoc = null;

		Act tmpAct = (Act)plan.getActsLegs().get(0);
		fromLoc = mapLocation(this.tvzLayer.getLocation(tmpAct.getRefId()));

		for (int i = 2, max = plan.getActsLegs().size(); i < max; i += 2) {
			tmpAct = (Act)plan.getActsLegs().get(i);
			toLoc = mapLocation(this.tvzLayer.getLocation(tmpAct.getRefId()));

			if (fromLoc != null && toLoc != null) {
				Entry<Integer> entry = this.matrix.getEntry(fromLoc, toLoc);
				if (entry == null) {
					this.matrix.setEntry(fromLoc, toLoc, 1);
				} else {
					this.matrix.setEntry(fromLoc, toLoc, entry.getValue() + 1);
				}
			}
			fromLoc = toLoc;
		}
	}

	public Location mapLocation(final Location loc) {
		TreeMap<IdI, Location> mapping = loc.getUpMapping();
		if (mapping.size() == 1) {
			return mapping.get(mapping.firstKey());
		}
		return null;
	}

	public Matrix<Integer> getMatrix() {
		return this.matrix;
	}

}
