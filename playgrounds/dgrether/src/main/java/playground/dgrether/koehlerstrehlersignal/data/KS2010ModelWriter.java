/* *********************************************************************** *
 * project: org.matsim.*
 * DgKoehlerStrehler2010ModelWriter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.data;

import java.io.Writer;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 * @author dgrether
 * @author tthunig
 *
 */
public class KS2010ModelWriter {

	private static final Logger log = Logger.getLogger(KS2010ModelWriter.class);

	// common tags
	private static final String CDATA = "CDATA";
	private static final String ID = "id";
	private static final String NODE = "node";
	private static final String FROM = "from";
	private static final String TO = "to";
	private static final String NETWORK = "network";
	private static final String EXPANDED = "expanded";
	private static final String NAME = "name";
	private static final String DESCRIPTION = "description";
	// tags for the crossings element
	private static final String TYPE = "type";
	private static final String CROSSINGS = "crossings";
	private static final String CROSSING = "crossing";
	private static final String NODES = "nodes";
	private static final String LIGHTS = "lights";
	private static final String LIGHT = "light";
	private static final String PROGRAMS = "programs";
	private static final String PROGRAM = "program";
	private static final String CYCLE = "cycle";
	private static final String GREEN = "green";
	private static final String OFFSET = "offset";
	private static final String LENGTH = "length";
	private static final String XCOORD = "x";
	private static final String YCOORD = "y";

	// tags for the streets element
	private static final String STREETS = "streets";
	private static final String STREET = "street";
	private static final String COST = "cost";
	private static final String CAPACITY = "capacity";
	// tags for the commodities element
	private static final String COMMODITIES = "commodities";
	private static final String COMMODITY = "commodity";
	private static final String SOURCES = "sources";
	private static final String FLOW = "flow";
	private static final String DRAINS = "drains";
	private static final String PERMITTED_ROADS = "permitted_roads";
	private static final String PERMIT = "permit";
		

	
	public void write(DgKSNetwork network, String outFile) {
		this.write(network, null, "", "", outFile);
	}

	
	public void write(DgKSNetwork network, DgCommodities coms, String name, String description, String outFile) {
		Writer writer;
		log.info("start writing KS Model to " + outFile);
		try {
			writer = IOUtils.getBufferedWriter(outFile);
			TransformerHandler hd = this.createContentHandler(writer);
			this.writeDocumentStart(hd, name, description);
			this.writeCrossings(network, hd);
			this.writeStreets(network, hd);
			if (coms != null){
				this.writeCommodities(coms, hd);
			}
			hd.endElement("", "", NETWORK);
			hd.endDocument();
			writer.flush();
			writer.close();
			log.info("finished model writing.");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void writeDocumentStart(TransformerHandler hd, String name, String desc) throws SAXException {
		hd.startDocument();
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("", "", EXPANDED, CDATA, "false");
		hd.startElement("", "", NETWORK, atts);
		atts.clear();
		hd.startElement("", "", NAME, atts);
		if (name == null){
			name = "";
		}
		if (desc == null) {
			desc = "";
		}
		char[] nameArray = name.toCharArray();
		hd.characters(nameArray, 0, nameArray.length);
		hd.endElement("", "", NAME);
		atts.clear();
		hd.startElement("", "", DESCRIPTION, atts);
		char[] descArray = desc.toCharArray();
		hd.characters(descArray, 0, descArray.length);
		hd.endElement("", "", DESCRIPTION);
	}

	private void writeCrossings(DgKSNetwork net, TransformerHandler hd) throws SAXException {
		AttributesImpl atts = new AttributesImpl();
		hd.startElement("", "", CROSSINGS, atts);
		for (DgCrossing crossing : net.getCrossings().values()) {
			atts.clear();
			atts.addAttribute("", "", ID, CDATA, crossing.getId().toString());
			atts.addAttribute("", "", TYPE, CDATA, crossing.getType());
			hd.startElement("", "", CROSSING, atts);
			// nodes
			atts.clear();
			hd.startElement("", "", NODES, atts);
			for (DgCrossingNode node : crossing.getNodes().values()) {
				atts.clear();
				atts.addAttribute("", "", ID, CDATA, node.getId().toString());
				atts.addAttribute("", "", XCOORD, CDATA, Double.toString(node.getCoordinate().getX()));
				atts.addAttribute("", "", YCOORD, CDATA, Double.toString(node.getCoordinate().getY()));
				hd.startElement("", "", NODE, atts);
				hd.endElement("", "", NODE);
			}
			hd.endElement("", "", NODES);
			// lights
			atts.clear();
			hd.startElement("", "", LIGHTS, atts);
			for (DgStreet light : crossing.getLights().values()) {
				atts.clear();
//				log.debug("writing light:  " + light.getId());
				atts.addAttribute("", "", ID, CDATA, light.getId().toString());
				atts.addAttribute("", "", FROM, CDATA, light.getFromNode().getId().toString());
//				log.error("  toCrossing: "  + light.getToNode());
//				log.error("  toCrossingId: "  + light.getToNode().getId());
				
				atts.addAttribute("", "", TO, CDATA, light.getToNode().getId().toString());
				hd.startElement("", "", LIGHT, atts);
				hd.endElement("", "", LIGHT);
			}
			hd.endElement("", "", LIGHTS);
			// programs for crossings with type fixed
			if (crossing.getType().equals(TtCrossingType.FIXED)){
				atts.clear();
				hd.startElement("", "", PROGRAMS, atts);
				for (DgProgram program : crossing.getPrograms().values()) {
					atts.clear();
					atts.addAttribute("", "", ID, CDATA, program.getId().toString());
					atts.addAttribute("", "", CYCLE, CDATA, Integer.toString(program.getCycle()));
					hd.startElement("", "", PROGRAM, atts);
					for (DgGreen g : program.getGreensByLightId().values()) {
						atts.clear();
						atts.addAttribute("", "", LIGHT, CDATA, g.getLightId().toString());
						atts.addAttribute("", "", OFFSET, CDATA, Integer.toString(g.getOffset()));
						atts.addAttribute("", "", LENGTH, CDATA, Integer.toString(g.getLength()));
						hd.startElement("", "", GREEN, atts);
						hd.endElement("", "", GREEN);
					}
					hd.endElement("", "", PROGRAM);
				}
				hd.endElement("", "", PROGRAMS);
			}
			hd.endElement("", "", CROSSING);
		}
		hd.endElement("", "", CROSSINGS);
	}

	private void writeCommodities(DgCommodities coms, TransformerHandler hd) throws SAXException {
		AttributesImpl atts = new AttributesImpl();
		hd.startElement("", "", COMMODITIES, atts);
		for (DgCommodity co : coms.getCommodities().values()) {
			atts.clear();
			atts.addAttribute("", "", ID, CDATA, co.getId().toString());
			hd.startElement("", "", COMMODITY, atts);
			atts.clear();
			hd.startElement("", "", SOURCES, atts);

			atts.clear();
			atts.addAttribute("", "", ID, CDATA, co.getSourceNodeId().toString());
			atts.addAttribute("", "", FLOW, CDATA, Double.toString(co.getFlow()));
			hd.startElement("", "", NODE, atts);
			hd.endElement("", "", NODE);

			hd.endElement("", "", SOURCES);
			atts.clear();
			hd.startElement("", "", DRAINS, atts);

			atts.clear();
			atts.addAttribute("", "", ID, CDATA, co.getDrainNodeId().toString());
			hd.startElement("", "", NODE, atts);
			hd.endElement("", "", NODE);

			hd.endElement("", "", DRAINS);
			
			if (co.hasRoute()){
				atts.clear();
				hd.startElement("", "", PERMITTED_ROADS, atts);

				for (Id<DgStreet> street : co.getRoute()){
					atts.clear();
					atts.addAttribute("", "", ID, CDATA, street.toString());
					hd.startElement("", "", PERMIT, atts);
					hd.endElement("", "", PERMIT);
				}
				
				hd.endElement("", "", PERMITTED_ROADS);				
			}
			
			hd.endElement("", "", COMMODITY);
		}
		hd.endElement("", "", COMMODITIES);
	}

	private void writeStreets(DgKSNetwork network, TransformerHandler hd)
			throws SAXException {
		AttributesImpl atts = new AttributesImpl();
		hd.startElement("", "", STREETS, atts);
		for (DgStreet street : network.getStreets().values()) {
			atts.clear();
			atts.addAttribute("", "", ID, CDATA, street.getId().toString());
//			DgStreet street = network.getStreets().get(link.getId());
			atts.addAttribute("", "", COST, CDATA, Long.toString(street.getCost()));
			atts.addAttribute("", "", CAPACITY, CDATA,
					Double.toString(street.getCapacity()));
			atts.addAttribute("", "", FROM, CDATA, street.getFromNode().getId().toString());
			atts.addAttribute("", "", TO, CDATA, street.getToNode().getId().toString());
			hd.startElement("", "", STREET, atts);
			hd.endElement("", "", STREET);
		}
		hd.endElement("", "", STREETS);
	}

	private TransformerHandler createContentHandler(Writer writer)
			throws TransformerConfigurationException {
		StreamResult streamResult = new StreamResult(writer);
		SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
		// SAX2.0 ContentHandler.
		TransformerHandler hd = tf.newTransformerHandler();
		Transformer serializer = hd.getTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
		// serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,"users.dtd");
		serializer.setOutputProperty(OutputKeys.INDENT, "yes");
		serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		hd.setResult(streamResult);
		return hd;
	}

}
