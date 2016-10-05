/**
 * 
 */
package org.matsim.vis.otfvis.opengl.layer;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.TextureCoords;
import org.matsim.api.core.v01.Coord;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawableReceiver;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import java.awt.geom.Point2D;
import java.util.Map;

/**
 * This sounds like "quad tree", but I think it is in the sense of "polygon with 4 corners".  kai, feb'11  
 */
public class OGLSimpleLineDrawer extends OTFGLAbstractDrawableReceiver {

	 private final Point2D.Float[] quad = new Point2D.Float[4];
	 private float coloridx = 0;
	 private char[] id;
	 private int nrLanes;
	private SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();

	@Override
	public void onDraw( GL2 gl) {
		gl.glLineWidth(1);
		gl.glBegin(GL2.GL_LINES);
		gl.glVertex2f(quad[0].x, quad[0].y);
		gl.glVertex2f(quad[1].x, quad[1].y);
		gl.glEnd();
	}

	public void setQuad(float startX, float startY, float endX, float endY, int nrLanes) {
		this.quad[0] = new Point2D.Float(startX, startY);
		this.quad[1] = new Point2D.Float(endX, endY);
		this.nrLanes = nrLanes;
	}

	public void setColor(float coloridx) {
		this.coloridx = coloridx;
	}

	public void setId(char[] id) {
		this.id = id;
	}
	
	@Override
	public void addToSceneGraph(SceneGraph graph) {
	}
	
}