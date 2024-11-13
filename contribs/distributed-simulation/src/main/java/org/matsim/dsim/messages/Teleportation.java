package org.matsim.dsim.messages;

import lombok.Builder;
import lombok.Getter;
import org.matsim.api.core.v01.Message;

@Builder(setterPrefix = "set")
@Getter
public class Teleportation {

	private final Message personMessage;
	private final double exitTime;

}
