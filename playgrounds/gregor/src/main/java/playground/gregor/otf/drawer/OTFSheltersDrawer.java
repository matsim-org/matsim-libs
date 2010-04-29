package playground.gregor.otf.drawer;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.evacuation.otfvis.drawer.OTFTimeDependentDrawer;
import org.matsim.vis.otfvis.opengl.gl.InfoText;
import org.matsim.vis.otfvis.opengl.gui.ValueColorizer;

import com.sun.opengl.util.j2d.TextRenderer;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class OTFSheltersDrawer extends OTFTimeDependentDrawer  implements Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4178231998542735528L;
	private final List<Shelter> shelters = new ArrayList<Shelter>();
	private final ValueColorizer colorizer;
	private final double on;
	private final double oe;
	private TextRenderer textRenderer = null;
	private int minTime = 3600 * 24;
	private int oldTime = 0;
	private int increment = 3600 * 24;

	public OTFSheltersDrawer(final FeatureSource features, final Map<String,ArrayList<Tuple<Integer,Double>>> occupancy, double on, double oe){
		this.oe = oe;
		this.on = on;
		double [] values = {-1.,0.,1.0,1.5,2.0};
		Color [] colors = {new Color(0,0,0),new Color(128,128,128,128), new Color(24,255,0,255), new Color(255,255,0,255), new Color(255,0,0,255)};
		this.colorizer = new ValueColorizer(values,colors);
		
		Iterator<Feature> it;
		try {
			it = features.getFeatures().iterator();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		while (it.hasNext()){
			final Feature ft = it.next();
			String id = ((Integer) ft.getAttribute("ID")).toString();
			ArrayList<Tuple<Integer,Double>> l = occupancy.get(id);
			if (l == null || l.size() == 0) {
				l = new ArrayList<Tuple<Integer,Double>>();
				l.add(new Tuple<Integer,Double>(0,-1.));
			}
			this.shelters.add(getShelter(ft,l));
		}
	}

	private Shelter getShelter(Feature ft, ArrayList<Tuple<Integer,Double>> arrayList) {
		Shelter s = new Shelter();
		if (arrayList == null) {
			throw new RuntimeException("this should not happen!");
		}
		s.occupancy = arrayList;
		final Geometry geo = ft.getDefaultGeometry();
		final LineString ls;
		final int glType;
		if (geo instanceof Polygon) {
			ls = ((Polygon) geo).getExteriorRing();
			glType = GL.GL_POLYGON;
		}else if (geo instanceof MultiPolygon) {
			ls = ((Polygon)((MultiPolygon)geo).getGeometryN(0)).getExteriorRing();
			glType = GL.GL_POLYGON;
		} else if (geo instanceof LineString) {
				ls = (LineString) geo;
				glType = GL.GL_LINE_STRIP;
		} else if (geo instanceof MultiLineString) {
			ls = (LineString)((MultiLineString) geo).getGeometryN(0);
			glType = GL.GL_LINE_STRIP;
		}else if (geo instanceof Point) {
				final GeometryFactory geofac  = new GeometryFactory();
				ls = geofac.createLineString(new Coordinate [] {geo.getCoordinate()});
				glType = GL.GL_POINTS;
		} else {
			throw new RuntimeException("Could not read Geometry from Feature!!");
		}
		final int npoints = ls.getNumPoints();
		final float [] xpoints = new float[npoints];
		final float [] ypoints = new float[npoints];
//		final float [] color = new float [] {.5f,.1f,.1f,.8f};
		for (int i = 0; i < npoints; i++) {
			xpoints[i] = (float) (ls.getPointN(i).getCoordinate().x - this.oe);
			ypoints[i] = (float) (ls.getPointN(i).getCoordinate().y - this.on);
		}
		
		s.glType = glType;
		s.xpoints = xpoints;
		s.ypoints = ypoints;
		
		return s;
	}

	@Override
	public void onDraw(GL gl, int time) {
		if (this.textRenderer == null) {
			initTextRenderer();
		}
		
		
		int timeSlotIdx = getTimeSlotIdx(time); 
//		int relTime = ((((time - 3*3600)/60))); //FIXME 60 should be replaced by snapshot period!!
//		
//		if (this.timeSlotIdx < 0) {
//			return;
//		}
		
		
		for (Shelter s : this.shelters) {
			int sTime = timeSlotIdx;
			if (timeSlotIdx >= s.occupancy.size()) {
				sTime = s.occupancy.size()-1;
			}
			Color color = this.colorizer.getColor(s.occupancy.get(sTime).getSecond());
			gl.glColor4d(color.getRed()/255., color.getGreen()/255.,color.getBlue()/255.,color.getAlpha()/255.);
			gl.glBegin(GL.GL_POLYGON);
			for (int i = 0; i < s.xpoints.length; i++) {

				gl.glVertex3f(s.xpoints[i],s.ypoints[i],1.f);
			}
			
			gl.glEnd();	
			
			this.textRenderer.begin3DRendering();
			float c = 1.f;
			String text = ""+s.occupancy.get(sTime).getFirst();
			float width = (float) this.textRenderer.getBounds(text).getWidth();
			// Render the text
			this.textRenderer.setColor(c, c, c, 1.f);
			this.textRenderer.draw3D(text,s.xpoints[0],s.ypoints[0],1.f,.5f);
			this.textRenderer.end3DRendering();
			
		}

	}

	
	private int getTimeSlotIdx(int time) {
		if (this.minTime > time || this.minTime == 0) {
			this.increment = Integer.MAX_VALUE; //needs to be initialized here, otherwise it won't work with older movies - GL sept. 2009
			this.minTime = time;
			return 0;
		}
		if (time > this.oldTime) {
			this.increment  = Math.min(this.increment, time-this.oldTime);
			this.oldTime = time;
		}
		
		return (time - this.minTime) / this.increment;
		
	}



	private void initTextRenderer() {
		// Create the text renderer
		Font font = new Font("SansSerif", Font.PLAIN, 30);
		this.textRenderer  = new TextRenderer(font, true, false);
		InfoText.setRenderer(this.textRenderer);
	}
	
	private static class Shelter implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 2583984535719211050L;
		int glType;
		List<Tuple<Integer,Double>> occupancy;
		float [] xpoints;
		float [] ypoints;
	}

}
