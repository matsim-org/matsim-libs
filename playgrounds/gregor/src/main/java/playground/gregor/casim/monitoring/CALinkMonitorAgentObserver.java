package playground.gregor.casim.monitoring;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

import playground.gregor.casim.simulation.physics.AbstractCANetwork;
import playground.gregor.casim.simulation.physics.CALink;
import playground.gregor.casim.simulation.physics.CAMoveableEntity;
import playground.gregor.utils.Variance;

public class CALinkMonitorAgentObserver implements Monitor {

	// private CALink l;
	private CAMoveableEntity[] parts;
	private int from;
	private int to;

	private double realRange;

	private double lastTriggered = -1;

	private final double h;
	private double cellWidth;

	private final Map<Id, AgentInfo> ais = new HashMap<>();

	private final int cellRange;
	private int num;

	public CALinkMonitorAgentObserver(CALink l, double range,
			CAMoveableEntity[] parts, double laneWidth) {
		this.parts = parts;
		this.num = l.getNumOfCells();
		this.cellWidth = l.getLink().getLength() / num;
		double cells = range / cellWidth;
		this.from = (int) (num / 2. - cells / 2 + .5);
		this.to = (int) (num / 2. + cells / 2 + .5);
		this.h = (to - from) / 4.;
		this.realRange = (1 + to - from) * cellWidth;
		this.cellRange = (1 + to - from);

	}

	@Override
	public void init() {

		trigger(0);
	}

	@Override
	public void trigger(double time) {
		if (time <= lastTriggered) {
			return;
		}
		lastTriggered = time;

		for (int i = 0; i < this.num; i++) {
			CAMoveableEntity p = this.parts[i];
			if (p != null) {
				AgentInfo ai = this.ais.get(p.getId());
				if (ai == null) {
					ai = new AgentInfo(p.getDir());
					ai.lastPosTime = 0;
					ai.lastpos = i;
					this.ais.put(p.getId(), ai);
				}
			}
		}

		for (int i = 1; i <= this.parts.length - 2; i++) {
			if (this.parts[i] != null) {
				AgentInfo ai = this.ais.get(this.parts[i].getId());
				if (ai != null && ai.lastpos != i) {
					ai.lastpos = i;
					ai.posTime.put(i, time);

				}
			}
		}

		for (int i = this.from - 1; i <= this.to + 1; i++) {

			if (this.parts[i] != null) {

				AgentInfo ai = this.ais.get(this.parts[i].getId());
				if (this.parts[i].getDir() == 1) {
					calcPosRho(i, ai);
				} else {
					calcPosRho(i, ai);
				}
			}
		}

	}

	private void calcPosRho(int center, AgentInfo ai) {
		double rho = 0;
		int ds = 0;
		int us = 0;
		for (int i = center - this.cellRange / 2; i <= center + this.cellRange
				/ 2; i++) {
			if (this.parts[i] != null) {

				rho += AbstractCANetwork.RHO_HAT
						* a1DBSplineKernel(Math.abs(i - center), this.h);
				if (this.parts[i].getDir() == 1) {
					ds++;
				} else {
					us++;
				}
			}

		}
		ai.posRho.put(center, rho);
		ai.posBal.put(center, (double) ds / (double) us);
	}

	@Override
	public void report(BufferedWriter bw) throws IOException {
		for (AgentInfo ai : this.ais.values()) {
			report(ai, bw);
		}

		bw.flush();

	}

	private void report(AgentInfo ai, BufferedWriter bw) throws IOException {
		Double enterT = null;
		Double leaveT = null;
		if (ai.dir == 1) {
			enterT = ai.posTime.get(this.from);
			leaveT = ai.posTime.get(this.to);
		} else {
			enterT = ai.posTime.get(this.to);
			leaveT = ai.posTime.get(this.from);
		}
		if (enterT == null || leaveT == null) {
			return;
		}
		double tt = leaveT - enterT;
		double avgSpd = this.realRange / tt;
		int start, stop;
		if (ai.dir == 1) {
			start = this.from;
			stop = this.to;
		} else {
			start = this.to;
			stop = this.from;
		}
		Variance v = new Variance();
		for (int i = start; i != stop; i += ai.dir) {
			Double r = ai.posRho.get(i);
			if (r == null) {
				return;
			}
			if (ai.posBal.get(i) < 0.87 || ai.posBal.get(i) > 1.15) {
				System.out.println("bal:" + ai.posBal.get(i));
				return;
			}
			v.addVar(r);

		}

		double m = v.getMean();
		double std = Math.sqrt(v.getVar());
		if (std < 0.1 * m) {
			bw.append("0 " + m + " " + avgSpd + "\n");
		} else {
			System.out.println(" " + m + " " + avgSpd + " " + std);
		}
	}

	private static final class AgentInfo {
		TreeMap<Integer, Double> posTime = new TreeMap<>();
		TreeMap<Integer, Double> posRho = new TreeMap<>();
		TreeMap<Integer, Double> posBal = new TreeMap<>();

		int dir;

		public AgentInfo(int dir) {
			this.dir = dir;
		};

		int lastpos;
		double lastPosTime;

		@Override
		public String toString() {
			return lastpos + " " + lastPosTime;
		}
	}

	private double a1DBSplineKernel(final double r, double h) {
		final double sigma = 2d / 3d; // 1d normalization
		final double v = 1d; // 1d
		final double term1 = sigma / Math.pow(h, v);
		double q = r / h;
		if (q <= 1d) {
			final double term2 = 1d - 3d / 2d * Math.pow(q, 2d) + 3d / 4d
					* Math.pow(q, 3d);
			return term1 * term2;
		} else if (q <= 2d) {
			final double term2 = 1d / 4d * Math.pow(2d - q, 3);
			return term1 * term2;
		}
		return 0;
	}

}
