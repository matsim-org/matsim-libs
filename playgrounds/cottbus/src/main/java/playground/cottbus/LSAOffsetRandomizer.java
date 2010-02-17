package playground.cottbus;

/**
 * @author 	rschneid-btu
 * generates some Signal System Configs for a given Config with randomized Offsets for each Crossing
 * IMPORTANT: given Config has to be in a certain way (exactly 2 Comments surround one crossing!)
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
	final static String signalSystemConfig = "./input/denver/signalSystemsConfigT60.xml"; // input
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
		    parser.parse(signalSystemConfig);
		    Document document = parser.getDocument();
		    
		    traverse (document);
		} catch (SAXException e) {
		    System.err.println (e);
		} catch (IOException e) {
		    System.err.println (e);
		}

		
    }

	private static void traverse (Document doc) {
		// parameter
		NodeList cycleTimeNode = doc.getElementsByTagName("cycleTime");
		int cycleTime = Integer.valueOf(cycleTimeNode.item(0).getAttributes().item(0).getNodeValue()).intValue();
//		System.out.println(cycleTime);
		NodeList children = doc.getElementsByTagName("signalSystemPlan").item(0).getChildNodes();
		
		for(int j=1; j<=amountOfRandomizedSignalPlans; j++) {
			try {
				int offsetDiff = (int)Math.floor(Math.random()*cycleTime);;
				short countComment = 0;
				for (int i=0; i< children.getLength(); i++) {
					Node child = children.item(i);
					// Annahme: aufeinander folgende SignalGroups gehören zu einer Kreuzung!
					// Trennung: 2 hintereinander folgende Kommentare 
					if (child.getNodeType() == Node.COMMENT_NODE) {
						countComment++;
						if (countComment == 2) {
							// zwei Comments => neue Kreuzung => neuer random Offset
							offsetDiff = (int)Math.floor(Math.random()*cycleTime);
							countComment = 0;
						}
					}
					if (child.getNodeName().equals("signalGroupSettings")) {
						countComment = 0;
						randomizeSignalGroup(child,offsetDiff,cycleTime);
					}
					
				}
				System.out.println("INFO Randomizing #"+j+" succesful.");
			} catch(Exception e) {
				System.err.println("ERR LSAOffsetRandomizer: randomizing failed.\n"+e);
				//System.exit(0);
			}
			
			printToFile(doc,OUTPUT_PATH+"signalsystemsconfigT"+cycleTime+"random"+j+".xml");	
			
		}
		
	}
	
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
		}
		
	}

	
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
