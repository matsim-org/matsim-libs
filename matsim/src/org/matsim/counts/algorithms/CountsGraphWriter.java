/* *********************************************************************** *
 * project: org.matsim.*
 * CountsGraphWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.counts.algorithms;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.matsim.counts.CountSimComparison;
import org.matsim.counts.Counts;
import org.matsim.counts.algorithms.graphs.CountsGraph;
import org.matsim.counts.algorithms.graphs.CountsGraphsCreator;
import org.matsim.counts.algorithms.graphs.helper.OutputDelegate;



public class CountsGraphWriter extends CountsAlgorithm {

	private String iter_path_;
	private List<CountSimComparison> ccl_;
	private int iteration_;
	private boolean pdfset_=true;
	private boolean htmlset_=true;
	private OutputDelegate outputDelegate_;
	private String projectName_="PROJECT X";
	private List<CountsGraphsCreator> graphsCreators;
	
	public CountsGraphWriter() {
	}
	
	public CountsGraphWriter(final String iter_path, final List<CountSimComparison> ccl, final int iteration, final boolean htmlset,final boolean pdfset) {
		this.iter_path_=iter_path+"/graphs/";
		this.ccl_=ccl;
		this.iteration_=iteration;
	
		// delegate pattern without callback
		this.outputDelegate_=new OutputDelegate(this.iter_path_);
		
		this.htmlset_=htmlset;
		this.pdfset_=pdfset;
		if (htmlset || pdfset) {
			new File(this.iter_path_).mkdir();
		}
		this.graphsCreators=new Vector<CountsGraphsCreator>();
	}
		
	public void setPDFOutput(final boolean pdfset) {
		this.pdfset_=pdfset;
	}
	
	public void setHTMLOutput(final boolean htmlset) {
		this.htmlset_=htmlset;
	}
	
	public OutputDelegate getOutput() {
		return this.outputDelegate_;
	}
	
	public void setProjectName(final String projectName) {
		this.projectName_=projectName;
	}
	
	public void setGraphsCreator(final CountsGraphsCreator graphsCreator) {
		this.graphsCreators.add(graphsCreator);
	}

	@Override
	public void run(final Counts count){	
		System.out.println("Creating graphs");
		this.outputDelegate_.setProjectName(this.projectName_);
		
		
		Iterator<CountsGraphsCreator> cgc_it = this.graphsCreators.iterator();		
		while (cgc_it.hasNext()) {
			CountsGraphsCreator cgc= cgc_it.next();	
			List<CountsGraph> graphs=cgc.createGraphs(this.ccl_, this.iteration_);
			
			this.outputDelegate_.addSection(cgc.getSection());
			
			Iterator<CountsGraph> cg_it = graphs.iterator();		
			while (cg_it.hasNext()) {
				CountsGraph cg=cg_it.next();
				this.outputDelegate_.addCountsGraph(cg);
			}	
		}
			
/*
		Section section0=new Section("sim vs. real volumes per hour");
		this.outputDelegate_.addSection(section0);
		for (int i=0; i<24; i++) {	
			String fileName="sim_vs_real_volumes_per_hour"+(i+1);
			CountsSimRealPerHourGraph sg=new CountsSimRealPerHourGraph(this.ccl_, this.factor_, this.iteration_, fileName );
			sg.createChart(i+1);
			this.outputDelegate_.addCountsGraph(sg);
			section0.addURL(new MyURL(fileName+".html", "hour: "+(i+1)));
		}

		
		Section section1=new Section("Error plots");
		this.outputDelegate_.addSection(section1);
		String fileName="mean_values";
		CountsErrorGraph ep=new CountsErrorGraph(this.ccl_, this.factor_, this.iteration_, fileName);
		ep.createChart(0);
		this.outputDelegate_.addCountsGraph(ep);
		section1.addURL(new MyURL(fileName+".html", "mean_values"));
		
		// this is really ugly:...-----------------------------------------------------------------	
		// replace asap
		Section section2=new Section("Load curve graph");		
		Iterator<CountSimComparison> l_it = this.ccl_.iterator();
		CountSimComparison cc_last=null;
		while (l_it.hasNext()) {
			CountsLoadCurveGraph lcg=new CountsLoadCurveGraph(this.ccl_, this.factor_,this.iteration_,  "dummy");
			if (cc_last!=null) {
				lcg.add2LoadCurveDataSets(cc_last);
			}
			CountSimComparison cc= l_it.next();	
			String link_id=cc.getId();
			while (cc.getId().equals(link_id)) {
				if (l_it.hasNext()) {
					lcg.add2LoadCurveDataSets(cc);
					cc= l_it.next();
				}
				else {
					lcg.add2LoadCurveDataSets(cc);
					break;
				}			
			}
			lcg.setChartTitle("link"+link_id);
			lcg.createChart(0);
			this.outputDelegate_.addCountsGraph(lcg);
			section2.addURL(new MyURL("link"+link_id+".html", "link"+link_id));
			cc_last=cc;			
		}//while	
		// ----------------------------------------------------------------------------------------
		this.outputDelegate_.addSection(section2);
		
*/
		
		this.outputDelegate_.outPutAll(this.htmlset_, this.pdfset_);
	}
}
