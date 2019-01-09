package org.matsim.contrib.carsharing.relocation.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.Attributes;

public class RelocationTimesReader extends MatsimXmlParser {
	private Map<String, List<Double>> relocationTimes;

	private String companyId;

	private Counter counter;

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if ( name.equals("relocationTimes" ) ) {
			this.relocationTimes = new HashMap<String, List<Double>>();

			counter = new Counter( "reading car sharing relocation time # " );
		}

		if (name.equals("company")) {
			this.companyId = atts.getValue("name");
			this.relocationTimes.put(this.companyId, new ArrayList<Double>());
		}

		if ( name.equals( "relocationTime" ) ) {
			counter.incCounter();
			this.relocationTimes.get(this.companyId).add(Time.parseTime(atts.getValue( "start_time" )));
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if ( name.equals( "relocationTimes" ) ) {
			counter.printCounter();
		}
	}

	public Map<String, List<Double>> getRelocationTimes() {
		return this.relocationTimes;
	}
}
