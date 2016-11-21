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
            return loadConfig(new FileInputStream(path));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not find config file " + path);
        }
    }

    public static Config loadConfig(InputStream stream) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactoryImpl.newInstance();
            DocumentBuilder documentBuilder = null;

            documentBuilder = documentBuilderFactory.newDocumentBuilder();

            Document document = documentBuilder.parse(stream);
            return new Config(document);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Error while configuring parser");
        } catch (SAXException e) {
            throw new RuntimeException("Error while parsing");
        } catch (IOException e) {
            throw new RuntimeException("Error while reading");
        }
    }

    public static void saveConfig(File path, Config config) {
        try {
            saveConfig(new FileOutputStream(path), config);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not find config file " + path);
        }
    }

    public static void saveConfig(OutputStream stream, Config config) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(config.getDocument());

            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, config.getDocument().getDoctype().getSystemId());

            OutputStreamWriter outputWriter = new OutputStreamWriter(stream);
            StreamResult streamResult = new StreamResult(outputWriter);
            transformer.transform(domSource, streamResult);

            outputWriter.flush();
            stream.flush();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found");
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException("Error while configuration transformer for ");
        } catch (IOException e) {
            throw new RuntimeException("Error while writing ");
        } catch (TransformerException e) {
            throw new RuntimeException("Error while transforming config to ");
        }
    }
}
