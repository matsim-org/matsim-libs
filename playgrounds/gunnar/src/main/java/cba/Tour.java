package cba;

import org.matsim.api.core.v01.network.Link;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class Tour {

	static enum Act {
		home, work, other
	};

	static enum Mode {
		car, pt
	};

	final Link destination;

	final Act act;

	final Mode mode;

	Tour(final Link destination, final Act act, final Mode mode) {
		this.destination = destination;
		this.act = act;
		this.mode = mode;
	}
}
