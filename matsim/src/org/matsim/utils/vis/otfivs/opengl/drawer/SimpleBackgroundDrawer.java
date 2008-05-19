package org.matsim.utils.vis.otfivs.opengl.drawer;

import static javax.media.opengl.GL.GL_QUADS;

import java.awt.geom.Rectangle2D;

import javax.media.opengl.GL;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;

public class SimpleBackgroundDrawer extends OTFGLDrawableImpl implements OTFGLDrawable{
	private Texture picture = null;
	private final Rectangle2D.Float abskoords;
	private final String name;
	private double offsetEast; 
	private double offsetNorth;
	
	public SimpleBackgroundDrawer(String picturePath, Rectangle2D.Float koords) {
		this.abskoords = koords;
		name = picturePath;
	}

	public void onDraw(GL gl) {
		if (picture == null) this.picture = OTFOGLDrawer.createTexture(name);
		if (picture == null) return;
        TextureCoords tc = picture.getImageTexCoords();
        float tx1 = tc.left();
        float ty1 = tc.top();
        float tx2 = tc.right();
        float ty2 = tc.bottom();


        float z = 1.1f;
        picture.enable();
        picture.bind();

        gl.glColor4f(1,1,1,1);

		Rectangle2D.Float koords = new Rectangle2D.Float((float)(abskoords.x - offsetEast), (float)(abskoords.y- offsetNorth), abskoords.width, abskoords.height);
		
        gl.glBegin(GL_QUADS);
        gl.glTexCoord2f(tx1, ty1); gl.glVertex3f(koords.x, koords.y, z);
        gl.glTexCoord2f(tx2, ty1); gl.glVertex3f(koords.x, koords.y + koords.height, z);
        gl.glTexCoord2f(tx2, ty2); gl.glVertex3f(koords.x + koords.width, koords.y + koords.height, z);
        gl.glTexCoord2f(tx1, ty2); gl.glVertex3f(koords.x + koords.width, koords.y, z);
        gl.glEnd();
        
        picture.disable();
	}

	public void setOffset(double offsetEast, double offsetNorth) {
		this.offsetEast = offsetEast;
		this.offsetNorth = offsetNorth;
	}
	
}

