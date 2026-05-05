package org.matsim.api.core.v01.messages;

import org.matsim.api.core.v01.Message;

/**
 * Exchanged between simulation start. Holds ordered ids that are synchronized between nodes.
 */
public record StartUpMessage(String[] linkIds, String[] nodeIds, String[] personIds) implements Message {
}
