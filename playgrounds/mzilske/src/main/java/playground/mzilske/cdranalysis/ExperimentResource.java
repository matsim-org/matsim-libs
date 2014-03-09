package playground.mzilske.cdranalysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ExperimentResource {
	
	private String wd;
	
	final String BASE = "/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/";

	public Collection<String> getRegimes() {
		final Set<String> REGIMES = new HashSet<String>();
		REGIMES.add("uncongested");
		REGIMES.add("congested");
		return REGIMES;
	}
	
	public MultiRateRunResource getRegime(String regime) {
		return new MultiRateRunResource(wd, regime);
	}
	
}
