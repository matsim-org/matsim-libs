package playground.gregor.otf;

import static javax.media.opengl.GL.GL_MODELVIEW_MATRIX;
import static javax.media.opengl.GL.GL_QUADS;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.util.HashMap;

import javax.media.opengl.GL;
import javax.media.opengl.GLException;

import org.matsim.vis.otfvis.opengl.drawer.AbstractBackgroundDrawer;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;
import com.sun.opengl.util.texture.TextureIO;

public class BackgroundFromStreamDrawer extends AbstractBackgroundDrawer {

	private Float abskoords;
	private Texture picture = null;
	public enum ZoomLevel {high,mid,near,brink};
	ZoomLevel oldZoom = null;
	private final HashMap<ZoomLevel,Texture> txCache = new HashMap<ZoomLevel, Texture>();

	private static final long MIN_UPDATE_INTERVAL = 100;
	private long lastUpdate = 0;

	private final double xs;
	private final double ys;
	private final BGLoader bgl;
	private final double topX;
	private final double topY;
	private  int pxSize;
	private  int pySize;
	private final double centerX;
	private final double centerY;
	
	private  BGRequest request = null;

	double [] modelview = new double [16];
	private double dist;
	private double hight;

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
		this.centerX = topX + xs/2;
		this.centerY = topY - ys/2;
	}


	private void upDateTx(GL gl) {
		long time = System.currentTimeMillis();
		if (time - this.lastUpdate < MIN_UPDATE_INTERVAL){
			return;
		}
		this.lastUpdate = time;
		
		if (this.request != null && !this.request.getLock()) {
//			System.out.println("111 cant get lock!");
			return;
		}
		
		if (this.request != null && this.request.getState() == BGRequest.State.processed) {
			this.request.unLock();
			return;
		}

		gl.glGetDoublev( GL_MODELVIEW_MATRIX, this.modelview,0 );
		this.hight = -this.modelview[14];
		double x = this.offsetEast - this.modelview[12];
		double y = this.offsetNorth - this.modelview[13];
		this.dist = Math.sqrt(Math.pow((this.centerX - x),2)+Math.pow((this.centerY - y),2));
		
		ZoomLevel desiredZoom;
		
		if (this.hight > 2000){
			desiredZoom = ZoomLevel.high;
		} else if (this.dist <= 4*this.xs && this.hight > 800) {
			desiredZoom = ZoomLevel.mid;
		}else if (this.dist <= 2*this.xs) {
			desiredZoom = ZoomLevel.near;
		} else {
			desiredZoom = ZoomLevel.high;
			if (this.dist > 10*this.xs) {
				this.txCache.remove(ZoomLevel.near);
			}
		}
		
		if (desiredZoom == this.oldZoom) {
//			System.out.println("222 already at this zoom level!");
			if (this.request != null) {
				this.request.setState(BGRequest.State.obsolete);
				this.request.unLock();
				this.request = null;
			}
			return;
		}
		
		Texture tx;
		if ((tx = this.txCache.get(desiredZoom)) != null) {
			this.picture = tx;
			this.oldZoom = desiredZoom;
//			System.out.println("333 got from cache! ---> desired zoom:" + desiredZoom);
			if (this.request != null) {
				this.request.setState(BGRequest.State.obsolete);
				this.request.unLock();
				this.request = null;
			}
			return;
		}

		if (this.request != null && this.request.getZoomLevel() == desiredZoom) {
//			System.out.println("444 already requesting this zoom level!");
			this.request.unLock();
			return;
		}
		
		if (this.request != null) {
			this.request.setState(BGRequest.State.obsolete);
//			System.out.println("555 old request obsoltete!");
			this.request.unLock();
			this.request = null;
		}
		
//		System.out.println("desired Zothis.centerYom:" + desiredZoom);
		double priority = 0;
		if (desiredZoom == ZoomLevel.high) {
			priority = Math.sqrt(Math.pow((651228 - this.centerX),2)+Math.pow((9895420 - this.centerY),2))*this.hight;//HACK
			this.pxSize = (int) Math.round(this.xs * 0.1/0.3);
			this.pySize = (int) Math.round(this.ys * 0.1/0.3);
		} else if (desiredZoom == ZoomLevel.mid) {
			priority = this.hight * this.dist;
			this.pxSize = (int) Math.round(this.xs * 0.25/0.3);
			this.pySize = (int) Math.round(this.ys * 0.25/0.3);
			if ((tx = this.txCache.get(ZoomLevel.near)) != null) {
				this.picture = tx;
			}
		} else if (desiredZoom == ZoomLevel.near) {
			priority = this.hight * this.dist;
			this.pxSize = (int) Math.round(this.xs * 1./0.3);
			this.pySize = (int) Math.round(this.ys * 1./0.3);
			if ((tx = this.txCache.get(ZoomLevel.mid)) != null) {
				this.picture = tx;
			}
		}
		
		sendPictureRequest(desiredZoom,priority);
	}

	public void onDraw(final GL gl) {

		upDateTx(gl);

		if (this.request != null && this.request.getLock()) {
			if (this.request.getState() == BGRequest.State.processed){
				try {
					this.picture = TextureIO.newTexture(this.request.getTxData());
				} catch (GLException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} 	
				this.abskoords = this.request.getKoords();
				this.txCache.put(this.request.getZoomLevel(), this.picture);
				this.oldZoom = this.request.getZoomLevel();
				this.request.setState(BGRequest.State.obsolete);
				this.request.unLock();
				this.request = null;
			} else {
				this.request.unLock();
			}
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





	private void sendPictureRequest(ZoomLevel desiredZoom, double priority) {
		this.request = new BGRequest(this,desiredZoom,priority);
		if (!this.request.getLock()) {
			throw new RuntimeException("could not get lock on new BGRequest");
		}
		this.request.setState(BGRequest.State.open);
		this.bgl.addRequest(this.request);
		this.request.unLock();
	}





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
	public double getHight() {
		return this.hight;
	}




}
