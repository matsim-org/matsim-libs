package playground.gregor.evacuation.traveltime;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.gregor.gis.helper.GTH;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class TravelTimeGridAnalysis {

	public static final String SVN = "/home/laemmel/arbeit/svn/runs-svn/run1022/output";
	private static final double WIDTH = 250;
	
	public static void main(String [] args) {
		
		
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(SVN + "/output_config.xml.gz");
		
		ScenarioImpl scenario = sl.getScenario();
		scenario.getConfig().network().setInputFile(SVN + "/output_network.xml.gz");
		
		int it = scenario.getConfig().controler().getLastIteration();
		it = 1000;
		sl.loadNetwork();
		String eventsFile = SVN + "/ITERS/it." + it + "/" + it + ".events.txt.gz";
		
		Envelope e = getEnvelope(sl.getScenario().getNetwork());
		List<MultiPolygon> l = getMultiPolygons(e);

		new TravelTimeAnalyzer(SVN + "/../analysis/evacuationTimeII.shp", eventsFile, l, sl.getScenario().getNetwork()).run();
		
	}
	
	private static List<MultiPolygon> getMultiPolygons(Envelope e) {
		GeometryFactory geofac = new GeometryFactory();
		GTH gth = new GTH(geofac);
		List<MultiPolygon> mps = new ArrayList<MultiPolygon>();
		int countX = 0;
		int countY = 0;
		for (double x = e.getMinX(); x <= e.getMaxX(); x += WIDTH) {
			System.out.println("countX:" + ++countX + "  countY:" + countY);
			for (double y = e.getMinY(); y <= e.getMaxY(); y += WIDTH) {
				countY++;
				Polygon p = gth.getSquare(new Coordinate(x,y),WIDTH);
				MultiPolygon mp = geofac.createMultiPolygon(new Polygon[]{p});
				mps.add(mp);
			}
		}
		return mps;
	}

	private static Envelope getEnvelope(NetworkLayer net) {
		Envelope e = null; 
		for (Node n : net.getNodes().values()) {
			if (e == null) {
				e = new Envelope(n.getCoord().getX(),n.getCoord().getX(),n.getCoord().getY(),n.getCoord().getY());
			} else {
				e.expandToInclude(n.getCoord().getX(), n.getCoord().getY());
			}
		}		
		return e;
	}
}
