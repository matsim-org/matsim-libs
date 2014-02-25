/**
 * This package is <i>ancient</i> and correspondingly <i>archaic.</i>  The queue simulation was (evidently) one of the first pieces
 * which had to be in place.  It then got patched up and patched up, for example by becoming more deterministic, including the public transit
 * dynamics, including lanes, including signals, including "additional agents on link", etc. etc.
 * <p>
 * Eventually, the qnetsimengine was carved out of the monolithic mess.  Now the qnetsimengine attempts to provide a somewhat meaningful
 * interface to the outside world, but internally it is still more complex than is good.
 * <p>
 * With respect to that public interface: The QSim needs one simengine which provides a "service network" to other pieces, where for example
 * agents at activities or waiting for pt can be located, in particular for the visualizer.  This "service network" capability could, in principle, be
 * moved to a separate simengine.  This would result in cleaner code ... but the provision for visualization (otfvis) would need to be
 * extracted as well.
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;