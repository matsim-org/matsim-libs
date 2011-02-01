package playground.mrieser.objectattributes;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.core.gbl.Gbl;
import org.xml.sax.SAXException;

public class MemoryTest {

	public static void main(String[] args) {
		ObjectAttributes oa = new ObjectAttributes();
		try {
			new ObjectAttributesXmlReader(oa).parse("/Users/cello/Desktop/poa.noGeoCodes.xml");
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Gbl.printMemoryUsage();
		System.gc();
		Gbl.printMemoryUsage();
		System.gc();
		Gbl.printMemoryUsage();
		System.gc();
		Gbl.printMemoryUsage();
	}

}
