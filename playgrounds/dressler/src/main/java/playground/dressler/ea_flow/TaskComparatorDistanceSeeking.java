package playground.dressler.ea_flow;

import java.util.Comparator;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;

class TaskComparatorDistanceSeeking implements TaskComparatorI {
	private Comparator<BFTask> fallback = new TaskComparator();
	private int[] _dist;
	private int lastArrival;


	public TaskComparatorDistanceSeeking(int[] dist, int lastArrival) {
		this._dist = dist;
		this.lastArrival = lastArrival;
	}

	public int compare(BFTask first, BFTask second) {
		if (first.node instanceof VirtualNormalNode 
				&& second.node instanceof VirtualNormalNode) {
			// normal nodes, this is the main work
			
		} else 	if (first.node instanceof VirtualSource) {
			// sources always come first, but a tie needs to be resolved
			if (second.node instanceof VirtualSource) {
				return fallback.compare(first, second);	
			} else {
				return -1;
			}
		} else if (second.node instanceof VirtualSource) {
			return 1; // the first one is not a source
		}
		
		int v1 = getValue(first);
		int v2 = getValue(second);

		if (v1 < v2 ) {			
			return -1;			
		} else if ( v1 > v2 ) {			
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
		
		if (task.node instanceof VirtualNormalNode) {
			// normal nodes		
			int d = _dist[task.node.getRealNode().getIndex()];

			// to arrive at lastArrival, we should be roughly at distance lastArrival - d
			d = lastArrival - d;

			int l = task.ival.getLowBound();
			int r = task.ival.getHighBound() - 1; // !
			if (d >= l && d <= r) return 0; // contained, very good

			// how close is the interval to this "correct time"?
			
			// is the interval too early? 
			// This is probably an unreachable area.
			if (d > r) return (d - r)  + 10000; // FIXME ... find good penalty
			
			// else: d < l, the interval is too late, maybe because we really can't be on time.
			return l - d; 
			
			//return Math.min(Math.abs(d - l), Math.abs(d - r));
		} else if (task.node instanceof VirtualSource) {
			// sources have high priority
			return 0; 
		} else if (task.node instanceof VirtualSink) {
			// sinks should have bad priority (although that is debatable), if they occur at all
			// FIXME bad hack
			return 10000;
		} else {
			throw new RuntimeException("Unknown kind of node!");
		}
	}
}
