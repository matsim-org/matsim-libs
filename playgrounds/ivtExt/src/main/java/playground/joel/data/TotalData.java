package playground.joel.data;

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

/**
 * Created by Joel on 01.03.2017.
 */
public class TotalData <Type> {
    String xmlTitle = "SimulationResult";
    String L1ElName = "TotalModeShare";
    String L1AttrName = "ModeShare";
    String L2ElName = "TotalTimeRatio";
    String L2AttrName = "TimeRatio";
    String L3ElName = "TotalDistanceRatio";
    String L3AttrName = "DistanceRatio";
    String L4ElName = "TotalWaitingTime";
    String L4AttrName = "WaitingTime";

    public TotalData() {}

    // Take the Map timeStepData which for some IDs contains series of changes for times Double
    // in the data format Type. Save them in an XML with the structure:
    // <xmlTitle>
    // <L1ElName L1AttrName="String1">
    // </L1ElName>
    // <L2ElName L2AttrName="String2">
    // </L1ElName>
    // ...
    public final void generate(String value1, String value2, String value3, String value4, File file) {
        try {
            Element results = new Element(xmlTitle);
            Document doc = new Document(results);
            doc.setRootElement(results);

            // mode share
            Element element1 = new Element(L1ElName);
            element1.setAttribute(new Attribute(L1AttrName, value1));
            doc.getRootElement().addContent(element1);

            // time ratio
            Element element2 = new Element(L2ElName);
            element2.setAttribute(new Attribute(L2AttrName, value2));
            doc.getRootElement().addContent(element2);

            // distance ratio
            Element element3 = new Element(L3ElName);
            element3.setAttribute(new Attribute(L3AttrName, value3));
            doc.getRootElement().addContent(element3);

            // waiting times
            Element element4 = new Element(L4ElName);
            element4.setAttribute(new Attribute(L4AttrName, value4));
            doc.getRootElement().addContent(element4);

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
