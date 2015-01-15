/* *********************************************************************** *
 * project: org.matsim.*
 * LinkStatsXmlWriter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.energy.trafficstate;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlWriter;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * @author dgrether
 *
 */
public class TrafficStateXmlWriter {

	
	private TrafficState state;

	public TrafficStateXmlWriter(TrafficState state){
		this.state = state;
	}
	
	private XMLGregorianCalendar getCalendar4Seconds(int seconds){
		TimeZone zone = TimeZone.getTimeZone("GMT+00:00");
		GregorianCalendar cal = new GregorianCalendar(2012,03,16,00,00,00);
		cal.setTimeZone(zone);
		cal.add(Calendar.SECOND, seconds);
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
	}
	
	private String getTimeString(double time){
		XMLGregorianCalendar cal = this.getCalendar4Seconds((int) time);
		return cal.toXMLFormat();
	}
	
	public void writeFile(String filename){
		XMLOutputFactory xof =  XMLOutputFactory.newInstance();
		XMLStreamWriter xtw = null;
		try {
			xtw = xof.createXMLStreamWriter(IOUtils.getOutputStream(filename));
			xtw.writeStartDocument("utf-8","1.0");
//			
			xtw.writeStartElement("traffic_state");
			xtw.setDefaultNamespace(MatsimXmlWriter.MATSIM_NAMESPACE);
			xtw.writeNamespace("", MatsimXmlWriter.MATSIM_NAMESPACE);

			for (EdgeInfo ei : state.getEdgeInfoMap().values()){
				xtw.writeStartElement("edge_info");
				xtw.writeAttribute("id", ei.getId().toString());
//				xtw.writeStartElement("edge_id");
//				xtw.writeCharacters(ei.getId().toString());
//				xtw.writeEndElement();
				for (TimeBin tb : ei.getTimeBins()){
					xtw.writeStartElement("time_bin");
					xtw.writeStartElement("start_time");
					xtw.writeCharacters(this.getTimeString( tb.getStartTime()));
					xtw.writeEndElement();
					xtw.writeStartElement("end_time");
					xtw.writeCharacters(this.getTimeString( tb.getEndTime()));
					xtw.writeEndElement();
					xtw.writeStartElement("average_speed");
//					xtw.writeAttribute("unit", "meter_per_second");
					xtw.writeCharacters(Double.toString(tb.getAverageSpeed()));
					xtw.writeEndElement();
					xtw.writeEndElement();
				}
				xtw.writeEndElement();
			}
			
			xtw.writeEndElement();
			xtw.writeEndDocument();

			xtw.flush();
			xtw.close();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	
	}
	
	
}
