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
package playground.dgrether.koehlerstrehlersignal;

import java.io.Writer;
import java.util.Map.Entry;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.IOUtils;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import playground.dgrether.koehlerstrehlersignal.data.DgCommodities;
import playground.dgrether.koehlerstrehlersignal.data.DgCommodity;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgCrossingNode;
import playground.dgrether.koehlerstrehlersignal.data.DgGreen;
import playground.dgrether.koehlerstrehlersignal.data.DgNetwork;
import playground.dgrether.koehlerstrehlersignal.data.DgProgram;
import playground.dgrether.koehlerstrehlersignal.data.DgStreet;

public class DgKoehlerStrehler2010ModelWriter {

	private static final Logger log = Logger.getLogger(DgKoehlerStrehler2010ModelWriter.class);

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

	
	public void write(ScenarioImpl sc, DgNetwork network, String outFile) {
		this.write(sc, network, null, outFile);
	}

	
	public void write(ScenarioImpl sc, DgNetwork network, DgCommodities coms, String outFile) {
		Writer writer;
		try {
			writer = IOUtils.getBufferedWriter(outFile);
			TransformerHandler hd = this.createContentHandler(writer);
			this.writeDocumentStart(hd, sc);
			this.writeCrossings(network, hd);
			this.writeStreets(network, sc.getNetwork(), hd);
			if (coms != null){
				this.writeCommodities(coms, hd);
			}
			hd.endElement("", "", NETWORK);
			hd.endDocument();
			writer.flush();
			writer.close();
			log.info("done");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void writeDocumentStart(TransformerHandler hd, ScenarioImpl sc) throws SAXException {
		hd.startDocument();
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("", "", EXPANDED, CDATA, "false");
		hd.startElement("", "", NETWORK, atts);
		atts.clear();
		hd.startElement("", "", NAME, atts);
		String name = sc.getNetwork().getName();
		if (name == null) {
			name = "noname";
		}
		char[] nameArray = name.toCharArray();
		hd.characters(nameArray, 0, nameArray.length);
		hd.endElement("", "", NAME);
		atts.clear();
		hd.startElement("", "", DESCRIPTION, atts);
		hd.characters(nameArray, 0, nameArray.length);
		hd.endElement("", "", DESCRIPTION);
	}

	private void writeCrossings(DgNetwork net, TransformerHandler hd) throws SAXException {
		AttributesImpl atts = new AttributesImpl();
		hd.startElement("", "", CROSSINGS, atts);
		for (DgCrossing crossing : net.getCrossings().values()) {
			atts.clear();
			atts.addAttribute("", "", ID, CDATA, crossing.getId().toString());
			hd.startElement("", "", CROSSING, atts);
			// nodes
			atts.clear();
			hd.startElement("", "", NODES, atts);
			for (DgCrossingNode node : crossing.getNodes().values()) {
				atts.clear();
				atts.addAttribute("", "", ID, CDATA, node.getId().toString());
				hd.startElement("", "", NODE, atts);
				hd.endElement("", "", NODE);
			}
			hd.endElement("", "", NODES);
			// lights
			atts.clear();
			hd.startElement("", "", LIGHTS, atts);
			for (DgStreet light : crossing.getLights().values()) {
				atts.clear();
				log.debug("writing light:  " + light.getId());
				atts.addAttribute("", "", ID, CDATA, light.getId().toString());
				atts.addAttribute("", "", FROM, CDATA, light.getFromNode().getId().toString());
				atts.addAttribute("", "", TO, CDATA, light.getToNode().getId().toString());
				hd.startElement("", "", LIGHT, atts);
				hd.endElement("", "", LIGHT);
			}
			hd.endElement("", "", LIGHTS);
			// programs
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
			for (Entry<Id, Double> e : co.getSourceNodesFlowMap().entrySet()) {
				atts.clear();
				atts.addAttribute("", "", ID, CDATA, e.getKey().toString());
				atts.addAttribute("", "", FLOW, CDATA, Double.toString(e.getValue()));
				hd.startElement("", "", NODE, atts);
				hd.endElement("", "", NODE);
			}
			hd.endElement("", "", SOURCES);
			atts.clear();
			hd.startElement("", "", DRAINS, atts);
			for (Id drainNodeId : co.getDrainNodes()) {
				atts.clear();
				atts.addAttribute("", "", ID, CDATA, drainNodeId.toString());
				hd.startElement("", "", NODE, atts);
				hd.endElement("", "", NODE);
			}
			hd.endElement("", "", DRAINS);
			hd.endElement("", "", COMMODITY);
		}
		hd.endElement("", "", COMMODITIES);
	}

	private void writeStreets(DgNetwork network, NetworkImpl matsimNet, TransformerHandler hd)
			throws SAXException {
		AttributesImpl atts = new AttributesImpl();
		hd.startElement("", "", STREETS, atts);
		for (Link link : matsimNet.getLinks().values()) {
			atts.clear();
			atts.addAttribute("", "", ID, CDATA, link.getId().toString());
			long fs = Math.round((link.getLength() / link.getFreespeed()));
			atts.addAttribute("", "", COST, CDATA, Long.toString(fs));
			atts.addAttribute("", "", CAPACITY, CDATA,
					Double.toString(link.getCapacity() / matsimNet.getCapacityPeriod() * 3600.0));
			DgStreet street = network.getStreets().get(link.getId());
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
