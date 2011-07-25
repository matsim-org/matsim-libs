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

import static javax.media.opengl.GL.GL_COLOR_BUFFER_BIT;
import static javax.media.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static javax.media.opengl.GL.GL_LINEAR;
import static javax.media.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static javax.media.opengl.GL.GL_TEXTURE_MIN_FILTER;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLJPanel;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFClientQuadTree;
import org.matsim.vis.otfvis.gui.OTFHostControlBar;
import org.matsim.vis.otfvis.gui.ZoomEntry;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFQueryHandler;
import org.matsim.vis.otfvis.opengl.gl.InfoText;
import org.matsim.vis.otfvis.opengl.gl.InfoTextContainer;
import org.matsim.vis.otfvis.opengl.gl.Point3f;
import org.matsim.vis.otfvis.opengl.gui.OTFScaleBarDrawer;
import org.matsim.vis.otfvis.opengl.gui.ValueColorizer;
import org.matsim.vis.otfvis.opengl.gui.VisGUIMouseHandler;

import com.sun.opengl.util.ImageUtil;
import com.sun.opengl.util.Screenshot;
import com.sun.opengl.util.j2d.TextRenderer;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureIO;


/**
 * OTFOGLDrawer is responsible for everything that goes on inside the OpenGL context.
 * The main functions are invalidate() and redraw(). The latter will simply redraw a given
 * SceneGraph, whilst invalidate() will update the content.
 * <p/>
 * As far as I can see, there is no invalidate().  kai, feb'11
 *
 * @author dstrippgen
 *
 */
public class OTFOGLDrawer implements OTFDrawer, GLEventListener {

	private final static Logger log = Logger.getLogger(OTFOGLDrawer.class);

	private boolean glInited = false;

	private int nRedrawn = 0;

	private GL gl = null;

	private VisGUIMouseHandler mouseMan = null;

	private final OTFClientQuadTree clientQ;

	private String lastTime = "";

	private int lastShot = -1;

	private final List<OTFGLAbstractDrawable> overlayItems = new ArrayList<OTFGLAbstractDrawable>();

	private StatusTextDrawer statusDrawer = null;

	private OTFQueryHandler queryHandler = null;

	private Component canvas = null;

	private final OTFScaleBarDrawer scaleBar;

	private BufferedImage current;

	private ZoomEntry lastZoom = null;

	private JDialog zoomD;

	private int now;

	private SceneGraph currentSceneGraph = null;

	private OTFHostControlBar hostControlBar;

	private Collection<ChangeListener> changeListeners = new ArrayList<ChangeListener>();

	// Experimental mode for michaz
	public static boolean USE_GLJPANEL = false;

	private float oldWidth = 0.0f;
	private float oldHeight = 0.0f;

	public static class StatusTextDrawer {

		private TextRenderer textRenderer;
		private String status;
		private int statusWidth;
		private final GLAutoDrawable drawable;

		public StatusTextDrawer(GLAutoDrawable drawable) {
			initTextRenderer();
			this.drawable = drawable;
		}

		private void initTextRenderer() {
			// Create the text renderer
			Font font = new Font("SansSerif", Font.PLAIN, 32);
			this.textRenderer = new TextRenderer(font, true, false);
			InfoText.setRenderer(this.textRenderer);
		}

		private void displayStatusText(String text) {
			this.status  = text;

			if (this.statusWidth == 0) {
				// Place it at a fixed offset wrt the upper right corner
				this.statusWidth = (int) this.textRenderer.getBounds("FPS: 10000.00").getWidth();
			}

			// Calculate text location and color
			int x = this.drawable.getWidth() - this.statusWidth - 5;
			int y = this.drawable.getHeight() - 30;

			// Render the text
			this.textRenderer.setColor(Color.DARK_GRAY);
			this.textRenderer.beginRendering(this.drawable.getWidth(), this.drawable.getHeight());
			this.textRenderer.draw(this.status, x, y);
			this.textRenderer.endRendering();
		}

	}


	public static class FastColorizer {

		final int grain;
		Color [] fastValues;
		double minVal, maxVal, valRange;

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

		public FastColorizer(double[] ds, Color[] colors) {
			this(ds, colors, 1000);
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

	public static class RandomColorizer {
		Color [] fastValues;
		private static final Random rand = new Random();

		public RandomColorizer(int size) {
			this.fastValues = new Color[size];
			for (int i = 0; i < size; i++) this.fastValues[i] = new Color(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255));
		}

		public Color getColor(int value) {
			return this.fastValues[value];
		}
	}

	private Component createGLCanvas(final OTFOGLDrawer drawer, final GLCapabilities caps) {
		Component canvas;
		if (USE_GLJPANEL) {
			GLJPanel glJPanel = new GLJPanel(caps);
			glJPanel.addGLEventListener(drawer);
			glJPanel.setOpaque(false);
			canvas = glJPanel;
		} else {
			GLCanvas glCanvas = new GLCanvas(caps);
			glCanvas.addGLEventListener(drawer);
			canvas = glCanvas;
		}
		return canvas;
	}

	public OTFOGLDrawer(OTFClientQuadTree clientQ, OTFHostControlBar hostControlBar) {
		this.clientQ = clientQ;
		this.hostControlBar = hostControlBar;
		GLCapabilities caps = new GLCapabilities();
		this.canvas = createGLCanvas(this, caps);
		this.mouseMan = new VisGUIMouseHandler(this);
		// Don't call this here! It is a straight lie, because the Drawer has now idea at this point what its bounds are (because it isn't visible yet).
		// This was apparently just a hack to fetch the whole area the first time "redraw" is called. Fixed this elsewhere.
		// michaz May '11
		// this.mouseMan.setBounds(null, (float)clientQ.getMinEasting(), (float)clientQ.getMinNorthing(), (float)clientQ.getMaxEasting(), (float)clientQ.getMaxNorthing());
		this.canvas.addMouseListener(this.mouseMan);
		this.canvas.addMouseMotionListener(this.mouseMan);
		this.canvas.addMouseWheelListener(this.mouseMan);
		this.scaleBar = new OTFScaleBarDrawer();

		OTFGLOverlay matsimLogo = new OTFGLOverlay("matsim_logo_blue.png", -0.03f, 0.05f, 1.5f, false);
		this.overlayItems.add(matsimLogo);

		Point3f initialZoom = OTFClientControl.getInstance().getOTFVisConfig().getZoomValue("*Initial*");
		if (initialZoom != null) {
			this.mouseMan.setToNewPos(initialZoom);
		}

	}

	public VisGUIMouseHandler getMouseHandler() {
		return this.mouseMan;
	}

	private void displayLinkIds(Map<Coord, String> linkIds) {
		String testText = "0000000";
		Rectangle2D test = InfoText.getBoundsOf(testText);
		Map<Coord, Boolean> xymap = new HashMap<Coord, Boolean>(); // Why is here a Map used, and not a Set?
		double xRaster = test.getWidth(), yRaster = test.getHeight();
		for( Entry<Coord, String> e : linkIds.entrySet()) {
			Coord coord = e.getKey();
			String linkId = e.getValue();
			float east = (float)coord.getX() ;
			float north = (float)coord.getY() ;
			float textX = (float) (((int)(east / xRaster) +1)*xRaster);
			float textY = north -(float)(north % yRaster) +80;
			CoordImpl text = new CoordImpl(textX,textY);
			int i = 1;
			while (xymap.get(text) != null) {
				text = new CoordImpl(textX,  i* (float)yRaster + textY);
				if(xymap.get(text) == null) break;
				text = new CoordImpl(textX + i* (float)xRaster, textY);
				if(xymap.get(text) == null) break;
				i++;
			}
			xymap.put(text, Boolean.TRUE);
			InfoTextContainer.showTextOnce(linkId, (float)text.getX(), (float)text.getY(), -0.0005f);
			this.gl.glColor4f(0.f, 0.2f, 1.f, 0.5f);//Blue
			this.gl.glLineWidth(2);
			this.gl.glBegin(GL.GL_LINE_STRIP);
			this.gl.glVertex3d(east, north,0);
			this.gl.glVertex3d((float)text.getX(), (float)text.getY(),0);
			this.gl.glEnd();
		}
	}

	private boolean isZoomBigEnoughForLabels() {
		CoordImpl size  = this.mouseMan.getPixelsize();
		final double cellWidth = OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth();
		final double pixelsizeStreet = 5;
		return (size.getX()*pixelsizeStreet < cellWidth) && (size.getX()*pixelsizeStreet < cellWidth);
	}

	private Map<Coord, String> findVisibleLinks() {
		Rect rect = this.mouseMan.getBounds();
		Rectangle2D.Double dest = new Rectangle2D.Double(rect.minX , rect.minY , rect.maxX - rect.minX, rect.maxY - rect.minY);
		CollectDrawLinkId linkIdQuery = new CollectDrawLinkId(dest);
		linkIdQuery.prepare(this.clientQ);
		Map<Coord, String> linkIds = linkIdQuery.getLinkIds();
		return linkIds;
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		float[] components = OTFClientControl.getInstance().getOTFVisConfig().getBackgroundColor().getColorComponents(new float[4]);
		this.gl = drawable.getGL();
		this.gl.glClearColor(components[0], components[1], components[2], components[3]);
		this.gl.glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		this.gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
		this.gl.glEnable(GL.GL_BLEND);
		this.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		this.mouseMan.setFrustrum(this.gl);
		components = OTFClientControl.getInstance().getOTFVisConfig().getNetworkColor().getColorComponents(components);
		this.gl.glColor4d(components[0], components[1], components[2], components[3]);
		if (this.currentSceneGraph != null) {
			this.currentSceneGraph.draw();
		}
		if (this.queryHandler != null) {
			this.queryHandler.drawQueries(this);
		}

		Map<Coord, String> coordStringPairs = findVisibleLinks();
		if (OTFClientControl.getInstance().getOTFVisConfig().isDrawingLinkIds() && isZoomBigEnoughForLabels()) {
			displayLinkIds(coordStringPairs);
		}

		if (OTFClientControl.getInstance().getOTFVisConfig().drawTime()) {
			this.statusDrawer.displayStatusText(this.lastTime);
		}

		this.mouseMan.drawElements(this.gl);

		if(OTFClientControl.getInstance().getOTFVisConfig().drawOverlays()) {
			for (OTFGLAbstractDrawable item : this.overlayItems) {
				item.draw();
			}
		}

		if (OTFClientControl.getInstance().getOTFVisConfig().drawScaleBar()) {
			this.scaleBar.draw();
		}

		if (OTFClientControl.getInstance().getOTFVisConfig().renderImages() && (this.lastShot < this.now)){
			this.lastShot = this.now;
			String nr = String.format("%07d", this.now);
			try {
				Screenshot.writeToFile(new File("movie"+ this +" Frame" + nr + ".jpg"), drawable.getWidth(), drawable.getHeight());
			} catch (GLException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// could happen for folded displays on split screen... ignore
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (this.current == null) {
			this.current = Screenshot.readToBufferedImage(drawable.getWidth(), drawable.getHeight());
		}
		Collection<String> visibleLinkIds = coordStringPairs.values();
		InfoTextContainer.drawInfoTexts(drawable, visibleLinkIds);
		this.gl.glDisable(GL.GL_BLEND);
	}

	@Override
	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		this.gl = drawable.getGL();
		this.gl.setSwapInterval(0);
		float[] components = OTFClientControl.getInstance().getOTFVisConfig().getBackgroundColor().getColorComponents(new float[4]);
		this.gl.glClearColor(components[0], components[1], components[2], components[3]);
		this.gl.glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		if (!glInited) {
			this.mouseMan.setBounds(drawable, (float)clientQ.getMinEasting(), (float)clientQ.getMinNorthing(), (float)clientQ.getMaxEasting(), (float)clientQ.getMaxNorthing());
		}
		this.mouseMan.init(this.gl);

		OTFGLAbstractDrawableReceiver.setGl(this.gl);
		for (OTFGLAbstractDrawable item : this.overlayItems) {
			item.glInit();
		}
		if (currentSceneGraph != null) {
			currentSceneGraph.glInit();
		}
		glInited = true;
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		if (mouseMan.ORTHO) {
			if (oldWidth != 0.0f) {
				double pixelSizeX = (mouseMan.viewBounds.maxX - mouseMan.viewBounds.minX) / oldWidth;
				double pixelSizeY = (mouseMan.viewBounds.maxY - mouseMan.viewBounds.minY) / oldHeight;
				mouseMan.viewBounds = new QuadTree.Rect(mouseMan.viewBounds.minX, mouseMan.viewBounds.maxY - pixelSizeY * height, mouseMan.viewBounds.minX + pixelSizeX * width, mouseMan.viewBounds.maxY);
				redraw();
			}
			oldWidth = width;
			oldHeight = height;
		} else {
			GL gl = drawable.getGL();
			gl.glViewport(0, 0, width, height);
			this.mouseMan.setFrustrum(gl);
			this.statusDrawer = new StatusTextDrawer(drawable);
		}
	}

	private void showZoomDialog() {
		this.zoomD = new JDialog();
		this.zoomD.setUndecorated(true);
		this.zoomD.setLocationRelativeTo(this.canvas.getParent());
		Point pD = this.canvas.getLocationOnScreen();
		this.canvas.getParent();
		this.zoomD.setLocation(pD);
		this.zoomD.setPreferredSize(this.canvas.getSize());
		GridLayout gbl = new GridLayout(3,3);
		this.zoomD.getContentPane().setLayout( gbl );
		ArrayList<JButton> buttons = new ArrayList<JButton>();
		final List<ZoomEntry> zooms = OTFClientControl.getInstance().getOTFVisConfig().getZooms();
		log.debug("Number of zooms: " + OTFClientControl.getInstance().getOTFVisConfig().getZooms().size());
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
					OTFOGLDrawer.this.mouseMan.setToNewPos(OTFOGLDrawer.this.lastZoom.getZoomstart());
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
		Point3f zoomstore = this.mouseMan.getView();
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
		OTFClientControl.getInstance().getOTFVisConfig().addZoom(new ZoomEntry(image,zoomstore, name));

	}

	@Override
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
					if(OTFOGLDrawer.this.lastZoom != null) OTFOGLDrawer.this.mouseMan.setToNewPos(OTFOGLDrawer.this.lastZoom.getZoomstart());
				}
			} );
			popmen.add( new AbstractAction("Delete last Zoom") {
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed( ActionEvent e ) {
					if(OTFOGLDrawer.this.lastZoom != null) {
						OTFClientControl.getInstance().getOTFVisConfig().deleteZoom(OTFOGLDrawer.this.lastZoom);
						OTFOGLDrawer.this.lastZoom = null;
					}
				}
			} );
			popmen.show(this.getComponent(),e.getX(), e.getY());
			return;
		}
		Point2D.Double origPoint = new Point2D.Double(point.x + this.clientQ.offsetEast, point.y + this.clientQ.offsetNorth);
		if(this.queryHandler != null) this.queryHandler.handleClick(origPoint,mouseButton);
	}

	@Override
	public void handleClick(Rectangle currentRect, int button) {
		Rectangle2D.Double origRect = new Rectangle2D.Double(currentRect.x + this.clientQ.offsetEast, currentRect.y + this.clientQ.offsetNorth, currentRect.width, currentRect.height);
		if(this.queryHandler != null) this.queryHandler.handleClick(origRect,button);
	}

	@Override
	public void redraw() {
		int time = this.hostControlBar.getOTFHostControl().getSimTime();
		if (time != -1) {
			this.now = time;
			this.lastTime = Time.writeTime(time, ':');
		}
		QuadTree.Rect rect;
		if (nRedrawn > 0) {
			rect = this.mouseMan.getBounds();
		} else {
			// The first time redraw() is called, it is important that clientQ.getSceneGraph() is called with the whole area rather with what may be visible.
			// This is because the display-list based StaticNetLayer is initialized then, and it must contain the whole network.
			// Secondly, we can't really know which part is visible the first time, because the window hasn't been opened and doesn't know its size yet.
			// So we pass the size of the whole network here and don't rely on anybody else for that.
			// michaz May '11
			rect = new QuadTree.Rect((float)clientQ.getMinEasting(), (float)clientQ.getMinNorthing(), (float)clientQ.getMaxEasting(), (float)clientQ.getMaxNorthing());
		}
		this.currentSceneGraph  = this.clientQ.getSceneGraph(time, rect, this);
		if (this.queryHandler != null) {
			this.queryHandler.updateQueries();
		}
		hostControlBar.updateScaleLabel();
		this.canvas.repaint();
		fireChangeListeners();
		++nRedrawn;
	}

	private void fireChangeListeners() {
		for (ChangeListener changeListener : changeListeners) {
			changeListener.stateChanged(new ChangeEvent(this));
		}
	}

	/**
	 * @return the canvas
	 */
	@Override
	public Component getComponent() {
		return this.canvas;
	}

	public GL getGL() {
		return this.gl;
	}

	@Override
	public OTFClientQuadTree getQuad() {
		return this.clientQ;
	}

	@Override
	public float getScale(){
		return this.mouseMan.getScale();
	}

	@Override
	public void setScale(float scale){
		this.mouseMan.scaleNetwork(scale);
		hostControlBar.updateScaleLabel();
	}


	public Rect getViewBounds() {
		return this.mouseMan.getBounds();
	}

	public SceneGraph getCurrentSceneGraph() {
		return this.currentSceneGraph;
	}

	static public Texture createTexture(String filename) {
		Texture t = null;
		if (filename.startsWith("./res/")){
			filename = filename.substring(6);
		}
		try {
			t = TextureIO.newTexture(MatsimResource.getAsInputStream(filename),
					true, null);
			t.setTexParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			t.setTexParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		} catch (IOException e) {
			log.error("Error loading " + filename, e);
		}
		return t;
	}

	static public Texture createTexture(final InputStream data) {
		Texture t = null;
		try {
			t = TextureIO.newTexture(data, true, null);
			t.setTexParameteri(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			t.setTexParameteri(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
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

	@Override
	public void clearCache() {
		if (this.clientQ != null) this.clientQ.clearCache();
	}

	@Override
	public void setQueryHandler(OTFQueryHandler queryHandler) {
		if(queryHandler != null) this.queryHandler = queryHandler;
	}

	public void addChangeListener(ChangeListener changeListener) {
		this.changeListeners.add(changeListener);
	}

}
