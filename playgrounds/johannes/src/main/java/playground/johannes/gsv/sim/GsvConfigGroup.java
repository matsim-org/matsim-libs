package playground.johannes.gsv.sim;

import org.matsim.core.config.ReflectiveConfigGroup;

public class GsvConfigGroup extends ReflectiveConfigGroup {
	public static final String GSV_CONFIG_MODULE_NAME = "gsv";

	private boolean replanCandidates;
	private boolean disableCadyts;
	private String countsfile;
	private String attributesFile;
	private double mutationError;
	private double distThreshold;

	public GsvConfigGroup() {
		super(GSV_CONFIG_MODULE_NAME, true);
	}

	@StringGetter("replanCandidates")
	public boolean isReplanCandidates() {
		return replanCandidates;
	}

	@StringSetter("replanCandidates")
	public void setReplanCandidates(boolean replanCandidates) {
		this.replanCandidates = replanCandidates;
	}

	@StringGetter("disableCadyts")
	public boolean isDisableCadyts() {
		return disableCadyts;
	}

	@StringSetter("disableCadyts")
	public void setDisableCadyts(boolean disableCadyts) {
		this.disableCadyts = disableCadyts;
	}

	@StringGetter("countsfile")
	public String getCountsfile() {
		return countsfile;
	}

	@StringSetter("countsfile")
	public void setCountsfile(String countsfile) {
		this.countsfile = countsfile;
	}

	@StringGetter("attributesFile")
	public String getAttributesFile() {
		return attributesFile;
	}

	@StringSetter("attributesFile")
	public void setAttributesFile(String attributesFile) {
		this.attributesFile = attributesFile;
	}

	@StringGetter("mutationError")
	public double getMutationError() {
		return mutationError;
	}

	@StringSetter("mutationError")
	public void setMutationError(double mutationError) {
		this.mutationError = mutationError;
	}

	@StringGetter("distThreshold")
	public double getDistThreshold() {
		return distThreshold;
	}

	@StringSetter("distThreshold")
	public void setDistThreshold(double distThreshold) {
		this.distThreshold = distThreshold;
	}
}
