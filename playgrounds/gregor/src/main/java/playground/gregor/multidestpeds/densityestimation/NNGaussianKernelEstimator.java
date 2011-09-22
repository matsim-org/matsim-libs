package playground.gregor.multidestpeds.densityestimation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import playground.gregor.sim2d_v2.events.DoubleValueStringKeyAtCoordinateEvent;
import playground.gregor.sim2d_v2.events.XYVxVyEvent;
import playground.gregor.sim2d_v2.events.XYVxVyEventsHandler;
import playground.gregor.sim2d_v2.simulation.floor.EnvironmentDistances;
import playground.gregor.sim2d_v2.simulation.floor.StaticEnvironmentDistancesField;

/**
 * 
 * @author laemmel
 *
 */
public class NNGaussianKernelEstimator implements XYVxVyEventsHandler{


	Stack<XYVxVyEvent> events = new Stack<XYVxVyEvent>();
	List<String> groupIDs = new ArrayList<String>();

	List<Coordinate> queryCoordinates = null;// = new ArrayList<Coordinate>();


	private double time = -1;
	private Envelope envelope;
	private double res = -1;
	private QuadTree<Coordinate> particleQuadTree;

	private double lambda;
	private double minDist;

	private final double searchRangeIncrement = 1;

	double maxRho = 0;
	private EventsManager eventsManger = null;
	private StaticEnvironmentDistancesField sedf;

	/*package*/ NNGaussianKernelEstimator() {

	}

	@Override
	public void reset(int iteration) {
		this.time = -1;
		this.events.clear();
	}

	@Override
	public void handleEvent(XYVxVyEvent event) {
		double eventTime = event.getTime();
		if (eventTime > this.time) {
			processFrame();
			this.time = eventTime;
		}
		this.events.push(event);
	}

	private void processFrame() {
		if (this.events.size() == 0) {
			return;
		}
		Map<String, List<Coordinate>> groups = new HashMap<String,List<Coordinate>>();
		Map<String, List<PersonInfo>> groupsDists = new HashMap<String,List<PersonInfo>>();
		List<Coordinate> all = new ArrayList<Coordinate>();
		while (!this.events.isEmpty()) {
			XYVxVyEvent e = this.events.pop();
			String key = getKeyFromPersonId(e.getPersonId());
			all.add(e.getCoordinate());
			List<Coordinate> l = groups.get(key);
			if (l == null) {
				l = new ArrayList<Coordinate>();
				groups.put(key, l);
			}
			l.add(e.getCoordinate());
		}
		initQuadTree(all);
		for (String key : this.groupIDs) {
			List<Coordinate> l = groups.get(key);
			if (l == null) {
				continue;
			}
			List<PersonInfo> groupDists = getGroupDists(l);
			groupsDists.put(key, groupDists);
		}


		for (Coordinate here : this.queryCoordinates) {

			for (String key : this.groupIDs) {
				double rho = 0;
				List<PersonInfo> l = groupsDists.get(key);
				if (l == null) {
					continue;
				}
				for (PersonInfo pi : l) {
					double k = kernel((here.x-pi.c.x)/this.lambda/(pi.dist),(here.y-pi.c.y)/this.lambda/(pi.dist));
					rho += 1./Math.pow(this.lambda,2)/Math.pow(pi.dist, 2)*k;
				}
				if (rho > this.maxRho) {
					this.maxRho = rho;
				}
				generateEvent(here,rho,key);
			}
		}

	}


	private double kernel(double x,double y) {
		double tmp = -1./2.*(Math.pow(x, 2)+Math.pow(y, 2));
		return 1./2./Math.PI * Math.exp(tmp);
	}

	private void generateEvent(Coordinate here, double value, String key) {
		Event e = new DoubleValueStringKeyAtCoordinateEvent(here, value, key, this.time);
		this.eventsManger.processEvent(e);
	}

	private List<PersonInfo> getGroupDists(List<Coordinate> list) {
		List<PersonInfo> ret = new ArrayList<PersonInfo>();
		for (Coordinate c : list) {
			PersonInfo info = new PersonInfo();
			info.c = c;
			double distance = 1;
			Collection<Coordinate> neighbors = this.particleQuadTree.get(c.x, c.y, distance);
			while (neighbors.size() <= 1 && distance <= 100) {
				distance += this.searchRangeIncrement;
				neighbors = this.particleQuadTree.get(c.x, c.y, distance);
			}
			double minDist = Double.POSITIVE_INFINITY;
			for (Coordinate tmp : neighbors) {
				if (tmp != c && tmp.distance(c) < minDist) {
					minDist = tmp.distance(c);
				}
			}

			EnvironmentDistances dists = this.sedf.getEnvironmentDistances(c);
			for (Coordinate tmp : dists.getObjects()) {
				if (tmp.distance(c) < minDist) {
					minDist = tmp.distance(c);
				}
			}

			info.dist = Math.max(minDist, this.minDist);
			ret.add(info);
		}
		return ret;
	}

	private void initQuadTree(List<Coordinate> all) {
		this.particleQuadTree.clear();
		for (Coordinate c : all) {
			this.particleQuadTree.put(c.x, c.y, c);
		}

	}

	private String getKeyFromPersonId(Id personId) {
		String tmp = personId.toString();
		for (String key : this.groupIDs) {
			if (tmp.startsWith(key)) {
				return key;
			}
		}
		return null;
	}

	private static class PersonInfo {
		Coordinate c;
		double dist;
	}

	/*package*/ void addGroupId(String id) {
		this.groupIDs.add(id);
	}

	/*package*/ void setEnvelope(Envelope e) {
		this.envelope = e;
		this.particleQuadTree = new QuadTree<Coordinate>(this.envelope.getMinX(), this.envelope.getMinY(), this.envelope.getMaxX(), this.envelope.getMaxY());
	}

	/*package*/ void setResolution(double res) {
		this.res = res;
	}

	/*package*/ void setLambda(double lambda) {
		this.lambda = lambda;
	}

	/*package*/ void setMinDist(double minDist) {
		this.minDist = minDist;
	}

	/*package*/ void setEventsManager(EventsManager events) {
		this.eventsManger  = events;
	}

	/*package*/ void setStaticEnvironmentDistancesField(StaticEnvironmentDistancesField sedf) {
		this.sedf = sedf;
	}

	/*package*/ void setQueryCoordinates(List<Coordinate> coordinates) {
		this.queryCoordinates = coordinates;
	}

}
