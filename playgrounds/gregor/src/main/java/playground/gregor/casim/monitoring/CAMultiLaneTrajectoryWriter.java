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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import playground.gregor.casim.simulation.physics.CAMoveableEntity;
import playground.gregor.casim.simulation.physics.CAMultiLaneLink;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v4.events.debug.LineEvent;

public class CAMultiLaneTrajectoryWriter implements Monitor {
	
	
	private CAMultiLaneLink l;
	private JsonGenerator generator;

	public CAMultiLaneTrajectoryWriter(CAMultiLaneLink l, String outDir) {
		this.l = l;
		JsonFactory factory = new JsonFactory();
        try {
			this.generator = factory.createGenerator(new FileWriter(new File(outDir + "/tr_link_"+l.getLink().getId()+ ".json")));
			this.generator.setPrettyPrinter(new DefaultPrettyPrinter());
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	

	@Override
	public void trigger(double time) {
		double dx = l.getLink().getToNode().getCoord().getX()
				- l.getLink().getFromNode().getCoord().getX();
		double dy = l.getLink().getToNode().getCoord().getY()
				- l.getLink().getFromNode().getCoord().getY();
		double length = Math.sqrt(dx * dx + dy * dy);
		dx /= length;
		dy /= length;
		double ldx = dx;
		double ldy = dy;
		double incr = l.getLink().getLength() / l.getNumOfCells();
		dx *= incr;
		dy *= incr;
		double laneWidth = l.getLaneWidth();
		int lanes = l.getNrLanes();
		double hx = -ldy;
		double hy = ldx;
		hx *= laneWidth;
		hy *= laneWidth;

		double x0 = l.getLink().getFromNode().getCoord().getX() - hx * lanes
				/ 2 + hx / 2 + dx / 2;
		double y0 = l.getLink().getFromNode().getCoord().getY() - hy * lanes
				/ 2 + hy / 2 + dy / 2;
		
		boolean allNull = true;
		for (int lane = 0; lane < l.getNrLanes(); lane++) {
			double x = x0 + lane * hx;
			double y = y0 + lane * hy;

			for (int i = 0; i < l.getNumOfCells(); i++) {
				if (l.getParticles(lane)[i] != null) {
					allNull = false;
					CAMoveableEntity o = l.getParticles(lane)[i];
					double ddx = 1;
					if (o.getDir() == -1) {
						ddx = -1;
					}
					
					try {
						generator.writeStartObject();
						generator.writeFieldName("time");
						generator.writeString(Double.toString(time));
						generator.writeFieldName("id");
						generator.writeString(o.getId().toString());
						generator.writeFieldName("x");
						generator.writeString(Double.toString(x));
						generator.writeFieldName("y");
						generator.writeString(Double.toString(y));
						generator.writeFieldName("vx");
						generator.writeString(Double.toString(ldx*ddx));
						generator.writeFieldName("vy");
						generator.writeString(Double.toString(ldy*ddx));
						generator.writeEndObject();
						
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}

				}
				x += dx;
				y += dy;
			}
		}
		if (allNull) {
			double x = x0;
			double y = y0;
			try {
				generator.writeStartObject();
				generator.writeFieldName("time");
				generator.writeString(Double.toString(time));
				generator.writeFieldName("id");
				generator.writeString("-1");
				generator.writeFieldName("x");
				generator.writeString(Double.toString(x));
				generator.writeFieldName("y");
				generator.writeString(Double.toString(y));
				generator.writeFieldName("vx");
				generator.writeString("0");
				generator.writeFieldName("vy");
				generator.writeString("0");
				generator.writeEndObject();
				
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			}
		}

	}

	@Override
	public void report(BufferedWriter bw) throws IOException {
		throw new UnsupportedOperationException();

	}

	@Override
	public void init() {
		throw new UnsupportedOperationException();
	}


	@Override
	public void reset() {
		try {
			generator.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

}
