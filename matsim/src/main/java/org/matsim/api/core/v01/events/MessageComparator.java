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
			int byTime = Double.compare(e1.getTime(), e2.getTime());
			if (byTime != 0)
				return byTime;

			// Events of one time step must not be left in the order their producing threads happened to hand them over. Where the producer
			// is known, order by it and then by its counter; this is a total order, and it is the same in every run.
			int byPartition = Integer.compare(e1.getSourcePartition(), e2.getSourcePartition());
			if (byPartition != 0)
				return byPartition;

			return Integer.compare(e1.getSourceSequence(), e2.getSourceSequence());
		}

		return 0;
	}
}
