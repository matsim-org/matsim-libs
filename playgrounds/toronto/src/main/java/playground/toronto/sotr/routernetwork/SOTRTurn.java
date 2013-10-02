package playground.toronto.sotr.routernetwork;

/**
 * Represents a turning penalty from one link to another
 * 
 * Similar to a link in structure, a turn is like a 'second-order' link in a 'pseudo-dual' or 'edge-based'
 * graph (hence the name). 
 * 
 * @author pkucirek
 *
 */
public class SOTRTurn {
	
	protected final SOTRLink fromLink;
	protected final SOTRLink toLink;
	
	protected SOTRTurn(final SOTRLink fromLink, final SOTRLink toLink){
		this.fromLink = fromLink;
		this.toLink = toLink;
	}
	
	public SOTRLink getFromLink() { return this.fromLink; }
	public SOTRLink getToLink() { return this.toLink; }
	
}
