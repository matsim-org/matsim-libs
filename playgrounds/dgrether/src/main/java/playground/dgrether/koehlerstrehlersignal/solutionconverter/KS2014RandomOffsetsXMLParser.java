/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.solutionconverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.dgrether.koehlerstrehlersignal.data.DgCrossing;
import playground.dgrether.koehlerstrehlersignal.data.DgProgram;

/**
 * class to read in randomized offsets as btu coordinations
 * there is no format description available.
 *  
 * @author tthunig
 *
 */
public class KS2014RandomOffsetsXMLParser extends MatsimXmlParser {

	private static final Logger log = Logger.getLogger(KS2014RandomOffsetsXMLParser.class);
	
	private final static String MINCOORD = "minCoord";
	private final static String MAXCOORD = "maxCoord";
	private final static String AVGCOORD = "avgCoord";
	private final static String MEDCOORD = "medCoord";
	private final static String CROSSING = "crossing";
	private final static String ID = "id";
	private final static String OFFSET = "offset";
	private final static String PROG = "prog";
	
	// 0 = minCoord, 1 = maxCoord, 2 = avgCoord, 3 = medCoord
	private Map<Integer, List<KS2010CrossingSolution>> randomOffsets = new HashMap<Integer, List<KS2010CrossingSolution>>();
	private Integer currentList = 0;
	// remark: min coord = minimal total travel time = best coordination

	public void readFile(final String filename) {
		this.setValidating(false);
		parse(filename);
		log.info("Read " + randomOffsets.get(0).size() + " + " + randomOffsets.get(1).size() + " + " + 
				randomOffsets.get(2).size() + " + " + randomOffsets.get(3).size() + " solutions");
	}
	
	@Override
	public void startTag(String elementName, Attributes atts, Stack<String> context) {
		if (elementName.equals(MINCOORD)){
			this.currentList = 0;
			this.randomOffsets.put(currentList, new ArrayList<KS2010CrossingSolution>());
		}
		else if (elementName.equals(MAXCOORD)){
			this.currentList = 1;
			this.randomOffsets.put(currentList, new ArrayList<KS2010CrossingSolution>());
		}
		else if (elementName.equals(AVGCOORD)){
			this.currentList = 2;
			this.randomOffsets.put(currentList, new ArrayList<KS2010CrossingSolution>());
		}
		else if (elementName.equals(MEDCOORD)){
			this.currentList = 3;
			this.randomOffsets.put(currentList, new ArrayList<KS2010CrossingSolution>());
		}
		else if (elementName.equals(CROSSING)){
			Id<DgCrossing> crossingId = Id.create(atts.getValue(ID), DgCrossing.class);
			int offsetSeconds = Integer.parseInt(atts.getValue(OFFSET));
			Id<DgProgram> programId = Id.create(atts.getValue(PROG), DgProgram.class); 
			KS2010CrossingSolution crossing = new KS2010CrossingSolution(crossingId);
			crossing.addOffset4Program(programId, offsetSeconds);
			this.randomOffsets.get(currentList).add(crossing);
		}
	}

	@Override
	public void endTag(String elementName, String content, Stack<String> context) {
		
	}

	public Map<Integer, List<KS2010CrossingSolution>> getRandomOffsets() {
		return randomOffsets;
	}

}
