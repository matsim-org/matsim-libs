package org.matsim.vis.otfvis.opengl.drawer;

import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;
import com.jogamp.opengl.GL2;

import org.matsim.core.gbl.MatsimResource;


class OTFGLOverlay extends OTFGLAbstractDrawable {
	private final float relX;
	private final float relY;
	private final boolean opaque;
	private final float size;
	private ByteBuffer bb;
	private int w;
	private int h;
	private String textureFileName;
	private IntBuffer textureName = IntBuffer.allocate(1);

	OTFGLOverlay(final String textureFileName, float relX, float relY, float size, boolean opaque) {
		this.textureFileName = textureFileName;
		this.relX = relX;
		this.relY = relY;
		this.size = size;
		this.opaque = opaque;
	}

	@Override
	public void onInit(GL2 gl) {
		
		// From:
		// http://wiki.tankaar.com/index.php?title=Displaying_an_Image_in_JOGL_%28Part_1%29
		
		BufferedImage bufferedImage = null;
		w = 0;
		h = 0;
		try {
			bufferedImage = ImageIO.read(MatsimResource.getAsInputStream(textureFileName));
			w = bufferedImage.getWidth();
			h = bufferedImage.getHeight();
		} catch (IOException e) {
			e.printStackTrace();
		}
		WritableRaster raster = 
			Raster.createInterleavedRaster (DataBuffer.TYPE_BYTE,
					w,
					h,
					4,
					null);
		ComponentColorModel colorModel=
			new ComponentColorModel (ColorSpace.getInstance(ColorSpace.CS_sRGB),
					new int[] {8,8,8,8},
					true,
					false,
					ComponentColorModel.TRANSLUCENT,
					DataBuffer.TYPE_BYTE);
		BufferedImage dukeImg = 
			new BufferedImage (colorModel,
					raster,
					false,
					null);

		Graphics2D g = dukeImg.createGraphics();
		g.drawImage(bufferedImage, null, null);
		DataBufferByte dukeBuf =
			(DataBufferByte)raster.getDataBuffer();
		byte[] dukeRGBA = dukeBuf.getData();
		bb = ByteBuffer.wrap(dukeRGBA);
		bb.position(0);
		bb.mark();
		gl.glGenTextures(1, textureName);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, textureName.get(0));
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
		gl.glTexImage2D (GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, w, h, 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, bb);
		gl.glDisable(GL2.GL_TEXTURE_2D);
	}

	@Override
	public void onDraw(GL2 gl) {
		int[] viewport = new int[4];
		gl.glGetIntegerv( GL2.GL_VIEWPORT, viewport ,0 );
		float height = this.size*h/viewport[3];
		float length = this.size*w/viewport[2];
		int z = 0;
		float startX = this.relX >= 0 ? -1.f + this.relX : 1.f -length +this.relX;
		float startY = this.relY >= 0 ? -1.f + this.relY : 1.f -height +this.relY;

		gl.glColor4d(1,1,1,1);

		//push 1:1 screen matrix
		gl.glMatrixMode( GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glMatrixMode( GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		if ( gl != getGl() ) {
			throw new RuntimeException("the `gl's are inconsistent; don't know what this means.  kai, jan'11") ;
		}

		if(!this.opaque) {
			gl.glEnable(GL2.GL_BLEND);
			gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		}

		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, textureName.get(0));
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0,1); 
		gl.glVertex3f(startX, startY,z);
		gl.glTexCoord2f(1,1); 
		gl.glVertex3f(startX + length, startY,z);
		gl.glTexCoord2f(1,0); 
		gl.glVertex3f(startX + length, startY + height,z);
		gl.glTexCoord2f(0,0); 
		gl.glVertex3f(startX, startY + height,z);
		gl.glEnd();
		gl.glDisable(GL2.GL_TEXTURE_2D);
		if(!this.opaque) {
			gl.glDisable(GL2.GL_BLEND);
		}
		gl.glMatrixMode( GL2.GL_MODELVIEW);
		gl.glPopMatrix();
		gl.glMatrixMode( GL2.GL_PROJECTION);
		gl.glPopMatrix();
	}

}