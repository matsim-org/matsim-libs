package org.matsim.utils.vis.otfivs.data;

import org.matsim.utils.vis.otfivs.caching.SceneGraph;

public interface OTFData {
	public static interface Receiver {
		public void invalidate(SceneGraph graph);
	}
	public static interface Provider {
	}
}
