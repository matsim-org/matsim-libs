package org.matsim.modechoice;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ByteArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMaps;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.groups.ChangeModeConfigGroup;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Config group for informed mode choice. Most options need to be configured via the builder.
 */
public class InformedModeChoiceConfigGroup extends ReflectiveConfigGroup {

	private static final String NAME = "informedModeChoice";

	public final static String CONFIG_PARAM_MODES = "modes";
	public final static String CONFIG_PARAM_KTH = "kthBest";

	/**
	 * The setter ensures, that this class always contains internal string representations.
	 */
	private Set<String> modes = Set.of(TransportMode.car, TransportMode.walk, TransportMode.pt, TransportMode.bike);

	/**
	 * Use kth best trips.
	 */
	private int k = 5;

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

	@StringSetter(CONFIG_PARAM_KTH)
	public void setK(int k) {
		this.k = k;
	}

	@StringGetter(CONFIG_PARAM_KTH)
	public int getK() {
		return k;
	}

	public Set<String> getModes() {
		return modes;
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(CONFIG_PARAM_MODES, "Defines all modes that are available and open for mode choice.");
		comments.put(CONFIG_PARAM_KTH, "Defines how many kth best trips should be generated.");

		return comments;
	}
}
