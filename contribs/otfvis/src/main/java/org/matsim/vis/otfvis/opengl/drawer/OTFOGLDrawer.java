/* *********************************************************************** *
 * project: org.matsim.*
 * OTFOGLDrawer.java
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

package org.matsim.vis.otfvis.opengl.drawer;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureIO;
import org.apache.log4j.Logger;
import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.interpolation.PropertySetter;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.config.groups.ZoomEntry;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFClientQuadTree;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.ValueColorizer;
import org.matsim.vis.otfvis.interfaces.OTFQueryHandler;
import org.matsim.vis.otfvis.opengl.gl.InfoText;
import org.matsim.vis.otfvis.opengl.gl.Point3f;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

public class OTFOGLDrawer implements GLEventListener {

	public static class FastColorizer {

		final int grain;
		Color [] fastValues;
		double minVal, maxVal, valRange;

		public FastColorizer(double[] ds, Color[] colors) {
			this(ds, colors, 1000);
		}

		public FastColorizer(double[] ds, Color[] colors, int grain) {
			ValueColorizer helper = new ValueColorizer(ds,colors);
			this.grain = grain;
			this.fastValues = new Color[grain];
			this.minVal = ds[0];
			this.maxVal = ds[ds.length-1];
			this.valRange = this.maxVal - this.minVal;
			// calc prerendered Values
			double step = this.valRange/grain;
			for(int i = 0; i< grain; i++) {
				double value = i*step + this.minVal;
				this.fastValues[i] = helper.getColor(value);
			}
		}

		public Color getColor(double value) {
			if (value >= this.maxVal) return this.fastValues[this.grain-1];
			if (value < this.minVal) return this.fastValues[0];
			return this.fastValues[(int)((value-this.minVal)*this.grain/this.valRange)];
		}

		public Color getColorZeroOne( double value ) {
			if ( value >= 1. ) return this.fastValues[this.grain-1] ;
			if ( value <= 0. ) return this.fastValues[0] ;
			return this.fastValues[(int)(value*this.grain)] ;
		}

	}

	private class VisGUIMouseHandler extends MouseInputAdapter {

		@Override
		public void mouseDragged(MouseEvent e) {
			if (button == 1 || button == 4) {
				Point3f newRectStart = getOGLPos(start.x, start.y);
				Point3f newRectEnd = getOGLPos(e.getX(), e.getY());
				currentRect = new Rectangle(new Point((int)newRectStart.getX(), (int)newRectStart.getY()));
				currentRect.add(newRectEnd.getX(), newRectEnd.getY());
				canvas.display();
			} else if (button == 2) {
				int deltax = start.x - e.getX();
				int deltay = start.y - e.getY();
				start.x = e.getX();
				start.y = e.getY();
				Point3f center = getOGLPos(viewport[2]/2, viewport[3]/2);
				Point3f excenter = getOGLPos(viewport[2]/2+deltax, viewport[3]/2+deltay);
				float glDeltaX = excenter.x - center.x;
				float glDeltaY = excenter.y - center.y;
				viewBounds = new Rect(viewBounds.minX + glDeltaX, viewBounds.minY + glDeltaY, viewBounds.maxX + glDeltaX, viewBounds.maxY + glDeltaY);
				canvas.display();
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			int mbutton = e.getButton();
			String function = "";
			switch (mbutton) {
			case 1:
				function = OTFClientControl.getInstance().getOTFVisConfig().getLeftMouseFunc();
				break;
			case 2:
				function = OTFClientControl.getInstance().getOTFVisConfig().getMiddleMouseFunc();
				break;
			case 3:
				function = OTFClientControl.getInstance().getOTFVisConfig().getRightMouseFunc();
				break;
			}
			if(function.equals("Zoom")) { 
			//	if (OTFClientControl.getInstance().getOTFVisConfig().isMapOverlayMode()) {
					// Zooming via a rectangle is disabled in map overlay mode,
					// because we can only zoom in straight powers of two. Compare Google maps.
		//			button = 0;
	//			} else {
					button = 1;
//				}
			}
			else if (function.equals("Pan")) button = 2;
			else if (function.equals("Menu")) button = 3;
			else if (function.equals("Select")) button = 4;
			else button = 0;
			start = new Point(x, y);
			alpha = 1.0f;
			currentRect = null;
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// update screen one last time
			mouseDragged(e);
			Rectangle screenRect = new Rectangle(start);
			screenRect.add(e.getPoint());
			if ((screenRect.getHeight() > 10)&& (screenRect.getWidth() > 10)) {
				if (button == 1 || button == 4) {
					if (button == 1) {
						int startxy[] = new int[]{start.x, start.y};
						canvas.getNativeSurface().convertToPixelUnits(startxy);
						int endxy[] = new int[]{e.getX(), e.getY()};
						canvas.getNativeSurface().convertToPixelUnits(endxy);
						int deltax = Math.abs(startxy[0] - endxy[0]);
						int deltay = Math.abs(startxy[1] - endxy[1]);
						double ratio =( (startxy[1] - endxy[1]) > 0 ? 1:0) + Math.max((double)deltax/viewport[2], (double)deltay/viewport[3]);
						Rectangle2D scaledNewViewBounds = quadTreeRectToRectangle2D(viewBounds.scale(ratio - 1, ratio - 1));
						Rectangle2D scaledAndTranslatedNewViewBounds = new Rectangle2D.Double(scaledNewViewBounds.getX() + (currentRect.getCenterX() - viewBounds.centerX), scaledNewViewBounds.getY() + (currentRect.getCenterY() - viewBounds.centerY), scaledNewViewBounds.getWidth(), scaledNewViewBounds.getHeight());
						Animator viewBoundsAnimator = PropertySetter.createAnimator(2020, OTFOGLDrawer.this, "viewBounds", quadTreeRectToRectangle2D(viewBounds), scaledAndTranslatedNewViewBounds);
						viewBoundsAnimator.start();
						Animator rectFader = PropertySetter.createAnimator(2020, OTFOGLDrawer.this, "alpha", 1.0f, 0.f);
						rectFader.setStartDelay(200);
						rectFader.setAcceleration(0.4f);
						rectFader.start();
					} else {
						handleClick(currentRect, button);
						currentRect = null;
						setAlpha(0);
					}
				}
			} else {
				Point3f newcameraStart = getOGLPos(start.x, start.y);
				Point2D.Double point = new Point2D.Double(newcameraStart.getX(), newcameraStart.getY());
				handleClick(point, button, e);
				currentRect = null;
			}
			button = 0;
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			scaleNetworkRelative((float) Math.pow(2.0f,e.getWheelRotation()));
		}

	}

	static public Texture createTexture(GL2 gl, final InputStream data) {
		Texture t = null;
		try {
			t = TextureIO.newTexture(data, true, null);
			t.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
			t.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
		} catch (IOException e) {
			log.error("Error loading Texture from stream.", e);
		}
		try {
			data.close();
		} catch (IOException e) {
			log.warn("Exception when closing resource.", e);
		}
		return t;
	}

	static public Texture createTexture(GL gl, String filename) {
		Texture t = null;
		if (filename.startsWith("./res/")){
			filename = filename.substring(6);
		}
		try {
			t = TextureIO.newTexture(MatsimResource.getAsInputStream(filename),
					true, null);
			t.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
			t.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
		} catch (IOException e) {
			log.error("Error loading " + filename, e);
		}
		return t;
	}

	private final static Logger log = Logger.getLogger(OTFOGLDrawer.class);

	private double[] modelview = new double[16];

	private double[] projection = new double[16];

	private int[] viewport = new int[4];

	private QuadTree.Rect viewBounds = null;

	private Point start = null;

	private Rectangle currentRect = null;

	private double scale = 1.;

	private int button = 0;

	private Texture marker = null;

	private float alpha = 1.0f;

	private boolean glInited = false;

	private int nRedrawn = 0;

    private final OTFClientQuadTree clientQ;

	private String lastTime = "";

	private int lastShot = -1;

	private final List<OTFGLAbstractDrawable> overlayItems = new ArrayList<OTFGLAbstractDrawable>();

	private OTFQueryHandler queryHandler = null;

	private final GLAutoDrawable canvas;

	private final OTFScaleBarDrawer scaleBar;

	private BufferedImage current;

	private ZoomEntry lastZoom = null;

	private JDialog zoomD;

	private int now;

	private SceneGraph currentSceneGraph = null;

	private OTFHostControlBar hostControlBar;

	private Collection<ChangeListener> changeListeners = new ArrayList<ChangeListener>();

	private float oldWidth = 0.0f;

	private float oldHeight = 0.0f;

	private TextRenderer textRenderer;

    private int statusWidth;

	private OTFVisConfigGroup otfVisConfig;
	
	private boolean includeLogo = true;
	
	private int screenshotInterval = 1;
	
	private int timeOfLastScreenshot = Integer.MAX_VALUE;

	public OTFOGLDrawer(OTFClientQuadTree clientQ, OTFHostControlBar hostControlBar, OTFVisConfigGroup otfVisConfig, GLAutoDrawable canvas) {
		Font font = new Font("SansSerif", Font.PLAIN, 32);
		this.textRenderer = new TextRenderer(font, true, false);
		this.clientQ = clientQ;
		this.hostControlBar = hostControlBar;
		this.otfVisConfig = otfVisConfig;
		this.canvas = canvas;
		canvas.addGLEventListener(this);

		VisGUIMouseHandler mouseMan = new VisGUIMouseHandler();
		((Component) canvas).addMouseListener(mouseMan);
		((Component) canvas).addMouseMotionListener(mouseMan);
		((Component) canvas).addMouseWheelListener(mouseMan);
		this.scaleBar = new OTFScaleBarDrawer();
		if (includeLogo) {
			OTFGLOverlay matsimLogo = new OTFGLOverlay("matsim_logo_blue.png", -0.03f, 0.05f, 1.5f, false);
			this.overlayItems.add(matsimLogo);
		}
		Rectangle2D initialZoom = otfVisConfig.getZoomValue("*Initial*");
		if (initialZoom != null) {
			this.setViewBounds(initialZoom);
		}
	}

	public void addChangeListener(ChangeListener changeListener) {
		this.changeListeners.add(changeListener);
	}

	public void clearCache() {
		this.clientQ.clearCache();
	}

	public static GLAutoDrawable createGLCanvas(OTFVisConfigGroup otfVisConfig) {
		GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
		if (otfVisConfig.isMapOverlayMode()) {
            // A GLJPanel is an OpenGL component which is "more Swing compatible" than a GLCanvas.
            // The JOGL doc says the tradeoff is that it is slower than a GLCanvas.
            // We use it if we want to put map tiles behind the agent drawer, because it can be made translucent!
            GLJPanel glJPanel = new GLJPanel(caps);
            if (otfVisConfig.isMapOverlayMode()) {
                glJPanel.setOpaque(false); // So that the map shines through
            }
			return glJPanel;
		} else {
			// This is the default JOGL component. JOGL doc recommends using it if you do not need a GLJPanel.
			return new GLCanvas(caps);
		}
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		int time = this.hostControlBar.getOTFHostControl().getSimTime();
		if (time != -1) {
			this.now = time;
			this.lastTime = Time.writeTime(time, ':');
		}
		QuadTree.Rect rect;
		if (nRedrawn > 0) {
			rect = this.getViewBoundsAsQuadTreeRect();
		} else {
			// The first time redraw() is called, it is important that clientQ.getSceneGraph() is called with the whole area rather with what may be visible.
			// This is because the display-list based StaticNetLayer is initialized then, and it must contain the whole network.
			// Secondly, we can't really know which part is visible the first time, because the window hasn't been opened and doesn't know its size yet.
			// So we pass the size of the whole network here and don't rely on anybody else for that.
			// michaz May '11
			rect = new QuadTree.Rect((float)clientQ.getMinEasting(), (float)clientQ.getMinNorthing(), (float)clientQ.getMaxEasting(), (float)clientQ.getMaxNorthing());
			this.hostControlBar.getOTFHostControl().fetchTimeAndStatus();
		}
		this.currentSceneGraph  = this.clientQ.getSceneGraph(time, rect);
		if (this.queryHandler != null) {
			this.queryHandler.updateQueries();
		}
		hostControlBar.updateScaleLabel();
		GL2 gl = drawable.getGL().getGL2();
		OTFGLAbstractDrawable.setGl(drawable);
		float[] components = otfVisConfig.getBackgroundColor().getColorComponents(new float[4]);
		gl.glClearColor(components[0], components[1], components[2], components[3]);
		gl.glClear( GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_FILL);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		this.setFrustrum(gl);
		components = otfVisConfig.getNetworkColor().getColorComponents(components);
		gl.glColor4d(components[0], components[1], components[2], components[3]);
		if (this.currentSceneGraph != null) {
			this.currentSceneGraph.draw();
		}
		if (this.queryHandler != null) {
			this.queryHandler.drawQueries(this);
		}

		if (otfVisConfig.isDrawingLinkIds() && isZoomBigEnoughForLabels()) {
			Map<Coord, String> coordStringPairs = findVisibleLinks();
			displayLinkIds(coordStringPairs, drawable);
		}

		if (otfVisConfig.drawTime()) {
			drawFrameRate(drawable);
		}

		if((this.currentRect != null) && (this.alpha >= 0.f)){
			gl.glEnable(GL2.GL_BLEND);
			gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			this.renderFace(gl, this.marker);
			gl.glDisable(GL2.GL_BLEND);
		} else {
			this.currentRect = null;
			this.alpha = 1.0f;
		}

		if(otfVisConfig.drawOverlays()) {
			for (OTFGLAbstractDrawable item : this.overlayItems) {
				item.draw();
			}
		}

		if (otfVisConfig.drawScaleBar()) {
			this.scaleBar.draw();
		}

		if (otfVisConfig.renderImages() && (this.lastShot < this.now)){
			this.lastShot = this.now;
			String nr = String.format("%07d", this.now);
			try {
				if (this.now % screenshotInterval == 0 && this.now <= timeOfLastScreenshot) {
					AWTGLReadBufferUtil glReadBufferUtil = new AWTGLReadBufferUtil(gl.getGLProfile(), true);
					glReadBufferUtil.readPixels(gl, false);
					glReadBufferUtil.write(new File("movie" + this + " Frame" + nr + ".jpg"));
				}
			} catch (GLException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// could happen for folded displays on split screen... ignore
			}
		}
		if (this.current == null) {
			AWTGLReadBufferUtil glReadBufferUtil = new AWTGLReadBufferUtil(gl.getGLProfile(), true);
			this.current = glReadBufferUtil.readPixelsToBufferedImage(gl, false);
		}
		gl.glDisable(GL.GL_BLEND);
		if (viewBounds != null) { // has glInit arrived yet? Otherwise we are not yet fully initialized. :-(
			fireChangeListeners();
		}
		++nRedrawn;
	}
	
	private void displayLinkIds(Map<Coord, String> linkIds, GLAutoDrawable glAutoDrawable) {
		String testText = "0000000";
		Rectangle2D test = textRenderer.getBounds(testText);
		Map<Coord, Boolean> xymap = new HashMap<Coord, Boolean>(); // Why is here a Map used, and not a Set?
		double xRaster = test.getWidth(), yRaster = test.getHeight();
		for( Entry<Coord, String> e : linkIds.entrySet()) {
			Coord coord = e.getKey();
			String linkId = e.getValue();
			float east = (float)coord.getX() ;
			float north = (float)coord.getY() ;
			float textX = (float) (((int)(east / xRaster) +1)*xRaster);
			float textY = north -(float)(north % yRaster) +80;
			Coord text = new Coord((double) textX, (double) textY);
			int i = 1;
			while (xymap.get(text) != null) {
				text = new Coord((double) textX, (double) (i * (float) yRaster + textY));
				if(xymap.get(text) == null) break;
				text = new Coord((double) (textX + i * (float) xRaster), (double) textY);
				if(xymap.get(text) == null) break;
				i++;
			}
			xymap.put(text, Boolean.TRUE);
            GL2 gl = glAutoDrawable.getGL().getGL2();
			gl.glColor4f(0.f, 0.2f, 1.f, 0.5f);//Blue
			gl.glLineWidth(2);
			gl.glBegin(GL2.GL_LINE_STRIP);
			gl.glVertex3d(east, north, 0);
			gl.glVertex3d((float) text.getX(), (float) text.getY(), 0);
			gl.glEnd();
			InfoText infoText = new InfoText(linkId, (float)text.getX(), (float)text.getY());
			infoText.draw(textRenderer, glAutoDrawable, this.getViewBoundsAsQuadTreeRect());
		}
	}

	private void drawFrameRate(GLAutoDrawable drawable) {
        String status = this.lastTime;

		if (this.statusWidth == 0) {
			// Place it at a fixed offset wrt the upper right corner
			this.statusWidth = (int) this.textRenderer.getBounds("FPS: 10000.00").getWidth();
		}

		// Calculate text location and color
		int x = drawable.getSurfaceWidth() - this.statusWidth - 5;
		int y = drawable.getSurfaceHeight() - 30;

		// Render the text
		this.textRenderer.setColor(Color.DARK_GRAY);
		this.textRenderer.beginRendering(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
		this.textRenderer.draw(status, x, y);
		this.textRenderer.endRendering();
	}

	private Map<Coord, String> findVisibleLinks() {
		Rect rect = this.getViewBoundsAsQuadTreeRect();
		Rectangle2D.Double dest = new Rectangle2D.Double(rect.minX , rect.minY , rect.maxX - rect.minX, rect.maxY - rect.minY);
		CollectDrawLinkId linkIdQuery = new CollectDrawLinkId(dest);
		linkIdQuery.prepare(this.clientQ);
        return linkIdQuery.getLinkIds();
	}

	private void fireChangeListeners() {
		for (ChangeListener changeListener : changeListeners) {
			changeListener.stateChanged(new ChangeEvent(this));
		}
	}

	public SceneGraph getCurrentSceneGraph() {
		return this.currentSceneGraph;
	}

	private Point3f getOGLPos(int x, int y) {
		int[] xy = new int[2]; xy[0] = x; xy[1] = y;
		canvas.getNativeSurface().convertToPixelUnits(xy);
		x = xy[0]; y = xy[1];
		double[] obj_pos = new double[3];
		float winX, winY;//, winZ = cameraStart.getZ();
		float posX, posY;//, posZ;
		double[] w_pos = new double[3];
        winX = x;
		winY = viewport[3] - y;
		GLU glu = new GLU();
		obj_pos[2]=0; // Check view relative z-koord of layer zero == visnet layer
		glu.gluProject( obj_pos[0], obj_pos[1],obj_pos[2], modelview,0, projection,0, viewport,0, w_pos,0);
		glu.gluUnProject( winX, winY, w_pos[2], modelview,0, projection,0, viewport,0, obj_pos,0);
		posX = (float)obj_pos[0];
		posY = (float)obj_pos[1];
		return new Point3f(posX, posY, 0);
	}

	private Coord getPixelsize() {
		Point3f p1 = getOGLPos(300,300);
		Point3f p2 = getOGLPos(301,301);
		return new Coord((double) Math.abs(p2.x - p1.x), (double) Math.abs(p2.y - p1.y));
	}

	public OTFClientQuadTree getQuad() {
		return this.clientQ;
	}

	public double getScale() {
		return OTFOGLDrawer.this.scale;
	}

	public TextRenderer getTextRenderer() {
		return textRenderer;
	}

	public QuadTree.Rect getViewBoundsAsQuadTreeRect() {
		return viewBounds;
	}

	public void handleClick(final Point2D.Double point, int mouseButton, MouseEvent e) {
		if(mouseButton == 4 ){
			this.current = null;

			JPopupMenu popmen = new JPopupMenu();
			JMenuItem menu1 = new JMenuItem( "Zoom");
			menu1.setBackground(Color.lightGray);
			popmen.add( menu1 );
			popmen.addSeparator();
			popmen.add( new AbstractAction("Store Zoom") {
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed( ActionEvent e ) {
					storeZoom(false, "");
				}
			} );
			popmen.add( new AbstractAction("Store inital Zoom") {
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed( ActionEvent e ) {
					storeZoom(false, "*Initial*");
				}
			} );
			popmen.add( new AbstractAction("Store named Zoom...") {
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed( ActionEvent e ) {
					storeZoom(true, "");
				}
			} );
			popmen.addSeparator();
			popmen.add( new AbstractAction("Load Zoom...") {
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed( ActionEvent e ) {
					showZoomDialog();
					if(OTFOGLDrawer.this.lastZoom != null) OTFOGLDrawer.this.setViewBounds(OTFOGLDrawer.this.lastZoom.getZoomstart());
				}
			} );
			popmen.add( new AbstractAction("Delete last Zoom") {
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed( ActionEvent e ) {
					if(OTFOGLDrawer.this.lastZoom != null) {
						otfVisConfig.deleteZoom(OTFOGLDrawer.this.lastZoom);
						OTFOGLDrawer.this.lastZoom = null;
					}
				}
			} );
			popmen.show((Component) this.canvas, e.getX(), e.getY());
			return;
		}
		if(this.queryHandler != null) this.queryHandler.handleClick(point,mouseButton);
	}

	public void handleClick(Rectangle currentRect, int button) {
		if(this.queryHandler != null) this.queryHandler.handleClick(new Rectangle2D.Double(currentRect.getX(), currentRect.getY(), currentRect.getWidth(), currentRect.getHeight()), button);
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		OTFGLAbstractDrawable.setGl(drawable);
		gl.setSwapInterval(0);
		float[] components = otfVisConfig.getBackgroundColor().getColorComponents(new float[4]);
		gl.glClearColor(components[0], components[1], components[2], components[3]);
		gl.glClear( GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

		if (!glInited) {
			// This method can (and will) be called several times.
			// (Gl contexts can change without notice.)
			float minEasting = (float)clientQ.getMinEasting();
			float minNorthing = (float)clientQ.getMinNorthing();
			float maxNorthing = (float)clientQ.getMaxNorthing();
			double aspectRatio = (double) drawable.getSurfaceWidth() / (double) drawable.getSurfaceHeight();
			double pixelRatio = (double) drawable.getSurfaceHeight() / (double) (maxNorthing-minNorthing);
			this.scale = 1.0f / (float) pixelRatio;
			this.viewBounds =  new QuadTree.Rect(minEasting, minNorthing, minEasting + (maxNorthing - minNorthing) * aspectRatio, maxNorthing);
			setZoomToNearestInteger();
			this.hostControlBar.getOTFHostControl().fetchTimeAndStatus();
			int time = this.hostControlBar.getOTFHostControl().getSimTime();
			QuadTree.Rect rect = new QuadTree.Rect((float)clientQ.getMinEasting(), (float)clientQ.getMinNorthing(), (float)clientQ.getMaxEasting(), (float)clientQ.getMaxNorthing());
			this.currentSceneGraph = this.clientQ.getSceneGraph(time, rect);
		}
		marker = OTFOGLDrawer.createTexture(gl, MatsimResource.getAsInputStream("otfvis/marker.png"));
		setFrustrum(gl);
		for (OTFGLAbstractDrawable item : this.overlayItems) {
			item.glInit();
		}
		currentSceneGraph.glInit();
		glInited = true;
	}

	private void setZoomToNearestInteger() {
		int zoom = (int) log2(scale);
		setScale(Math.pow(2, zoom));
	}

	private static double log2 (double scale) {
		return Math.log(scale) / Math.log(2);
	}

	private boolean isZoomBigEnoughForLabels() {
		Coord size = getPixelsize();
		final double cellWidth = otfVisConfig.getLinkWidth();
		final double pixelsizeStreet = 5;
		return (size.getX()*pixelsizeStreet < cellWidth) && (size.getX()*pixelsizeStreet < cellWidth);
	}

	private Rectangle2D quadTreeRectToRectangle2D(QuadTree.Rect viewBounds) {
		return new Rectangle2D.Double(viewBounds.minX, viewBounds.minY, viewBounds.maxX-viewBounds.minX, viewBounds.maxY-viewBounds.minY);
	}

	public void redraw() {
		this.canvas.display();
	}

	private void renderFace(GL2 gl, Texture t) {
		TextureCoords tc = t.getImageTexCoords();
		float tx1 = tc.left();
		float ty1 = tc.top();
		float tx2 = tc.right();
		float ty2 = tc.bottom();
		t.enable(gl);
		t.bind(gl);

		if (button==4) gl.glColor4f(0.8f, 0.2f, 0.2f, alpha);
		else gl.glColor4f(alpha, alpha, alpha, alpha);

		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(tx1, ty1); gl.glVertex2f(currentRect.x, currentRect.y);
		gl.glTexCoord2f(tx2, ty1); gl.glVertex2f(currentRect.x, currentRect.y + currentRect.height);
		gl.glTexCoord2f(tx2, ty2); gl.glVertex2f(currentRect.x + currentRect.width, currentRect.y + currentRect.height);
		gl.glTexCoord2f(tx1, ty2); gl.glVertex2f(currentRect.x + currentRect.width, currentRect.y);
		gl.glEnd();
		t.disable(gl);
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		if (oldWidth != 0.0f) {
			double pixelSizeX = (getViewBoundsAsQuadTreeRect().maxX - getViewBoundsAsQuadTreeRect().minX) / oldWidth;
			double pixelSizeY = (getViewBoundsAsQuadTreeRect().maxY - getViewBoundsAsQuadTreeRect().minY) / oldHeight;
			this.viewBounds = new QuadTree.Rect(getViewBoundsAsQuadTreeRect().minX, getViewBoundsAsQuadTreeRect().maxY - pixelSizeY * height, getViewBoundsAsQuadTreeRect().minX + pixelSizeX * width, getViewBoundsAsQuadTreeRect().maxY);
		}
		oldWidth = width;
		oldHeight = height;
	}

	private void scaleNetworkRelative(double scale) {
		OTFOGLDrawer.this.scale *= scale;
		viewBounds = viewBounds.scale(scale - 1, scale - 1);
		this.canvas.display();
	}

	public void setAlpha(float a){
		this.alpha = a;
		this.canvas.display();
	}

	private void setFrustrum(GL2 gl) {
		GLU glu = new GLU();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluOrtho2D(viewBounds.minX, viewBounds.maxX, viewBounds.minY, viewBounds.maxY);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		// update matrices for mouse position calculation
		gl.glGetDoublev( GL2.GL_MODELVIEW_MATRIX, modelview,0);
		gl.glGetDoublev( GL2.GL_PROJECTION_MATRIX, projection,0);
		gl.glGetIntegerv( GL2.GL_VIEWPORT, viewport,0 );
	}

	public void setQueryHandler(OTFQueryHandler queryHandler) {
		if(queryHandler != null) this.queryHandler = queryHandler;
	}

	public void setScale(double scale) {
		double scaleFactor = scale / this.scale;
		scaleNetworkRelative(scaleFactor);
		hostControlBar.updateScaleLabel();
	}

	public void setViewBounds(Rectangle2D viewBounds) {
		this.viewBounds = new QuadTree.Rect(viewBounds.getMinX(), viewBounds.getMinY(), viewBounds.getMaxX(), viewBounds.getMaxY());
	}
	
	private void showZoomDialog() {
		this.zoomD = new JDialog();
		this.zoomD.setUndecorated(true);
		this.zoomD.setLocationRelativeTo(((Component) this.canvas).getParent());
		Point pD = ((Component) this.canvas).getLocationOnScreen();
		this.zoomD.setLocation(pD);
		this.zoomD.setPreferredSize(((Component) this.canvas).getSize());
		GridLayout gbl = new GridLayout(3,3);
		this.zoomD.getContentPane().setLayout( gbl );
		ArrayList<JButton> buttons = new ArrayList<JButton>();
		final List<ZoomEntry> zooms = otfVisConfig.getZooms();
		log.debug("Number of zooms: " + otfVisConfig.getZooms().size());
		for(int i=0; i<zooms.size();i++) {
			ZoomEntry z = zooms.get(i);
			JButton b = new JButton(z.getName());//icon);
			b.setToolTipText(z.getName());
			b.setPreferredSize(new Dimension(220, 100));
			buttons.add(i, b);
			b.setActionCommand(Integer.toString(i));
			b.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed( ActionEvent e ) {
					int num = Integer.parseInt(e.getActionCommand());
					OTFOGLDrawer.this.lastZoom = zooms.get(num);
					OTFOGLDrawer.this.setViewBounds(OTFOGLDrawer.this.lastZoom.getZoomstart());
					OTFOGLDrawer.this.zoomD.setVisible(false);
				}
			} );
			this.zoomD.getContentPane().add(b);
		}
		JPanel pane = new JPanel();
		JButton bb = new JButton("Cancel");
		bb.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				OTFOGLDrawer.this.lastZoom = null;
				OTFOGLDrawer.this.zoomD.setVisible(false);
			}
		} );
		bb.setPreferredSize(new Dimension(120, 40));
		pane.add(bb);
		this.zoomD.getContentPane().add(pane);
		this.zoomD.doLayout();
		this.zoomD.pack();
		for(int i=0; i<zooms.size();i++) {
			ZoomEntry z = zooms.get(i);
			JButton b = buttons.get(i);
			ImageIcon icon = new ImageIcon(ImageUtil.createThumbnail(z.getSnap(),Math.min(z.getSnap().getWidth(),b.getSize().width)-20));
			b.setIcon(icon);
		}
		this.zoomD.setVisible(true);
	}

	private void storeZoom(boolean withName, String name) {
		Rectangle2D zoomstore = this.quadTreeRectToRectangle2D(this.viewBounds);
		if(withName) {
			final JDialog d = new JDialog((JFrame)null,"Name for this zoom", true);
			JTextField field = new JTextField(20);
			ActionListener al =  new ActionListener() {
				@Override
				public void actionPerformed( ActionEvent e ) {
					d.setVisible(false);
				} };
				field.addActionListener(al);
				d.getContentPane().add(field);
				d.pack();
				d.setVisible(true);
				name = field.getText();
		}
		((Component) this.canvas).repaint();
		BufferedImage image = ImageUtil.createThumbnail(this.current, 300);
		otfVisConfig.addZoom(new ZoomEntry(image,zoomstore, name));
	}
	
	public void setIncludeLogo(boolean includeLogo) {
		this.includeLogo = includeLogo;
	}
	
	public void setScreenshotInterval(int screenshotInterval) {
		this.screenshotInterval = screenshotInterval;
	}
	
	public void setTimeOfLastScreenshot(int timeOfLastScreenshot) {
		this.timeOfLastScreenshot = timeOfLastScreenshot;
	}

	@Override
	public void dispose(GLAutoDrawable arg0) {}

}
