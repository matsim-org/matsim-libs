package org.matsim.contrib.common.zones.io;

import org.matsim.api.core.v01.Id;

import org.matsim.contrib.common.zones.Zone;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ZoneXmlWriter extends MatsimXmlWriter {
	private final Map<Id<Zone>, Zone> zones;

	public ZoneXmlWriter(Map<Id<Zone>, Zone> zones) {
		this.zones = zones;
	}

	public void write(String file) {
		openFile(file);
		writeDoctype("zones", "http://matsim.org/files/dtd/zones_v1.dtd");
		writeStartTag("zones", Collections.emptyList());
		writeZones();
		writeEndTag("zones");
		close();
	}

	private void writeZones() {
		for (Zone z : zones.values()) {
			List<Tuple<String, String>> atts = Arrays.asList(Tuple.of("id", z.getId().toString()),
					Tuple.of("type", z.getType()));
			writeStartTag("zone", atts, true);
		}
	}
}
