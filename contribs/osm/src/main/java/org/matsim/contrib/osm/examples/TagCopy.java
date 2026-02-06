package org.matsim.contrib.osm.examples;

import org.matsim.api.core.v01.network.Link;
import java.util.List;
import java.util.Map;

public final class TagCopy {
	private final List<String> keys;
	private final String prefix;

	public TagCopy(List<String> keys, String prefix) {
		this.keys = keys;
		this.prefix = prefix;
	}

	public void copy(Link link, Map<String,String> tags) {
		for (String k : keys) {
			String v = tags.get(k);
			if (v != null && !v.isBlank()) {
				link.getAttributes().putAttribute(prefix + k, v);
			}
		}
	}
}

