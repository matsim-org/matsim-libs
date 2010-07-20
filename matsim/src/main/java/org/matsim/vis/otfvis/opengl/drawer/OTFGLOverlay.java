package org.matsim.vis.otfvis.opengl.drawer;

import static javax.media.opengl.GL.GL_VIEWPORT;

import java.io.InputStream;

import javax.media.opengl.GL;

import org.matsim.vis.otfvis.caching.SceneGraph;

import com.sun.opengl.util.texture.Texture;

class OTFGLOverlay extends OTFGLDrawableImpl {
	private final InputStream texture;
	private final float relX;
	private final float relY;
	private final boolean opaque;
	private final float size;
	private Texture t = null;

	OTFGLOverlay(final InputStream texture, float relX, float relY, float size, boolean opaque) {
		this.texture = texture;
		this.relX =relX;
		this.relY = relY;
		this.size = size;
		this.opaque = opaque;
	}

	public void onDraw(GL gl) {
		if(this.t == null) {
			this.t = OTFOGLDrawer.createTexture(this.texture);
		}
		int[] viewport = new int[4];
		gl.glGetIntegerv( GL_VIEWPORT, viewport ,0 );
		float height = this.size*this.t.getHeight()/viewport[3];
		float length = this.size*this.t.getWidth()/viewport[2];
		int z = 0;
		float startX = this.relX >= 0 ? -1.f + this.relX : 1.f -length +this.relX;
		float startY = this.relY >= 0 ? -1.f + this.relY : 1.f -height +this.relY;

		gl.glColor4d(1,1,1,1);

		//push 1:1 screen matrix
		gl.glMatrixMode( GL.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glMatrixMode( GL.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		//drawQuad
		if(!this.opaque) {
			getGl().glEnable(GL.GL_BLEND);
			getGl().glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		}

		this.t.enable();
		this.t.bind();
		gl.glBegin(GL.GL_QUADS);
		gl.glTexCoord2f(0,1); gl.glVertex3f(startX, startY, z);
		gl.glTexCoord2f(1,1); gl.glVertex3f(startX + length, startY, z);
		gl.glTexCoord2f(1,0); gl.glVertex3f(startX + length, startY + height, z);
		gl.glTexCoord2f(0,0); gl.glVertex3f(startX, startY + height, z);
		gl.glEnd();
		//restore old mode
		this.t.disable();
		if(!this.opaque) {
			getGl().glDisable(GL.GL_BLEND);
		}
		gl.glMatrixMode( GL.GL_MODELVIEW);
		gl.glPopMatrix();
		gl.glMatrixMode( GL.GL_PROJECTION);
		gl.glPopMatrix();
	}

	@Override
	public void invalidate(SceneGraph graph) {
	}

}