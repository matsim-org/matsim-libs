package cottbusAnalysis;

import java.io.Writer;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.matsim.core.utils.io.IOUtils;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 * @author tthunig
 * @deprecated
 */
public class TtWriteXML {

	private static final String CDATA = "CDATA";
	private static final String ID = "id";
	private static final String COMMODITIES = "commodities";
	private static final String COMMODITY = "commodity";
	private static final String TTT = "totalTravelTime";
	private static final String TTT90 = "totalTravelTimeTimes90";
	
	public static void writeComTravelTimes(
			Map<String, Double> avarageComTravelTime, String outFile) {
		
		Writer writer = IOUtils.getBufferedWriter(outFile);
		try {
			TransformerHandler hd = createContentHandler(writer);
			hd.startDocument();
			AttributesImpl atts = new AttributesImpl();
			hd.startElement("", "", COMMODITIES, atts);
			for (String comId : avarageComTravelTime.keySet()) {
				atts.clear();
				atts.addAttribute("", "", ID, CDATA, comId.toString());
				atts.addAttribute("", "", TTT, CDATA, avarageComTravelTime.get(comId).toString());
				atts.addAttribute("", "", TTT90, CDATA, Double.toString(avarageComTravelTime.get(comId) * 90));
				hd.startElement("", "", COMMODITY, atts);
				hd.endElement("", "", COMMODITY);
			}
			hd.endElement("", "", COMMODITIES);
			hd.endDocument();
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	private static TransformerHandler createContentHandler(Writer writer)
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
