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

/**
 * Config group for informed mode choice. Most options need to be configured via the builder.
 */
public class InformedModeChoiceConfigGroup extends ReflectiveConfigGroup {

	private static final String NAME = "informedModeChoice";

	public final static String CONFIG_PARAM_MODES = "modes";

	private Set<String> modes = Set.of(TransportMode.car, TransportMode.pt, TransportMode.bike);
	private Object2ByteMap<String> mapping = new Object2ByteArrayMap<>();

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
		this.modes = Set.copyOf(Sets.newHashSet(modes));

		Object2ByteMap<String> map = new Object2ByteArrayMap<>();
		byte b = 1;
		for (String mode : modes) {
			map.put(mode, b);
			b++;
		}

		mapping = Object2ByteMaps.unmodifiable(map);
	}

	// TODO: rather use .intern() mode string instead
	/**
	 * Return unmodifiable mapping of modes to byte value.
	 */
	public Object2ByteMap<String> getMapping() {
		return mapping;
	}

	public Set<String> getModes() {
		return modes;
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(CONFIG_PARAM_MODES, "Defines all modes that are available and open for mode choice.");

		return comments;
	}
}
