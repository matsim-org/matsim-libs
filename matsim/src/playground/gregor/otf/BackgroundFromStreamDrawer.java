package playground.gregor.otf;

import static javax.media.opengl.GL.GL_MODELVIEW_MATRIX;
import static javax.media.opengl.GL.GL_QUADS;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;

import javax.media.opengl.GL;
import javax.media.opengl.GLException;

import org.matsim.vis.otfvis.opengl.drawer.AbstractBackgroundDrawer;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;
import com.sun.opengl.util.texture.TextureData;
import com.sun.opengl.util.texture.TextureIO;

public class BackgroundFromStreamDrawer extends AbstractBackgroundDrawer {

	private Float abskoords;
	private Texture picture = null;
	private Texture defaultPicture;
	private Texture cache3 = null;
	private Texture cache4 = null;

	private static final long MIN_UPDATE_INTERVAL = 200;
	private long lastUpdate = 0;

	private final double xs;
	private final double ys;
	private final BGLoader bgl;
	private boolean openRequest = false;
	private boolean newInput = false;
	private Float newKoords;
	private TextureData tData;
	private final double topX;
	private final double topY;
	private  int pxSize;
	private  int pySize;
	private final double centerX;
	private final double centerY;
	private int oldZoom = -1;

	double [] modelview = new double [16];
	private double dist;

	//	public BackgroundFromStreamDrawer(InputStream is, Float koords) {
	//		this.abskoords = koords;
	//		this.is = is;
	//	}

	public BackgroundFromStreamDrawer(BGLoader bgl, double topX,
			double topY, double xs, double ys,int pxSize, int pySize) {
		this.topX = topX;
		this.topY = topY;
		this.xs = xs;
		this.ys = ys;
		this.pxSize = pxSize;
		this.pySize = pySize;
		this.bgl = bgl;
		this.centerX = topX - xs/2;
		this.centerY = topY - ys/2;
	}


	private void upDateTx(GL gl) {
		if (this.openRequest) {
			return;
		}
		long time = System.currentTimeMillis();
		if (time - this.lastUpdate < MIN_UPDATE_INTERVAL || this.openRequest){
			return;
		}
		gl.glGetDoublev( GL_MODELVIEW_MATRIX, this.modelview,0 );
		this.lastUpdate = time;

		double hight = -this.modelview[14];
		double x = this.offsetEast - this.modelview[12];
		double y = this.offsetNorth - this.modelview[13];
		this.dist = Math.sqrt(Math.pow((this.centerX - x),2)+Math.pow((this.centerY - y),2));
		//		System.out.println("hight:" + hight + " dist:" + dist);
		if (hight > 1000){
			//			this.cache4 = null;
			//			this.cache3 = null;
			if (this.oldZoom == 2) {
				return;
			}
			this.oldZoom = 2;
			if (this.defaultPicture == null){
				this.pxSize = (int) Math.round(this.xs * 0.1/0.3);
				this.pySize = (int) Math.round(this.ys * 0.1/0.3);			
			} else {
				this.picture = this.defaultPicture;
				return;
			}
		} else if (this.dist <= 3*this.xs && hight > 500) {
			if (this.oldZoom == 3) {
				return;
			}
			this.oldZoom = 3;
			if (this.cache3 != null) {
				this.picture = this.cache3;
				return;
			}

			this.pxSize = (int) Math.round(this.xs * 0.25/0.3);
			this.pySize = (int) Math.round(this.ys * 0.25/0.3);
		} else if (this.dist <= 2*this.xs) {
			if (this.oldZoom == 4){
				return;
			}
			this.oldZoom = 4;
			if (this.cache4 != null) {
				this.picture = this.cache4;
				return;
			} else if (this.cache3 != null){
				this.picture = this.cache3;
			}
			this.pxSize = (int) Math.round(this.xs * 1.0/0.3);
			this.pySize = (int) Math.round(this.ys * 1.0/0.3);
		} else if (this.defaultPicture != null){
			if (this.dist > 10*this.xs) {
				this.cache4 = null;
				//				this.cache3 = null;				
			}

			this.picture = this.defaultPicture;
			this.oldZoom = 2;
			return;
		}
		sendPictureRequest();
	}

	public void onDraw(final GL gl) {

		upDateTx(gl);

		if (this.newInput) {
			this.abskoords = this.newKoords;
			try {
				this.picture = TextureIO.newTexture(this.tData);
			} catch (GLException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} 
			if (this.defaultPicture == null) {
				this.defaultPicture = this.picture;
			}
			if (this.oldZoom == 4) {
				this.cache4 = this.picture;

			}
			if (this.oldZoom == 3) {
				this.cache3 = this.picture;
			}

			this.newInput = false;
			this.openRequest = false;
		}

		if (this.picture == null) return;

		final TextureCoords tc = this.picture.getImageTexCoords();
		final float tx1 = tc.left();
		final float ty1 = tc.top();
		final float tx2 = tc.right();
		final float ty2 = tc.bottom();

		//		System.out.println("tx1:" + tx1 + " this.offsetEast" + this.offsetEast);


		final float z = 1.1f;
		this.picture.enable();
		this.picture.bind();

		gl.glColor4f(1,1,1,1);

		final Rectangle2D.Float koords = new Rectangle2D.Float((float)(this.abskoords.x - this.offsetEast), (float)(this.abskoords.y- this.offsetNorth), this.abskoords.width, this.abskoords.height);

		gl.glBegin(GL_QUADS);
		gl.glTexCoord2f(tx1, ty1); gl.glVertex3f(koords.x, koords.y, z);
		gl.glTexCoord2f(tx1, ty2); gl.glVertex3f(koords.x + koords.width, koords.y, z);
		gl.glTexCoord2f(tx2, ty2); gl.glVertex3f(koords.x + koords.width, koords.y + koords.height, z);
		gl.glTexCoord2f(tx2, ty1); gl.glVertex3f(koords.x, koords.y + koords.height, z);
		
		gl.glEnd();

		this.picture.disable();
	}





	private void sendPictureRequest() {
		if (!this.openRequest) {
			this.bgl.addRequest(new BGRequest(this));
			this.openRequest  = true;
		}

	}

	public void response(TextureData t, Float koords) {
		this.tData = t;
		this.newKoords = koords;
		this.newInput = true;
	
	}


	//	private Texture getPicture() {
	//		InputStream is = null;
	//		try {
	//			is = this.bgl.getBGImageStream(this.centerX, this.centerY, this.z, this.xs, this.ys);
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//		
	//		double z2 = this.z / 0.3;
	//		double topX = (this.centerX - (this.xs/2)/z2) + 2*(this.xs/2)/z2;
	//		double topY = this.centerY - (this.ys/2)/z2;
	//		double xS = -this.xs/z2;
	//		double yS = this.ys/z2;
	//		this.abskoords =  new Rectangle2D.Float((int)topX,(int)topY,(int)xS,(int)yS);
	//		
	//		Texture tx = OTFOGLDrawer.createTexture(is);
	//		try {
	//			is.close();
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//		return tx;
	//	}




	public double getXs() {
		return this.xs;
	}


	public double getTopX() {
		return this.topX;
	}


	public double getTopY() {
		return this.topY;
	}


	public int getPxSize() {
		return this.pxSize;
	}


	public int getPySize() {
		return this.pySize;
	}


	public double getYs() {
		return this.ys;
	}

	public double getDist() {
		return this.dist;
	}




}
