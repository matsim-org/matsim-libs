package playground.toronto.sotr.routernetwork2;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.matsim.core.router.priorityqueue.HasIndex;

public abstract class RoutingLink implements HasIndex {
	
	protected RoutingNode fromNode;
	protected RoutingNode toNode;
	
	protected HashSet<RoutingLink> prohibitedOutgoingTurns;
	
	protected int index;
	
	/**
	 * The pending cost to the tail of this link
	 */
	public double pendingCost;
	
	/**
	 * The pending time to the tail of this link
	 */
	public double pendingTime;
	
	public RoutingLink previousLink;
	
	public RoutingNode getFromNode(){ return this.fromNode;}
	public RoutingNode getToNode(){ return this.toNode;}
	
	public RoutingLink(RoutingNode fromNode, RoutingNode toNode){
		this.fromNode = fromNode;
		this.toNode = toNode;
		
		this.prohibitedOutgoingTurns = new HashSet<RoutingLink>();
		this.index = -1; //This only gets set a meaningful value if it's created by the RoutingNetworkDelegate
		
		reset();
	}
	
	public void reset(){
		this.pendingCost = Double.POSITIVE_INFINITY;
		this.pendingTime = Double.POSITIVE_INFINITY;
		this.previousLink = null;
	}
	
	@Override
	public int getArrayIndex(){
		return index;
	}
	
	/**
	 * Returns an iterator for outgoing turns.
	 * 
	 * @param allowUTurns True if U-Turns are permitted
	 * @return
	 */

	public Iterable<RoutingLink> getOutgoingTurns(final boolean allowUTurns){
		
		final Iterator<RoutingLink> base = this.toNode.outgoingLinks.iterator();
		
		return new Iterable<RoutingLink>() {
			@Override
			public Iterator<RoutingLink> iterator() {
				return new Iterator<RoutingLink>() {	
					
					boolean _hasNext = true;
					
					@Override
					public void remove() { throw new UnsupportedOperationException(); }
					
					@Override
					public RoutingLink next() {
						if (base.hasNext()){
							RoutingLink nextLink = base.next();
							
							while (prohibitedOutgoingTurns.contains(nextLink)){
								if (base.hasNext()){nextLink = base.next();}
								else{ 
									_hasNext = false;
									throw new NoSuchElementException();
								}
							}
							
							if (allowUTurns) return nextLink; //If u-turns are permitted, no need to skip u-turned links
							
							while (nextLink.getToNode() == fromNode){
								if (base.hasNext()){nextLink = base.next();}
								else{
									_hasNext = false;
									throw new NoSuchElementException();
								}
							}
							return nextLink;
						}
						_hasNext = false;
						throw new NoSuchElementException();
					}
					
					@Override
					public boolean hasNext() {
						return _hasNext;
					}
				};
			}
		};
	}
}
