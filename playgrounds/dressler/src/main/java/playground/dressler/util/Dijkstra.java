package playground.dressler.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;

import playground.dressler.control.FlowCalculationSettings;


public class Dijkstra {
   private FlowCalculationSettings _settings;
   private PriorityQueue<Id> _queue;
   private HashMap<Id, Integer> _dist;
   
   
   public Dijkstra(FlowCalculationSettings settings) {
	   this._settings = settings;
	   this._dist = new HashMap<Id, Integer>(settings.getNetwork().getNodes().size() * 3 / 2);
	   
	   for (Node node : settings.getNetwork().getNodes().values()) {
		   this._dist.put(node.getId(), Integer.MAX_VALUE);
	   }
	   
	   this._queue = new PriorityQueue<Id>(1, new Comparator<Id>() {
	       public int compare(Id id1, Id id2) {
	     	   Integer v1 = _dist.get(id1);
	     	   Integer v2 = _dist.get(id2);	     	   
	     	   	     	   	     	   
	     	   if (v1 > v2) {
	     		  return 1;
	     	   } else if (v1 < v2) {
	     		   return -1;
	     	   } else {
	     		   // tie breaker
	     		   return id1.compareTo(id2);
	     	   }	     	   		              
	       }
	  });
   }
   
   public void setStart(Collection<Node> all) {
	   for (Node node : all) {
		   setStart(node.getId());
	   }
   }
   
   public void setStart(Id id) {
	   _dist.put(id, 0);
	   this._queue.add(id);	
   }
   
   public HashMap<Id, Integer> calcDistances(boolean forward, boolean reverse) {
	   Id id;
	   NetworkImpl network = _settings.getNetwork();
	   
	   while (_queue.size() > 0) {
		    id = _queue.poll();
		    Node node = network.getNodes().get(id);
		    int dist = _dist.get(id);
		    
		    if (forward) {
		    	for (Link link : node.getOutLinks().values()) {
		    		int l = _settings.getLength(link);
		    		Id other = link.getToNode().getId();
		    		int distother = _dist.get(other);
		    		if (dist + l < distother) {
		    			_dist.put(other, dist + l);
		    			_queue.add(other);
		    		}
		    	}
		    }
		    
		    if (reverse) {
		    	for (Link link : node.getInLinks().values()) {
		    		int l = _settings.getLength(link);
		    		Id other = link.getFromNode().getId();
		    		int distother = _dist.get(other);
		    		if (dist + l < distother) {
		    			_dist.put(other, dist + l);
		    			_queue.add(other);
		    		}
		    	}
		    }
	   }
	   return _dist;
   }
}
