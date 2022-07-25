package playground.vsp.ev;

import com.fasterxml.jackson.databind.node.TextNode;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.List;

public  class CreateNewXML {
    private final List<ChargerSpecification> chargers;
    public static final String xmlFilePath = "C:\\Users\\admin\\Desktop\\chargers.xml";

    public CreateNewXML(List<ChargerSpecification> chargers) {
        this.chargers = chargers;

        try {
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();

            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();

            Document document = documentBuilder.newDocument();

            Element rootElement = document.createElement("chargers");
            document.appendChild(rootElement);
            rootElement.appendChild(document.createTextNode("\n"));

            for (ChargerSpecification charger : chargers) {

                Element chargername = document.createElement("charger");
                rootElement.appendChild(chargername);


                Attr id = document.createAttribute("id");
                id.setValue(String.valueOf(charger.getId()));
                chargername.setAttributeNode(id);

                Attr linkId = document.createAttribute("link");
                linkId.setValue(String.valueOf(charger.getLinkId()));
                chargername.setAttributeNode(linkId);

                Attr chargerType = document.createAttribute("charger_type");
                chargerType.setValue(charger.getChargerType());
                chargername.setAttributeNode(chargerType);

                Attr plugPower = document.createAttribute("plug_power");
                plugPower.setValue(String.valueOf(charger.getPlugPower()));
                chargername.setAttributeNode(plugPower);

                Attr plugCount = document.createAttribute("plug_count");
                plugCount.setValue(String.valueOf(charger.getPlugCount()));
                chargername.setAttributeNode(plugCount);
                rootElement.appendChild(document.createTextNode("\n"));





            }


            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(new File(xmlFilePath));


            transformer.transform(domSource, streamResult);


            System.out.println("Done creating XML File");
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }
    }
}


