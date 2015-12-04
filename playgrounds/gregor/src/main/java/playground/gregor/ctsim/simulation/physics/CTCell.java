package playground.gregor.ctsim.simulation.physics;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import playground.gregor.ctsim.run.CTRunner;
import playground.gregor.ctsim.simulation.CTEvent;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.events.debug.LineEvent;

import java.util.*;

public abstract class CTCell {

	protected static final Logger log = Logger.getLogger(CTCell.class);

	private static final double RHO_M = 6.667;
	private static final double V_0 = 1.5;
	private static final double GAMMA = 0.3;
	private static final double P0 = 0.2;
	private static double Q;

	static {
		Q = (V_0 * RHO_M) / (V_0 / GAMMA + 1);
	}

	protected final CTNetwork net;
	//	protected final EventsManager em;
	protected final double width;
	protected final CTNetworkEntity parent;
	private final List<CTCellFace> faces = new ArrayList<>();
	//	private final HashSet<CTPed> peds = new HashSet<>();
//	private final Map<Double,LinkedList<CTPed>> pop = new ArrayMap<>();//TODO is this faster than HashMap?
	private final List<CTCell> neighbors = new ArrayList<>();
	protected CTPed next = null;
	protected double nextCellJumpTime;
	protected CTEvent currentEvent = null;
	protected int n = 0; //nr peds
	int r = 0;
	int g = 0;
	int b = 192;
	private double alpha; //area
	private double rho; //current density
	private double x;
	private double y;
	private int N; //max number of peds


	public CTCell(double x, double y, CTNetwork net, CTNetworkEntity parent, double width, double area) {
		this.x = x;
		this.y = y;
		this.net = net;
		this.parent = parent;
		this.width = width;
		this.setArea(area);
//		pop.put(Math.PI/6., new LinkedList<CTPed>());
//		pop.put(Math.PI/2., new LinkedList<CTPed>());
//		pop.put(5*Math.PI/6., new LinkedList<CTPed>());
//		pop.put(-5*Math.PI/6., new LinkedList<CTPed>());
//		pop.put(-Math.PI/2., new LinkedList<CTPed>());
//		pop.put(-Math.PI/6., new LinkedList<CTPed>());
	}

	public void setArea(double a) {
		this.alpha = a;
		this.N = (int) (RHO_M * this.getAlpha() + 0.5);
	}

	public double getAlpha() {
		return this.alpha;
	}

	public void addFace(CTCellFace face) {
		faces.add(face);
		getNeighbors().add(face.nb);
	}

	public List<CTCell> getNeighbors() {
		return neighbors;
	}

	public void addNeighbor(CTCell nb) {
		this.getNeighbors().add(nb);
	}

	public void debug(EventsManager em) {
		if (!CTRunner.DEBUG) {
			return;
		}
//		for (CTCellFace f : faces) {
//			debug(f, em);
//		}

	}

//	protected double getFHHi(CTPed ped, CTCellFace face) {
//		return 1 + Math.cos(ped.getDesiredDir() - face.h_i);
//	}

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

	public void jumpAndUpdateNeighbors(double now) {

		CTCell nb = next.getNextCellAndJump(now);
		next = null;
		this.nextCellJumpTime = Double.NaN;


		Set<CTCell> affectedCells = new HashSet<>();
		for (CTCell ctCell : this.getNeighbors()) {
			affectedCells.add(ctCell);
		}
		for (CTCell ctCell : nb.getNeighbors()) {
			affectedCells.add(ctCell);
		}
		for (CTCell cell : affectedCells) {
			cell.updateIntendedCellJumpTimeAndChooseNextJumper(now);
		}
	}

	public abstract void updateIntendedCellJumpTimeAndChooseNextJumper(double now);

	protected double chooseNextCellAndReturnJ(CTPed ped) {
//		boolean debug = false;
//		DebugInfo i = new DebugInfo();
//		if (ped.getDriver().getId().toString().equals("4")) {
//			debug = true;
//		}


		CTCell bestNB = null;
//		CTCellFace bestFace = null;
		double maxFJ = 0;
		double maxJ = Double.NaN;
		for (CTCellFace face : this.getFaces()) {

			double fDir = face.h_i;
			double f = getFHHi(ped, face);
			double j = this.getJ(face.nb, ped);
			double fJ = f * j;
//			i.addDirInfo(fDir,f,j,fJ);
			if (fJ > maxFJ) {
//				bestFace = face;
				maxJ = j;
				maxFJ = fJ;
				bestNB = face.nb;

			}
		}
		if (bestNB == null) {
			return Double.NaN;
		}
//		if (Math.abs(bestFace.h_i-ped.getDesiredDir())>0.1) {
//			log.info("lane switch" + ped.getDesiredDir() + "  " + bestFace.h_i);
//			log.info("lane switch" + ped.getDesiredDir() + "  " + bestFace.h_i);
//		}
		ped.setTentativeNextCell(bestNB);

//		if (debug) {
//			System.out.print(i);
//			System.out.println();
//		}
		return maxJ;
	}

	abstract double getFHHi(CTPed ped, CTCellFace face);

	public double getJ(CTCell n_i, CTPed ped) { //flow to cell n_i
		double demand = getDelta(ped);
		double supply = n_i.getSigma(ped);
		return width * Math.min(demand, supply) * 1.5;
	}

	private double getDelta(CTPed ped) { //demand function
//		return Math.min(Q, V_0 * Math.max(this.getRho(), 1 / (Math.sqrt(3)*width*width)));
		double coeff = getDirCoeff(ped);
		return Math.min(coeff * Q, V_0 * this.getRho());
	}

	public double getRho() { //current density
		return this.rho;
	}

	public void setRho(double rho) {
//		double myRho = this.n/this.N*RHO_M;
//		log.info("diff rho-myRho:" + (rho-myRho));
		this.rho = rho;
	}

	private double getSigma(CTPed ped) { //supply function

		double coeff = getDirCoeff(ped);
		return Math.min(coeff * Q, GAMMA * (RHO_M - this.getRho()));
	}

	private double getDirCoeff(CTPed ped) {
		double ph = getDirectionalProportion(ped);
		return P0 + (1. - P0) * ph;
	}

	public List<CTCellFace> getFaces() {
		return faces;
	}

	abstract void jumpOffPed(CTPed ctPed, double time);

	public abstract boolean jumpOnPed(CTPed ctPed, double time);

	abstract HashSet<CTPed> getPeds();

	abstract double getDirectionalProportion(CTPed ped);

	private static final class DebugInfo {
		Map<Integer, DirInfo> dirs = new HashMap<>();

		public void addDirInfo(double dir, double f, double j, double jf) {
			DirInfo i = new DirInfo();
			i.dir = dir;
			i.f = f;
			i.j = j;
			i.jf = jf;
			this.dirs.put((int) (100 * dir), i);
		}

		@Override
		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append("dir\tf\tj\tjf\n");
			{
				DirInfo i = this.dirs.get((int) (100 * Math.PI / 6));
				if (i != null) {

					buf.append("pi/6\t" + i.f + "\t" + i.j + "\t" + i.jf + "\n");
				}
			}
			{
				DirInfo i = this.dirs.get((int) (100 * Math.PI / 2));
				if (i != null) {

					buf.append("pi/2\t" + i.f + "\t" + i.j + "\t" + i.jf + "\n");
				}
			}
			{
				DirInfo i = this.dirs.get((int) (100 * 5 * Math.PI / 6));
				if (i != null) {

					buf.append("5 pi/6\t" + i.f + "\t" + i.j + "\t" + i.jf + "\n");
				}
			}
			return buf.toString();
		}
	}

	private static final class DirInfo {
		double dir;
		double f;
		double j;
		double jf;
	}
}
