package org.matsim.api.core.v01.events;

import org.matsim.api.core.v01.Message;

import java.util.Comparator;

/**
 * Comparator for messages and events.
 */
public class MessageComparator implements Comparator<Message> {

	public static final MessageComparator INSTANCE = new MessageComparator();

	@Override
	public int compare(Message o1, Message o2) {
		if (o1 instanceof Event e1 && o2 instanceof Event e2) {
			return Double.compare(e1.getTime(), e2.getTime());
		}

		return 0;
	}
}
