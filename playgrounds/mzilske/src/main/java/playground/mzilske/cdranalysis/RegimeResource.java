package playground.mzilske.cdranalysis;

public class RegimeResource {

	private String WD;

	private String regime;

	public RegimeResource(String wd, String regime) {
		this.WD = wd;
		this.regime = regime;
	}

	public MultiRateRunResource getMultiRateRun(String alternative) {
		return new MultiRateRunResource(WD + "/alternatives/" + alternative, regime, alternative);
	}
	
	public RunResource getBaseRun() {
		return new RunResource(WD + "/output-berlin", "2kW.15");
	}
	

}
