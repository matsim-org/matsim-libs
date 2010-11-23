package playground.mzilske.freight;

import java.util.ArrayList;
import java.util.Collection;

public class TSPPlan {
	private Collection<TransportChain> chains = new ArrayList<TransportChain>();
	
	private Double score = null;

	public TSPPlan(Collection<TransportChain> chains) {
		super();
		this.chains = chains;
	}

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}

	public Collection<TransportChain> getChains() {
		return chains;
	}
	
	
}
