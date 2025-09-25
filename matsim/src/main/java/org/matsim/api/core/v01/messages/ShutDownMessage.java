package org.matsim.api.core.v01.messages;

import org.matsim.api.core.v01.Message;

/**
 * Exchanged to coordinate the shutdown of multiple nodes.
 */
public record ShutDownMessage() implements Message {
}
