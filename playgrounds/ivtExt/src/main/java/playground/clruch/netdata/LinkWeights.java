package playground.clruch.netdata;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class LinkWeights {
    public static Map<VirtualLink, Double> fillLinkWeights(File file, VirtualNetwork virtualNetwork) {
        // open the linkWeightFile and parse the parameter values
        SAXBuilder builder = new SAXBuilder();
        try {
            Map<VirtualLink, Double> linkWeights = new HashMap<>();
            Document document = (Document) builder.build(file);
            Element rootNode = document.getRootElement();
            Element virtualNodesXML = rootNode.getChild("weights");
            List<Element> virtualLinkXML = virtualNodesXML.getChildren("virtuallink");
            for (Element vLinkelem : virtualLinkXML) {
                String vlinkID = vLinkelem.getAttributeValue("id");
                Double weight = Double.parseDouble(vLinkelem.getAttributeValue("weight"));


                // find the virtual link with the corresponding ID and assign the weight to it.
                linkWeights.put((virtualNetwork.getVirtualLinks()).stream()
                                .filter(vl -> vl.getId().toString().equals(vlinkID))
                                .findFirst()
                                .get(),
                        weight);
            }

            return linkWeights;
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
