package playground.gregor.otf;

import javax.media.opengl.GL;

import org.matsim.vis.otfvis.opengl.drawer.OTFGLDrawableImpl;

public class Dummy extends OTFGLDrawableImpl {

	public static OTFInundationDrawer myDrawer;
	int time;
	
//	public Dummy(double time2) {
//		this.time = (int) time2;
//	}

//	@Override
//	public void invalidate(SceneGraph graph) {
//		this.time = (int) graph.getTime();
//		
//	}

	public void onDraw(GL gl) {
		myDrawer.onDraw(gl,this.time);
		
	}

	public void setTime(double time2) {
		this.time = (int) time2;
		
	}

}
