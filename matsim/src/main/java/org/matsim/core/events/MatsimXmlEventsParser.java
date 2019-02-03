package org.matsim.core.events;

import org.matsim.core.utils.io.MatsimXmlParser;

abstract class MatsimXmlEventsParser extends MatsimXmlParser {
	public abstract void addCustomEventMapper( String key, EventsReaderXMLv1.CustomEventMapper value );
}
