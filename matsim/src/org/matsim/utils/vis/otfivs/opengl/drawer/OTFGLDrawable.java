package org.matsim.utils.vis.otfivs.opengl.drawer;

import javax.media.opengl.GL;

import org.matsim.utils.vis.otfivs.data.OTFData;
import org.matsim.utils.vis.otfivs.gui.OTFDrawable;


public interface OTFGLDrawable extends OTFDrawable , OTFData.Receiver{
	public void onDraw(GL gl);
}

