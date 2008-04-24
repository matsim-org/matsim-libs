package playground.david.vis.gui;

import playground.david.vis.caching.SceneGraph;

public interface OTFDrawable {
	public void draw();
	public void invalidate(SceneGraph graph);
}
