/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package org.matsim.population;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.PopulationSchemaV5Names;
import org.matsim.facilities.Activity;
import org.matsim.facilities.OpeningTime;
import org.matsim.utils.collections.Tuple;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.misc.Time;
import org.matsim.writer.MatsimXmlWriter;


/**
 * @author dgrether
 *
 */
public class PopulationWriterHandlerImplV5 extends MatsimXmlWriter implements PopulationWriterHandler {

	private int indentationLevel = 0;
	private String indentationString = "\t";
	private boolean doPrettyPrint = true;
	
	private List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();
	
	private Tuple<String, String> createTuple(String one, String two){
		return new Tuple<String, String>(one, two);
	}
	
	private Tuple<String, String> createTuple(String one, int two) {
		return this.createTuple(one, Integer.toString(two));
	}

	private Tuple<String, String> createTuple(String one, double two) {
		return this.createTuple(one, Double.toString(two));
	}
	
	private Tuple<String, String> createTuple(String one, boolean two) {
		return this.createTuple(one, Boolean.toString(two));
	}

	private Tuple<String, String> createTimeTuple(String one, double sec) {
		return this.createTuple(one, Time.writeTime(sec));
	}
	
	
	
	private void indent(BufferedWriter out) throws IOException{
		for (int i = 0; i < this.indentationLevel; i++) {
			out.write(this.indentationString);
		}
	}
	
	private void writeStartTag(String tagname, List<Tuple<String, String>> attributes, BufferedWriter out) throws IOException{
		if (doPrettyPrint) {
			this.indentationLevel++;
			indent(out);
		}
		out.write("<" + tagname);
		if (attributes != null) {
			for (Tuple<String, String> t : attributes){
				out.write(" " + t.getFirst() + "=\"" + t.getSecond() + "\"");
			}
		}
		out.write(">");
		if (doPrettyPrint) 
			out.write(NL);
	}
	
	private void writeEndTag(String tagname, BufferedWriter out) throws IOException {
		if (doPrettyPrint) {
			indent(out);
		}
		out.write("</" + tagname + ">");
		if (doPrettyPrint) {
			out.write(NL);
			this.indentationLevel--;
		}
	}
	
	
	public void writeHeaderAndStartElement(BufferedWriter out) throws IOException {
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + NL);
		out.write("<population xmlns=\"" + MatsimXmlWriter.DEFAULT_DTD_LOCATION + "\"");
		if (doPrettyPrint)
			out.write(NL);
	  out.write(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");  
	  if (doPrettyPrint)
	  	out.write(NL);
	  out.write(" xsi:schemaLocation=\"" + MatsimXmlWriter.DEFAULT_DTD_LOCATION  +
	  " " + MatsimXmlWriter.DEFAULT_DTD_LOCATION + "population_v5.00.xsd\">" + NL );
	}
	
	
	public void endAct(BufferedWriter out) throws IOException {
		this.writeEndTag(PopulationSchemaV5Names.ACT, out);
	}


	public void endActivity(BufferedWriter out) throws IOException {
		this.writeEndTag(PopulationSchemaV5Names.ACTIVITY, out);
	}

	public void endCapacity(BufferedWriter out) throws IOException {
		
	}

	public void endDesires(BufferedWriter out) throws IOException {
		
	}

	public void endKnowledge(BufferedWriter out) throws IOException {
		this.writeEndTag(PopulationSchemaV5Names.KNOWLEDGE, out);
	}

	public void endLeg(BufferedWriter out) throws IOException {
		this.writeEndTag(PopulationSchemaV5Names.LEG, out);
	}

	public void endOpentime(BufferedWriter out) throws IOException {
	}

	public void endPerson(BufferedWriter out) throws IOException {
		this.writeEndTag(PopulationSchemaV5Names.PERSON, out);
	}

	public void endPlan(BufferedWriter out) throws IOException {
		this.writeEndTag(PopulationSchemaV5Names.PLAN, out);
	}

	public void endPlans(BufferedWriter out) throws IOException {
		out.write("</" + PopulationSchemaV5Names.POPULATION + ">");
	}

	public void endRoute(BufferedWriter out) throws IOException {
		this.writeEndTag(PopulationSchemaV5Names.ROUTE, out);
	}

	public void endTravelCard(BufferedWriter out) throws IOException {
		this.writeEndTag(PopulationSchemaV5Names.TRAVELCARD, out);
	}

	private void printLocation(Id linkId, Id facilityId, Coord coord, BufferedWriter out) throws IOException {
		
	}
	
	
	public void startAct(Act act, BufferedWriter out) throws IOException {
		atts.clear();
		atts.add(this.createTuple(PopulationSchemaV5Names.TYPE, act.getType()));		
		if (act.getStartTime() != Time.UNDEFINED_TIME)
			atts.add(this.createTimeTuple(PopulationSchemaV5Names.STARTTIME, act.getStartTime()));
		if (act.getDur() != Time.UNDEFINED_TIME)
			atts.add(this.createTimeTuple(PopulationSchemaV5Names.DURATION, act.getDur()));
		if (act.getEndTime() != Time.UNDEFINED_TIME)
			atts.add(this.createTimeTuple(PopulationSchemaV5Names.ENDTIME, act.getDur()));
		
		this.writeStartTag(PopulationSchemaV5Names.ACT, atts, out);
		this.printLocation(act.getLinkId(), act.getFacilityId(), act.getCoord(), out);
	}


	public void startCapacity(Activity activity, BufferedWriter out)
			throws IOException {
		
	}

	public void startDesires(Desires desires, BufferedWriter out)
			throws IOException {
		
	}

	public void startKnowledge(Knowledge knowledge, BufferedWriter out)
			throws IOException {
		atts.clear();
		//TODO
		this.writeStartTag(PopulationSchemaV5Names.KNOWLEDGE, atts, out);		
	}

	public void startLeg(Leg leg, BufferedWriter out) throws IOException {
		atts.clear();
		//TODO
		this.writeStartTag(PopulationSchemaV5Names.LEG, atts, out);
	}

	public void startOpentime(OpeningTime opentime, BufferedWriter out)
			throws IOException {
		// TODO Auto-generated method stub
		
	}



	public void startPerson(Person p, BufferedWriter out) throws IOException {
		atts.clear();
		atts.add(this.createTuple(PopulationSchemaV5Names.ID, p.getId().toString()));
		if (p.getAge() != Integer.MIN_VALUE)
			atts.add(this.createTuple(PopulationSchemaV5Names.AGE, p.getAge()));
		if (p.getLicense() != null)
			atts.add(this.createTuple(PopulationSchemaV5Names.LICENSE, p.hasLicense()));
		if (p.getCarAvail() != null)
			atts.add(this.createTuple(PopulationSchemaV5Names.CARAVAILABLE, p.getCarAvail()));
		if (p.getEmployed() != null)
			atts.add(this.createTuple(PopulationSchemaV5Names.ISEMPLOYED, p.isEmployed()));
		this.writeStartTag(PopulationSchemaV5Names.PERSON, atts, out);
	}



	public void startPlan(Plan plan, BufferedWriter out) throws IOException {
		atts.clear();
		if (!plan.hasUndefinedScore())
			atts.add(this.createTuple(PopulationSchemaV5Names.SCORE, plan.getScore()));
		atts.add(this.createTuple(PopulationSchemaV5Names.SELECTED, plan.isSelected()));
		this.writeStartTag(PopulationSchemaV5Names.PLAN, atts, out);
	}



	public void startRoute(Route route, BufferedWriter out) throws IOException {
		atts.clear();
		//TODO
		this.writeStartTag(PopulationSchemaV5Names.ROUTE, atts, out);
	}


	public void startTravelCard(String travelcard, BufferedWriter out)
			throws IOException {
		atts.clear();
		//TODO
		this.writeStartTag(PopulationSchemaV5Names.TRAVELCARD, atts, out);
	}


	
	//TRASH BIN!!!
	//Garbage methods only existing due to badly designed interface and writer mechanism
	public void startActivitySpace(ActivitySpace as, BufferedWriter out)
	throws IOException {}
	public void startActivity(String act_type, BufferedWriter out)
	throws IOException {}
	public void startActDur(String act_type, double dur, BufferedWriter out)
	throws IOException {}
	public void startPlans(Population plans, BufferedWriter out)
	throws IOException {}
	public void writeSeparator(BufferedWriter out) throws IOException {
	}
	public void endActivitySpace(BufferedWriter out) throws IOException {
	}
	public void endActDur(BufferedWriter out) throws IOException {
	}
	public void startSecondaryLocation(Activity activity, BufferedWriter out)
	throws IOException {
}
	public void startPrimaryLocation(Activity activity, BufferedWriter out)
	throws IOException {
}
	public void startParam(String name, String value, BufferedWriter out)
	throws IOException {}
	public void endSecondaryLocation(BufferedWriter out) throws IOException {
	}
	public void endPrimaryLocation(BufferedWriter out) throws IOException {
	}
	public void endParam(BufferedWriter out) throws IOException {
	}


}
