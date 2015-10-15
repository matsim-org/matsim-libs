package playground.gregor.ctsim.simulation.physics;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import playground.gregor.ctsim.run.CTRunner;
import playground.gregor.ctsim.simulation.CTEvent;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.events.debug.LineEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class CTCell {

	protected static final Logger log = Logger.getLogger(CTCell.class);

	private static final double RHO_M = 6.667;
	private static final double V_0 = 1.5;
	private static final double GAMMA = 0.3;
	private static double Q;

	static {
		Q = (V_0 * RHO_M) / (V_0 / GAMMA + 1);
	}

	protected final CTNetwork net;
	protected final EventsManager em;
	protected final double width;
	private final List<CTCellFace> faces = new ArrayList<>();
	//	private final HashSet<CTPed> peds = new HashSet<>();
//	private final Map<Double,LinkedList<CTPed>> pop = new ArrayMap<>();//TODO is this faster than HashMap?
	private final List<CTCell> neighbors = new ArrayList<>();
	private final CTNetworkEntity parent;
	protected double alpha; //area
	protected double rho; //current density
	protected CTPed next = null;
	protected double nextCellJumpTime;
	protected CTEvent currentEvent = null;
	protected int n = 0; //nr peds
	int r = 0;
	int g = 0;
	int b = 192;
	private double x;
	private double y;
	private int N; //max number of peds


	public CTCell(double x, double y, CTNetwork net, CTNetworkEntity parent, double width) {
		this.x = x;
		this.y = y;
		this.net = net;
		this.parent = parent;
		this.em = net.getEventsManager();
		this.width = width;
//		pop.put(Math.PI/6., new LinkedList<CTPed>());
//		pop.put(Math.PI/2., new LinkedList<CTPed>());
//		pop.put(5*Math.PI/6., new LinkedList<CTPed>());
//		pop.put(-5*Math.PI/6., new LinkedList<CTPed>());
//		pop.put(-Math.PI/2., new LinkedList<CTPed>());
//		pop.put(-Math.PI/6., new LinkedList<CTPed>());
	}


	public void addFace(CTCellFace face) {
		faces.add(face);
		neighbors.add(face.nb);
	}

	public void addNeighbor(CTCell nb) {
		this.neighbors.add(nb);
	}

	public void debug(EventsManager em) {
		if (!CTRunner.DEBUG) {
			return;
		}
		for (CTCellFace f : faces) {
			debug(f, em);
		}

	}

	private void debug(CTCellFace f, EventsManager em) {
		if (!CTRunner.DEBUG) {
			return;
		}
//		if (MatsimRandom.getRandom().nextDouble() > 0.1 ){
//			return;
//		}

		{
			LineSegment s = new LineSegment();
			s.x0 = f.x0;
			s.y0 = f.y0;
			s.x1 = f.x1;
			s.y1 = f.y1;
			LineEvent le = new LineEvent(0, s, true, r, g, b, 255, 50);
			em.processEvent(le);
		}
	}

	public double getIntendedNextCellJumpTime() {
		return this.nextCellJumpTime;
	}

	public void setArea(double a) {
		this.alpha = a;
		this.N = (int) (RHO_M * this.alpha + 0.5);
	}

//	protected double getFHHi(CTPed ped, CTCellFace face) {
//		return 1 + Math.cos(ped.getDesiredDir() - face.h_i);
//	}

	public double getX() {
		return this.x;
	}

	public double getY() {
		return this.y;
	}

	public int getN() {
		return this.N;
	}

	public CTNetworkEntity getParent() {
		return this.parent;
	}

	private double getRho() { //current density
		return this.rho;
	}

	public void jumpAndUpdateNeighbors(double now) {

		CTCell nb = next.getNextCellAndJump(now);
		next = null;
		this.nextCellJumpTime = Double.NaN;


		Set<CTCell> affectedCells = new HashSet<>();
		for (CTCell ctCell : this.neighbors) {
			affectedCells.add(ctCell);
		}
		for (CTCell ctCell : nb.neighbors) {
			affectedCells.add(ctCell);
		}
		for (CTCell cell : affectedCells) {
			cell.updateIntendedCellJumpTimeAndChooseNextJumper(now);
		}
	}

	public abstract void updateIntendedCellJumpTimeAndChooseNextJumper(double now);

	protected double chooseNextCellAndReturnJumpRate(CTPed ped) {
		CTCell bestNB = null;
		double maxFlowFactor = 0;
		for (CTCellFace face : this.getFaces()) {
			double flowFactor = getFHHi(ped, face) * this.getJ(face.nb);
			if (flowFactor > maxFlowFactor) {
				maxFlowFactor = flowFactor;
				bestNB = face.nb;

			}
		}
		if (bestNB == null) {
			return Double.NaN;
		}
		ped.setTentativeNextCell(bestNB);
		return this.getJ(ped.getTentativeNextCell()) * maxFlowFactor;
	}

	abstract double getFHHi(CTPed ped, CTCellFace face);

	public double getJ(CTCell n_i) { //flow to cell n_i
		double demand = getDelta();
		double supply = n_i.getSigma();
		return width * Math.min(demand, supply);
	}

	private double getDelta() { //demand function
		return Math.min(Q, V_0 * this.rho);
	}

	private double getSigma() { //supply function
		return Math.min(Q, GAMMA * (RHO_M - this.rho));
	}

	public List<CTCellFace> getFaces() {
		return faces;
	}

	abstract void jumpOffPed(CTPed ctPed, double time);

	public abstract boolean jumpOnPed(CTPed ctPed, double time);


	abstract HashSet<CTPed> getPeds();
}
