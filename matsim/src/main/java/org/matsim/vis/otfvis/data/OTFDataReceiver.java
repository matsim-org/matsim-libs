package org.matsim.vis.otfvis.data;

import org.matsim.vis.otfvis.caching.SceneGraph;

/**
 * The OTFDataReceiver marks classes that can receive Data from the
 * OTFData.Reader The invalidate() method will be called whenever the screen
 * content needs to be updated.
 * <p/>
 * There is, however, no standardized method for the receive operation, and so imo
 * the intent does not become very clear ... the "receiver" part ends up being a
 * marker interface, with its actual implementations elsewhere (and specific to
 * the data type: addAgent, updateGreenState, ...).
 * <p/>
 * Also, I (kn) do not fully understand the invalidate method. It seems to me
 * that, once the receiver receives something, it should invalidate itself, so
 * it is not clear to me why this is called from the outside. Also, actual
 * implementations of invalidate(...) add "this" to the SceneGraph, at least in
 * one case into an ArrayList, so calling the invalidate operation more than
 * once per actual invalidation adds the same drawer more than once.
 * <p/>
 * Design thoughts:<ul>
 * <li> This interface is at many places.  What it certifies, however, seems to be a "ReceiverAndDrawer".
 * Should possibly be replaced.
 * <li> Although the inheritance diagram might get more confused, it might make sense to do as follows:<ul>
 * <li> OTFDrawableReceiver extends OTFDataReceiver, but also specialized non-drawable receiver interfaces:
 * <li> OTFAgentReceiver extends OTFDataReceiver etc.
 * </ul> 
 * <li> To my intuition, would make more sense to have "invalidate" with the drawer.  Then the receiver would tell the drawer
 * to become invalid, i.e. to eventually re-schedule itself for redraw.  In the receiver, it does not seem to make that
 * much sense.
 * </ul>
 * 
 * @author dstrippgen
 */
public interface OTFDataReceiver {

	/**
	 * Marks a graphics element for re-draw.  This is mostly (if not entirely) important because of the viewing rectangle
	 * on the client side: Only elements within that rectangle need to be re-drawn.  One may imagine every agent, every link, ...
	 * being an OTFDataReceiver.  Only those within the rectangle are invalidated, and in some of the implementations calling
	 * invalidate(...) means that they schedule themselves for re-draw with the SceneGraph.
	 * 
	 */
	public void invalidate(SceneGraph graph);
}