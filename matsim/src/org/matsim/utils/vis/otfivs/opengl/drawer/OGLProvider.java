package org.matsim.utils.vis.otfivs.opengl.drawer;

import javax.media.opengl.GL;

import org.matsim.utils.vis.otfivs.data.OTFClientQuad;
import org.matsim.utils.vis.otfivs.opengl.gl.Point3f;


public interface OGLProvider {
	public GL getGL();
	public Point3f getView() ;
	OTFClientQuad getQuad();
}
