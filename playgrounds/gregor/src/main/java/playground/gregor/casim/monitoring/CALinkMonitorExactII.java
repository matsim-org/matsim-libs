package playground.gregor.casim.monitoring;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

import playground.gregor.casim.simulation.physics.AbstractCANetwork;
import playground.gregor.casim.simulation.physics.CALink;
import playground.gregor.casim.simulation.physics.CAMoveableEntity;
import playground.gregor.utils.Variance;

public class CALinkMonitorExactII extends CALinkMonitorExact {

	// private CALink l;
	private CAMoveableEntity[] parts;
	private int from;
	private int to;

	private double realRange;

	private List<Measure> ms = new ArrayList<Measure>();
	private double lastTriggered = -1;

	private final double h;
	private double cellWidth;

	private final Map<Id, AgentInfo> ais = new HashMap<>();
	private BufferedWriter spaceTimePlotter;

	private List<CAMoveableEntity> agents = new ArrayList<>();
	private CALink l;
	private double lastSpaceTimeSeriesReport = 0;
	private CAMoveableEntity[] parts2;
	private final int cellRange;

	public CALinkMonitorExactII(CALink l, double range,
			CAMoveableEntity[] parts, double laneWidth) {
		super(l, range, parts, laneWidth);
		this.l = l;
		this.parts = parts;
		int num = l.getNumOfCells();
		this.cellWidth = l.getLink().getLength() / num;
		double cells = range / cellWidth;
		this.from = (int) (num / 2. - cells / 2 + .5);
		this.to = (int) (num / 2. + cells / 2 + .5);
		this.h = (to - from) / 4.;
		this.realRange = (to - from) * cellWidth;
		this.cellRange = (to - from);

	}

	@Override
	public void addSpaceTimePlotter(BufferedWriter bw, CAMoveableEntity[] parts2) {
		this.spaceTimePlotter = bw;
		this.parts2 = parts2;
	}

	@Override
	public void init() {

		for (int i = this.from; i <= this.to; i++) {
			CAMoveableEntity p = this.parts[i];
			if (p != null) {
				AgentInfo ai = new AgentInfo(p.getDir(), 0);
				ai.lastPosTime = 0;
				ai.lastpos = i;
				this.ais.put(p.getId(), ai);
				if (this.spaceTimePlotter != null) {
					this.agents.add(p);
				}
			}
		}

		if (this.spaceTimePlotter != null) {
			for (int i = 0; i < this.parts.length; i++) {
				CAMoveableEntity p = this.parts[i];
				if (p != null) {
					this.agents.add(p);
				}
			}
			// unidir exp
			for (int i = 0; i < this.parts2.length; i++) {
				CAMoveableEntity p = this.parts2[i];
				if (p != null) {
					this.agents.add(p);
				}
			}

		}
		trigger(0);
	}

	@Override
	public void trigger(double time) {
		if (time <= lastTriggered) {
			return;
		}
		if (this.spaceTimePlotter != null
				&& time > lastSpaceTimeSeriesReport + 1 && time <= 600) {
			reportSpaceTimeSeries(time);
			lastSpaceTimeSeriesReport = time;
		}
		lastTriggered = time;
		// 1. check density
		int cnt = 0;
		// int dsCnt = 0;
		// int usCnt = 0;

		for (int i = this.from - 1; i <= this.to + 1; i++) {
			CAMoveableEntity p = this.parts[i];
			if (p != null) {
				AgentInfo ai = this.ais.get(p.getId());
				if (ai == null) {
					ai = new AgentInfo(p.getDir(), 0);
					ai.lastPosTime = 0;
					ai.lastpos = i;
					this.ais.put(p.getId(), ai);
				}
			}
		}

		if (this.parts[this.from - 1] != null
				&& this.parts[this.from - 1].getDir() == 1) {
			AgentInfo ai = this.ais.get(this.parts[this.from - 1].getId());
			if (ai == null) {
				ai = new AgentInfo(1, time);
				ai.lastPosTime = time;
				ai.lastpos = this.from - 1;
				this.ais.put(this.parts[this.from - 1].getId(), ai);
			}

		}
		if (this.parts[this.to + 1] != null
				&& this.parts[this.to + 1].getDir() == -1) {
			AgentInfo ai = this.ais.get(this.parts[this.to + 1].getId());
			if (ai == null) {
				ai = new AgentInfo(-1, time);

				ai.lastPosTime = time;
				ai.lastpos = this.to + 1;
				this.ais.put(this.parts[this.to + 1].getId(), ai);
			}

		}

		{
			CAMoveableEntity part = this.parts[0];
			if (part != null && part.getDir() == -1) {
				AgentInfo ai = this.ais.remove(part.getId());
			}
		}
		{
			CAMoveableEntity part = this.parts[this.parts.length - 1];
			if (part != null && part.getDir() == 1) {
				AgentInfo ai = this.ais.remove(part.getId());
			}
		}

		Measure m = new Measure(time);
		for (int i = 1; i <= this.parts.length - 2; i++) {
			if (this.parts[i] != null) {
				AgentInfo ai = this.ais.get(this.parts[i].getId());
				if (ai != null && ai.lastpos != i) {
					// double tt = time - ai.lastLastLastPosTime;
					// ai.lastLastLastPosTime = ai.lastLastPosTime;
					// ai.lastLastPosTime = ai.lastPosTime;
					// ai.lastPosTime = time;
					ai.lastpos = i;
					ai.timePos.put(time, i);
				}
			}
		}

		double dsRho = 0;
		double usRho = 0;
		double rho = 0;
		double usCnt = 0;
		double dsCnt = 0;
		int center = (this.from + this.to) / 2;
		// int spdRange = (to - 2) - (from + 2) - 1;
		// double spdH = spdRange / 4.;

		Variance vDs = new Variance();
		Variance vUs = new Variance();
		// int dsCnt = 0;
		for (int i = this.from; i <= this.to; i++) {

			if (this.parts[i] != null) {

				AgentInfo ai = this.ais.get(this.parts[i].getId());
				rho += AbstractCANetwork.RHO_HAT
						* a1DBSplineKernel(Math.abs(i - center), this.h);
				if (this.parts[i].getDir() == 1) {
					dsCnt++;
					updateVariance(vDs, i, 1, ai, time);
					// if (i > from + 10 && i < to - 10) {
					m.dsAis.add(ai);
					// }
					// * a1DBSplineKernel(Math.abs(i - center), this.h);
					// dsRho += AbstractCANetwork.RHO_HAT
					// * a1DBSplineKernel(Math.abs(i - center), this.h);
					dsRho += ai.timeRho.get(time);
				} else {
					usCnt++;
					updateVariance(vUs, i, -1, ai, time);
					// if (i > from + 10 && i < to - 10) {
					m.usAis.add(ai);
					// }
					// * a1DBSplineKernel(Math.abs(i - center), this.h);
					// usRho += AbstractCANetwork.RHO_HAT
					// * a1DBSplineKernel(Math.abs(i - center), this.h);
					usRho += ai.timeRho.get(time);
				}
			}
		}

		dsRho /= dsCnt;
		usRho /= usCnt;
		m.dsRho = dsRho; // dsCnt;
		m.usRho = usRho;// usCnt;
		m.rho = rho;
		double meanDs = vDs.getMean();
		double varDs = vDs.getVar();
		double meanUs = vUs.getMean();
		double varUs = vUs.getVar();
		// if (time > 10) {
		// System.out.println("stop");
		// }
		double balance = dsRho / usRho;
		// if (Math.sqrt(varDs) < 0.1 * meanDs && Math.sqrt(varUs) < 0.1 *
		// meanUs
		// && balance >= 0.9 && balance <= 1.1111111111 && time > 1) {
		this.ms.add(m);
		// }
	}

	private void updateVariance(Variance v, int center, int dir, AgentInfo ai,
			double time) {
		double rho = 0;
		for (int i = center - this.cellRange / 2; i <= center + this.cellRange
				/ 2; i++) {
			if (this.parts[i] != null && this.parts[i].getDir() == dir) {
				rho += AbstractCANetwork.RHO_HAT
						* a1DBSplineKernel(Math.abs(i - center), this.h);
			}

		}
		ai.timeRho.put(time, rho);
		v.addVar(rho);
	}

	private void reportSpaceTimeSeries(double time) {

		try {
			this.spaceTimePlotter.append(time + " ");
			for (CAMoveableEntity a : this.agents) {
				if (a.getCurrentCANetworkEntity() == this.l) {
					this.spaceTimePlotter.append(a.getPos() + " ");
				} else {
					this.spaceTimePlotter.append("0 ");
				}
			}
			this.spaceTimePlotter.append("\n");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void report(BufferedWriter bw) throws IOException {

		for (Measure m : this.ms) {
			if (!cmptSpd(m)) {
				m.dsRho = Double.NaN;
			}
			bw.append(m.time + " " + m.dsRho + " " + m.dsSpd + " " + m.usRho
					+ " " + m.usSpd + " " + m.rho + "\n");
		}
		if (true) {
			return;
		}
		List<Tuple<Integer, Integer>> ranges = new ArrayList<Tuple<Integer, Integer>>();
		int from = 0;
		int to = 0;
		int cnt = 0;
		Variance vDs = new Variance();
		Variance vUs = new Variance();
		for (Measure m : this.ms) {
			cnt++;
			vDs.addVar(m.dsRho);
			vUs.addVar(m.usRho);
			if (cnt < 2) {
				continue;
			}
			double stdDs = Math.sqrt(vDs.getVar());
			double stdUs = Math.sqrt(vUs.getVar());

			if (stdDs > 0.1 * vDs.getMean() || stdUs > 0.1 * vUs.getMean()
					|| cnt == ms.size()) {
				if (cnt > 5) {
					ranges.add(new Tuple<Integer, Integer>(from, to));
				}
				vDs = new Variance();
				vUs = new Variance();
				cnt = 0;
				from = to;
			}

			to++;
		}
		double stdDs = Math.sqrt(vDs.getVar());
		double stdUs = Math.sqrt(vUs.getVar());
		int ccnt = 0;
		for (Tuple<Integer, Integer> t : ranges) {
			// if (ccnt++ > 5) {
			// continue;
			// }
			double range = t.getSecond() - t.getFirst();
			Measure m = new Measure(0);
			for (int i = t.getFirst(); i < t.getSecond(); i++) {

				Measure c = this.ms.get(i);
				m.dsRho += c.dsRho / range;
				m.dsSpd += c.dsSpd / range;
				m.usRho += c.usRho / range;
				m.usSpd += c.usSpd / range;
				m.time += c.time / range;
				m.rho += c.rho / range;

				// if (c.dsSpd < 0.0001 || c.usSpd < 0.0001) {
				// continue;
				// }
				// bw.append(c.time + " " + c.dsRho + " " + c.dsSpd + " "
				// + c.usRho + " " + c.usSpd + " " + c.rho + "\n");
			}
			bw.append(m.time + " " + m.dsRho + " " + m.dsSpd + " " + m.usRho
					+ " " + m.usSpd + " " + m.rho + "\n");
		}
		bw.flush();

	}

	private boolean cmptSpd(Measure m) {
		double dsSpd = 0;
		double time = m.time;
		int cnt = 0;
		// Variance vDsSpd = new Variance();
		double totalDsSpace = 0;
		for (AgentInfo ai : m.dsAis) {
			Entry<Double, Integer> floor = ai.timePos.floorEntry(time - 1.5);
			Entry<Double, Integer> ceil = ai.timePos.ceilingEntry(time + 1.5);
			if (floor == null || ceil == null) {
				continue;
			}
			cnt++;
			double tt = ceil.getKey() - floor.getKey();
			double dist = (ceil.getValue() - floor.getValue()) * this.cellWidth;
			double spd = (dist / tt);
			double rhoF = ai.timeRho.floorEntry(time).getValue();
			double rhoC = ai.timeRho.ceilingEntry(time).getValue();
			double rho = (rhoF + rhoC) / 2;
			double space = 1 / rho;
			// vDsSpd.addVar(spd);
			dsSpd += spd * space;
			totalDsSpace += space;
		}
		if (cnt > 0)
			dsSpd /= totalDsSpace;
		m.dsSpd = dsSpd;

		double usSpd = 0;
		cnt = 0;
		double totalUsSpace = 0;
		// Variance vUsSpd = new Variance();
		for (AgentInfo ai : m.usAis) {
			Entry<Double, Integer> floor = ai.timePos.floorEntry(time - 1.5);
			Entry<Double, Integer> ceil = ai.timePos.ceilingEntry(time + 1.5);
			if (floor == null || ceil == null) {
				continue;
			}
			cnt++;
			double tt = ceil.getKey() - floor.getKey();
			double dist = (ceil.getValue() - floor.getValue()) * this.cellWidth;
			double spd = -(dist / tt);
			// vUsSpd.addVar(spd);
			double rhoF = ai.timeRho.floorEntry(time).getValue();
			double rhoC = ai.timeRho.ceilingEntry(time).getValue();
			double rho = (rhoF + rhoC) / 2;
			double space = 1 / rho;
			usSpd += spd * space;
			totalUsSpace += space;
		}
		if (cnt > 0)
			usSpd /= totalUsSpace;
		m.usSpd = usSpd;

		// if (Math.sqrt(vDsSpd.getVar()) < 0.2 * vDsSpd.getMean()
		// && Math.sqrt(vUsSpd.getVar()) < 0.2 * vUsSpd.getMean()) {
		// return true;
		// }
		return true;

	}

	private static final class AgentInfo {
		TreeMap<Double, Integer> timePos = new TreeMap<>();
		TreeMap<Double, Double> timeRho = new TreeMap<>();

		public double lastLastLastPosTime;
		public double lastLastPosTime;
		int dir;
		double enterT;

		public AgentInfo(int dir, double enterT) {
			this.dir = dir;
			this.enterT = enterT;
		};

		int lastpos;
		double lastPosTime;

		@Override
		public String toString() {
			return lastpos + " " + lastPosTime;
		}
	}

	private final class Measure {

		public Measure(double time) {
			this.time = time;
		}

		double dsSpd;
		double pDsSpd;
		double pUsSpd;
		double dsRho;
		double usSpd;
		double usRho;
		double rho;
		double time;
		List<AgentInfo> dsAis = new ArrayList<>();
		List<AgentInfo> usAis = new ArrayList<>();
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
