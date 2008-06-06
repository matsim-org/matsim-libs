/* *********************************************************************** *
 * project: org.matsim.*
 * VisGUIMouseHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.utils.vis.otfvis.opengl.gui;

import static javax.media.opengl.GL.GL_BLEND;
import static javax.media.opengl.GL.GL_MODELVIEW;
import static javax.media.opengl.GL.GL_MODELVIEW_MATRIX;
import static javax.media.opengl.GL.GL_ONE;
import static javax.media.opengl.GL.GL_PROJECTION;
import static javax.media.opengl.GL.GL_PROJECTION_MATRIX;
import static javax.media.opengl.GL.GL_QUADS;
import static javax.media.opengl.GL.GL_SRC_ALPHA;
import static javax.media.opengl.GL.GL_VIEWPORT;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.swing.event.MouseInputAdapter;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTargetAdapter;
import org.jdesktop.animation.timing.interpolation.KeyFrames;
import org.jdesktop.animation.timing.interpolation.KeyValues;
import org.jdesktop.animation.timing.interpolation.PropertySetter;
import org.matsim.gbl.Gbl;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.vis.otfvis.gui.OTFVisConfig;
import org.matsim.utils.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.utils.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.utils.vis.otfvis.opengl.gl.Camera;
import org.matsim.utils.vis.otfvis.opengl.gl.EvaluatorPoint3f;
import org.matsim.utils.vis.otfvis.opengl.gl.Point3f;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;

public class VisGUIMouseHandler extends MouseInputAdapter
implements MouseWheelListener{
    private Point3f cameraStart = new Point3f(30000f, 3000f, 1500f);
    private Point3f cameraTarget = new Point3f(30000f, 3000f, 0f);
 	double[] modelview = new double[16];
	double[] projection = new double[16];
	int[] viewport = new int[4];
    //private GL gl = null;
    private QuadTree.Rect viewBounds = null;


    private Camera camera;
    //private Animator cameraAnimator;
	//private Point2D.Float clickPoint = null;

    private double aspectRatio = 1;
	public Point start = null;

	public Rectangle currentRect = null;
	public float scale = 1.f;

	public int button = 0;
	private final OTFDrawer clickHandler;
	
	public VisGUIMouseHandler(OTFDrawer clickHandler) {
		this.clickHandler = clickHandler;
	}

	private void invalidateHandler() {
       	try {
			clickHandler.invalidate(-1);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void scrollCamera(Point3f start, Point3f end, String prop){
        KeyValues<Point3f> values = KeyValues.create(new EvaluatorPoint3f(),
                start, end);

        //KeyTimes times = new KeyTimes(0f, 1f);
        KeyFrames frames = new KeyFrames(values);
        PropertySetter ps = new PropertySetter(camera, prop, frames);
        Animator cameraAnimator = new Animator(2000, ps);
        cameraAnimator.setStartDelay(0);
        cameraAnimator.setAcceleration(0.2f);
        cameraAnimator.setDeceleration(0.3f);
        cameraAnimator.addTarget(new TimingTargetAdapter() {
            @Override
			public void end() {
            	cameraStart = camera.getLocation();
            	cameraTarget = camera.getTarget();
             }});
        cameraAnimator.start();
	}

	   private void initCamera() {
	        camera = new Camera();
	    	camera.setLocation(cameraStart);
	    	camera.setTarget(cameraTarget);
	    }
	   
	void scrollToNewPos(Point3f cameraEnd){
		Point3f targetEnd = new Point3f(cameraEnd.getX(), cameraEnd.getY(),0);
		scrollCamera(cameraStart, cameraEnd, "location");
		scrollCamera(cameraTarget, targetEnd, "target");
	}

	void setToNewPos(Point3f cameraEnd){
    	Point3f targetEnd = new Point3f(cameraEnd.getX(), cameraEnd.getY(),0);
		cameraStart = cameraEnd;
		cameraTarget = targetEnd;
		camera.setTarget(cameraTarget);
		camera.setLocation(cameraStart);
    	invalidateHandler();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		int mbutton = e.getButton();
		String function = "";
		switch (mbutton) {
		case 1:
			function = Gbl.getConfig().getParam(OTFVisConfig.GROUP_NAME, OTFVisConfig.LEFT_MOUSE_FUNC);
			break;
		case 2:
			function = Gbl.getConfig().getParam(OTFVisConfig.GROUP_NAME, OTFVisConfig.MIDDLE_MOUSE_FUNC);
			break;
		case 3:
			function = Gbl.getConfig().getParam(OTFVisConfig.GROUP_NAME, OTFVisConfig.RIGHT_MOUSE_FUNC);
			break;
		}
		if(function.equals("Zoom")) button = 1;
		else if (function.equals("Pan")) button = 2;
		else if (function.equals("Menu")) button = 3;
		else if (function.equals("Select")) button = 4;
		else button = 0;
		start = new Point(x, y);
		Point3f pp = getOGLPos(x, y);
		//clickPoint = new Point2D.Float(pp.getX(),pp.getY());
		alpha = 1.0f;
		currentRect = null;

	}


	@Override
	public void mouseDragged(MouseEvent e) {
		if (button == 1)
			updateSize(e);
		else if (button == 2) {
			int deltax = start.x - e.getX();
			int deltay = start.y - e.getY();
			start.x = e.getX();
			start.y = e.getY();
			setToNewPos(getOGLPos(viewport[2]/2+deltax, viewport[3]/2+deltay));
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// update screen one last time
		mouseDragged(e);
		
		Rectangle screenRect = new Rectangle(start);
		screenRect.add(e.getPoint());
		if ((screenRect.getHeight() > 10)&& (screenRect.getWidth() > 10)) {
			if (button == 1) {
				int deltax = Math.abs(start.x - e.getX());
				int deltay = Math.abs(start.y - e.getY());
				double ratio = Math.max((double)deltax/viewport[2], (double)deltay/viewport[3]);
				//System.out.println(ratio);
				Point3f newPos = new Point3f((float)currentRect.getCenterX(),(float)currentRect.getCenterY(),(float)(cameraStart.getZ()*ratio));
				scrollToNewPos(newPos);
				//InfoText.showText("Zoom", newPos.getX(),newPos.getY(),newPos.getZ());
		        // Cube fader
		        Animator rectFader = PropertySetter.createAnimator(2020, this, "alpha", 1.0f, 0.f);
		        rectFader.setStartDelay(200);
		        rectFader.setAcceleration(0.4f);
		        rectFader.start();
				//currentRect = null;
			}
		} else {
			Point3f newcameraStart = getOGLPos(start.x, start.y);
			//setToNewPos(newcameraStart);
			Point2D.Double point = new Point2D.Double(newcameraStart.getX(), newcameraStart.getY());
			clickHandler.handleClick(point, button);
			currentRect = null;
		}
		button = 0;
	}

	void updateSize(MouseEvent e) {
		Point3f newRectStart = getOGLPos(start.x, start.y);
		Point3f newRectEnd = getOGLPos(e.getX(), e.getY());
		currentRect = new Rectangle(new Point((int)newRectStart.getX(), (int)newRectStart.getY()));
		currentRect.add(newRectEnd.getX(), newRectEnd.getY());
		// This only redraws GUI Elements, no need to invalidate(), just redraw()
		clickHandler.redraw();
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		float delta = -0.1f*e.getWheelRotation();
		scaleNetworkRelative(1.f -delta);
		//InfoText.showText("Scale");
	}

	private float alpha = 1.0f;
	public float getAlpha() {return alpha;};

	public void setAlpha(float a){
		alpha = a;
		// This only redraws GUI Elements, no need to invalidate(), just redraw()
       if (clickHandler != null) clickHandler.redraw();
	}
	
	Texture marker = null;


	    /**
	     * Renders the given texture so that it is centered within the given
	     * dimensions.
	     */
	    private void renderFace(GL gl, Texture t) {
	        TextureCoords tc = t.getImageTexCoords();
	        float tx1 = tc.left();
	        float ty1 = tc.top();
	        float tx2 = tc.right();
	        float ty2 = tc.bottom();

//	        int imgw = t.getImageWidth();
//	        int imgh = t.getImageHeight();
//	        if (imgw > imgh) {
//	            h *= ((float)imgh) / imgw;
//	        } else {
//	            w *= ((float)imgw) / imgh;
//	        }
//	        float w2 = w/2f;
//	        float h2 = h/2f;

	        float z = 20f;
	        t.enable();
	        t.bind();

	        gl.glColor4f(alpha, alpha, alpha, alpha);

	        gl.glBegin(GL_QUADS);
	        gl.glTexCoord2f(tx1, ty1); gl.glVertex3f(currentRect.x, currentRect.y, z);
	        gl.glTexCoord2f(tx2, ty1); gl.glVertex3f(currentRect.x, currentRect.y + currentRect.height, z);
	        gl.glTexCoord2f(tx2, ty2); gl.glVertex3f(currentRect.x + currentRect.width, currentRect.y + currentRect.height, z);
	        gl.glTexCoord2f(tx1, ty2); gl.glVertex3f(currentRect.x + currentRect.width, currentRect.y, z);
	        gl.glEnd();
	        t.disable();

	    }

	    public void updateBounds() {
			Point3f p1 = getOGLPos(viewport[0], viewport[1]);
			Point3f p2 = getOGLPos(viewport[2], viewport[3]);
			viewBounds =  new QuadTree.Rect(p1.x, p1.y, p2.x, p2.y);
	    }
	    
	    public void updateMatrices(GL gl) {
	        // update matrices for mouse position calculation
			gl.glGetDoublev( GL_MODELVIEW_MATRIX, modelview,0);
			gl.glGetDoublev( GL_PROJECTION_MATRIX, projection,0);
			gl.glGetIntegerv( GL_VIEWPORT, viewport,0 );
			updateBounds();
	    }
	    
	public void drawElements(GL gl){
		if((currentRect != null) && (alpha >= 0.f)){
			if(marker == null) marker = OTFOGLDrawer.createTexture("res/otfvis/marker.png");

			float z = 20f;
	        gl.glEnable(GL_BLEND);
	        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE);

	        renderFace(gl, marker);
			gl.glDisable(GL_BLEND);
		} else {
			currentRect = null;
			alpha = 1.0f;
		}
		
	}

	   public void setFrustrum(GL gl) {
		   
		   GLU glu = new GLU();
	       gl.glMatrixMode(GL_PROJECTION);
	       gl.glLoadIdentity();
	       glu.gluPerspective(45.0, aspectRatio, 1.0, cameraStart.getZ()*1.1);

	       gl.glMatrixMode(GL_MODELVIEW);
	       gl.glLoadIdentity();
	       
	       camera.setup(gl, glu);
		   updateMatrices(gl);

	   }


	public void scaleNetworkRelative(float scale) {
		this.scale *= scale;
		double zPos = (cameraStart.getZ()*(scale));
		if (zPos < minZoom) zPos = minZoom;
		if (zPos > maxZoom) zPos =maxZoom;
		double effectiveScale = (zPos -cameraStart.getZ()) /cameraStart.getZ();
		viewBounds = viewBounds.scale(effectiveScale, effectiveScale);
		setToNewPos(new Point3f(cameraStart.getX(),cameraStart.getY(),(float)zPos));
	}
	
	public void scaleNetwork(float scale) {
		this.scale = scale;
    	float test = bounds.height*0.7f;
		float zPos = (test*scale);
		if (zPos < minZoom) zPos =minZoom;
		if (zPos > maxZoom) zPos =maxZoom;
		setToNewPos(new Point3f(cameraStart.getX(),cameraStart.getY(),zPos));
	}
	
	synchronized public Point3f getOGLPos(int x, int y)
	{
		double[] obj_pos = new double[3];
		float winX, winY;//, winZ = cameraStart.getZ();
		float posX, posY;//, posZ;
		double[] w_pos = new double[3];
		double[] z_pos = new double[1];


		winX = x;
		winY = viewport[3] - y;
		//gl.glReadPixels( x, (int)(winY), 1, 1,gl.GL_DEPTH_COMPONENT, gl.GL_FLOAT, DoubleBuffer.wrap(z_pos) );
		z_pos[0]=1;

		GLU glu = new GLU();
		obj_pos[2]=0; // Check view relative z-koord of layer zero == visnet layer
		glu.gluProject( obj_pos[0], obj_pos[1],obj_pos[2], modelview,0, projection,0, viewport,0, w_pos,0);
//		glu.gluUnProject( winX, winY, winZ, DoubleBuffer.wrap(modelview), DoubleBuffer.wrap(projection), IntBuffer.wrap(viewport), DoubleBuffer.wrap(obj_pos));
		glu.gluUnProject( winX, winY, w_pos[2], modelview,0, projection,0, viewport,0, obj_pos,0);

		posX = (float)obj_pos[0];
		posY = (float)obj_pos[1];
		//posZ = (float)obj_pos[2];
		// maintain z-pos == zoom level
		return new Point3f(posX, posY, cameraStart.getZ());
	}

	public float getScale() {
		return this.scale;
	}

	public void init(GL gl) {
		//this.gl = gl;
		initCamera();
	}

	public void setAspectRatio(double aspectRatio) {
		this.aspectRatio = aspectRatio;
	}
	
	Rectangle2D.Float bounds = null;
	private float minZoom;
	private float maxZoom;
	public void setBounds(float minEasting, float minNorthing, float maxEasting, float maxNorthing, float minZoom) {
		this.minZoom = minZoom;
		this.maxZoom = Math.max(maxNorthing- minNorthing, (maxEasting-minEasting));
		bounds = new Rectangle2D.Float(minEasting, minNorthing, maxEasting - minEasting, maxNorthing- minNorthing);
		
    	cameraStart = new Point3f((maxEasting-minEasting)/2, (maxNorthing-minNorthing)/2, (maxNorthing-minNorthing)*0.8f);
    	cameraTarget = new Point3f(cameraStart.getX(), cameraStart.getY(),0);
		viewBounds =  new QuadTree.Rect(minEasting, minNorthing, maxEasting - minEasting, maxNorthing- minNorthing);
	}

	public QuadTree.Rect getBounds(){
		return viewBounds;
	}
	
	public Point3f getView() {
		return cameraStart;
	}
}
