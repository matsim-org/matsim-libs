package playground.msieg.structure;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.Transformer;


/**
 * This is to define a pathbased flow representation of an flow
 * A path flow is expressed as a set of flows for any commodity 
 * instance, that is to any commodity there is an associated set
 * of Paths (given as an ordered set of links) and to every path
 * an associated flow value (most likely steored as double).
 * 
 * 
 * @author msieg
 *
 * @param <V>
 * @param <E>
 */

public interface PathFlow<V,E> {

	public boolean add(V from, V to, List<E> path, double f);
	public boolean add(Commodity<V> c, List<E> path, double f);
	
	public Set<Commodity<V>> getCommodities();
	
	public Set<List<E>> getFlowPaths(Commodity<V> c);
	
	public Double getFlowValue(Commodity<V> c, List<E> path);
	
	public Map<E, Double> getArcFlowMap();
	
	public String getArcFlowXMLString(int leadingTabs);
	
	public String getPathFlowXMLString(int leadingTabs);

	//Delete this one later:
	public Map<Commodity<V>, Map<List<E>, Double>> getPathFlow();
}
