package playground.florian.JFreeTest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;


public class Dom4jTest {
	//Vorlage: http://www.dom4j.org/dom4j-1.6.1/guide.html
	
	public Document styleDocument(Document document, String stylesheet){
		// load the transformer using JAXP
		TransformerFactory fac = TransformerFactory.newInstance();
		Document transformedDoc = null;
		try {
			Transformer trafo = fac.newTransformer(new StreamSource(stylesheet));
			// Transform the given Document
			DocumentSource source = new DocumentSource(document);
			DocumentResult result = new DocumentResult();
			trafo.transform(source, result);
			
			//return the result
			transformedDoc = result.getDocument();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return transformedDoc;
	}
	
    public Document parse(String file) throws DocumentException {
//    	URL url = getClass().getResource(file);
        SAXReader reader = new SAXReader();
        Document document = reader.read(file);
        return document;
    }

	
	public static void serializeToXML(Document doc, OutputStream out){
		try {
			XMLWriter writer = new XMLWriter(out);
			writer.write(doc);
			writer.flush();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public static void serializeToXML(Document doc, XMLWriter writer){
		try {
			writer.write(doc);
			writer.flush();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public static InputStream serializeToXMLInput(Document doc){
		InputStream in = null;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			XMLWriter writer = new XMLWriter(out);
			writer.write(doc);
			writer.flush();			
			in = new ByteArrayInputStream(out.toByteArray());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return in;
	}
	
	public static void writeTheDocumentToFile(Document doc, String filename){
		try {
			XMLWriter writer = new XMLWriter(new FileWriter(filename), OutputFormat.createPrettyPrint());
			writer.write(doc);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
