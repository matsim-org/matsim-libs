package playground.gregor.otf.drawer;

import java.awt.Color;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.vis.netvis.renderers.ValueColorizer;

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

	public OTFSheltersDrawer(final FeatureSource features, final Map<String,ArrayList<Double>> occupancy, double on, double oe){
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
			ArrayList<Double> l = occupancy.get(id);
			if (l == null || l.size() == 0) {
				l = new ArrayList<Double>();
				l.add(-1.);
			}
			this.shelters.add(getShelter(ft,l));
		}
	}

	private Shelter getShelter(Feature ft, ArrayList<Double> arrayList) {
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
		
		int relTime = ((((time - 3*3600)/60)));
		
		if (relTime < 0) {
			return;
		}
		
		
		for (Shelter s : this.shelters) {
			int sTime = relTime;
			if (relTime >= s.occupancy.size()) {
				sTime = s.occupancy.size()-1;
			}
			Color color = this.colorizer.getColor(s.occupancy.get(sTime));
			gl.glColor4d(color.getRed()/255., color.getGreen()/255.,color.getBlue()/255.,color.getAlpha()/255.);
			gl.glBegin(GL.GL_POLYGON);
			for (int i = 0; i < s.xpoints.length; i++) {

				gl.glVertex3f(s.xpoints[i],s.ypoints[i],1.f);
			}
			gl.glEnd();	
			
		}

	}

	private static class Shelter implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 2583984535719211050L;
		int glType;
		List<Double> occupancy;
		float [] xpoints;
		float [] ypoints;
	}

}
