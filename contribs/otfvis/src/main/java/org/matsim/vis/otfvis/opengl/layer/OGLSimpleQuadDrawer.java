/**
 * 
 */
package org.matsim.vis.otfvis.opengl.layer;

import java.awt.geom.Point2D;
import java.util.Map;

import com.jogamp.opengl.GL2;

import org.matsim.api.core.v01.Coord;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawableReceiver;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import com.jogamp.opengl.util.texture.TextureCoords;

/**
 * This sounds like "quad tree", but I think it is in the sense of "polygon with 4 corners".  kai, feb'11  
 */
public class OGLSimpleQuadDrawer extends OTFGLAbstractDrawableReceiver {

	protected final Point2D.Float[] quad = new Point2D.Float[4];
	protected float coloridx = 0;
	protected char[] id;
	protected int nrLanes;
	private SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();

	@Override
	public void onDraw( GL2 gl) {
		linkWidthCalculator.setLaneWidth(OTFClientControl.getInstance().getOTFVisConfig().getEffectiveLaneWidth());
		linkWidthCalculator.setLinkWidthForVis(OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth());
		float width = (float) linkWidthCalculator.calculateLinkWidth(this.nrLanes);
		final Point2D.Float ortho = calcOrtho(this.quad[0].x, this.quad[0].y, this.quad[1].x, this.quad[1].y, 
				width);
		// (yy this is where the width of the links for drawing is set)

		this.quad[2] = new Point2D.Float(this.quad[0].x + ortho.x, this.quad[0].y + ortho.y);
		this.quad[3] = new Point2D.Float(this.quad[1].x + ortho.x, this.quad[1].y + ortho.y);
		//Draw quad
		TextureCoords co = new TextureCoords(0,0,1,1);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(co.right(),co.bottom()); gl.glVertex3f(quad[0].x, quad[0].y, 0);
		gl.glTexCoord2f(co.right(),co.top()); gl.glVertex3f(quad[1].x, quad[1].y, 0);
		gl.glTexCoord2f(co.left(), co.top()); gl.glVertex3f(quad[3].x, quad[3].y, 0);
		gl.glTexCoord2f(co.left(),co.bottom()); gl.glVertex3f(quad[2].x, quad[2].y, 0);
		gl.glEnd();
	}

	public void prepareLinkId(Map<Coord, String> linkIds) {
		double alpha = 0.4;
		double middleX = alpha*this.quad[0].x + (1.0-alpha)*this.quad[3].x;
		double middleY = alpha*this.quad[0].y + (1.0-alpha)*this.quad[3].y;
		String idstr = "" ;
		if ( id != null ) { // yyyy can't say if this is a meaningful fix but it works for the problem that I have right now.  kai, may'10
			idstr = new String(id);
		}
		linkIds.put(new Coord(middleX, middleY), idstr);
	}

	public static Point2D.Float calcOrtho(double startx, double starty, double endx, double endy, double len){
		double dx = endy - starty;
		double dy = endx -startx;
		double sqr1 = Math.sqrt(dx*dx +dy*dy);

		dx = dx*len/sqr1;
		dy = -dy*len/sqr1;

		return new Point2D.Float((float)dx,(float)dy);
	}

	public void setQuad(float startX, float startY, float endX, float endY) {
		setQuad(startX, startY,endX, endY, 1);
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
		graph.addStaticItem(this);
	}
	
}