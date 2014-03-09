package playground.mzilske.cdranalysis;

public class RunResource {

	private String wd;

	public RunResource(String wd) {
		this.wd = wd;
	}

	public IterationResource getIteration(int iteration) {
		return new IterationResource(wd + "/ITERS/it."+iteration, iteration);
	}

}
