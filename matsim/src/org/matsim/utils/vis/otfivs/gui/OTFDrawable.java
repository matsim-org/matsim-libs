package org.matsim.utils.vis.otfivs.gui;

import org.matsim.utils.vis.otfivs.caching.SceneGraph;

public interface OTFDrawable {
	public void draw();
	public void invalidate(SceneGraph graph);
}
