/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.config;

/**
 * Policy for assigning an incident to a remote guidance operator. An incident occupies exactly one operator for its
 * whole duration (no concurrent incidents per operator, no multiple operators per incident); if no operator is free the
 * incident queues and the vehicle keeps holding. The policy only decides <em>which</em> free operator is picked.
 * <p>
 * Note that incident processing is decoupled from passive supervision: an operator busy on an incident is still counted
 * as passively supervising its vehicles. In particular the operator processing an incident need not be the one the
 * affected vehicle is passively bound to.
 *
 * @author nkuehnel / MOIA
 */
public enum IncidentAssignmentPolicy {
	/**
	 * Assign the incident to a uniformly random operator among those currently free (the default, per Lion 2026-07-12).
	 */
	RANDOM_FREE,
	/**
	 * Assign the incident to the free operator currently handling the fewest incidents (load balancing).
	 */
	LEAST_LOADED
}
