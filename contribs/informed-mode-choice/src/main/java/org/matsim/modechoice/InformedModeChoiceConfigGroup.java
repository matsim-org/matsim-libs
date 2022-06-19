package org.matsim.modechoice;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Config group for informed mode choice. Most options need to be configured via the builder.
 */
public class InformedModeChoiceConfigGroup extends ReflectiveConfigGroup {

	private static final String NAME = "informedModeChoice";

	public final static String CONFIG_PARAM_MODES = "modes";
	public final static String CONFIG_PARAM_TOP_K = "topK";
	public final static String CONFIG_PARAM_CUTOFF = "cutoff";

	/**
	 * The setter ensures, that this class always contains internal string representations.
	 */
	private Set<String> modes = Set.of(TransportMode.car, TransportMode.walk, TransportMode.pt, TransportMode.bike);

	/**
	 * Use kth best trips.
	 */
	private int k = 5;

	/**
	 * Discard solution worse than certain percent.
	 */
	private double cutoff = 0;

	// TODO: some plans have large deviations in score points
	// this threshold may depend on difference between estimated and executed scores
	// probably better to remove this option, and determine such factor during iterations

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
				.collect(Collectors.toSet());
	}

	@StringSetter(CONFIG_PARAM_TOP_K)
	public void setTopK(int k) {
		this.k = k;
	}

	@StringGetter(CONFIG_PARAM_TOP_K)
	public int getTopK() {
		return k;
	}


	@StringSetter(CONFIG_PARAM_CUTOFF)
	public void setCutoff(double cutoff) {
		this.cutoff = cutoff;
	}

	@StringGetter(CONFIG_PARAM_CUTOFF)
	public double getCutoff() {
		return cutoff;
	}

	public Set<String> getModes() {
		return modes;
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(CONFIG_PARAM_MODES, "Defines all modes that are available and open for mode choice.");
		comments.put(CONFIG_PARAM_TOP_K, "Defines how many top k best trips of each category should be generated.");

		return comments;
	}
}
