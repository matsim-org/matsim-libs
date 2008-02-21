package playground.david.vis.data;

public interface OTFData {
	public static interface Receiver {
		public void invalidate(SceneGraph graph);
	}
	public static interface Provider {
	}
}
