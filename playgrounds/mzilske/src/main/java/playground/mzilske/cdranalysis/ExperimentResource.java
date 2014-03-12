package playground.mzilske.cdranalysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ExperimentResource {
	
	private final String wd;
	
	public ExperimentResource(String wd) {
		this.wd = wd;
	}
	
	public Collection<String> getRegimes() {
		final Set<String> REGIMES = new HashSet<String>();
		REGIMES.add("uncongested");
		REGIMES.add("congested");
		return REGIMES;
	}
	
	public MultiRateRunResource getRegime(String regime) {
		return new MultiRateRunResource(wd + "regimes/" + regime, regime);
	}
	
}
