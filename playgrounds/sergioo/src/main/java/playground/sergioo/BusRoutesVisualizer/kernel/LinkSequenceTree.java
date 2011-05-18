package playground.sergioo.BusRoutesVisualizer.kernel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.network.Link;

public class LinkSequenceTree {
	private List<Link> linkSequence;
	private Collection<LinkSequenceTree> nextSequences;
	/**
	 * @param linkSequence
	 */
	public LinkSequenceTree(List<Link> linkSequence) {
		super();
		this.linkSequence = linkSequence;
		nextSequences = new ArrayList<LinkSequenceTree>();
	}
	/**
	 * @return the nextSequences
	 */
	public Collection<LinkSequenceTree> getNextSequences() {
		return nextSequences;
	}
	/**
	 * @return the linkSequence
	 */
	public List<Link> getLinkSequence() {
		return linkSequence;
	}
	
}
