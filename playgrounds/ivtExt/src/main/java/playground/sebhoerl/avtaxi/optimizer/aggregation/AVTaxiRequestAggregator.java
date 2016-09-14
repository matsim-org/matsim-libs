package playground.sebhoerl.avtaxi.optimizer.aggregation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.matsim.contrib.taxi.data.TaxiRequest;

public class AVTaxiRequestAggregator {
	Map<String, Set<AVAggregateTaxiRequest>> aggregates = new HashMap<>();
	Queue<AVAggregateTaxiRequest> aggregateQueue = new LinkedList<AVAggregateTaxiRequest>();
	
	static public String createODHash(TaxiRequest request) {
		return String.format("%s:%s", request.getFromLink().toString(), request.getToLink().toString());
	}
	
	static public String createODHash(AVAggregateTaxiRequest aggregate) {
		return String.format("%s:%s", aggregate.getPickupLink().toString(), aggregate.getDropoffLink().toString());
	}
	
	public void register(TaxiRequest request) {
		String odHash = createODHash(request);
		Set<AVAggregateTaxiRequest> odSet = aggregates.get(odHash);
		
		if (odSet == null) {
			odSet = new HashSet<>();
			odSet.add(new AVAggregateTaxiRequest(request));
			aggregateQueue.add(odSet.iterator().next());
		} else {
			AVAggregateTaxiRequest match = null;
			
			for (AVAggregateTaxiRequest agg : odSet) {
				if (agg.covers(request) && agg.getRequests().size() < 4) {
					match = agg;
					break;
				}
			}
			
			if (match == null) {
				match = new AVAggregateTaxiRequest(request);
				odSet.add(match);
				aggregateQueue.add(match);
			} else {
				match.addRequest(request);
			}
		}
	}
	
	public void unregister(AVAggregateTaxiRequest aggregate) {
		String odHash = createODHash(aggregate);
		Set<AVAggregateTaxiRequest> set = aggregates.get(odHash);
		
		if (set != null) {
			set.remove(aggregate);
		}
	}
	
	public Queue<AVAggregateTaxiRequest> getAggregateRequests() {
		return aggregateQueue;
	}
	
	public AVAggregateTaxiRequest pollAggregateRequest() {
		AVAggregateTaxiRequest agg = aggregateQueue.poll();
		unregister(agg);
		return agg;
	}
}
