package playground.toronto.sotr.routernetwork2;

import java.util.Iterator;

public abstract class RoutingLink {
	
	protected RoutingNode fromNode;
	protected RoutingNode toNode;
	
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
		
		reset();
	}
	
	public void reset(){
		this.pendingCost = Double.POSITIVE_INFINITY;
		this.pendingTime = Double.POSITIVE_INFINITY;
		this.previousLink = null;
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
					
					RoutingLink nextLink;
					
					@Override
					public void remove() { throw new UnsupportedOperationException(); }
					
					@Override
					public RoutingLink next() {
						return nextLink;
					}
					
					@Override
					public boolean hasNext() {
						if (base.hasNext()){
							nextLink = base.next();
							if (allowUTurns) return true; //If u-turns are permitted, no need to skip u-turned links
							
							while (nextLink.getToNode() == fromNode){
								if (base.hasNext()){nextLink = base.next();}
								else{ return false;}
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
