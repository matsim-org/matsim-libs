package org.matsim.vis.otfvis.opengl.drawer;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.gui.OTFHostControl;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

class ScreenshotTaker implements GLEventListener {

	private int lastShot = -1;
	private OTFHostControl hostControlBar;

	ScreenshotTaker(OTFHostControl hostControlBar) {
		this.hostControlBar = hostControlBar;
	}

	@Override
	public void init(GLAutoDrawable glAutoDrawable) {

	}

	@Override
	public void dispose(GLAutoDrawable glAutoDrawable) {

	}

	@Override
	public void display(GLAutoDrawable glAutoDrawable) {
		if (OTFClientControl.getInstance().getOTFVisConfig().getRenderImages()) {
			GL2 gl = glAutoDrawable.getGL().getGL2();
			if (this.lastShot < hostControlBar.getSimTime()) {
				this.lastShot = hostControlBar.getSimTime();
				try {
					int screenshotInterval = 1;
					if (hostControlBar.getSimTime() % screenshotInterval == 0) {
						AWTGLReadBufferUtil glReadBufferUtil = new AWTGLReadBufferUtil(gl.getGLProfile(), false);
						BufferedImage bufferedImage = glReadBufferUtil.readPixelsToBufferedImage(gl, true);
						ImageIO.write(bufferedImage, "png", new File("frame" + String.format("%07d", hostControlBar.getSimTime()) + ".png"));
					}
				} catch (GLException | IOException | IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void reshape(GLAutoDrawable glAutoDrawable, int i, int i1, int i2, int i3) {

	}
}
