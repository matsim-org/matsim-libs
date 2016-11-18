package playground.sebhoerl.mexec;

import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

public class ConfigUtils {
    public static Config loadConfig(File path) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactoryImpl.newInstance();
            DocumentBuilder documentBuilder = null;

            documentBuilder = documentBuilderFactory.newDocumentBuilder();

            Document document = documentBuilder.parse(path);
            return new Config(document);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Error while configuring parser for " + path);
        } catch (SAXException e) {
            throw new RuntimeException("Error while parsing " + path);
        } catch (IOException e) {
            throw new RuntimeException("Error while reading " + path);
        }
    }

    public static void saveConfig(File path, Config config) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(config.getDocument());

            OutputStream outputStream = new FileOutputStream(path);
            OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream);

            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, config.getDocument().getDoctype().getSystemId());

            StreamResult streamResult = new StreamResult(outputWriter);
            transformer.transform(domSource, streamResult);

            outputWriter.flush();
            outputStream.flush();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found: " + path);
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException("Error while configuration transformer for " + path);
        } catch (IOException e) {
            throw new RuntimeException("Error while writing " + path);
        } catch (TransformerException e) {
            throw new RuntimeException("Error while transforming config to " + path);
        }
    }
}
