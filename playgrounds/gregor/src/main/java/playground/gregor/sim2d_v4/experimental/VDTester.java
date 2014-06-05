/* *********************************************************************** *
 * project: org.matsim.*
 * VDTester.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.experimental;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import playground.gregor.sim2d_v4.cgal.VoronoiCell;
import playground.gregor.sim2d_v4.cgal.VoronoiCenter;
import playground.gregor.sim2d_v4.cgal.VoronoiDiagramCells;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v4.events.XYVxVyEventsHandler;

import com.vividsolutions.jts.geom.Envelope;

public class VDTester implements XYVxVyEventsHandler, IterationEndsListener{

	private final VoronoiDiagramCells<VoronoiCenter> vd;
	private final List<VoronoiCenter> ves = new ArrayList<VoronoiCenter>();
	private final List<Measurement> ms = new ArrayList<Measurement>();
	private double time = 0;
	private final EventsManager em;
	private final Envelope e;
	private final BufferedWriter bf;

	private final int ws = 10;

	public VDTester(Envelope e, EventsManager em) {
		this.e = e;
		this.vd = new VoronoiDiagramCells<VoronoiCenter>(e,em);
		this.em = em;
		try {
			this.bf = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/bipedexp/fnd/fnd")));
			return;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		throw new RuntimeException();
	}

	@Override
	public void reset(int iteration) {


	}

	@Override
	public void handleEvent(XYVxVyEventImpl event) {
		if (this.time < event.getTime()) {
			processFrame();
			this.time = event.getTime();
			this.ves.clear();
		}
		VDObj ve = new VDObj(event);
		this.ves.add(ve);
	}

	private void processFrame() {

		List<VoronoiCell> cells = this.vd.update(this.ves);

		double v = 0;
		double a = 0;
		double vccw = 0;
		double vcw = 0;
		double acw = 0;
		double accw = 0;
		int ccw = 0;
		int cw = 0;
		for (VoronoiCell c : cells) {
			a += c.getArea();
			VDObj ve = (VDObj) c.getVoronoiCenter();
			if (ve.getEvent().getPersonId().toString().startsWith("d")) {
				v += ve.getEvent().getVX() * c.getArea();
				vccw += ve.getEvent().getVX() * c.getArea();
				accw += c.getArea();
				ccw++;
			} else {
				v -= ve.getEvent().getVX() * c.getArea();
				vcw -= ve.getEvent().getVX() * c.getArea();
				acw += c.getArea();
				cw++;
			}
		}
		if (a == 0){// || accw == 0 || acw == 0 || ccw < 1 || cw < 1) {
			return;
		}
		v /= a;
		vccw /= accw;
		vcw /= acw;
		double rho = cells.size()/a;
		double j = rho*v;
		double rhoccw = ccw /a;
		
		double rhocw = cw /a;
		double jccw = rhoccw * vccw;
		double jcw = rhocw * vcw;
//		if (rho >= 1){
//			System.out.println(rho + " " + rhoccw + " " + rhocw + " " + v + " " + j + " " + vccw + " " + jccw + " " + vcw + " " + jcw);
//		}
		Measurement m = new Measurement();
		m.j = j;//(jccw*accw + jcw*acw)/a;
		m.rho = rho;
		m.v = v;
		m.cwV = vcw;
		m.cwJ = jcw;
		m.cwRho = rhocw;
		m.ccwV = vccw;
		m.ccwJ = jccw;
		m.ccwRho = rhoccw;
		m.ccw = ccw;
		m.cw = cw;
		this.ms.add(m);
		//		try {
		//			this.bf.append(rho + " " + rhoccw + " " + rhocw + " " + v + " " + j + " " + vccw + " " + jccw + " " + vcw + " " + jcw +"\n");
		//		} catch (IOException e) {
		//			e.printStackTrace();
		//		}
		//		a /= cells.size();
	}

	private static final class VDObj implements VoronoiCenter {

		XYVxVyEventImpl e;
		private VoronoiCell cell;


		public VDObj(XYVxVyEventImpl e) {
			this.e = e;
		}

		public XYVxVyEventImpl getEvent() {
			return this.e;
		}

		@Override
		public double getX() {
			return this.e.getX();
		}

		@Override
		public double getY() {
			return this.e.getY();
		}

		@Override
		public void setVoronoiCell(VoronoiCell cell) {
			this.cell = cell;
		}

		@Override
		public VoronoiCell getVoronoiCell() {
			return this.cell;
		}

	}
	private final class Measurement {
		public int cw;
		public int ccw;
		double rho;
		double v;
		double j;
		double ccwRho;
		double ccwV;
		double ccwJ;
		double cwRho;
		double cwV;
		double cwJ;
	}

	private final class MComp implements Comparator<Measurement> {

		@Override
		public int compare(Measurement o1, Measurement o2) {
			if (o1.rho < o2.rho) {
				return -1;
			}
			if (o1.rho > o2.rho) {
				return 1;
			}
			return 0;
		}

	}


	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		MComp comp = new MComp();
//		Collections.sort(this.ms,comp);
		double rho = 0;
		double v = 0;
		double j = 0;
		double rhocw = 0;
		double vcw = 0;
		double jcw = 0;
		double rhoccw = 0;
		double vccw = 0;
		double jccw = 0;
		int cw = 0;
		int ccw = 0;
		int cnt = 0;
		//		int qunatilSize = this.ms.size()/1;
		for (Measurement m : this.ms) {
			rho += m.rho;
			v += m.v;
			j += m.j;
			vccw += m.ccwV;
			rhoccw += m.ccwRho;
			jccw += m.ccwJ;
			vcw += m.cwV;
			rhocw += m.cwRho;
			jcw += m.cwJ;
			cw += m.cw;
			ccw += m.ccw;
			//			if (++cnt % qunatilSize == 0) {
			//				rho /= qunatilSize;
			//				v /= qunatilSize;
			//				j /= qunatilSize;
			//				vccw /= qunatilSize;
			//				rhoccw /= qunatilSize;
			//				jccw /= qunatilSize;
			//				vcw /= qunatilSize;
			//				rhocw /= qunatilSize;
			//				jcw /= qunatilSize;
			try {
				this.bf.append(rho + " " + rhoccw + " " + rhocw + " " + v + " " + j + " " + vccw + " " + jccw + " " + vcw + " " + jcw + " " + ccw + " " + cw + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
			rho = 0;
			v = 0;
			j = 0;
			vccw = 0;
			rhoccw = 0;
			jccw = 0;
			vcw = 0;
			rhocw = 0;
			jcw = 0;
			cw = 0;
			ccw = 0;

			//			}
		}

		try {
			this.bf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
