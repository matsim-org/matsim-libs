/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

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

import playground.gregor.casim.simulation.physics.CAMoveableEntity;
import playground.gregor.casim.simulation.physics.CAMultiLaneLink;
import playground.gregor.utils.Variance;

public class CALinkMultiLaneMonitor implements Monitor {

	private final double cellsize;
	private final int from;
	private final int to;
	private final double w;
	private final double range;
	private final double a;
	private final int lanes;
	private final CAMultiLaneLink l;

	private final List<Measure> ms = new ArrayList<Measure>();

	private final Map<Id, AgentInfo> ais = new HashMap<>();
	private int dsIn;
	private int usOut;
	private int dsOut;
	private int usIn;
	private final int cellRange;
	private final int h;

	public CALinkMultiLaneMonitor(CAMultiLaneLink l, double range) {
		this.cellsize = l.getLink().getLength() / l.getNumOfCells();
		int cells = (int) (range / this.cellsize);
		this.from = (int) (l.getNumOfCells() / 2. - cells / 2.);
		this.to = (int) (l.getNumOfCells() / 2. + cells / 2.);
		this.w = l.getLink().getCapacity();
		this.range = (this.to - this.from + 1) * this.cellsize;
		this.a = this.w * this.range;
		this.lanes = l.getNrLanes();
		this.l = l;
		this.cellRange = this.to - this.from + 1;
		this.h = this.cellRange;
	}

	@Override
	public void trigger(double time) {
		Measure m = new Measure();
		m.time = time;
		int cnt = 0;
		double dsCnt = 0;
		double usCnt = 0;
		double dsRho = 0;
		double usRho = 0;
		for (int lane = 0; lane < this.lanes; lane++) {
			CAMoveableEntity[] p = this.l.getParticles(lane);
			for (int i = this.from; i <= this.to; i++) {
				if (p[i] != null) {
					cnt++;
					m.in.add(p[i].getId());
					AgentInfo ai = this.ais.get(p[i].getId());
					// updateTimeRho(i, ai.dir, ai, time, p);
					if (p[i].getDir() == 1) {

						dsCnt++;
						// dsRho += ai.timeRho.get(time);
					} else {
						usCnt++;
						// usRho += ai.timeRho.get(time);
					}
				}
			}
		}
		double rho = cnt / this.a;

		m.rho = rho;
		m.dsRho = dsCnt / this.a;
		m.usRho = usCnt / this.a;
		// m.dsRho = dsRho / dsCnt;
		// m.usRho = usRho / usCnt;

		for (int lane = 0; lane < this.lanes; lane++) {
			CAMoveableEntity[] p = this.l.getParticles(lane);
			for (int i = 0; i < this.l.getNumOfCells(); i++) {
				CAMoveableEntity part = p[i];
				if (part != null) {
					Id id = part.getId();
					AgentInfo ai = this.ais.get(id);
					if (ai == null
							&& (i == 0 || i == this.l.getNumOfCells() - 1)) {
						ai = new AgentInfo(part.getDir());
						// ai.lastPos = i;
						this.ais.put(id, ai);
					}
					if (ai.lastPos != i) {
						ai.timePos.put(time, i);
						ai.lastPos = i;
						if (i == 0) {
							if (part.getDir() == 1) {
								this.dsIn++;
							} else {
								this.usOut++;
							}
						} else if (i == this.l.getNumOfCells() - 1) {
							if (part.getDir() == 1) {
								this.dsOut++;
							} else {
								this.usIn++;
							}
						}

					}
				}
			}
		}
		if (m.rho > 0) {
			this.ms.add(m);
		}
	}

	// private void updateTimeRho(int center, int dir, AgentInfo ai, double
	// time,
	// CAMoveableEntity[] parts) {
	// double rho = 0;
	// for (int i = center - 2 * this.h; i <= center + 2 * this.h; i++) {
	// if (parts[i] != null && parts[i].getDir() == dir) {
	// rho += AbstractCANetwork.RHO_HAT
	// * a1DBSplineKernel(Math.abs(i - center), this.h);
	// }
	//
	// }
	// ai.timeRho.put(time, rho);
	// }
	//
	// private double a1DBSplineKernel(final double r, double h) {
	// final double sigma = 2d / 3d; // 1d normalization
	// final double v = 1d; // 1d
	// final double term1 = sigma / Math.pow(h, v);
	// double q = r / h;
	// if (q <= 1d) {
	// final double term2 = 1d - 3d / 2d * Math.pow(q, 2d) + 3d / 4d
	// * Math.pow(q, 3d);
	// return term1 * term2;
	// } else if (q <= 2d) {
	// final double term2 = 1d / 4d * Math.pow(2d - q, 3);
	// return term1 * term2;
	// }
	// return 0;
	// }

	@Override
	public void report(BufferedWriter bw) throws IOException {
		int in = this.dsIn + this.usIn - this.dsOut - this.usOut;
		System.out.println(in + " " + " dsIn:" + this.dsIn + " dsOut:"
				+ this.dsOut + " usIn:" + this.usIn + " usOut:" + this.usOut);

		List<Tuple<Integer, Integer>> ranges = new ArrayList<>();
		Variance vd = new Variance();
		Variance vs = new Variance();
		Variance vf = new Variance();
		int cnt = 0;
		double tm = 15;
		int from = 0;
		int to = 0;
		for (Measure m : this.ms) {
			cpdSpd(m);
			bw.append(m.time + " " + m.dsRho + " " + m.dsSpd + " " + m.usRho
					+ " " + m.usSpd + " " + m.rho + " " + " " + m.spd + "\n");

			if (true)
				continue;
			if (m.time < 15 || m.time > 100) {
				to++;
				from++;
				continue;
			}
			cpdSpd(m);
			vd.addVar(m.rho);
			vs.addVar(m.spd);
			vf.addVar(m.rho * m.spd);
			cnt++;
			if (cnt < 5) {
				to++;
				continue;
			}
			double md = vd.getMean();
			double std = Math.sqrt(vd.getVar());

			double ms = vs.getMean();
			double sts = Math.sqrt(vs.getVar());

			double mf = vf.getMean();
			double stf = Math.sqrt(vf.getVar());

			double bal = m.dsRho / m.usRho;
			bal = bal > 1 ? 1 / bal : bal;
			// bal = 1;
			double timespan = m.time - tm;
			if (md > 1 && std > 0.1 * md || sts > 0.1 * ms || bal < 0.5
					|| timespan > 20) {
				if (timespan > 5) {
					Tuple<Integer, Integer> t = new Tuple<>(from, to);
					ranges.add(t);
				}
				tm = m.time;
				from = to;
				cnt = 0;
				vd = new Variance();
				vs = new Variance();
			}
			to++;
		}

		int rng = ranges.size();
		int cntt = 0;
		for (Tuple<Integer, Integer> r : ranges) {
			cntt++;
			// if (cntt < rng / 2 + rng / 4 || cntt > rng / 2 + rng / 4 + rng /
			// 8) {
			// continue;
			// }
			from = r.getFirst();
			to = r.getSecond();
			Measure m = new Measure();

			double range = to - from;
			double ccnt = 0;
			double lastTime = 0;
			for (int idx = from; idx < to; idx++) {
				Measure mm = this.ms.get(idx);
				if (lastTime + 1 > mm.time) {
					continue;
				}
				ccnt++;
				m.rho += mm.rho;
				m.spd += mm.spd;
				m.time += mm.time;
				m.dsRho += mm.dsRho;
				m.usRho += mm.usRho;
				m.dsSpd += mm.dsSpd;
				m.usSpd += mm.usSpd;
				bw.append(mm.time + " " + mm.dsRho + " " + mm.dsSpd + " "
						+ mm.usRho + " " + mm.usSpd + " " + mm.rho + " " + " "
						+ mm.spd + "\n");
				lastTime = mm.time;
			}
			m.rho /= ccnt;
			m.spd /= ccnt;
			m.dsRho /= ccnt;
			m.dsSpd /= ccnt;
			m.usRho /= ccnt;
			m.usSpd /= ccnt;
			m.time /= ccnt;
			// bw.append(m.time + " " + m.dsRho + " " + m.dsSpd + " " + m.usRho
			// + " " + m.usSpd + " " + m.rho + " " + " " + m.spd + "\n");

		}
		// bw.append(m.time + " " + m.rho + " " + m.spd + "\n");

	}

	private void cpdSpd(Measure m) {
		double fr = m.time - 2;
		double to = m.time + 2;
		double spd = 0;
		double cnt = 0;
		double dsSpd = 0;
		double dsCnt = 0;
		double usSpd = 0;
		double usCnt = 0;
		for (Id id : m.in) {
			AgentInfo ai = this.ais.get(id);
			Entry<Double, Integer> frEntr = ai.timePos.floorEntry(fr);
			Entry<Double, Integer> toEntr = ai.timePos.ceilingEntry(to);
			if (frEntr == null) {
				frEntr = ai.timePos.ceilingEntry(fr);
			}
			if (toEntr == null) {
				toEntr = ai.timePos.floorEntry(to);
			}
			double tt = toEntr.getKey() - frEntr.getKey();
			int cells = toEntr.getValue() - frEntr.getValue();
			double dist = Math.abs(cells) * this.cellsize;
			double s = dist / tt;
			spd += s;
			cnt++;
			if (ai.dir == 1) {
				dsSpd += s;
				dsCnt++;
			} else {
				usSpd += s;
				usCnt++;
			}
		}
		m.spd = spd / cnt;
		if (dsCnt > 0) {
			m.dsSpd = dsSpd / dsCnt;
		}
		if (usCnt > 0) {
			m.usSpd = usSpd / usCnt;
		}

	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	private static final class AgentInfo {

		private final int dir;

		public AgentInfo(int dir) {
			this.dir = dir;
		}

		int lastPos = -1;
		TreeMap<Double, Integer> timePos = new TreeMap<>();
		// TreeMap<Double, Double> timeRho = new TreeMap<>();

	}

	private static final class Measure {
		public double usSpd;
		public double dsSpd;
		public double usRho;
		public double dsRho;
		public double spd;
		List<Id> in = new ArrayList<>();
		double rho;
		double time;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
}
