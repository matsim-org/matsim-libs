package playground.gregor.sim2d.otfdebug.drawer;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import org.matsim.vis.otfvis.opengl.drawer.OTFGLDrawableImpl;

public class ForceArrowDrawer  extends OTFGLDrawableImpl  {

	List<float[]> forces = new ArrayList<float[]>();

	public void onDraw(GL gl) {

		gl.glLineWidth(4.f);
		
		for (float [] force : this.forces) {
			gl.glColor4f(force[4],force[5],force[6],.7f);
			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3f(force[0], force[1], 0.f);
			gl.glVertex3f(force[2], force[3], 0.f);
			gl.glEnd();
		}

	}

	public void addForce(float x, float y, float dx, float dy, float color) {
		
		float [] arrowColor = null;
		if (color <= 0.f) {
			arrowColor = new float[] {0.f,0.f,.99f};
		} else if (color <= 1.f) {
			arrowColor = new float[] {.99f,.99f,0.f};
		} else if (color <= 2.f) {
			arrowColor = new float[] {.99f,0.f,0.f};
		} else if (color <= 3.f) {
			arrowColor = new float[] {0.f,.99f,0.f};
		} else if (color <= 4.f) {
			arrowColor = new float[] {.0f,.99f,.99f};
		}
			
		this.forces.add(new float[] {x,y,x+dx,y+dy,arrowColor[0],arrowColor[1],arrowColor[2]});
	}

}
