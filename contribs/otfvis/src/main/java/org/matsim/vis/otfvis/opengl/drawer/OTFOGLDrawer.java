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
import org.matsim.vis.otfvis.gui.OTFHostControl;
import org.matsim.vis.otfvis.gui.OTFQueryControl;
import org.matsim.vis.otfvis.opengl.gl.GLUtils;
import org.matsim.vis.otfvis.opengl.gl.Point3f;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleStaticNetLayer;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OTFOGLDrawer implements GLEventListener {

	private final static Logger log = Logger.getLogger(OTFOGLDrawer.class);

	private double[] modelview = new double[16];

	private double[] projection = new double[16];

	private int[] viewport = new int[4];

	private QuadTree.Rect viewBounds = null;

	private Point start = null;

	private Rectangle currentRect = null;

	private double scale = 1.;
	private double translateX = 0.;
	private double translateY = 0.;

	private int button = 0;

	private Texture marker = null;

	private float alpha = 1.0f;

	private boolean glInited = false;

	private int nRedrawn = 0;

    private final OTFClientQuadTree clientQ;

	private OTFQueryControl queryHandler = null;

	public GLAutoDrawable getCanvas() {
		return (GLAutoDrawable) canvas;
	}

	private final Component canvas;

	private BufferedImage current;

	private ZoomEntry lastZoom = null;

	private JDialog zoomD;

	private SceneGraph currentSceneGraph = null;

	private OTFHostControl hostControlBar;

	private Collection<ChangeListener> changeListeners = new ArrayList<>();

	private float oldWidth = 0.0f;

	private float oldHeight = 0.0f;

	private TextRenderer textRenderer;

    private int statusWidth;

	private OTFVisConfigGroup otfVisConfig;

	public OTFOGLDrawer(OTFClientQuadTree clientQ, final OTFVisConfigGroup otfVisConfig, Component canvas, OTFHostControl otfHostControl) {
		OTFClientControl.getInstance().setMainOTFDrawer(this);
		this.clientQ = clientQ;
		this.hostControlBar = otfHostControl;
		this.otfVisConfig = otfVisConfig;
		this.canvas = canvas;
		((GLAutoDrawable) canvas).addGLEventListener(new OGLSimpleStaticNetLayer());
		((GLAutoDrawable) canvas).addGLEventListener(this);
		((GLAutoDrawable) canvas).addGLEventListener(new OTFScaleBarDrawer());
		((GLAutoDrawable) canvas).addGLEventListener(new OTFGLOverlay("/res/matsim_logo_blue.png", -0.03f, 0.05f, 1.5f, false));
		((GLAutoDrawable) canvas).addGLEventListener(new ScreenshotTaker(this.hostControlBar));
		clientQ.getConstData();

		MouseInputAdapter mouseMan = new MouseInputAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (button == 1 || button == 4) {
					Point3f newRectStart = getOGLPos(start.x, start.y);
					Point3f newRectEnd = getOGLPos(e.getX(), e.getY());
					currentRect = new Rectangle(new Point((int)newRectStart.getX(), (int)newRectStart.getY()));
					currentRect.add(newRectEnd.getX(), newRectEnd.getY());
					OTFOGLDrawer.this.canvas.repaint();
				} else if (button == 2) {
					int deltax = start.x - e.getX();
					int deltay = start.y - e.getY();
					start.x = e.getX();
					start.y = e.getY();
					Point3f center = getOGLPos(viewport[2]/2, viewport[3]/2);
					Point3f excenter = getOGLPos(viewport[2]/2+deltax, viewport[3]/2+deltay);
					float glDeltaX = excenter.x - center.x;
					float glDeltaY = excenter.y - center.y;
					setTranslateX(getTranslateX() + glDeltaX);
					setTranslateY(getTranslateY() + glDeltaY);
					OTFOGLDrawer.this.canvas.repaint();
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				String function;
				switch (e.getButton()) {
					case 1:
						function = OTFClientControl.getInstance().getOTFVisConfig().getLeftMouseFunc();
						break;
					case 2:
						function = OTFClientControl.getInstance().getOTFVisConfig().getMiddleMouseFunc();
						break;
					case 3:
						function = OTFClientControl.getInstance().getOTFVisConfig().getRightMouseFunc();
						break;
					default:
						throw new RuntimeException();
				}
				switch (function) {
					case "Zoom":
						button = 1;
						break;
					case "Pan":
						button = 2;
						break;
					case "Menu":
						button = 3;
						break;
					case "Select":
						button = 4;
						break;
					default:
						button = 0;
						break;
				}
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
							int startxy[] = new int[]{OTFOGLDrawer.this.start.x, OTFOGLDrawer.this.start.y};
							((GLAutoDrawable) OTFOGLDrawer.this.canvas).getNativeSurface().convertToPixelUnits(startxy);
							int endxy[] = new int[]{e.getX(), e.getY()};
							((GLAutoDrawable) OTFOGLDrawer.this.canvas).getNativeSurface().convertToPixelUnits(endxy);
							int deltax = Math.abs(startxy[0] - endxy[0]);
							int deltay = Math.abs(startxy[1] - endxy[1]);
							double ratio =( (startxy[1] - endxy[1]) > 0 ? 1:0) + Math.max((double)deltax/viewport[2], (double)deltay/viewport[3]);
							Point3f start = getOGLPos(OTFOGLDrawer.this.start.x, OTFOGLDrawer.this.start.y);
							Point3f end = getOGLPos(e.getX(), e.getY());
							double translatex = (start.getX() + end.getX()) / 2 - getViewBoundsAsQuadTreeRect().centerX;
							double translatey = (start.getY() + end.getY()) / 2 - getViewBoundsAsQuadTreeRect().centerY;
							double newScale = getScale() * ratio;
							if (otfVisConfig.isMapOverlayMode()) {
								newScale = nearestPowerOfTwo(newScale);
							}
							PropertySetter.createAnimator(2020, OTFOGLDrawer.this, "scale", newScale).start();
							PropertySetter.createAnimator(2020, OTFOGLDrawer.this, "translateX", getTranslateX() + translatex).start();
							PropertySetter.createAnimator(2020, OTFOGLDrawer.this, "translateY", getTranslateY() + translatey).start();
							Animator rectFader = PropertySetter.createAnimator(2020, OTFOGLDrawer.this, "alpha", 1.0f, 0.f);
							rectFader.setStartDelay(200);
							rectFader.setAcceleration(0.4f);
							rectFader.start();
						} else {
							if(OTFOGLDrawer.this.queryHandler != null) OTFOGLDrawer.this.queryHandler.handleClick(new Rectangle2D.Double(currentRect.getX(), currentRect.getY(), currentRect.getWidth(), currentRect.getHeight()), button);
							currentRect = null;
							setAlpha(0);
						}
					}
				} else {
					Point3f newcameraStart = getOGLPos(start.x, start.y);
					Point2D.Double point = new Point2D.Double(newcameraStart.getX(), newcameraStart.getY());
					if(button == 4 ){
						OTFOGLDrawer.this.current = null;

						JPopupMenu popmen = new JPopupMenu();
						JMenuItem menu1 = new JMenuItem( "Zoom");
						menu1.setBackground(Color.lightGray);
						popmen.add( menu1 );
						popmen.addSeparator();
						popmen.add( new AbstractAction("Store Zoom") {
							private static final long serialVersionUID = 1L;
							@Override
							public void actionPerformed( ActionEvent e11) {
								storeZoom(false, "");
							}
						} );
						popmen.add( new AbstractAction("Store inital Zoom") {
							private static final long serialVersionUID = 1L;
							@Override
							public void actionPerformed( ActionEvent e11) {
								storeZoom(false, "*Initial*");
							}
						} );
						popmen.add( new AbstractAction("Store named Zoom...") {
							private static final long serialVersionUID = 1L;
							@Override
							public void actionPerformed( ActionEvent e11) {
								storeZoom(true, "");
							}
						} );
						popmen.addSeparator();
						popmen.add( new AbstractAction("Load Zoom...") {
							private static final long serialVersionUID = 1L;
							@Override
							public void actionPerformed( ActionEvent e11) {
								showZoomDialog();
								if(OTFOGLDrawer.this.lastZoom != null) {
									Rectangle2D viewBounds1 = OTFOGLDrawer.this.lastZoom.getZoomstart();
									OTFOGLDrawer.this.viewBounds = new Rect(viewBounds1.getMinX(), viewBounds1.getMinY(), viewBounds1.getMaxX(), viewBounds1.getMaxY());
								}
							}
						} );
						popmen.add( new AbstractAction("Delete last Zoom") {
							private static final long serialVersionUID = 1L;
							@Override
							public void actionPerformed( ActionEvent e11) {
								if(OTFOGLDrawer.this.lastZoom != null) {
									OTFOGLDrawer.this.otfVisConfig.deleteZoom(OTFOGLDrawer.this.lastZoom);
									OTFOGLDrawer.this.lastZoom = null;
								}
							}
						} );
						popmen.show(OTFOGLDrawer.this.canvas, e.getX(), e.getY());
					}
					if(OTFOGLDrawer.this.queryHandler != null) OTFOGLDrawer.this.queryHandler.handleClick(point, button);
					currentRect = null;
				}
				button = 0;
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				scaleNetworkRelative((float) Math.pow(2.0f,e.getWheelRotation()));
			}
		};
		canvas.addMouseListener(mouseMan);
		canvas.addMouseMotionListener(mouseMan);
		canvas.addMouseWheelListener(mouseMan);
		Rectangle2D initialZoom = otfVisConfig.getZoomValue("*Initial*");
		if (initialZoom != null) {
			this.viewBounds = new Rect(initialZoom.getMinX(), initialZoom.getMinY(), initialZoom.getMaxX(), initialZoom.getMaxY());
		}
	}

	public void addChangeListener(ChangeListener changeListener) {
		this.changeListeners.add(changeListener);
	}

	public static Component createGLCanvas(OTFVisConfigGroup otfVisConfig) {
		//turn off HiDPI uiScaling in Windows (issue MATSIM-875)
		if (System.getProperty("os.name").startsWith("Windows")) {
			System.setProperty("sun.java2d.uiScale", "1.0");
		}

		GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
		if (otfVisConfig.isMapOverlayMode()) {
			caps.setBackgroundOpaque(false);

			// A GLJPanel is an OpenGL component which is "more Swing compatible" than a GLCanvas.
            // The JOGL doc says the tradeoff is that it is slower than a GLCanvas.
            // We use it if we want to put map tiles behind the agent drawer, because it can be made translucent!
			GLJPanel glJPanel = new GLJPanel(caps);
			glJPanel.setOpaque(false); // So that the map shines through
			return glJPanel;
		} else {
			// This is the default JOGL component. JOGL doc recommends using it if you do not need a GLJPanel.
			return new GLCanvas(caps);
		}
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		int now = this.hostControlBar.getSimTime();
		QuadTree.Rect rect;
		if (nRedrawn > 0) {
			rect = this.getViewBoundsAsQuadTreeRect();
		} else {
			// The first time display() is called, it is important that clientQ.getSceneGraph() is called with the whole area rather with what may be visible.
			// This is because the display-list based StaticNetLayer is initialized then, and it must contain the whole network.
			// Secondly, we can't really know which part is visible the first time, because the window hasn't been opened and doesn't know its size yet.
			// So we pass the size of the whole network here and don't rely on anybody else for that.
			// michaz May '11
			rect = new QuadTree.Rect((float)clientQ.getMinEasting(), (float)clientQ.getMinNorthing(), (float)clientQ.getMaxEasting(), (float)clientQ.getMaxNorthing());
		}
		this.currentSceneGraph  = this.clientQ.getSceneGraph(now, rect);
		if (this.queryHandler != null) {
			this.queryHandler.updateQueries();
		}
		GL2 gl = drawable.getGL().getGL2();
		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_FILL);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		this.setFrustrum(gl);
		if (this.currentSceneGraph != null) {
			this.currentSceneGraph.draw();
		}

		if (otfVisConfig.drawTime()) {
			drawFrameRate(drawable, now);
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

	private void drawFrameRate(GLAutoDrawable drawable, int now) {

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
		this.textRenderer.draw(Time.writeTime(now, ':'), x, y);
		this.textRenderer.endRendering();
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
		((GLAutoDrawable) canvas).getNativeSurface().convertToPixelUnits(xy);
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

	public Coord getPixelsize() {
		Point3f p1 = getOGLPos(300,300);
		Point3f p2 = getOGLPos(301,301);
		return new Coord((double) Math.abs(p2.x - p1.x), (double) Math.abs(p2.y - p1.y));
	}

	public OTFClientQuadTree getQuad() {
		return this.clientQ;
	}

	public double getScale() {
		return this.scale;
	}

	public QuadTree.Rect getViewBoundsAsQuadTreeRect() {
		return viewBounds;
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		this.textRenderer = new TextRenderer(new Font("SansSerif", Font.PLAIN, 32), true, false);
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
			if (otfVisConfig.isMapOverlayMode()) {
				setScale(nearestPowerOfTwo(this.scale));
			}
			int time = this.hostControlBar.getSimTime();
			QuadTree.Rect rect = new QuadTree.Rect((float)clientQ.getMinEasting(), (float)clientQ.getMinNorthing(), (float)clientQ.getMaxEasting(), (float)clientQ.getMaxNorthing());
			this.currentSceneGraph = this.clientQ.getSceneGraph(time, rect);
		}
		marker = GLUtils.createTexture(gl, MatsimResource.getAsInputStream("otfvis/marker.png"));
		currentSceneGraph.glInit();
		glInited = true;
	}

	private double nearestPowerOfTwo(double scale) {
		return Math.pow(2, (int) log2(scale));
	}

	private static double log2 (double scale) {
		return Math.log(scale) / Math.log(2);
	}

	private Rectangle2D quadTreeRectToRectangle2D(QuadTree.Rect viewBounds) {
		return new Rectangle2D.Double(viewBounds.minX, viewBounds.minY, viewBounds.maxX-viewBounds.minX, viewBounds.maxY-viewBounds.minY);
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
		this.canvas.repaint();
	}

	public void setAlpha(float a){
		this.alpha = a;
		this.canvas.repaint();
	}

	public void setFrustrum(GL2 gl) {
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

	public void setQueryHandler(OTFQueryControl queryHandler) {
		this.queryHandler = queryHandler;
		((GLAutoDrawable) canvas).addGLEventListener(((GLAutoDrawable) canvas).getGLEventListenerCount()-2, queryHandler);
	}

	public void setScale(double scale) {
		double scaleFactor = scale / this.scale;
		scaleNetworkRelative(scaleFactor);
	}

	private void showZoomDialog() {
		this.zoomD = new JDialog();
		this.zoomD.setUndecorated(true);
		this.zoomD.setLocationRelativeTo(this.canvas.getParent());
		Point pD = this.canvas.getLocationOnScreen();
		this.zoomD.setLocation(pD);
		this.zoomD.setPreferredSize(this.canvas.getSize());
		GridLayout gbl = new GridLayout(3,3);
		this.zoomD.getContentPane().setLayout( gbl );
		ArrayList<JButton> buttons = new ArrayList<>();
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
					Rectangle2D viewBounds1 = OTFOGLDrawer.this.lastZoom.getZoomstart();
					OTFOGLDrawer.this.viewBounds = new Rect(viewBounds1.getMinX(), viewBounds1.getMinY(), viewBounds1.getMaxX(), viewBounds1.getMaxY());
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
		this.canvas.repaint();
		BufferedImage image = ImageUtil.createThumbnail(this.current, 300);
		otfVisConfig.addZoom(new ZoomEntry(image,zoomstore, name));
	}

	@Override
	public void dispose(GLAutoDrawable arg0) {}

	public double getTranslateX() {
		return translateX;
	}

	public void setTranslateX(double translateX) {
		this.viewBounds = new Rect(viewBounds.minX + (translateX - this.translateX), viewBounds.minY, viewBounds.maxX + (translateX - this.translateX), viewBounds.maxY);
		this.translateX = translateX;
	}

	public double getTranslateY() {
		return translateY;
	}

	public void setTranslateY(double translateY) {
		this.viewBounds = new Rect(viewBounds.minX, viewBounds.minY + (translateY - this.translateY), viewBounds.maxX, viewBounds.maxY + (translateY - this.translateY));
		this.translateY = translateY;
	}
}
