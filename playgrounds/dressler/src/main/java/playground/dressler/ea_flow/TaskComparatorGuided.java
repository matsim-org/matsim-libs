package playground.dressler.ea_flow;

import java.util.Comparator;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;

class TaskComparatorGuided implements Comparator<BFTask> {
	private Comparator<BFTask> fallback = new TaskComparator();
	private HashMap<Id, Integer> _dist;


	public TaskComparatorGuided(HashMap<Id, Integer> dist) {
		this._dist = dist;
	}

	public int compare(BFTask first, BFTask second) {
		int d1 = _dist.get(first.node.getRealNode().getId());
		int d2 = _dist.get(second.node.getRealNode().getId());

		if (d1 + first.time < d2 + second.time) {			
			return -1;			
		} else if ( d1 + first.time > d2 + second.time) {			
			return 1;
		} else {
			// do some BFS-ish things
			if (first.depth < second.depth) {
				return -1;
			} else if (first.depth > second.depth ) {
				return 1;
			} else {
				// Comparator needs total order!!!
				return fallback.compare(first, second);				
			}
		}

	}
}
