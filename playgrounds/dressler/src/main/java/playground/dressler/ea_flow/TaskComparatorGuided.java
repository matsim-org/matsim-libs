package playground.dressler.ea_flow;

import java.util.Comparator;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;

class TaskComparatorGuided implements TaskComparatorI {
	private Comparator<BFTask> fallback = new TaskComparator();
	private int[] _dist;


	public TaskComparatorGuided(int[] dist) {
		this._dist = dist;
	}

	public int compare(BFTask first, BFTask second) {
		int d1 = _dist[first.node.getRealNode().getIndex()];
		int d2 = _dist[second.node.getRealNode().getIndex()];

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
	
	// not equivalent to the above!
	// and might work better if task.depth is added ... 
	// weird, the BucketQueue already has BFS within each category 
	public int getValue(BFTask task) {
		return _dist[task.node.getRealNode().getIndex()] + task.time; 
	}
}
