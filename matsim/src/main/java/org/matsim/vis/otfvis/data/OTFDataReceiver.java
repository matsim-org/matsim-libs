package org.matsim.vis.otfvis.data;

import org.matsim.vis.otfvis.caching.SceneGraph;

/**
 * @author dstrippgen
 * The OTFData.Receiver marks classes that can receive Data from the OTFData.Reader
 * The invalidate() method will be called whenever the screen content needs to be updated
 *
 */
public interface OTFDataReceiver {
	public void invalidate(SceneGraph graph);
}