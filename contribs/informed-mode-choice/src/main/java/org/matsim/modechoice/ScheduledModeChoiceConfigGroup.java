package org.matsim.modechoice;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.*;

/**
 * Config group for {@link ScheduledModeChoiceModule}.
 */
public class ScheduledModeChoiceConfigGroup extends ReflectiveConfigGroup {

	private static final String NAME = "scheduledModeChoice";
	private final List<ModeTargetParameters> modeTargetParameters = new ArrayList<>();
	@Parameter
	@PositiveOrZero
	@Comment("Initial iterations before schedule starts")
	private int warumUpIterations = 5;
	@Parameter
	@Positive
	@Comment("Number of iterations to be scheduled")
	private int scheduleIterations = 24;
	@Parameter
	@PositiveOrZero
	@Comment("Number of iterations between scheduled mode choice")
	private int betweenIterations = 1;

	@Parameter
	@Positive
	@Comment("Target number of trip modes that should change between iterations")
	private double targetSwitchShare = 0.15;

	@Parameter
	@Comment("Enable scheduled mode choice for these subpopulations by adjusting strategy settings.")
	private Set<String> subpopulations = new HashSet<>();

	@Parameter
	@Comment("Automatically adjust number of iterations according to schedule and innovation switch off.")
	private boolean adjustTargetIterations = false;

	public ScheduledModeChoiceConfigGroup() {
		super(NAME);
	}

	public int getWarumUpIterations() {
		return warumUpIterations;
	}

	public void setWarumUpIterations(int warumUpIterations) {
		this.warumUpIterations = warumUpIterations;
	}

	public int getScheduleIterations() {
		return scheduleIterations;
	}

	public void setScheduleIterations(int scheduleIterations) {
		this.scheduleIterations = scheduleIterations;
	}

	public int getBetweenIterations() {
		return betweenIterations;
	}

	public void setBetweenIterations(int betweenIterations) {
		this.betweenIterations = betweenIterations;
	}

	public Set<String> getSubpopulations() {
		return subpopulations;
	}

	public void setSubpopulations(String... subpopulations) {
		this.subpopulations = new HashSet<>(Arrays.asList(subpopulations));
	}
	public void setSubpopulations(Set<String> subpopulations) {
		this.subpopulations = subpopulations;
	}

	public double getTargetSwitchShare() {
		return targetSwitchShare;
	}

	public boolean isAdjustTargetIterations() {
		return adjustTargetIterations;
	}

	public void setAdjustTargetIterations(boolean adjustTargetIterations) {
		this.adjustTargetIterations = adjustTargetIterations;
	}

	public void setTargetSwitchShare(double targetSwitchShare) {
		this.targetSwitchShare = targetSwitchShare;
	}

	/**
	 * Return the defined mode target parameters.
	 */
	public List<ModeTargetParameters> getModeTargetParameters() {
		return Collections.unmodifiableList(modeTargetParameters);
	}

	/**
	 * Clear all defined mode target parameters.
	 */
	public void clearModeTargetParameters() {
		modeTargetParameters.clear();
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		if (type.equals(ModeTargetParameters.GROUP_NAME)) {
			return new ModeTargetParameters();
		} else {
			throw new IllegalArgumentException("Unsupported parameter set type: " + type);
		}
	}

	@Override
	public void addParameterSet(ConfigGroup set) {
		if (set instanceof ModeTargetParameters p) {
			super.addParameterSet(set);
			modeTargetParameters.add(p);
		} else {
			throw new IllegalArgumentException("Unsupported parameter set class: " + set);
		}
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);

		if (imc.getTopK() > scheduleIterations) {
			throw new IllegalArgumentException(String.format(
				"Top-K (%d) must be less or equal to schedule iterations (%d).", imc.getTopK(), scheduleIterations)
			);
		}
	}
}
