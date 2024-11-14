package org.matsim.dsim.messages;

import lombok.Builder;
import lombok.Data;
import org.matsim.api.core.v01.Message;

import java.util.List;

/**
 * Container holding a vehicle and its driver and passengers.
 */
@Builder(setterPrefix = "set")
@Data
public class VehicleContainer implements Message {

	private final Message vehicle;
	private final Message driver;
	private final List<Message> passengers;

}
