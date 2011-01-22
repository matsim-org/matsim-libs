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
import playground.dressler.network.IndexedLinkI;
import playground.dressler.network.IndexedNetworkI;
import playground.dressler.network.IndexedNodeI;


public class Dijkstra {
   private FlowCalculationSettings _settings;
   private PriorityQueue<IndexedNodeI> _queue;
   private int[] _dist;
   
   
   public Dijkstra(FlowCalculationSettings settings) {
	   this._settings = settings;
	   this._dist = new int[settings.getNetwork().getLargestIndexNodes() + 1];
	   
	   for (int i = 0; i < this._dist.length; i++) {
		   this._dist[i] = Integer.MAX_VALUE;
	   }
	   
	   this._queue = new PriorityQueue<IndexedNodeI>(1, new Comparator<IndexedNodeI>() {
	       public int compare(IndexedNodeI node1, IndexedNodeI node2) {
	     	   Integer v1 = _dist[node1.getIndex()];
	     	   Integer v2 = _dist[node2.getIndex()];	     	   
	     	   	     	   	     	   
	     	   if (v1 > v2) {
	     		  return 1;
	     	   } else if (v1 < v2) {
	     		   return -1;
	     	   } else {
	     		   // tie breaker
	     		   return node1.getMatsimNode().getId().compareTo(node2.getMatsimNode().getId());
	     	   }	     	   		              
	       }
	  });
   }
   
   public void setStart(Collection<IndexedNodeI> all) {
	   for (IndexedNodeI node : all) {
		   setStart(node);
	   }
   }
   
   public void setStart(IndexedNodeI node) {
	   _dist[node.getIndex()] = 0;
	   this._queue.add(node);	
   }
   
   public int[] calcDistances(boolean forward, boolean reverse) {
	   IndexedNodeI node;
	   
	   while (_queue.size() > 0) {
		    node = _queue.poll();
		    
		    int dist = _dist[node.getIndex()];
		    
		    if (forward) {
		    	for (IndexedLinkI link : node.getOutLinks()) {
		    		int l = _settings.getLength(link);
		    		IndexedNodeI other = link.getToNode();
		    		int distother = _dist[other.getIndex()];
		    		if (dist + l < distother) {
		    			_dist[other.getIndex()] = dist + l;
		    			_queue.add(other);
		    		}
		    	}
		    }
		    
		    if (reverse) {
		    	for (IndexedLinkI link : node.getInLinks()) {
		    		int l = _settings.getLength(link);
		    		IndexedNodeI other = link.getFromNode();
		    		int distother = _dist[other.getIndex()];
		    		if (dist + l < distother) {
		    			_dist[other.getIndex()] = dist + l;
		    			_queue.add(other);
		    		}
		    	}
		    }
	   }
	   return _dist;
   }
}
