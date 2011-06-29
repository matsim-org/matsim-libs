package playground.gregor.multidestpeds.denistyestimation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import playground.gregor.sim2d_v2.events.XYZAzimuthEvent;
import playground.gregor.sim2d_v2.events.XYZEventsHandler;

/**
 * 
 * @author laemmel
 *
 */
public class NNGaussianKernelEstimator implements XYZEventsHandler{

	Stack<XYZAzimuthEvent> events = new Stack<XYZAzimuthEvent>();
	List<String> groupIDs = new ArrayList<String>();

	Map<String,double[][]> densityArrays;

	private double time = -1;
	private Envelope envelope;
	private double res = -1;
	private QuadTree<Coordinate> particleQuadTree;

	private double lambda;
	private double minDist;

	private final double searchRangeIncrement = 1;

	/*package*/ NNGaussianKernelEstimator() {

	}

	@Override
	public void reset(int iteration) {
		this.time = -1;
		this.events.clear();
	}

	@Override
	public void handleEvent(XYZAzimuthEvent event) {
		double eventTime = event.getTime();
		if (eventTime > this.time) {
			processFrame();
		}
		this.events.push(event);
	}

	private void processFrame() {
		initDensityArrays();
		Map<String, List<Coordinate>> groups = new HashMap<String,List<Coordinate>>();
		Map<String, List<PersonInfo>> groupsDists = new HashMap<String,List<PersonInfo>>();
		List<Coordinate> all = new ArrayList<Coordinate>();
		for (XYZAzimuthEvent e = this.events.pop();!this.events.isEmpty(); e = this.events.pop()) {
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
			List<PersonInfo> groupDists = getGroupDists(groups.get(key));
			groupsDists.put(key, groupDists);
		}

		double x = this.envelope.getMinX() + this.res/2;
		double y = this.envelope.getMinY() + this.res/2;

		int xpos = 0;
		for (; x < this.envelope.getMaxX(); x += this.res){
			int ypos = 0;
			for (; y < this.envelope.getMaxY(); y+=this.res) {
				Coordinate here = new Coordinate(x,y);
				for (String key : this.groupIDs) {
					double rho = 0;
					for (PersonInfo pi : groupsDists.get(key)) {
						rho += 1/Math.pow(this.lambda * pi.dist,2) * Math.exp(-Math.pow(pi.c.distance(here), 2)/(2*Math.pow(this.lambda*pi.dist, 2)));
					}
					rho *= 1/(2*Math.PI);
					this.densityArrays.get(key)[xpos][ypos] = rho;
				}
				ypos++;
			}
			xpos++;
		}



	}

	private List<PersonInfo> getGroupDists(List<Coordinate> list) {
		List<PersonInfo> ret = new ArrayList<PersonInfo>();
		for (Coordinate c : list) {
			PersonInfo info = new PersonInfo();
			info.c = c;
			double distance = 1;
			Collection<Coordinate> neighbors = this.particleQuadTree.get(c.x, c.y, distance);
			while (neighbors.size() <= 1) {
				distance += this.searchRangeIncrement;
				neighbors = this.particleQuadTree.get(c.x, c.y, distance);
			}
			double minDist = Double.POSITIVE_INFINITY;
			for (Coordinate tmp : neighbors) {
				if (tmp != c && tmp.distance(c) < minDist) {
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

	private void initDensityArrays() {
		this.densityArrays = new HashMap<String,double[][]>();
		int xsize = (int) ((this.envelope.getMaxX() - this.envelope.getMinX())/this.res);
		int ysize = (int) ((this.envelope.getMaxY() - this.envelope.getMinY())/this.res);
		for (String key : this.groupIDs) {
			double[][] array = new double[xsize][ysize];
			this.densityArrays.put(key, array);
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
}
