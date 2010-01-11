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
import static javax.media.opengl.GL.GL_VIEWPORT;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimResource;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.netvis.renderers.ValueColorizer;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFClientQuad;
import org.matsim.vis.otfvis.data.OTFDataSimpleAgentReceiver;
import org.matsim.vis.otfvis.data.OTFClientQuad.ClassCountExecutor;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.gui.OTFVisConfig.ZoomEntry;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFQueryHandler;
import org.matsim.vis.otfvis.opengl.gl.InfoText;
import org.matsim.vis.otfvis.opengl.gl.InfoTextContainer;
import org.matsim.vis.otfvis.opengl.gl.Point3f;
import org.matsim.vis.otfvis.opengl.gui.OTFScaleBarDrawer;
import org.matsim.vis.otfvis.opengl.gui.VisGUIMouseHandler;

import com.sun.opengl.util.ImageUtil;
import com.sun.opengl.util.Screenshot;
import com.sun.opengl.util.j2d.TextRenderer;
import com.sun.opengl.util.j2d.TextureRenderer;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureIO;

/**
 * Call TextRenderHack to fix the TextRender problem on the Mac This class should only be used until
 * Apple puts out their fix.
 * 
 * @author Jeff Addison - Southgate Software Ltd. www.southgatesoftware.com
 */
class TextRenderHack
{
	/**
	 * Call this function in your drawing code to fix the TextRender rendering problem on the Mac
	 * 
	 * @param tr Text Renderer to fix
	 */
	public static void fixIt(TextRenderer tr)
	{
		// Get the OS Name
		String osName = System.getProperty("os.name");

		// Only fix it if it's broke :)
		if ((osName != null) && osName.toLowerCase().contains("mac"))
		{
			// Call the TextRenderer's private function getBackingStore to get the backingStore
			TextureRenderer backingStore = (TextureRenderer) invokePrivateMethod(tr, "getBackingStore",
					null);

			// If we have a valid backing store, mark the entire thing dirty.
			if (backingStore != null)
			{
				backingStore.markDirty(0, 0, backingStore.getWidth(), backingStore.getHeight());
			}
		}
	}

	/**
	 * Invokes a private method on and Object.
	 * 
	 * @param o Object to call private method on
	 * @param methodName Name of the method to call
	 * @param params Array of parameters to be passed to the function
	 * @return Object that the method called normally returns (Cast to proper type) NOTE: This function
	 *         was found on the Internet. Lost the link so we are unable to give the author the proper
	 *         credit.
	 */
	public static Object invokePrivateMethod(Object o, String methodName, Object[] params)
	{
		// Go and find the private method...
		final Method methods[] = o.getClass().getDeclaredMethods();
		for (int i = 0; i < methods.length; ++i)
		{
			if (methodName.equals(methods[i].getName()))
			{
				try
				{
					methods[i].setAccessible(true);
					return methods[i].invoke(o, params);
				}
				catch (IllegalAccessException ex)
				{
					System.out.println("IllegalAccessException accessing " + methodName);
				}
				catch (InvocationTargetException ite)
				{
					System.out.println("InvocationTargetException accessing " + methodName);
				}
			}
		}
		return null;
	}

}

class OTFGLOverlay extends OTFGLDrawableImpl {
	private final InputStream texture;
	private final float relX;
	private final float relY;
	private final boolean opaque;
	private final float size;
	private Texture t = null;

	OTFGLOverlay(final InputStream texture, float relX, float relY, float size, boolean opaque) {
		this.texture = texture;
		this.relX =relX;
		this.relY = relY;
		this.size = size;
		this.opaque = opaque;
	}

	public void onDraw(GL gl) {
		if(this.t == null) {
			this.t = OTFOGLDrawer.createTexture(this.texture);
		}
		int[] viewport = new int[4];
		gl.glGetIntegerv( GL_VIEWPORT, viewport ,0 );
		float height = this.size*this.t.getHeight()/viewport[3];
		float length = this.size*this.t.getWidth()/viewport[2];
		int z = 0;
		float startX = this.relX >= 0 ? -1.f + this.relX : 1.f -length +this.relX; 
		float startY = this.relY >= 0 ? -1.f + this.relY : 1.f -height +this.relY; 

		gl.glColor4d(1,1,1,1);

		//push 1:1 screen matrix
		gl.glMatrixMode( GL.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glMatrixMode( GL.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		//drawQuad
		if(!this.opaque) {
			this.gl.glEnable(GL.GL_BLEND);
			this.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		}

		this.t.enable();
		this.t.bind();
		gl.glBegin(GL.GL_QUADS);
		gl.glTexCoord2f(0,1); gl.glVertex3f(startX, startY, z);
		gl.glTexCoord2f(1,1); gl.glVertex3f(startX + length, startY, z);
		gl.glTexCoord2f(1,0); gl.glVertex3f(startX + length, startY + height, z);
		gl.glTexCoord2f(0,0); gl.glVertex3f(startX, startY + height, z);
		gl.glEnd();
		//restore old mode
		this.t.disable();
		if(!this.opaque) {
			this.gl.glDisable(GL.GL_BLEND);
		}
		gl.glMatrixMode( GL.GL_MODELVIEW);
		gl.glPopMatrix();
		gl.glMatrixMode( GL.GL_PROJECTION);
		gl.glPopMatrix();
	}

	@Override
	public void invalidate(SceneGraph graph) {
	}

}
/**
 * OTFOGLDrawer is responsible for everything that goes on inside the OpenGL context.
 * The main functions are invalidate() and redraw(). The latter will simply redraw a given 
 * SceneGraph, whilst invalidate() will update the content.
 * 
 * @author dstrippgen
 *
 */
public class OTFOGLDrawer implements OTFDrawer, GLEventListener, OGLProvider{
	
	private final static Logger log = Logger.getLogger(OTFOGLDrawer.class);
	
	private static int linkTexWidth = 0;
	private static float agentSize = 10.f;
	private int netDisplList = 0;
	private GL gl = null;
	private VisGUIMouseHandler mouseMan = null;
	private final OTFClientQuad clientQ;
	private String lastTime = "";
	private int lastShot = -1;

	//Handle these separately, as the agents needs textures set, which should only be done once
	private final List<OTFGLDrawable> netItems = new ArrayList<OTFGLDrawable>();
	private final List<OTFGLDrawable> overlayItems = new ArrayList<OTFGLDrawable>();

	private static List<OTFGLDrawable> newItems = new ArrayList<OTFGLDrawable>();

	private static volatile GLContext motherContext = null;

	private StatusTextDrawer statusDrawer = null;

	private OTFVisConfig config = null;
	private OTFQueryHandler queryHandler = null;

	private Component canvas = null;

	private final OTFScaleBarDrawer scaleBar;

	private BufferedImage current;

	private ZoomEntry lastZoom = null;

	private final Object blockRefresh = new Object();

	private JDialog zoomD;

	private int now;

	private SceneGraph actGraph = null;

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
			float c = 0.75f;

			// Render the text
			this.textRenderer.setColor(c, c, c, c);
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

	public static class AgentDrawer extends OTFGLDrawableImpl implements OTFDataSimpleAgentReceiver {
		//Anything above 50km/h should be yellow!
		private final static FastColorizer colorizer = new FastColorizer(
				new double[] { 0.0, 25, 50, 75}, new Color[] {
						Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE});

		protected char[] id;
		protected float startX, startY, color;
		protected int state;

		public static  Texture  carjpg = null;
		
		//for backward compatibility only 
		@Deprecated 
		public static  Texture  wavejpg = null;
		//for backward compatibility only
		@Deprecated 
		public static  Texture  pedpng = null;

		public void setAgent(char[] id, float startX, float startY, int state, int user, float color) {
			this.id = id;
			this.startX = startX;
			this.startY = startY;
			this.color = color;
			this.state = state;
		}

		public void displayPS(GL gl) {
			gl.glEnable(GL.GL_POINT_SPRITE_NV);
			gl.glPointSize(agentSize/10);
			gl.glBegin(GL.GL_POINTS);
			gl.glVertex3f(this.startX,this.startY, 0);
			gl.glEnd();
			gl.glDisable(GL.GL_POINT_SPRITE_NV);
		}

		protected void setColor(GL gl) {
			Color color = colorizer.getColor(0.1 + 0.9*this.color);
			if ((this.state & 1) != 0) {
				color = Color.lightGray;
			}
			gl.glColor4d(color.getRed()/255., color.getGreen()/255.,color.getBlue()/255.,.8);

		}

		public void onDraw(GL gl) {
			setColor(gl);
			displayPS(gl);
		}
	}

	private static class MyGLCanvas2 extends GLCanvas {

		private static final long serialVersionUID = 1L;

		public MyGLCanvas2(GLCapabilities caps) {
			super(caps);
		}

		public MyGLCanvas2(GLCapabilities caps, GLCapabilitiesChooser object,
				GLContext motherContext, GraphicsDevice object2) {
			super(caps, object,motherContext, object2);
		}

		@Override
		public void paint(Graphics arg0) {
			synchronized (newItems) {
				super.paint(arg0);
			}
		}
		
	}

	private Component createGLCanvas(final OTFOGLDrawer drawer, final GLCapabilities caps, final GLContext motherContext) {
		GLCanvas canvas;
		if (motherContext == null) {
			canvas = new MyGLCanvas2(caps);
		} else {
			canvas = new MyGLCanvas2(caps, null, motherContext, null);
		}
		canvas.addGLEventListener(drawer);
		return canvas;
	}
	
	public OTFOGLDrawer (OTFVisConfig visconf, JFrame frame, OTFClientQuad clientQ) {
		this.clientQ = clientQ;
		GLCapabilities caps = new GLCapabilities();
		this.config = visconf;
		this.canvas = createGLCanvas(this, caps, motherContext);
		this.mouseMan = new VisGUIMouseHandler(this);
		this.mouseMan.setBounds((float)clientQ.getMinEasting(), (float)clientQ.getMinNorthing(), (float)clientQ.getMaxEasting(), (float)clientQ.getMaxNorthing(), 100);
		Point3f initialZoom = this.config.getZoomValue("*Initial*");
		if (initialZoom != null) {
			this.mouseMan.setToNewPos(initialZoom);
		}
		this.canvas.addMouseListener(this.mouseMan);
		this.canvas.addMouseMotionListener(this.mouseMan);
		this.canvas.addMouseWheelListener(this.mouseMan);
		this.canvas.setMinimumSize(new Dimension(50,50));
		this.canvas.setPreferredSize(new Dimension(300,300));
		this.canvas.setMaximumSize(new Dimension(1024,1024));
		OTFClientQuad.ClassCountExecutor counter = new ClassCountExecutor(OTFDefaultLinkHandler.class);
		clientQ.execute(null, counter);
		double linkcount = counter.getCount();
		int size = linkTexWidth + (int)(0.5*Math.sqrt(linkcount))*2 +2;
		linkTexWidth = size;
		this.overlayItems.add(new OTFGLOverlay(MatsimResource.getAsInputStream("matsim_logo_blue.png"), -0.03f, 0.05f, 1.5f, false));
		this.scaleBar = new OTFScaleBarDrawer();
	}

	public void replaceMouseHandler(VisGUIMouseHandler newHandler) {
		if(newHandler == null) {
			// replace with a copy that is autark
			this.canvas.removeMouseListener(this.mouseMan);
			this.canvas.removeMouseMotionListener(this.mouseMan);
			this.canvas.removeMouseWheelListener(this.mouseMan);
			this.mouseMan = new VisGUIMouseHandler(this.mouseMan);
		} else {
			this.mouseMan = newHandler;
		}
		this.canvas.addMouseListener(this.mouseMan);
		this.canvas.addMouseMotionListener(this.mouseMan);
		this.canvas.addMouseWheelListener(this.mouseMan);
	}
	
	public VisGUIMouseHandler getMouseHandler() {
		return this.mouseMan;
	}

	private void drawNetList(){
		// make quad filled to hit every pixel/texel
		this.gl.glNewList(this.netDisplList, GL.GL_COMPILE);
		log.info("DRAWING NET ONCE: objects count: " + this.netItems.size() );
		OTFGLDrawableImpl.gl = this.gl;
		for (OTFGLDrawable item : this.netItems) {
			item.draw();
		}
		this.gl.glEndList();
	}

	private void displayLinkIds(Map<Coord, String> linkIds) {
		String testText = "0000000";
		Rectangle2D test = InfoText.getBoundsOf(testText);
		Map<Coord, Boolean> xymap = new HashMap<Coord, Boolean>(); // Why is here a Map used, and not a Set?
		double xRaster = test.getWidth(), yRaster = test.getHeight();
		for( Coord coord : linkIds.keySet()) {
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
			InfoTextContainer.showTextOnce(linkIds.get(coord), (float)text.getX(), (float)text.getY(), 1.f);
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
		final double cellWidth = this.config.getLinkWidth();
		final double pixelsizeStreet = 5;
		return (size.getX()*pixelsizeStreet < cellWidth) && (size.getX()*pixelsizeStreet < cellWidth);
	}

	private Map<Coord, String> findVisibleLinks() {
		if (isZoomBigEnoughForLabels()) {
			Rect rect = this.mouseMan.getBounds();
			Rectangle2D.Double dest = new Rectangle2D.Double(rect.minX , rect.minY , rect.maxX - rect.minX, rect.maxY - rect.minY);
			CollectDrawLinkId linkIdQuery = new CollectDrawLinkId(dest);
			linkIdQuery.prepare(this.clientQ);
			Map<Coord, String> linkIds = linkIdQuery.getLinkIds();
			return linkIds;
		} else {
			return Collections.emptyMap();
		}
	}

	synchronized public void display(GLAutoDrawable drawable) {
		float[] components = this.config.getBackgroundColor().getColorComponents(new float[4]);
		this.gl = drawable.getGL();
		this.gl.glClearColor(components[0], components[1], components[2], components[3]);
		this.gl.glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		this.gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
		this.gl.glEnable(GL.GL_BLEND);
		this.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		this.mouseMan.setFrustrum(this.gl);
		components = this.config.getNetworkColor().getColorComponents(components);
		this.gl.glColor4d(components[0], components[1], components[2], components[3]);
		if (this.actGraph != null) {
			this.actGraph.draw();
		}
		if (this.queryHandler != null) {
			this.queryHandler.drawQueries(this);
		}
		Map<Coord, String> coordStringPairs = findVisibleLinks();
		if (this.config.drawLinkIds()) {
			displayLinkIds(coordStringPairs);
		}
		this.gl.glDisable(GL.GL_BLEND);

		// Mac OS X impl of TextRenderer is broken as of 2008/10/24
		// remove this if there is --ever-- a fix
		TextRenderHack.fixIt( this.statusDrawer.textRenderer );
		
		Collection<String> visibleLinkIds = coordStringPairs.values();
		InfoTextContainer.drawInfoTexts(drawable, visibleLinkIds);
		
		if (this.config.drawTime()) {
			this.statusDrawer.displayStatusText(this.lastTime);
		}

		this.mouseMan.drawElements(this.gl);

		if(this.config.drawOverlays()) {
			for (OTFGLDrawable item : this.overlayItems) {
				item.draw();
			}
		}
		
		if (this.config.drawScaleBar()) {
			this.scaleBar.draw();
		}
		
		if (this.config.renderImages() && (this.lastShot < this.now)){
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
	}

	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
	}

	public void init(GLAutoDrawable drawable) {
		if(motherContext == null) motherContext = drawable.getContext();
		
		this.gl = drawable.getGL();
		this.gl.setSwapInterval(0);
		float[] components = this.config.getBackgroundColor().getColorComponents(new float[4]);
		this.gl.glClearColor(components[0], components[1], components[2], components[3]);
		this.gl.glClear( GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		this.mouseMan.init(this.gl);

		AgentDrawer.carjpg = createTexture(MatsimResource.getAsInputStream("car.png"));	
		AgentDrawer.wavejpg = createTexture(MatsimResource.getAsInputStream("square.png"));		
		AgentDrawer.pedpng = createTexture(MatsimResource.getAsInputStream("ped.png"));

		this.netDisplList = this.gl.glGenLists(1);

		drawNetList();
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		GL gl = drawable.getGL();
		gl.glViewport(0, 0, width, height);
		this.mouseMan.setAspectRatio((double)width / (double)height);
		this.mouseMan.setFrustrum(gl);
		this.statusDrawer = new StatusTextDrawer(drawable);
	}

	private void showZoomDialog() {
		this.zoomD = new JDialog(  );
		this.zoomD.setUndecorated(true);
		this.zoomD.setLocationRelativeTo(this.canvas.getParent());
		Point pD = this.canvas.getLocationOnScreen();
		this.canvas.getParent();
		this.zoomD.setLocation(pD);
		this.zoomD.setPreferredSize(this.canvas.getSize());
		GridLayout gbl = new GridLayout(3,3); 
		this.zoomD.getContentPane().setLayout( gbl ); 
		ArrayList<JButton> buttons = new ArrayList<JButton>();
		final List<ZoomEntry> zooms = this.config.getZooms();

		for(int i=0; i<zooms.size();i++) {
			ZoomEntry z = zooms.get(i);
			JButton b = new JButton(z.getName());//icon);
			b.setToolTipText(z.getName());
			b.setPreferredSize(new Dimension(220, 100));
			buttons.add(i, b);
			b.setActionCommand(Integer.toString(i));
			b.addActionListener( new ActionListener() { 
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
		this.config.addZoom(new ZoomEntry(image,zoomstore, name));

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
				public void actionPerformed( ActionEvent e ) {
					storeZoom(false, "");
				} 
			} ); 
			popmen.add( new AbstractAction("Store inital Zoom") { 
				private static final long serialVersionUID = 1L;
				public void actionPerformed( ActionEvent e ) {
					storeZoom(false, "*Initial*");
				} 
			} ); 
			popmen.add( new AbstractAction("Store named Zoom...") { 
				private static final long serialVersionUID = 1L;
				public void actionPerformed( ActionEvent e ) {
					storeZoom(true, "");
				} 
			} );
			popmen.addSeparator();
			popmen.add( new AbstractAction("Load Zoom...") { 
				private static final long serialVersionUID = 1L;
				public void actionPerformed( ActionEvent e ) { 
					showZoomDialog();
					if(OTFOGLDrawer.this.lastZoom != null) OTFOGLDrawer.this.mouseMan.setToNewPos(OTFOGLDrawer.this.lastZoom.getZoomstart()); 
				} 
			} ); 
			popmen.add( new AbstractAction("Delete last Zoom") { 
				private static final long serialVersionUID = 1L;
				public void actionPerformed( ActionEvent e ) { 
					if(OTFOGLDrawer.this.lastZoom != null) {
						OTFOGLDrawer.this.config.deleteZoom(OTFOGLDrawer.this.lastZoom);
						OTFOGLDrawer.this.lastZoom = null;
					}
				} 
			} );  
			popmen.show(this.getComponent(),e.getX(), e.getY());
			return;
		}
		Point2D.Double origPoint = new Point2D.Double(point.x + this.clientQ.offsetEast, point.y + this.clientQ.offsetNorth);
		if(this.queryHandler != null) this.queryHandler.handleClick(this.clientQ.getId(),origPoint, mouseButton);
	}

	public void handleClick(Rectangle currentRect, int button) {
		Rectangle2D.Double origRect = new Rectangle2D.Double(currentRect.x + this.clientQ.offsetEast, currentRect.y + this.clientQ.offsetNorth, currentRect.width, currentRect.height);
		if(this.queryHandler != null) this.queryHandler.handleClick(this.clientQ.getId(),origRect, button);
	}

	/***
	 * redraw refreshes the displayed graphic i.e. it draws the same items as last time something was drawn
	 * useful for zooming, or overlaying GUI items
	 * if the displayed Rect is moved or enlarged, we need to call invalidate, to get the correct data from the host
	 */
	public void redraw() {
		this.canvas.repaint();
	}

	/***
	 * invalidate, gets the actual correct data from the host, to display the given rect
	 * This method is used in most cases
	 * @throws RemoteException
	 */
	public void invalidate(int time) throws RemoteException {
		agentSize = Float.parseFloat(Gbl.getConfig().getParam(OTFVisConfig.GROUP_NAME, OTFVisConfig.AGENT_SIZE));
		if(time != -1) {
			this.now = time;
			this.lastTime = Time.writeTime(time, ':');
		}

		// do something like
		// getTimeStep from somewhere
		// check: is there a cached version for timestep
		// use chached version, else get the real one

		{
			synchronized (this.blockRefresh) {

				QuadTree.Rect rect = this.mouseMan.getBounds();
				synchronized (newItems) {
					this.actGraph  = this.clientQ.getSceneGraph(time, rect, this);
				}
			}
		}
		// Todo put drawing to displyLists here and in
		// display(gl) we only display the two lists

		if(this.queryHandler != null) this.queryHandler.updateQueries();
		redraw();
	}

	/**
	 * @return the canvas
	 */
	public Component getComponent() {
		return this.canvas;
	}

	public GL getGL() {
		return this.gl;
	}

	public OTFClientQuad getQuad() {
		return this.clientQ;
	}

	public Point3f getView() {
		return this.mouseMan.getView();
	}

	public Rect getViewBounds() {
		return this.mouseMan.getBounds();
	}

	/**
	 * @return the actGraph
	 */
	public SceneGraph getActGraph() {
		return this.actGraph;
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

	public void clearCache() {
		if (this.clientQ != null) this.clientQ.clearCache();
	}

	/**
	 * @param queryHandler the queryHandler to set
	 */
	public void setQueryHandler(OTFQueryHandler queryHandler) {
		if(queryHandler != null) this.queryHandler = queryHandler;
	}

	public Point3f getOGLPos(int x, int y) {
		return this.mouseMan.getOGLPos(x, y);
	}

}
