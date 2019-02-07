package org.matsim.contrib.zone.io;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

public class ZoneXmlWriter extends MatsimXmlWriter {
	private Map<Id<Zone>, Zone> zones;

	public ZoneXmlWriter(Map<Id<Zone>, Zone> zones) {
		this.zones = zones;
	}

	public void write(String file) {
		openFile(file);
		writeDoctype("zones", "http://matsim.org/files/dtd/zones_v1.dtd");
		writeStartTag("zones", Collections.<Tuple<String, String>>emptyList());
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
