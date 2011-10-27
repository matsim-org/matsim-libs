/* *********************************************************************** *
 * project: org.matsim.*
 * MateProposer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.thibautd.agentsmating.logitbasedmating.framework;

import java.util.List;

/**
 * Class which selects possible mates, to choose from based on some model.
 *
 * @author thibautd
 */
public interface MateProposer {
	/**
	 * @param trip the trip for which a mate is searched
	 * @param allPossibleMates a list of all requests corresponding to the possible
	 * mates, from the mate perspective.
	 *
	 * @return a list of mates proposals, from the mate perspective. This should
	 * be a sublist of allPossibleMates.
	 *
	 * @throws UnhandledMatingDirectionException if the direction (affecting drivers to passengers
	 * or passengers to driver) is not handled. This must be thrown for one direction
	 * at most, and always the same.
	 */
	public <T extends TripRequest> List<T> proposeMateList(
			TripRequest trip,
			List<T> allPossibleMates) throws UnhandledMatingDirectionException;

	public class UnhandledMatingDirectionException extends Exception {
		private static final long serialVersionUID = 1L;

		public UnhandledMatingDirectionException() {
			super();
		}

		public UnhandledMatingDirectionException(final String msg) {
			super(msg);
		}

		public UnhandledMatingDirectionException(final String msg, final Throwable cause) {
			super(msg, cause);
		}

		public UnhandledMatingDirectionException(final Throwable cause) {
			super(cause);
		}
	}
}

