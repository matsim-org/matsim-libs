package playground.toronto.sotr.routernetwork2;

import java.util.HashSet;
import java.util.Iterator;

import org.matsim.core.router.priorityqueue.HasIndex;

public abstract class AbstractRoutingLink implements HasIndex {
	
	AbstractRoutingNode fromNode;
	AbstractRoutingNode toNode;
	
	HashSet<AbstractRoutingLink> prohibitedOutgoingTurns;
	boolean isCopy = false;
	int index;
	
	/**
	 * The pending cost to the tail of this link
	 */
	public double pendingCost;
	
	/**
	 * The pending time to the tail of this link
	 */
	public double pendingTime;
	
	public AbstractRoutingLink previousLink;
	
	public AbstractRoutingNode getFromNode(){ return this.fromNode;}
	public AbstractRoutingNode getToNode(){ return this.toNode;}
	
	public AbstractRoutingLink(AbstractRoutingNode fromNode, AbstractRoutingNode toNode){
		this.fromNode = fromNode;
		this.toNode = toNode;
		
		this.prohibitedOutgoingTurns = new HashSet<AbstractRoutingLink>();
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

	public Iterable<AbstractRoutingLink> getOutgoingTurns(final boolean allowUTurns){
		
		final Iterator<AbstractRoutingLink> base = this.toNode.outgoingLinks.iterator();
		
		return new Iterable<AbstractRoutingLink>() {
			@Override
			public Iterator<AbstractRoutingLink> iterator() {
				return new Iterator<AbstractRoutingLink>() {	
					
					//boolean _hasNext = true;
					AbstractRoutingLink nextLink = null;
					
					@Override
					public void remove() { throw new UnsupportedOperationException(); }
					
					@Override
					public AbstractRoutingLink next() {
						return nextLink;
					}
					
					@Override
					public boolean hasNext() {
						if (base.hasNext()){
							nextLink = base.next();
							
							//Cycle until we get a non-prohibited turn.
							while (prohibitedOutgoingTurns.contains(nextLink)){
								if (base.hasNext()){nextLink = base.next();}
								else{ return false; }
							}
							
							if (allowUTurns) return true; //If u-turns are permitted, no need to skip u-turned links
							
							//Cycle until we get a non-u-turn
							while (nextLink.getToNode() == fromNode){
								if (base.hasNext()){nextLink = base.next();}
								else{
									return false;
								}
							}
							return true;
						}
						return false;
					}
				};
			}
		};
	}
}
