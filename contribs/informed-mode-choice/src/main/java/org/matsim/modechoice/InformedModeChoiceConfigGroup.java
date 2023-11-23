package org.matsim.modechoice;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Config group for informed mode choice. Most options need to be configured via the builder.
 */
public class InformedModeChoiceConfigGroup extends ReflectiveConfigGroup {

	private static final String NAME = "informedModeChoice";

	public final static String CONFIG_PARAM_MODES = "modes";

	/**
	 * The setter ensures, that this class always contains internal string representations.
	 */
	private List<String> modes = List.of(TransportMode.car, TransportMode.walk, TransportMode.pt, TransportMode.bike);

	@Parameter
	@Comment("Defines how many top k best trips of each category should be generated.")
	private int topK = 5;

	@Parameter
	@Comment("1/beta parameter to trade-off of exploration for alternatives. Parameter of 0 is equal to best choice.")
	private double invBeta = 2.5;

	@Parameter
	@Comment("Name of the candidate pruner to apply, needs to be bound with guice.")
	private String pruning = null;

	@Parameter
	@Comment("Annealing for the invBeta parameter.")
	private Schedule anneal = Schedule.quadratic;

	@Parameter
	@Comment("Require that new plan modes are always different from the current one.")
	private boolean requireDifferentModes = true;

	@Parameter
	@Comment("Defines how constraint violations are handled.")
	private ConstraintCheck constraintCheck = ConstraintCheck.abort;

	@Parameter
	@Comment("Probability to re-estimate an existing plan model.")
	private double probaEstimate = 0.1;

	public InformedModeChoiceConfigGroup() {
		super(NAME);
	}

	@StringSetter(CONFIG_PARAM_MODES)
	private void setModes(final String value) {
		setModes(Splitter.on(",").split(value));
	}

	@StringGetter(CONFIG_PARAM_MODES)
	private String getStringModes() {
		return Joiner.on(",").join(modes);
	}

	public void setModes(Iterable<String> modes) {
		this.modes = StreamSupport.stream(modes.spliterator(), false)
				.map(String::intern)
				.distinct()
				.collect(Collectors.toList());
	}

	public void setTopK(int topK) {
		this.topK = topK;
	}

	public int getTopK() {
		return topK;
	}

	public double getInvBeta() {
		return invBeta;
	}

	public void setInvBeta(double invBeta) {
		this.invBeta = invBeta;
	}


	public String getPruning() {
		return pruning;
	}

	public void setPruning(String pruning) {
		this.pruning = pruning;
	}

	public Schedule getAnneal() {
		return anneal;
	}

	public void setAnneal(Schedule anneal) {
		this.anneal = anneal;
	}

	public double getProbaEstimate() {
		return probaEstimate;
	}

	public void setProbaEstimate(double probaEstimate) {
		this.probaEstimate = probaEstimate;
	}

	public ConstraintCheck getConstraintCheck() {
		return constraintCheck;
	}

	public void setConstraintCheck(ConstraintCheck constraintCheck) {
		this.constraintCheck = constraintCheck;
	}

	public List<String> getModes() {
		return modes;
	}

	public void setRequireDifferentModes(boolean requireDifferentModes) {
		this.requireDifferentModes = requireDifferentModes;
	}

	public boolean isRequireDifferentModes() {
		return requireDifferentModes;
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(CONFIG_PARAM_MODES, "Defines all modes that are available and open for mode choice.");
		return comments;
	}

	public enum Schedule {
		off,
		linear,
		quadratic,
		cubic,
		exponential,
		trigonometric
	}

	public enum ConstraintCheck {
		none,
		warn,
		abort,
		repair
	}
}
