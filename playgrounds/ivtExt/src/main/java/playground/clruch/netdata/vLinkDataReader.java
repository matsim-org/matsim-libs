// code by clruch
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

@Deprecated // this will be deletd as traveltime directly included in virtual network
public class vLinkDataReader {
    public static Map<VirtualLink, Double> fillvLinkData(File file, VirtualNetwork virtualNetwork, String datastring) {
        // open the linkWeightFile and parse the parameter values
        SAXBuilder builder = new SAXBuilder();
        try {
            Map<VirtualLink, Double> linkWeights = new HashMap<>();
            Document document = (Document) builder.build(file);
            Element rootNode = document.getRootElement();
            Element virtualNodesXML = rootNode.getChild("virtualLinks");
            List<Element> virtualLinkXML = virtualNodesXML.getChildren("virtualLink");
            for (Element vLinkelem : virtualLinkXML) {
                String vlinkID = vLinkelem.getAttributeValue("id");
                Double weight = Double.parseDouble(vLinkelem.getAttributeValue(datastring));


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
