package playground.cottbus;

/**
 * @author 	rschneid-btu
 * generates some Signal System Configs for a given Config with randomized Offsets for each Crossing
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

public class LSAOffsetRandomizer {
	
	final static String OUTPUT_PATH = "./input/denver/random/"; // output path
	final static String SIGNAL_SYSTEMS_CONFIG = "./input/denver/signalSystemsConfigT60.xml"; // input
	final static short amountOfRandomizedSignalPlans = 20; // how many plans to generate? (with enumerated output)

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
		    
		    buildRandomizedSignalSystemConfigs (document);
		} catch (SAXException e) {
		    System.err.println (e);
		} catch (IOException e) {
		    System.err.println (e);
		}

    }

	/**
	 * @param doc	the DOM of the signalSystemConfig
	 */
	private static void buildRandomizedSignalSystemConfigs (Document doc) {
		int cycleTime = 0;
		try {
			NodeList cycleTimeNode = doc.getElementsByTagName("cycleTime");
			cycleTime = Integer.valueOf(cycleTimeNode.item(0).getAttributes().item(0).getNodeValue()).intValue();
		} catch(Exception e) {
			System.err.println("ERR LSAOffsetRandomizer: CycleTime not found in "+SIGNAL_SYSTEMS_CONFIG+".\n"+e);
		}
		NodeList signalSystemPlanChildren = doc.getElementsByTagName("signalSystemPlan").item(0).getChildNodes();
		
		for(int j=1; j<=amountOfRandomizedSignalPlans; j++) {
			try {
				int offsetDiff = (int)Math.floor(Math.random()*cycleTime); // difference to old offset
				short countComment = 0;
				for (int i=0; i< signalSystemPlanChildren.getLength(); i++) {
					Node child = signalSystemPlanChildren.item(i);
					// assuming: consecutive SignalGroups belong to one crossing!
					// how to isolate crossings: 2 comments with no signalGroup in between
					if (child.getNodeType() == Node.COMMENT_NODE) {
						countComment++;
						if (countComment == 2) {
							// two comments = new crossing => new random offset
							offsetDiff = (int)Math.floor(Math.random()*cycleTime);
							countComment = 0;
						}
					}
					if (child.getNodeName().equals("signalGroupSettings")) {
						countComment = 0; // reset
						randomizeSignalGroup(child,offsetDiff,cycleTime);
					}
				} //for
				System.out.println("INFO Randomizing #"+j+" succesful.");
			} catch(Exception e) {
				System.err.println("ERR Randomizing signalSystemsConfig failed.\n"+e);
			}
			printToFile(doc,OUTPUT_PATH+"signalsystemsconfigT"+cycleTime+"random"+j+".xml");	
		} //for
	}
	
	
	/**
	 * randomizes one Signal Group with specified offset
	 * 
	 * @param signalGroup	the signalGroup to be randomized
	 * @param offset		difference to old offset
	 * @param cycleTime		cycleTime (needed if overflow)
	 */
	private static void randomizeSignalGroup(Node signalGroup,int offset,int cycleTime) {
		NodeList children = signalGroup.getChildNodes();
		
		for(int i=0; i<children.getLength(); i++) {
			Node elem = children.item(i);
			if ((elem.getNodeName().equals("roughcast")) || (elem.getNodeName().equals("dropping"))) {
				Node sec = elem.getAttributes().getNamedItem("sec");
				int oldTime = Integer.valueOf(sec.getNodeValue()).intValue();
				int newTime = oldTime + offset;
				if (newTime > cycleTime) {
					newTime -= cycleTime;
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
