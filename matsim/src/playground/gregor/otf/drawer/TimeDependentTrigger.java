package playground.gregor.otf.drawer;

import javax.media.opengl.GL;

import org.matsim.vis.otfvis.opengl.drawer.OTFGLDrawableImpl;


public class TimeDependentTrigger extends OTFGLDrawableImpl {

	public OTFTimeDependentDrawer myDrawer;
	int time;
	

	public void onDraw(GL gl) {
		this.myDrawer.onDraw(gl,this.time);
		
	}

	public void setTime(double time2) {
		this.time = (int) time2;
		
	}
	
	public void setDrawer(OTFTimeDependentDrawer drawer) {
		this.myDrawer = drawer;
	}

}
