/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.johannes.gsv.analysis;

import gnu.trove.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.xml.sax.Attributes;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author johannes
 *
 */
public class RailCounts {
	
	private static final Logger logger = Logger.getLogger(RailCounts.class);

	private Map<Id, RailCountsContainer> countsMap = new HashMap<Id, RailCounts.RailCountsContainer>();
	
	private TransitLineAttributes lineAttribs;
	
	public RailCounts(TransitLineAttributes attribs) {
		this.lineAttribs = attribs;
	}
	
	public void addCounts(Id<Link> linkId, Id lineId, double counts) {
		RailCountsContainer container = countsMap.get(linkId);
		
		if(container == null) {
			container = new RailCountsContainer();
			countsMap.put(linkId, container);
		}
		
		container.addCount(lineId, counts);
	}
	
	public double counts(Id<Link> linkId) {
		RailCountsContainer container = countsMap.get(linkId);
		if(container == null) {
			return 0;
		} else {
			double val = 0;
			for(Id line : container.lines()) {
				val += container.counts(line);
			}
			return val;
		}
	}
	
	public double counts(Id<Link> linkId, Id lineId) {
		RailCountsContainer container = countsMap.get(linkId);
		if(container == null) {
			return 0;
		} else {
			return container.counts(lineId);
		}
	}
	
	public double counts(Id<Link> linkId, String tSys) {
		RailCountsContainer container = countsMap.get(linkId);
		if(container == null) {
			return 0;
		} else {
			double val = 0;
			for(Id line : container.lines()) {
				String sys = lineAttribs.getTransportSystem(line.toString());
				if(sys == null) {
					logger.warn(String.format("Transport system unknown for line %s.", line.toString()));
				} else {
					if(sys.equalsIgnoreCase(tSys)) {
						val += container.counts(line);
					}
				}
			}
			
			return val;
		}
	}
	
	public Id[] lines(Id<Link> linkId) {
		RailCountsContainer container = countsMap.get(linkId);
		if(container == null) {
			return null;
		} else {
			return container.lines();
		}
	}
	
	public void writeToFile(String file) throws IOException {
		new RailCountsXMLWriter().write(this, file);
	}
	
	public static RailCounts createFromFile(String file, TransitLineAttributes attribs, Network network, TransitSchedule schedule) {
		RailCounts counts = new RailCounts(attribs);
		RailCountsXMLParser parser = new RailCountsXMLParser(counts, network, schedule);
		parser.setValidating(false);
		parser.parse(file);
		
		return counts;
	}
	
	private static class RailCountsContainer {
		
		private TObjectDoubleHashMap<Id> lineCounts = new TObjectDoubleHashMap<Id>();
		
		public void addCount(Id lineId, double count) {
			lineCounts.adjustOrPutValue(lineId, count, count);
		}
		
		public Id[] lines() {
			Id[] keys = new Id[1];
			return lineCounts.keys(keys);
		}
		
		public double counts(Id line) {
			return lineCounts.get(line);
		}
	}
	
	private static class RailCountsXMLWriter extends MatsimXmlWriter {
		
		public void write(RailCounts railCounts, String file) {
			openFile(file);
			
			writeXmlHead();
			
			writeStartTag("links", null);
			for(Entry<Id, RailCountsContainer> entry : railCounts.countsMap.entrySet()) {
				Id linkId = entry.getKey();
				
				List<Tuple<String, String>> attribs = new ArrayList<Tuple<String, String>>(1);
				attribs.add(new Tuple<String, String>("id", linkId.toString()));
				writeStartTag("link", attribs);
				
				RailCountsContainer container = entry.getValue();
				for(Id line : container.lines()) {
					List<Tuple<String, String>> attribs2 = new ArrayList<Tuple<String, String>>(2);
					attribs2.add(new Tuple<String, String>("id", line.toString()));
					attribs2.add(new Tuple<String, String>("count", String.valueOf(container.counts(line))));
					writeStartTag("line", attribs2, true);
				}
				
				writeEndTag("link");
			}
			writeEndTag("links");
			
			close();
		}
	}
	
	private static class RailCountsXMLParser extends MatsimXmlParser {

		private RailCounts railCounts;
		
		private Link currentLink;
		
		private Network network;
		
		private TransitSchedule schedule;
		
		public RailCountsXMLParser(RailCounts railCounts, Network network, TransitSchedule schedule) {
			this.railCounts = railCounts;
			this.network = network;
			this.schedule = schedule;
		}
		
		/* (non-Javadoc)
		 * @see org.matsim.core.utils.io.MatsimXmlParser#startTag(java.lang.String, org.xml.sax.Attributes, java.util.Stack)
		 */
		@Override
		public void startTag(String name, Attributes atts, Stack<String> context) {
			if(name.equalsIgnoreCase("link")) {
				currentLink = network.getLinks().get(Id.create(atts.getValue("id"), Link.class));
			} else if(name.equalsIgnoreCase("line")) {
				TransitLine line = schedule.getTransitLines().get(Id.create(atts.getValue("id"), TransitLine.class));
				if(line != null && currentLink != null) {
					railCounts.addCounts(currentLink.getId(), line.getId(), Double.parseDouble(atts.getValue("count")));
				}
			}
			
		}

		/* (non-Javadoc)
		 * @see org.matsim.core.utils.io.MatsimXmlParser#endTag(java.lang.String, java.lang.String, java.util.Stack)
		 */
		@Override
		public void endTag(String name, String content, Stack<String> context) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
