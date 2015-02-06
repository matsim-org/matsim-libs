package playground.dgrether.koehlerstrehlersignal.solutionconverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgProgram;

/**
 * 
 * class to read in the optimized btu coordinations
 * for the format description see the documentation at shared-svn/projects/cottbus/DOC/xml_doc.pdf
 * 
 * @author tthunig
 *
 */
public class KS2014SolutionXMLParser extends MatsimXmlParser {

	private static final Logger log = Logger.getLogger(KS2014SolutionXMLParser.class);
	
	private final static String FIXEDCROSSING = "fixedCrossing";
	private final static String ID = "id";
	private final static String OFFSET = "offset";
	private final static String PROG = "prog";
	private final static String EDGEFLOW = "edgeFlow";
	private final static String ORIG_ID = "orig_id";
	private final static String TOTALFLOW = "totalFlow";
	
	private List<KS2010CrossingSolution> crossingSolutions = new ArrayList<KS2010CrossingSolution>();
	private Map<Id<Link>, Double> streetFlow = new HashMap<>();
	
	public void readFile(final String filename) {
		this.setValidating(false);
		parse(filename);
		log.info("Read " + crossingSolutions.size() + " solutions");
	}
	
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		
	}

	@Override
	public void startTag(String elementName, Attributes atts, Stack<String> context) {
		// read the crossings program offset
		if (elementName.equals(FIXEDCROSSING)){
			Id<DgCrossing> crossingId = Id.create(atts.getValue(ID), DgCrossing.class);
			int offsetSeconds = Integer.parseInt(atts.getValue(OFFSET));
			Id<DgProgram> programId = Id.create(atts.getValue(PROG), DgProgram.class);
			KS2010CrossingSolution crossing = new KS2010CrossingSolution(crossingId);
			crossing.addOffset4Program(programId, offsetSeconds);
			this.crossingSolutions.add(crossing);
		}
		// read the flow per street
		if (elementName.equals(EDGEFLOW)){
			Id<Link> streetId = Id.create(atts.getValue(ORIG_ID), Link.class);
			Double streetFlow = Double.parseDouble(atts.getValue(TOTALFLOW));
			this.streetFlow.put(streetId, streetFlow);
		}
	}
	
	public List<KS2010CrossingSolution> getCrossingSolutions(){
		return this.crossingSolutions;
	}
	
	public Map<Id<Link>, Double> getStreetFlow(){
		return this.streetFlow;
	}
}
