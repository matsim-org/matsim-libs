/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityDesires.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.mental;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class ActivityDesires {

	private final static Logger log = Logger.getLogger(ActivityDesires.class);
	private final Map<String, Double> startTimes;
	
	private String activityType;
	private String desc = null;
	private Map<String,Double> act_durs = null;

	public ActivityDesires() {
		this.desc = null;
		startTimes = new HashMap<String, Double>();
	}
	
	public void putActivityStartTime(String type, Double time) {
		startTimes.put(type, time);
	}
	
	public Double getActivityStartTime(String type) {
		return startTimes.get(type);
	}

	public void setActivityType(String type) {
		this.activityType = type;
	}
	
	public String getActivityType() {
		return activityType;
	}
	
	private Map<String, Double> getStartTimes() {
		return startTimes;
	}
	
	public static void write(Map<Person, ActivityDesires> personDesires, String file) {
		new XMLWriter().write(personDesires, file);
		
	}
	
	public static Map<SocialVertex, ActivityDesires> read(SocialGraph graph, String file) {
		Map<String, SocialVertex> vertexMap = new HashMap<String, SocialVertex>();
		for(SocialVertex v : graph.getVertices()) {
			vertexMap.put(v.getPerson().getPerson().getId().toString(), v);
		}
		XMLDesireReader reader = new XMLDesireReader(vertexMap, file);
		
		return reader.getVertexDesires();
	}

	@Deprecated
	public final boolean accumulateActivityDuration(final String act_type, final double duration) {
		if (this.act_durs == null) { return this.putActivityDuration(act_type,duration); }
		if (this.act_durs.get(act_type) == null)  { return this.putActivityDuration(act_type,duration); }
		double dur = this.act_durs.get(act_type);
		dur += duration;
		if (dur <= 0.0) { return false; }
		return this.putActivityDuration(act_type,dur);
	}

	@Deprecated // The "Desires" matsim toplevel container was never properly finished.
	public final boolean putActivityDuration(final String act_type, final double duration) {
		if (duration <= 0.0) { log.fatal("duration=" + duration + " not allowed!"); return false; }
		if (this.act_durs == null) { this.act_durs = new HashMap<String, Double>(); }
		this.act_durs.put(act_type,duration);
		return true;
	}

	@Deprecated // The "Desires" matsim toplevel container was never properly finished.
	public final boolean putActivityDuration(final String act_type, final String duration) {
		double dur = Time.parseTime(duration);
		return this.putActivityDuration(act_type,dur);
	}

	@Deprecated // The "Desires" matsim toplevel container was never properly finished.
	public final boolean removeActivityDuration(final String act_type) {
		if (this.act_durs.remove(act_type) == null) { return false; }
		if (this.act_durs.isEmpty()) { this.act_durs = null; }
		return true;
	}

	@Deprecated // The "Desires" matsim toplevel container was never properly finished.
	public void setDesc(String desc) {
		this.desc = desc;
	}

	@Deprecated // The "Desires" matsim toplevel container was never properly finished.
	public final String getDesc() {
		return this.desc;
	}

	@Deprecated // The "Desires" matsim toplevel container was never properly finished.
	public final double getActivityDuration(final String act_type) {
		if (this.act_durs == null) { return Time.UNDEFINED_TIME; }
		Double d = this.act_durs.get(act_type);
		if (d == null) { return Time.UNDEFINED_TIME; }
		return d;
	}

	@Deprecated // The "Desires" matsim toplevel container was never properly finished.
	public final Map<String,Double> getActivityDurations() {
		return this.act_durs;
	}

	@Override
	@Deprecated // The "Desires" matsim toplevel container was never properly finished.
	public final String toString() {
		return "[desc=" + this.desc + "]" + "[nof_act_durs=" + this.act_durs.size() + "]";
	}

	private static class XMLWriter extends MatsimXmlWriter {
		
		public void write(Map<Person, ActivityDesires> personDesires, String file) {
			openFile(file);
			setPrettyPrint(true);
			
			writeXmlHead();
			
			int indent = 0;
			
			setIndentationLevel(indent++);
			writeStartTag("desires", null);
			
			
			for(Entry<Person, ActivityDesires> entry : personDesires.entrySet()) {
				Tuple<String, String> attr = new Tuple<String, String>("id", entry.getKey().getId().toString());
				List<Tuple<String, String>> attrs = new ArrayList<Tuple<String,String>>(1);
				attrs.add(attr);
				
				setIndentationLevel(indent++);
				writeStartTag("person", attrs);
				
				ActivityDesires desires = entry.getValue();
				
				attr = new Tuple<String, String>("value", desires.getActivityType());
				attrs.set(0, attr);
				setIndentationLevel(indent);
				writeStartTag("type", attrs, true);
				
				for(Entry<String, Double> durations : desires.getActivityDurations().entrySet()) {
					attrs = new ArrayList<Tuple<String,String>>(2);
					attr = new Tuple<String, String>("type", durations.getKey());
					attrs.add(attr);
					attr = new Tuple<String, String>("value", String.valueOf(durations.getValue()));
					attrs.add(attr);
					writeStartTag("duration", attrs, true);
				}
				
				for(Entry<String, Double> startTimes : desires.getStartTimes().entrySet()) {
					attrs = new ArrayList<Tuple<String,String>>(2);
					attr = new Tuple<String, String>("type", startTimes.getKey());
					attrs.add(attr);
					attr = new Tuple<String, String>("value", String.valueOf(startTimes.getValue()));
					attrs.add(attr);
					writeStartTag("starttime", attrs, true);
				}
				
				setIndentationLevel(--indent);
				writeEndTag("person");
			}
			
			setIndentationLevel(--indent);
			writeEndTag("desires");
			
			close();
		}
	}
	
	private static class XMLDesireReader extends DefaultHandler {
		
		private Map<String, SocialVertex> vertexMap;
		
		private ActivityDesires desire;
		
		private Map<SocialVertex, ActivityDesires> vertexDesires;
		
		public XMLDesireReader(Map<String, SocialVertex> vertexMap, String file) {
			this.vertexMap = vertexMap;
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(false);
			SAXParser parser;
			try {
				parser = factory.newSAXParser();

				XMLReader reader = parser.getXMLReader();
				reader.setContentHandler(this);
				
				vertexDesires = new HashMap<SocialVertex, ActivityDesires>();
				reader.parse(file);
				
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public Map<SocialVertex, ActivityDesires> getVertexDesires() {
			return vertexDesires;
		}
		
		public void startElement(String uri, String localName, String name,
				Attributes atts) throws SAXException {
			if("person".equalsIgnoreCase(name)) {
				SocialVertex v = vertexMap.get(atts.getValue("id"));
				desire = new ActivityDesires();
				vertexDesires.put(v, desire);
			} else if("type".equalsIgnoreCase(name)) {
				desire.setActivityType(atts.getValue("value"));
			} else if("duration".equalsIgnoreCase(name)) {
				desire.putActivityDuration(atts.getValue("type"), Double.parseDouble(atts.getValue("value")));
			} else if("starttime".equalsIgnoreCase(name)) {
				desire.putActivityStartTime(atts.getValue("type"), Double.parseDouble(atts.getValue("value")));
			}
		}
	}
}
