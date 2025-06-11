package org.matsim.modechoice;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import jakarta.validation.constraints.PositiveOrZero;
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

	public final static String CONFIG_PARAM_MODES = "modes";
	private static final String NAME = "informedModeChoice";
	/**
	 * The setter ensures, that this class always contains internal string representations.
	 */
	private List<String> modes = List.of(TransportMode.car, TransportMode.walk, TransportMode.pt, TransportMode.bike);

	@Parameter
	@Comment("Defines how many top k best trips of each category should be generated.")
	private int topK = 5;

	@Parameter
	@PositiveOrZero
	@Comment("1/beta parameter (or temperature tau) to trade-off of exploration for alternatives. Parameter of 0 is equal to best choice." +
		" POSITIVE_INFINITY will select randomly.")
	private double invBeta = Double.POSITIVE_INFINITY;

	@Parameter
	@Comment("Normalize utility values when selecting")
	private boolean normalizeUtility = false;

	@Parameter
	@Comment("Name of the candidate pruner to apply, needs to be bound with guice.")
	private String pruning = null;

	@Parameter
	@Comment("Annealing for the invBeta parameter.")
	private Schedule anneal = Schedule.off;

	@Parameter
	@Comment("Require that new plan modes are always different from the current one.")
	private boolean requireDifferentModes = false;

	@Parameter
	@Comment("Defines how constraint violations are handled.")
	private ConstraintCheck constraintCheck = ConstraintCheck.abort;

	@Parameter
	@PositiveOrZero
	@Comment("Probability to drop all estimates for a plan.")
	private double probaEstimate = 0;

	public InformedModeChoiceConfigGroup() {
		super(NAME);
	}

	@StringGetter(CONFIG_PARAM_MODES)
	private String getStringModes() {
		return Joiner.on(",").join(modes);
	}

	public int getTopK() {
		return topK;
	}

	public void setTopK(int topK) {
		this.topK = topK;
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

	@StringSetter(CONFIG_PARAM_MODES)
	private void setModes(final String value) {
		setModes(Splitter.on(",").split(value));
	}

	public void setModes(Iterable<String> modes) {
		this.modes = StreamSupport.stream(modes.spliterator(), false)
			.map(String::intern)
			.distinct()
			.collect(Collectors.toList());
	}

	public boolean isRequireDifferentModes() {
		return requireDifferentModes;
	}

	public void setRequireDifferentModes(boolean requireDifferentModes) {
		this.requireDifferentModes = requireDifferentModes;
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(CONFIG_PARAM_MODES, "Defines all modes that are available and open for mode choice.");
		return comments;
	}

	public boolean isNormalizeUtility() {
		return normalizeUtility;
	}

	public void setNormalizeUtility(boolean normalizeUtility) {
		this.normalizeUtility = normalizeUtility;
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
