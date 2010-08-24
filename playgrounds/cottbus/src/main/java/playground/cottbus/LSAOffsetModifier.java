package playground.cottbus;

/**
 * @author 	rschneid-btu
 * generates some Signal System Configs for a given Config with altered Offsets for each Crossing
 * IMPORTANT: given Config has to be in a certain way (exactly 2 Comments surrounding one crossing!)
 * 			  (this issue could be fixed in the future, maybe with cross-referring to the other config files...)
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.xerces.parsers.DOMParser;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public class LSAOffsetModifier {
	
	final static String OUTPUT_PATH = "./output/"; // output path
	final static String SIGNAL_SYSTEMS_CONFIG = "./input/denver/signalSystemsConfigT60.xml"; // input

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		DOMParser parser = new DOMParser();	
		
		try {
		    parser.setFeature ("http://xml.org/sax/features/validation", false);
		} catch (SAXNotRecognizedException e) {
		    System.err.println (e);
		} catch (SAXNotSupportedException e) {
		    System.err.println (e);
		}

		try {
		    parser.parse(SIGNAL_SYSTEMS_CONFIG);
		    Document document = parser.getDocument();
		    
		    changeOffsetOfSignalSystemConfig (document);
		} catch (SAXException e) {
		    System.err.println (e);
		} catch (IOException e) {
		    System.err.println (e);
		}

    }

	/**
	 * @param doc	the DOM of the signalSystemConfig
	 */
	private static void changeOffsetOfSignalSystemConfig (Document doc) {
		int cycleTime = 0;
		try {
			NodeList cycleTimeNode = doc.getElementsByTagName("cycleTime");
			cycleTime = Integer.valueOf(cycleTimeNode.item(0).getAttributes().item(0).getNodeValue()).intValue();
		} catch(Exception e) {
			System.err.println("ERR LSAOffsetModifier: CycleTime not found in "+SIGNAL_SYSTEMS_CONFIG+".\n"+e);
		}
		NodeList signalSystemPlanChildren = doc.getElementsByTagName("signalSystemPlan").item(0).getChildNodes();
		
		//array with new Offsets
		int[] newOffsets = {0,16,40,0,20,36,
							30,10,42,22,2,46,
							10,44,24,4,40,16,
							38,22,24,58,2,44,
							0,52,18,56,22,28,
							20,40,0,26,40,6
							};
		// array with old Offsets
		int[] oldOffsets = {0,12,24,30,36,54,
							12,24,24,24,30,18,
							24,12,6,0,54,48,
							36,30,24,54,0,24,
							54,48,42,42,36,18,
							48,48,54,54,0,36
							};
		// (crossings are read from up to down as in the XML-File, arrays will be read from the end to the beginning!)
		
		try {
			int offsetDiff = 0; // difference to old offset
			short countComment = 0;
			int crossingId = 1;
			for (int i=0; i< signalSystemPlanChildren.getLength(); i++) {
				Node child = signalSystemPlanChildren.item(i);
				// assuming: consecutive SignalGroups belong to one crossing!
				// how to isolate crossings: 2 comments with no signalGroup in between
				if (child.getNodeType() == Node.COMMENT_NODE) {
					countComment++;
					if (countComment == 2) {
						// two comments = new crossing => get next offset
						offsetDiff = newOffsets[newOffsets.length-(crossingId)]-oldOffsets[newOffsets.length-(crossingId)];
						crossingId++;
						countComment = 0;
					}
				}
				if (child.getNodeName().equals("signalGroupSettings")) {
					countComment = 0; // reset
					alterOffsetOfSignalGroup(child,offsetDiff,cycleTime);
				}
			} //for
			System.out.println("INFO Changing Offset was succesful.");
		} catch(Exception e) {
			System.err.println("ERR Changing Offset of signalSystemsConfig failed.\n"+e);
		}
		printToFile(doc,OUTPUT_PATH+"signalsystemsconfigT"+cycleTime+"newOffset"+".xml");	
	}
	
	
	/**
	 * alters one Signal Group with specified offset
	 * 
	 * @param signalGroup	the signalGroup to be modified
	 * @param offsetDiff	difference to old offset
	 * @param cycleTime		cycleTime (needed if overflow)
	 */
	private static void alterOffsetOfSignalGroup(Node signalGroup,int offsetDiff,final int cycleTime) {
		NodeList children = signalGroup.getChildNodes();
		
		for(int i=0; i<children.getLength(); i++) {
			Node elem = children.item(i);
			if ((elem.getNodeName().equals("roughcast")) || (elem.getNodeName().equals("dropping"))) {
				Node sec = elem.getAttributes().getNamedItem("sec");
				int oldTime = Integer.valueOf(sec.getNodeValue()).intValue();
				int newTime = oldTime + offsetDiff;
//				System.out.println(oldTime+" new "+newTime);
				if (newTime > cycleTime) {
					newTime -= cycleTime;
				}
				if (newTime < 0) {
					newTime += cycleTime;
				}
				sec.setNodeValue(Integer.toString(newTime));
			}
		} //for
	}

	
	/**
	 * generates the XML-file or shows the XML on the console
	 * 
	 * @param doc			contains the DOM tree of signalSystemsConfig
	 * @param OUTPUT_PATH	specifies the output path of XML-file
	 */
	private static void printToFile(Document doc,final String OUTPUT_PATH) {
		try {
			//print
			OutputFormat format = new OutputFormat(doc);
			format.setIndenting(true);
			
			// generate output to console
//			XMLSerializer serializer = new XMLSerializer(System.out, format);
			
			// generate file output
			XMLSerializer serializer = new XMLSerializer(new FileOutputStream(new File(OUTPUT_PATH)),format);
			
			serializer.serialize(doc);
			
		} catch(IOException e) {
			System.err.println("ERR Could not write signalSystemsConfig file.\n"+e);
		}
	}
	
}
