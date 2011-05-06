package playground.mzilske.osm;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.openstreetmap.gui.jmapviewer.OsmMercator;

public class WGS84ToOSMMercator {

	public static final int SCALE = 16;

	public static class Project implements CoordinateTransformation {



		@Override
		public Coord transform(Coord coord) {
			return new CoordImpl(OsmMercator.LonToX(coord.getX(), SCALE),- OsmMercator.LatToY(coord.getY(), SCALE));
		}

	}


	public static class Deproject implements CoordinateTransformation {



		@Override
		public Coord transform(Coord coord) {
			return new CoordImpl(OsmMercator.XToLon((int) coord.getX(), SCALE), OsmMercator.YToLat( -(int) coord.getY(), SCALE));
		}

	}
}
