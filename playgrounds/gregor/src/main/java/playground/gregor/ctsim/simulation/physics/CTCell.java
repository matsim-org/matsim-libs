package playground.gregor.ctsim.simulation.physics;

import org.matsim.core.api.experimental.events.EventsManager;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.events.debug.LineEvent;

import java.util.ArrayList;
import java.util.List;

public class CTCell {

	private final List<CTCellFace> faces = new ArrayList<>();
	int r = 0;
	int g = 0;
	int b = 192;
	private double x;
	private double y;


	public CTCell(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public void addFace(CTCellFace face) {
		faces.add(face);
	}
	public void debug(EventsManager em) {
		for (CTCellFace f : faces) {
			debug(f,em);
		}

	}
	private void debug(CTCellFace f, EventsManager em) {
//		if (MatsimRandom.getRandom().nextDouble() > 0.1 ){
//			return;
//		}


		{
			LineSegment s = new LineSegment();
			s.x0 = f.x0;
			s.y0 = f.y0;
			s.x1 = f.x1;
			s.y1 = f.y1;
			LineEvent le = new LineEvent(0, s, true, r, g, b, 255, 0);
			em.processEvent(le);
		}
	}

}
