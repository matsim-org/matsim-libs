package org.matsim.utils.vis.otfivs.opengl.layer;

import java.awt.Color;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;

import org.matsim.utils.vis.otfivs.caching.SceneGraph;
import org.matsim.utils.vis.otfivs.data.OTFClientQuad;
import org.matsim.utils.vis.otfivs.data.OTFDataQuad;
import org.matsim.utils.vis.otfivs.handler.OTFDefaultLinkHandler;
import org.matsim.utils.vis.otfivs.opengl.drawer.OTFGLDrawable;
import org.matsim.utils.vis.otfivs.opengl.drawer.OTFOGLDrawer.FastColorizer;
import org.matsim.utils.vis.otfivs.opengl.gl.Point3f;
import org.matsim.utils.vis.otfivs.opengl.layer.ColoredStaticNetLayer;
import org.matsim.utils.vis.otfivs.opengl.layer.SimpleStaticNetLayer;


public class ColoredStaticNetLayer extends SimpleStaticNetLayer {
	private int linkTexWidth = 0;
	private byte [] linkTexBuffer = null;
	private int linkcolors = -1;
	private int gTexIdx = 0;

	private static ColoredStaticNetLayer actLayer = null;

	/* (non-Javadoc)
	 * @see otfvis.layer.SimpleStaticNetLayer#draw()
	 */
	@Override
	public void draw() {
		GL gl = this.myDrawer.getGL();
		Point3f cam = this.myDrawer.getView();
		float z = cam.z - 500;

		checkNetList(gl);
		enableLinkTexture(gl);

		gl.glColor4d(1,1,1,1);
		gl.glCallList(this.netDisplList);

//			gl.glBegin(gl.GL_QUADS);
//			gl.glTexCoord2f(0,0); gl.glVertex3f(cam.x -300, cam.y-200, z);
//			gl.glTexCoord2f(0,1); gl.glVertex3f(cam.x -300, cam.y+200, z);
//			gl.glTexCoord2f(1,1); gl.glVertex3f(cam.x +300, cam.y+200, z);
//			gl.glTexCoord2f(1,0); gl.glVertex3f(cam.x +300, cam.y-200, z);
//			gl.glEnd();
//
//		System.out.println("set items - drawn items " + (killme - items.size()));
		gl.glDisable(GL.GL_TEXTURE_2D);
	}

	private void enableLinkTexture(GL gl) {
		if(this.linkcolors == -1) createLinkTexture(gl);

		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glBindTexture(GL.GL_TEXTURE_2D, this.linkcolors);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, this.linkTexWidth, this.linkTexWidth, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(this.linkTexBuffer));
	}

	private void createLinkTexture(GL gl) {

		int[] texID = new int[1];
		gl.glGenTextures(1, texID, 0);
		this.linkcolors = texID[0];
		gl.glBindTexture(GL.GL_TEXTURE_2D, texID[0]);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);
	}

	static int killme = 0;
	/* (non-Javadoc)
	 * @see otfvis.layer.SimpleStaticNetLayer#init(playground.david.vis.data.SceneGraph)
	 */
	@Override
	public void init(SceneGraph graph) {
		super.init(graph);
		if (this.myDrawer != null){
			OTFClientQuad clientQ = this.myDrawer.getQuad();
			OTFClientQuad.ClassCountExecutor counter = clientQ.new ClassCountExecutor(OTFDefaultLinkHandler.class);
			clientQ.execute(null, counter);
			double linkcount = counter.getCount();

			System.out.println("Count: " + linkcount);
			int size = (int)(Math.sqrt(linkcount)+ 1);
			this.linkTexWidth = size;
			this.linkTexBuffer = new byte [size*size*4];
//			for (int i=0;i<linkTexWidth*linkTexWidth*4;i++)linkTexBuffer[i] = (byte)(Math.random()*255);
			for (int i=0;i<this.linkTexWidth*this.linkTexWidth*4;i+=4){
				this.linkTexBuffer[i] = 0;
				this.linkTexBuffer[i+1] = 0;
				this.linkTexBuffer[i+2] = (byte)200;
			}
			for (int j=3;j<this.linkTexWidth*this.linkTexWidth*4;j+=4)this.linkTexBuffer[j] = (byte)(128);
		}
		actLayer = this;
		killme = 0;
	}

	private final static FastColorizer colorizer = new FastColorizer(
			new double[] { 0.0, 0.5,  1.0}, new Color[] {
					Color.GREEN, Color.YELLOW, Color.RED});

	private final static FastColorizer colorizerLinkSpeed = new FastColorizer(
			new double[] { 0.0, 8.3,  13.8}, new Color[] {
					Color.RED, Color.YELLOW, Color.GREEN});

	public static class QuadDrawer extends SimpleStaticNetLayer.SimpleQuadDrawer implements OTFDataQuad.Receiver, OTFGLDrawable{
		protected final int texIdx;

		public QuadDrawer(){
			this.texIdx = actLayer.gTexIdx++;
		}

		@Override
		public void onDraw( GL gl) {
			final double tx = ((this.texIdx % actLayer.linkTexWidth) *1.0 + 0.5) /actLayer.linkTexWidth;
			final double ty = ((this.texIdx / actLayer.linkTexWidth)*1.0 + 0.5)/actLayer.linkTexWidth ;
			//Draw quad
			gl.glBegin(gl.GL_QUADS);
			gl.glTexCoord2d(tx,ty); gl.glVertex3f(this.quad[0].x, this.quad[0].y, 0);
			gl.glTexCoord2d(tx,ty);gl.glVertex3f(this.quad[1].x, this.quad[1].y, 0);
			gl.glTexCoord2d(tx,ty);gl.glVertex3f(this.quad[3].x, this.quad[3].y, 0);
			gl.glTexCoord2d(tx,ty);gl.glVertex3f(this.quad[2].x, this.quad[2].y, 0);
			gl.glEnd();
		}

		@Override
		public void setColor(float coloridx) {
			killme++;

			Color color = colorizer.getColor(coloridx);
			if (coloridx == 0.0) color = Color.WHITE;
			else {
				System.out.println(coloridx);
			}

			int texPos = this.texIdx*4;
			actLayer.linkTexBuffer[texPos + 0] = (byte)(color.getRed());
			actLayer.linkTexBuffer[texPos + 1] = (byte)(color.getGreen());
			actLayer.linkTexBuffer[texPos + 2] = (byte)(color.getBlue());
			actLayer.linkTexBuffer[texPos + 3] = (byte)180;//(color.getAlpha()*255);
//			actLayer.linkTexBuffer[texPos + 0] = (byte)(texIdx %actLayer.linkTexWidth);
//			actLayer.linkTexBuffer[texPos + 1] = (byte)(texIdx %actLayer.linkTexWidth);
//			actLayer.linkTexBuffer[texPos + 2] = (byte)((texIdx %actLayer.linkTexWidth));
//			actLayer.linkTexBuffer[texPos + 3] = (byte)255;//(color.getAlpha()*255);
		}

	}

	public static class QuadDrawerLinkSpeed extends QuadDrawer {
		@Override
		public void setColor(float coloridx) {
			killme++;

			Color color = colorizerLinkSpeed.getColor(coloridx);
			if (coloridx == 0.0) color = Color.WHITE;
			else {
				System.out.println(coloridx);
			}

			int texPos = this.texIdx*4;
			actLayer.linkTexBuffer[texPos + 0] = (byte)(color.getRed());
			actLayer.linkTexBuffer[texPos + 1] = (byte)(color.getGreen());
			actLayer.linkTexBuffer[texPos + 2] = (byte)(color.getBlue());
			actLayer.linkTexBuffer[texPos + 3] = (byte)180;//(color.getAlpha()*255);
//			actLayer.linkTexBuffer[texPos + 0] = (byte)(texIdx %actLayer.linkTexWidth);
//			actLayer.linkTexBuffer[texPos + 1] = (byte)(texIdx %actLayer.linkTexWidth);
//			actLayer.linkTexBuffer[texPos + 2] = (byte)((texIdx %actLayer.linkTexWidth));
//			actLayer.linkTexBuffer[texPos + 3] = (byte)255;//(color.getAlpha()*255);
		}

	}
}
