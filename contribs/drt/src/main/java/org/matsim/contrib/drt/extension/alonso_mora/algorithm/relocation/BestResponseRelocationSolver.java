package org.matsim.contrib.drt.extension.alonso_mora.algorithm.relocation;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This is a best-response implementation of a relocation solver for the
 * algorithm by Alonso-Mora et al. It iteratively loops over all potential
 * vehicle-destinations and assigns the one with the lowest cost. A destination
 * can not be assigned twice.
 * 
 * @author sebhoerl
 */
public class BestResponseRelocationSolver implements RelocationSolver {
	static public final String TYPE = "BestResponse";
	
	@Override
	public Collection<Relocation> solve(List<Relocation> candidates) {
		LinkedList<Relocation> relocationList = new LinkedList<>(candidates);
		List<Relocation> selection = new LinkedList<>();

		Collections.sort(relocationList, (a, b) -> {
			return Double.compare(a.cost, b.cost);
		});

		while (relocationList.size() > 0) {
			Relocation selectedTrip = relocationList.removeFirst();
			selection.add(selectedTrip);

			Iterator<Relocation> iterator = relocationList.iterator();

			while (iterator.hasNext()) {
				Relocation trip = iterator.next();

				if (trip.destination == selectedTrip.destination) {
					iterator.remove();
				}
			}
		}

		return selection;
	}
}
