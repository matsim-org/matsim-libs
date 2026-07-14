package org.matsim.core.mobsim.dsim;

import org.matsim.api.core.v01.Message;

/**
 * Classes implementing this interface provide messages to be sent across domain boundaries in a distributed simulation. This is here in addition to
 * {@link DistributedMobsimVehicle} and {@link DistributedMobsimAgent}, because those two also tag, vehicles and agents which are ready for the
 * distributed simulation.
 * <p>
 * This interface can be implemented on structures which should also be sent across boundaries, but are not vehicles nor agents.
 * <p>
 * Possibly, merge this with the aforementioned interfaces?
 */
public interface ToMessage {

	Message toMessage();
}
