package org.matsim.vis.otfvis.data;

import org.matsim.vis.otfvis.caching.SceneGraph;

/**
 * The OTFDataReceiver marks classes that can receive Data from the
 * OTFData.Reader The invalidate() method will be called whenever the screen
 * content needs to be updated.
 * <p/>
 * The prime use of invalidate() is that objects outside the screen rectangle are <i>not</i>
 * invalidated and thus not redrawn.  Could also be named "markForRedraw" or similar. kai, feb'11
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