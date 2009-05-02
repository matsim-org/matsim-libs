package playground.gregor.otf;

import static javax.media.opengl.GL.GL_MODELVIEW_MATRIX;
import static javax.media.opengl.GL.GL_PROJECTION_MATRIX;
import static javax.media.opengl.GL.GL_QUADS;
import static javax.media.opengl.GL.GL_VIEWPORT;

import java.awt.Font;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import org.matsim.vis.otfvis.opengl.drawer.OTFGLDrawableImpl;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gl.InfoText;

import com.sun.opengl.util.j2d.TextRenderer;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;

public class ScalelableBackgroundDraw extends OTFGLDrawableImpl {

	
	private TextRenderer textRenderer = null;
	double[] modelview = new double[16];
	double[] projection = new double[16];
	int[] viewport = new int[4];
	
	Texture back = null;
	private Texture sc;
	
	private final String bg;
	private final String sb;


	public ScalelableBackgroundDraw(String picturePath, String sb) {
		this.bg = picturePath;
		this.sb = sb;
		initTextRenderer();
//		this.sb2 = sb2;
	}

	
	private void initTextRenderer() {
		// Create the text renderer
		Font font = new Font("SansSerif", Font.PLAIN, 32);
		this.textRenderer = new TextRenderer(font, true, false);
		InfoText.setRenderer(this.textRenderer);
	}
	
	public void onDraw(GL gl) {

		
	
		if (this.back == null){
			this.back = OTFOGLDrawer.createTexture(this.bg);
//			this.sc2 = OTFOGLDrawer.createTexture(this.sb2);
			this.sc = OTFOGLDrawer.createTexture(this.sb);
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
		this.back.enable();
		this.back.bind();
		gl.glColor4f(1,1,1,1);
//		final Rectangle2D.Float koords = new Rectangle2D.Float((float)(this.offsetEast), (float)(this.offsetNorth), 1000, 1000);

		gl.glBegin(GL_QUADS);
		gl.glTexCoord2f(tx1, ty1); gl.glVertex3f(fl[4], fl[5], z);
		gl.glTexCoord2f(tx2, ty1); gl.glVertex3f(fl[4], fl[7], z);
		gl.glTexCoord2f(tx2, ty2); gl.glVertex3f(fl[2]+width2 + width2/3.f, fl[7], z);
		gl.glTexCoord2f(tx1, ty2); gl.glVertex3f(fl[2]+width2 + width2/3.f, fl[5], z);
		gl.glEnd();
		this.back.disable();

		gl.glDisable(GL.GL_BLEND);


		this.sc.enable();
		this.sc.bind();
		

		
		gl.glColor4f(1,1,1,1);
//		final Rectangle2D.Float koords = new Rectangle2D.Float((float)(this.offsetEast), (float)(this.offsetNorth), 1000, 1000);
		
		gl.glBegin(GL_QUADS);
		gl.glTexCoord2f(tx1, ty1); gl.glVertex3f(fl[0], fl[1], z);
		gl.glTexCoord2f(tx2, ty1); gl.glVertex3f(fl[0], fl[3], z);
		gl.glTexCoord2f(tx2, ty2); gl.glVertex3f(fl[2], fl[3], z);
		gl.glTexCoord2f(tx1, ty2); gl.glVertex3f(fl[2], fl[1], z);
		gl.glEnd();
		this.sc.disable();
		
		Font font = new Font("SansSerif", Font.PLAIN, 32);
		this.textRenderer = new TextRenderer(font, true, false);
		
			
		
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
		float glTY = tmp[1]; 
		
		tmp = getOGLPos(scrBX,scrBY);
		float glBX = tmp[0];
		float glBY = tmp[1];
		
		
		
		float glWidth = glBX - glTX;
		float glHeight = glBY - glTY;
		float xFactor = Math.abs(glWidth/scrWidth);
		
		float diff30km = Math.abs((glWidth*.4f) - 30000);
		float diff3km = Math.abs((glWidth*.4f) - 3000);
		float diff300m = Math.abs((glWidth*.4f) - 300);
		float diff30m = Math.abs((glWidth*.4f) - 30);
		
//		float diff3 = (glWidth*.4f) - 3.f*modWidth3;
		
//		float diff2 = (glWidth*.4f) - 2.f*modWidth2;
		
		float width;
		if (diff30km < diff3km) {
			width = 30000.f;
		} else if (diff3km < diff300m) {
			width = 3000.f;
		} else if (diff300m < diff30m) {
			width = 300.f;
		} else {
			width = 30.f;
		}
		
		float ret[]  = new float [11];
		
//		int scTXTX = (int) (0.01 * scrBX);
//		int scTXTY = (int) (scrBY - (0.04 * scrBY));
//		tmp = getOGLPos(scTXTX,scTXTY);
//		ret[0] = tmp[0];
//		ret[1] = tmp[1];
		
		int scTXBX = (int) (0.4 * diagonal);
		int scTXBY = scrBY - 20; //(int) (scrBY - (0.01 * diagonal));
		tmp = getOGLPos(scTXBX,scTXBY);
//		System.out.println(width + " glWidth:" + glWidth);
		
		ret[3] = tmp[1];
		
		int scTXTX = (int) (0.01 * diagonal);
		int scTXTY = (int) (scTXBY - (0.01 * diagonal));
		tmp = getOGLPos(scTXTX,scTXTY);
		ret[0] = tmp[0];
		ret[1] = tmp[1];
		ret[2] = ret[0]+width;
		
		float txWidth = width;
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

	public void updateMatrices(GL gl) {
		// update matrices for mouse position calculation
		gl.glGetDoublev( GL_MODELVIEW_MATRIX, this.modelview,0);
		gl.glGetDoublev( GL_PROJECTION_MATRIX, this.projection,0);
		gl.glGetIntegerv( GL_VIEWPORT, this.viewport,0 );
	}

	private float [] getOGLPos(int x, int y)
	{
		
		
		double[] obj_pos = new double[3];
		float winX, winY;//, winZ = cameraStart.getZ();
		float posX, posY;//, posZ;
		double[] w_pos = new double[3];
		double[] z_pos = new double[1];


		winX = x;
		winY = this.viewport[3] - y;
		//gl.glReadPixels( x, (int)(winY), 1, 1,gl.GL_DEPTH_COMPONENT, gl.GL_FLOAT, DoubleBuffer.wrap(z_pos) );
		z_pos[0]=1;

		GLU glu = new GLU();
		obj_pos[2]=0; // Check view relative z-koord of layer zero == visnet layer
		glu.gluProject( obj_pos[0], obj_pos[1],obj_pos[2], this.modelview,0, this.projection,0, this.viewport,0, w_pos,0);
		//		glu.gluUnProject( winX, winY, winZ, DoubleBuffer.wrap(modelview), DoubleBuffer.wrap(projection), IntBuffer.wrap(viewport), DoubleBuffer.wrap(obj_pos));
		glu.gluUnProject( winX, winY, w_pos[2], this.modelview,0, this.projection,0, this.viewport,0, obj_pos,0);

		posX = (float)obj_pos[0];
		posY = (float)obj_pos[1];
		//posZ = (float)obj_pos[2];
		// maintain z-pos == zoom level
		return new float []{posX, posY};
	}


}
