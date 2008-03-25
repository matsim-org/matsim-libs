package playground.ciarif.retailers;

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class MatsimRetailersReader extends MatsimXmlParser {

	public MatsimRetailersReader(Retailers singleton) {
		// TODO Auto-generated constructor stub
	}

	public void readFile(final String filename) {
		try {
			parse(filename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		// TODO Auto-generated method stub
		
	}
}
