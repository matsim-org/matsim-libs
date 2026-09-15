package org.matsim.api.core.v01.messages;

import org.matsim.api.core.v01.Message;

/**
 * Singleton instance of the {@link Message} interface to represent empty messages.
 */
public class EmptyMessage implements Message {

	public static final EmptyMessage INSTANCE = new EmptyMessage();

}
