package playground.dgrether.koehlerstrehlersignal.solutionconverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

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
	private Map<Id, Double> streetFlow = new HashMap<Id, Double>();
	
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
			Id crossingId = new IdImpl(atts.getValue(ID));
			int offsetSeconds = Integer.parseInt(atts.getValue(OFFSET));
			Id programId = new IdImpl(atts.getValue(PROG));
			KS2010CrossingSolution crossing = new KS2010CrossingSolution(crossingId);
			crossing.addOffset4Program(programId, offsetSeconds);
			this.crossingSolutions.add(crossing);
		}
		// read the flow per street
		if (elementName.equals(EDGEFLOW)){
			Id streetId = new IdImpl(atts.getValue(ORIG_ID));
			Double streetFlow = Double.parseDouble(atts.getValue(TOTALFLOW));
			this.streetFlow.put(streetId, streetFlow);
		}
	}
	
	public List<KS2010CrossingSolution> getCrossingSolutions(){
		return this.crossingSolutions;
	}
	
	public Map<Id, Double> getStreetFlow(){
		return this.streetFlow;
	}
}
