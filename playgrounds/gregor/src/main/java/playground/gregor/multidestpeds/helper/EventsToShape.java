package playground.gregor.multidestpeds.helper;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import playground.gregor.sim2d_v2.events.XYVxVyEvent;
import playground.gregor.sim2d_v2.events.XYVxVyEventsFileReader;
import playground.gregor.sim2d_v2.events.XYVxVyEventsHandler;
import playground.gregor.sim2d_v2.helper.gisdebug.GisDebugger;

public class EventsToShape implements XYVxVyEventsHandler{


	private final GeometryFactory geofac = new GeometryFactory();


	public static void main (String [] args) {
		String events = "/Users/laemmel/devel/dfg/events.xml";
		EventsManager manager = EventsUtils.createEventsManager();
		EventsToShape handler = new EventsToShape();
		manager.addHandler(handler);
		XYVxVyEventsFileReader reader = new XYVxVyEventsFileReader(manager);
		reader.parse(events);
		GisDebugger.dump("/Users/laemmel/tmp/points.shp");
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(XYVxVyEvent event) {
		Point p = this.geofac.createPoint(event.getCoordinate());
		GisDebugger.addGeometry(p);

	}
}
