package playground.david.vis.gui;

import java.awt.geom.Point2D;

import org.matsim.utils.collections.QuadTree.Rect;

public interface OTFEventHandler {

	public void invalidate(Rect rect);
	public void redraw();
	public void handleClick(Point2D.Double point);
}
