package playground.gregor.sim2d.otfdebug.drawer;


import static javax.media.opengl.GL.GL_QUADS;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLDrawableImpl;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;

public class Agent2DDrawer extends OTFGLDrawableImpl{

	
	private final List<Agent> agents = new ArrayList<Agent>();
	private Texture texture_r;
	private Texture texture_l;

	public void onDraw(GL gl) {
		if (this.texture_r == null) {
			this.texture_r = OTFOGLDrawer.createTexture(MatsimResource.getAsInputStream("ped_r.png"));
		}
		if (this.texture_l == null) {
			this.texture_l = OTFOGLDrawer.createTexture(MatsimResource.getAsInputStream("ped_l.png"));
		}
		
		final TextureCoords tc = this.texture_r.getImageTexCoords();
		
		final float tx1 = tc.left();
		final float ty1 = tc.top();
		final float tx2 = tc.right();
		final float ty2 = tc.bottom();
		
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		

		gl.glColor4f(1.f,1,1,1);

		for (Agent a : this.agents) {
			boolean right = MatsimRandom.getRandom().nextBoolean();
//	        gl.glLoadIdentity();                                // Reset The Current Modelview Matrix
//	        gl.glRotatef(20, 0, 0, 1);           // Rotate On The X Axis
			
			if (right) {
				this.texture_r.enable();
				this.texture_r.bind();
			} else {
				this.texture_l.enable();
				this.texture_l.bind();			
			}

			gl.glPushMatrix();
			gl.glTranslatef(a.x, a.y,0); //center of square
			gl.glRotatef( a.azimuth, 0.0f, 0.0f, 1.0f );
			
			gl.glBegin(GL_QUADS);
//			gl.glTexCoord2f(tx1, ty1); gl.glVertex3f(a.x-.355f, a.y-.13f,0.f);
//			gl.glTexCoord2f(tx2, ty1); gl.glVertex3f(a.x+.355f,a.y-.13f,0.f);
//			gl.glTexCoord2f(tx2, ty2); gl.glVertex3f(a.x+.355f,a.y+.13f,0.f);
//			gl.glTexCoord2f(tx1, ty2); gl.glVertex3f(a.x-.355f,a.y+.13f,0.f);
			gl.glTexCoord2f(tx1, ty1); gl.glVertex3f(-.355f,-.18f,0.f);
			gl.glTexCoord2f(tx2, ty1); gl.glVertex3f(.355f,-.18f,0.f);
			gl.glTexCoord2f(tx2, ty2); gl.glVertex3f(.355f,.18f,0.f);
			gl.glTexCoord2f(tx1, ty2); gl.glVertex3f(-.355f,.18f,0.f);
			gl.glEnd();
			gl.glPopMatrix();
			if (right) {
				this.texture_r.disable();
			} else {
				this.texture_l.disable();
			}

		}

		gl.glDisable(GL.GL_BLEND);
	}

	public void addAgent(float x, float y, float azimuth, String id) {
				Agent a = new Agent();
				a.x = x;
				a.y = y;
				a.azimuth = azimuth;
				a.id = id;
				this.agents.add(a);
	}
	
	private static class Agent {
		float x;
		float y;
		float azimuth;
		String id;
	}
	
	
}
