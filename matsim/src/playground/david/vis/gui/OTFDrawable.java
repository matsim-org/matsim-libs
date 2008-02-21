package playground.david.vis.gui;

import playground.david.vis.data.SceneGraph;

public interface OTFDrawable {
	public void draw();
	public void invalidate(SceneGraph graph);
}
