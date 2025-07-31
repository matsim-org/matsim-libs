package org.matsim.contrib.profiling.analysis;

import jdk.jfr.Event;
import jdk.jfr.Name;

public class JfrEventUtils {

	private JfrEventUtils() {}

	public static String getEventName(Class<? extends Event> event) {
		var nameAnnotation = event.getAnnotation(Name.class);
		if (nameAnnotation != null) {
			return nameAnnotation.value();
		}
		return event.getName();
	}

}
