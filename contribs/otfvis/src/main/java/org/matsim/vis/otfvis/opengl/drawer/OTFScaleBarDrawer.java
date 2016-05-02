package org.matsim.vis.otfvis.opengl.drawer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.vis.otfvis.caching.SceneGraph;

import java.awt.*;

/**
 * OTFScaleBarDrawer draws a scale bar as an overlay.
 * It depends on the chosen coordinates being in meters.
 * 
 * @author laemmel
 *
 */
public class OTFScaleBarDrawer extends OTFGLAbstractDrawableReceiver {

	private TextRenderer textRenderer = null;
	double[] modelview = new double[16];
	double[] projection = new double[16];
	int[] viewport = new int[4];
	
	Texture back = null;
	private Texture sc;
	
	private final String bg;
	private final String sb;

	public OTFScaleBarDrawer() {
		this.bg = "sb_background.png";
		this.sb = "scalebar.png";
		// Create the text renderer
		Font font = new Font("SansSerif", Font.PLAIN, 30);
		this.textRenderer = new TextRenderer(font, true, false);
	}

	@Override
	public void onDraw(GL2 gl) {
		if (this.back == null){
			this.back = OTFOGLDrawer.createTexture(gl, MatsimResource.getAsInputStream(this.bg));
			this.sc = OTFOGLDrawer.createTexture(gl, MatsimResource.getAsInputStream(this.sb));
		}
		
		updateMatrices(gl);
		float [] fl = getKoords();
		
		final TextureCoords tc = this.back.getImageTexCoords();
		final float tx1 = tc.left();
		final float ty1 = tc.top();
		final float tx2 = tc.right();
		final float ty2 = tc.bottom();

		final float z = 1.1f;

		float width2 = (float) this.textRenderer.getBounds("METERS").getWidth() * fl[8];
		
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		this.back.enable(gl);
		this.back.bind(gl);
		gl.glColor4f(1,1,1,1);

		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(tx1, ty1); gl.glVertex3f(fl[4], fl[5], z);
		gl.glTexCoord2f(tx2, ty1); gl.glVertex3f(fl[4], fl[7], z);
		gl.glTexCoord2f(tx2, ty2); gl.glVertex3f(fl[2]+width2 + width2/3.f, fl[7], z);
		gl.glTexCoord2f(tx1, ty2); gl.glVertex3f(fl[2]+width2 + width2/3.f, fl[5], z);
		gl.glEnd();
		this.back.disable(gl);

		gl.glDisable(GL.GL_BLEND);

		this.sc.enable(gl);
		this.sc.bind(gl);
		
		gl.glColor4f(1,1,1,1);
		
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(tx1, ty1); gl.glVertex3f(fl[0], fl[1], z);
		gl.glTexCoord2f(tx2, ty1); gl.glVertex3f(fl[0], fl[3], z);
		gl.glTexCoord2f(tx2, ty2); gl.glVertex3f(fl[2], fl[3], z);
		gl.glTexCoord2f(tx1, ty2); gl.glVertex3f(fl[2], fl[1], z);
		gl.glEnd();
		this.sc.disable(gl);
		
		this.textRenderer.begin3DRendering();
		float c = 0.f;
		String text = ""+(int)fl[10];
		float width = (float) this.textRenderer.getBounds(text).getWidth() * fl[8];
		// Render the text
		this.textRenderer.setColor(c, c, c, 1.f);
		this.textRenderer.draw3D(text, fl[2] - width/2.f,fl[9],1.1f,fl[8]);

		this.textRenderer.draw3D("METERS", fl[2] + width2/4.f,fl[3],1.1f,fl[8]);
		this.textRenderer.end3DRendering();

	}
	
	private float [] getKoords() {
		int scrTX = this.viewport[0];
		int scrTY = this.viewport[1];
		int scrBX = this.viewport[2];
		int scrBY = this.viewport[3];
		
		
		
		int scrWidth = scrBX -scrTX;
		int diagonal = (int) Math.sqrt(scrBX*scrBX + scrBY * scrBY);
		
		
		float[] tmp = getOGLPos(scrTX,scrTY);
		float glTX = tmp[0];
		
		tmp = getOGLPos(scrBX,scrBY);
		float glBX = tmp[0];
		
		
		
		float glWidth = glBX - glTX;
		float xFactor = Math.abs(glWidth/scrWidth);
		
		float diff30km = Math.abs((glWidth*.2f) - 30000);
		float diff15km = Math.abs((glWidth*.2f) - 15000);
		float diff3km = Math.abs((glWidth*.2f) - 3000);
		float diff1500m = Math.abs((glWidth*.2f) - 1500);
		float diff300m = Math.abs((glWidth*.2f) - 300);
		float diff150m = Math.abs((glWidth*.2f) - 150);
		float diff30m = Math.abs((glWidth*.2f) - 30);
		
		float width;
		if (diff30km < diff15km) {
			width = 30000.f;
		} else if (diff15km < diff3km) {
			width = 15000.f;
		} else if (diff3km < diff1500m) {
			width = 3000.f;
		} else if (diff1500m < diff300m) {
			width = 1500.f;
		} else if (diff300m < diff150m) {
			width = 300.f;
		} else if (diff150m < diff30m) {
			width = 150.f;
		} else {
			width = 30.f;
		}
		
		float ret[]  = new float [11];
		
		int scTXBX = (int) (0.4 * diagonal);
		int scTXBY = scrBY - 20; //(int) (scrBY - (0.01 * diagonal));
		tmp = getOGLPos(scTXBX,scTXBY);
		
		ret[3] = tmp[1];
		
		int scTXTX = (int) (0.01 * diagonal);
		int scTXTY = (int) (scTXBY - (0.01 * diagonal));
		tmp = getOGLPos(scTXTX,scTXTY);
		ret[0] = tmp[0];
		ret[1] = tmp[1];
		ret[2] = ret[0]+width;
		
		float txHeight = ret[3] - ret[1];
		ret[4] = ret[0] - 3 * xFactor; //bg tx
		ret[5] = ret[1] - 0.99f * txHeight; //bg ty
		ret[6] = ret[2] + 0.15f * scrWidth * xFactor; //bg bx
		ret[7] = ret[3] - 3 * xFactor; //bg by
		ret[8] = (float) (xFactor * diagonal *0.0004);
		ret[9] = ret[1] - 0.05f * txHeight;
		ret[10] = width;
		return ret;
		
	}

	public void updateMatrices(GL2 gl) {
		// update matrices for mouse position calculation
		gl.glGetDoublev( GL2.GL_MODELVIEW_MATRIX, this.modelview,0);
		gl.glGetDoublev( GL2.GL_PROJECTION_MATRIX, this.projection,0);
		gl.glGetIntegerv( GL2.GL_VIEWPORT, this.viewport,0 );
	}

	private float [] getOGLPos(int x, int y) {
		
		
		double[] obj_pos = new double[3];
		float winX, winY;//, winZ = cameraStart.getZ();
		float posX, posY;//, posZ;
		double[] w_pos = new double[3];
		double[] z_pos = new double[1];


		winX = x;
		winY = this.viewport[3] - y;
		z_pos[0]=1;

		GLU glu = new GLU();
		obj_pos[2]=0; // Check view relative z-coord of layer zero == visnet layer
		glu.gluProject( obj_pos[0], obj_pos[1],obj_pos[2], this.modelview,0, this.projection,0, this.viewport,0, w_pos,0);
		glu.gluUnProject( winX, winY, w_pos[2], this.modelview,0, this.projection,0, this.viewport,0, obj_pos,0);

		posX = (float)obj_pos[0];
		posY = (float)obj_pos[1];
		return new float []{posX, posY};
	}
	
	@Override
	public void addToSceneGraph(SceneGraph graph) {
		graph.addItem(this);
	}


}
