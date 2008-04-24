package playground.david.vis.data;

import playground.david.vis.caching.SceneGraph;

public interface OTFData {
	public static interface Receiver {
		public void invalidate(SceneGraph graph);
	}
	public static interface Provider {
	}
}
