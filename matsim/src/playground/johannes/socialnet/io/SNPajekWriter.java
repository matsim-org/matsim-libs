/* *********************************************************************** *
 * project: org.matsim.*
 * SNPajekWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnet.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPlanElement;

import playground.johannes.graph.io.PajekAttributes;
import playground.johannes.graph.io.PajekWriter;
import playground.johannes.graph.io.PajekColorizer;
import playground.johannes.socialnet.Ego;
import playground.johannes.socialnet.SocialNetwork;
import playground.johannes.socialnet.SocialTie;

/**
 * @author illenberger
 *
 */
public class SNPajekWriter<P extends BasicPerson<? extends BasicPlan<? extends BasicPlanElement>>> extends
		PajekWriter<SocialNetwork<P>,
		Ego<P>, SocialTie>
		implements PajekAttributes<Ego<P>, SocialTie> {

	private PajekColorizer<Ego<P>, SocialTie> colorizer;

	public void write(SocialNetwork<P> g, PajekColorizer<Ego<P>, SocialTie> colorizer, String file) throws IOException {
		this.colorizer = colorizer;
		super.write(g, this, file);
	}
	
	@Override
	protected String getVertexLabel(Ego<P> v) {
		return v.getPerson().getId().toString();
	}

	@Override
	protected String getVertexX(Ego<P> v) {
		return String.valueOf(v.getCoord().getX());
	}

	@Override
	protected String getVertexY(Ego<P> v) {
		return String.valueOf(v.getCoord().getY());
	}

	@Override
	public void write(SocialNetwork<P> g, String file) throws IOException {
		colorizer = new DefaultColorizer();
		super.write(g, this, file);
	}

	public List<String> getEdgeAttributes() {
		return new ArrayList<String>();
	}

	public String getEdgeValue(SocialTie e, String attribute) {
		return null;
	}

	public List<String> getVertexAttributes() {
		List<String> attrs = new ArrayList<String>();
		attrs.add(PajekAttributes.VERTEX_FILL_COLOR);
		return attrs;
	}

	public String getVertexValue(Ego<P> v, String attribute) {
		if (PajekAttributes.VERTEX_FILL_COLOR.equals(attribute))
			return colorizer.getVertexFillColor(v);
		else
			return null;
	}

	private class DefaultColorizer extends PajekColorizer<Ego<P>, SocialTie> {

		private static final String COLOR_BLACK = "13";

		public String getVertexFillColor(Ego<P> ego) {
			return COLOR_BLACK;
		}

		@Override
		public String getEdgeColor(SocialTie e) {
			return COLOR_BLACK;
		}
		
	}
}
