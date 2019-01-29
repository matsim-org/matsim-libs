package org.matsim.vis.otfvis.opengl.drawer;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.opengl.gl.GLUtils;


class OTFGLOverlay implements GLEventListener {
	private final float relX;
	private final float relY;
	private final boolean opaque;
	private final float size;
	private int w;
	private int h;
	private String textureResourcePath;
	private Texture texture;

	OTFGLOverlay(final String textureResourcePath, float relX, float relY, float size, boolean opaque) {
		this.textureResourcePath = textureResourcePath;
		this.relX = relX;
		this.relY = relY;
		this.size = size;
		this.opaque = opaque;
	}

	@Override
	public void init(GLAutoDrawable glAutoDrawable) {
		GL2 gl = (GL2) glAutoDrawable.getGL();
		texture = GLUtils.createTexture(gl, getClass().getResourceAsStream(textureResourcePath));
		h = texture.getHeight();
		w = texture.getWidth();
	}

	@Override
	public void display(GLAutoDrawable glAutoDrawable) {
		if(OTFClientControl.getInstance().getOTFVisConfig().drawOverlays()) {
			GL2 gl = (GL2) glAutoDrawable.getGL();
			int[] viewport = new int[4];
			gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);
			float height = this.size * h / viewport[3];
			float width = this.size * w / viewport[2];
			int z = 0;
			float startX = this.relX >= 0 ? -1.f + this.relX : 1.f - width + this.relX;
			float startY = this.relY >= 0 ? -1.f + this.relY : 1.f - height + this.relY;

			gl.glColor4d(1, 1, 1, 1);

			//push 1:1 screen matrix
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPushMatrix();
			gl.glLoadIdentity();
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glPushMatrix();
			gl.glLoadIdentity();

			if (!this.opaque) {
				gl.glEnable(GL2.GL_BLEND);
				gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
			}

			texture.enable(gl);
			texture.bind(gl);
			TextureCoords tc = texture.getImageTexCoords();
			gl.glBegin(GL2.GL_QUADS);
			// In these lines, 'top' and 'bottom' must be flipped since the switch to jogl 2.3.2
			// for no apparent reason.
			gl.glTexCoord2f(tc.left(), tc.bottom());
			gl.glVertex3f(startX, startY, z);
			gl.glTexCoord2f(tc.right(), tc.bottom());
			gl.glVertex3f(startX + width, startY, z);
			gl.glTexCoord2f(tc.right(), tc.top());
			gl.glVertex3f(startX + width, startY + height, z);
			gl.glTexCoord2f(tc.left(), tc.top());
			gl.glVertex3f(startX, startY + height, z);
			gl.glEnd();
			texture.disable(gl);
			if (!this.opaque) {
				gl.glDisable(GL2.GL_BLEND);
			}
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glPopMatrix();
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPopMatrix();
		}
	}

	@Override
	public void dispose(GLAutoDrawable glAutoDrawable) {

	}

	@Override
	public void reshape(GLAutoDrawable glAutoDrawable, int i, int i1, int i2, int i3) {

	}
}