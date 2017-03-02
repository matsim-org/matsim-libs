package playground.joel.data;

/**
 * Created by Joel on 28.02.2017.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

abstract class AbstractDataXML<Type> {
    String xmlTitle;
    String L1ElName;
    String L1AttrName;
    String L2ElName;
    String L2Attr1Name;
    String L2Attr2Name;

    public AbstractDataXML(String xmlTitleIn, String L1ElNameIn, String L1AttrNameIn, String L2ElNameIn, String L2Attr1NameIn, String L2Attr2NameIn) {
        xmlTitle = xmlTitleIn;
        L1ElName = L1ElNameIn;
        L1AttrName = L1AttrNameIn;
        L2ElName = L2ElNameIn;
        L2Attr1Name = L2Attr1NameIn;
        L2Attr2Name = L2Attr2NameIn;
    }

    // Take the Map timeStepData which for some IDs contains series of changes for times Double
    // in the data format Type. Save them in an XML with the structure:
    // <xmlTitle>
    // <L1ElName L1AttrName="String1">
    // <L2ElName L2Attr1Name="Double1ofString1" L2Attr2Name="Type1ofString1" />
    // <L2ElName L2Attr1Name="Double2ofString1" L2Attr2Name="Type2ofString1" />
    // <L2ElName L2Attr1Name="Double3ofString1" L2Attr2Name="Type3ofString1" />
    // ...
    // </L1ElName>
    // <L1ElName L1AttrName="String1">
    // <L2ElName L2Attr1Name="Double1ofString2" L2Attr2Name="Type1ofString2" />
    // <L2ElName L2Attr1Name="Double2ofString2" L2Attr2Name="Type2ofString2" />
    // <L2ElName L2Attr1Name="Double3ofString2" L2Attr2Name="Type3ofString2" />
    // ...
    // </L1ElName>
    // ...
    public final void generate(Map<String, NavigableMap<Double, Type>> timeStepData, File file) {
        try {
            Element SimulationResult = new Element(xmlTitle);
            Document doc = new Document(SimulationResult);
            doc.setRootElement(SimulationResult);
            Set<String> s = timeStepData.keySet();
            Iterator<String> e = s.iterator();
            // Iterate through all L1ElName entries
            while (e.hasNext()) {
                String statID = (String) e.next();
                Element node = new Element(L1ElName);
                node.setAttribute(new Attribute(L1AttrName, statID));
                // iterate through step function to save L2ElName entris
                NavigableMap<Double, Type> StepFctn = timeStepData.get(statID);
                for (Double doubleVal : StepFctn.keySet()) {
                    Element event = new Element(L2ElName);
                    event.setAttribute(L2Attr1Name, doubleVal.toString());
                    event.setAttribute(L2Attr2Name, StepFctn.get(doubleVal).toString());
                    node.addContent(event);
                }
                doc.getRootElement().addContent(node);
            }
            // output the XML
            XMLOutputter xmlOutput = new XMLOutputter();
            // display nice nice
            xmlOutput.setFormat(Format.getPrettyFormat());
            xmlOutput.output(doc, new FileWriter(file));

            System.out.println("exported " + file.getName());
        } catch (IOException io) {
            System.out.println(io.getMessage());
        }
    }
}
