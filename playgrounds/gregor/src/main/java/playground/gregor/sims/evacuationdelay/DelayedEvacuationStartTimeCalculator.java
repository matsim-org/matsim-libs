package playground.gregor.sims.evacuationdelay;

import java.io.IOException;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.evacuation.base.EvacuationStartTimeCalculator;


import com.vividsolutions.jts.geom.Envelope;

public class DelayedEvacuationStartTimeCalculator implements EvacuationStartTimeCalculator {

	
	
	
	private final double baseTime;
	private QuadTree<Coord> coordQuadTree;

	public DelayedEvacuationStartTimeCalculator(double earliestEvacTime, String shorelineFile) {
		this.baseTime = earliestEvacTime;
		try {
			loadShapeFile(shorelineFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void loadShapeFile(String shorelineFile) throws IOException {
		FeatureSource fs = ShapeFileReader.readDataFile(shorelineFile);
		Envelope e = fs.getBounds();
		this.coordQuadTree = new QuadTree<Coord>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		Iterator it = fs.getFeatures().iterator();
		while (it.hasNext()) {
			Feature f = (Feature) it.next();
			Coord c = MGC.coordinate2Coord(f.getDefaultGeometry().getCoordinate());
			this.coordQuadTree.put(c.getX(), c.getY(), c);
		}
		
	}

	public QuadTree<Coord> getCoordinateQuadTree() {
		return this.coordQuadTree;
	}
	
	public double getEvacuationStartTime(ActivityImpl act) {
		Coord c = act.getCoord();
		if (c == null) {
			c = act.getLink().getCoord();
		}
		double dist = ((CoordImpl) this.coordQuadTree.get(c.getX(), c.getY())).calcDistance(c);
		return this.baseTime + getOffset(dist);
	}

	private double getOffset(double dist) {
		if (dist <= 3000) {
			double rnd = MatsimRandom.getRandom().nextDouble();
			if (rnd <= .214) {
				return 25 * 60;
			} else if (rnd <= .631 ) {
				return 15 * 60;
			} else {
				return .0;
			}
		} else {
			double rnd = MatsimRandom.getRandom().nextDouble();
			if (rnd <= .357) {
				return 25 * 60;
			} else if (rnd <= .596 ) {
				return 15 * 60;
			} else {
				return .0;
			}			
		}
	}

}
