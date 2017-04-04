package playground.clruch.netdata;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class VirtualNetworkLoader {
    /**
     * Function that loads a virtual network from a virtualNetwork.xml file
     *
     * @param network
     *            the regular network used in the simulation
     * @param file
     *            the file location of the virtualNetwork.xml file
     * @return
     */
    public static VirtualNetwork fromXML(Network network, File file) {
        VirtualNetwork virtualNetwork = new VirtualNetwork();

        // save all the links from the network in the linkMap and create a string-Id<Link> map for lookup purposes
        Map<String, Link> stringLinkIdMap = new HashMap<>();
        for (Entry<Id<Link>, ? extends Link> linkEntry : network.getLinks().entrySet()) {
            stringLinkIdMap.put(linkEntry.getKey().toString(), linkEntry.getValue());
        }

        // from the XML fill the list of virtual nodes and the link to virtual node map
        SAXBuilder builder = new SAXBuilder();
        try {
            final Map<String, VirtualNode> virtualNodeList = new HashMap<>(); // has unique references
            {
                // from the XML fill the list of virtual links
                Document document = (Document) builder.build(file);
                Element rootNode = document.getRootElement();
                Element virtualNodesXML = rootNode.getChild("virtualNodes");
                List<Element> virtualNodes = virtualNodesXML.getChildren("virtualNode");
                for (Element virtualNodeElement : virtualNodes) {
                    // get the virtual node and save it
                    String virtualNodeId = virtualNodeElement.getAttributeValue("id");
                    int neighCount       = Integer.parseInt(virtualNodeElement.getAttributeValue("nNeigh"));
                    // get the links associated to the node from the XML
                    Element links = virtualNodeElement.getChild("links");
                    List<Element> linkList = links.getChildren("link");
                    Set<Link> linkSet = new LinkedHashSet<>(); // keep the ordering as in the XML file

                    for (Element linkElement : linkList) {
                        // add the links to the virtual node
                        String linkkey = linkElement.getAttributeValue("id");
                        if (stringLinkIdMap.containsKey(linkkey)) {
                            Link correspondingLink = stringLinkIdMap.get(linkkey);
                            linkSet.add(correspondingLink);
                        } else {
                            throw new RuntimeException("link key from not found in network");
                        }
                    }
                    final VirtualNode virtualNode = virtualNetwork.addVirtualNode(virtualNodeId, linkSet,neighCount);
                    // new VirtualNode();
                    virtualNodeList.put(virtualNodeId, virtualNode);

                }

            }
            // From the XML fill the list of virtual links
            {
                Document document = (Document) builder.build(file);
                Element rootNode = document.getRootElement();
                Element virtualLinksXML = rootNode.getChild("virtualLinks");
                List<Element> list = virtualLinksXML.getChildren("virtualLink");
                for (Element virtualLinkXML : list) {
                    String virtualLinkId = virtualLinkXML.getAttributeValue("id");
                    String virtualLinkfrom = virtualLinkXML.getAttributeValue("from");
                    String virtualLinkto = virtualLinkXML.getAttributeValue("to");

                    virtualNetwork.addVirtualLink(virtualLinkId, //
                            virtualNodeList.get(virtualLinkfrom), //
                            virtualNodeList.get(virtualLinkto)); //
                }
            }
            return virtualNetwork;
        } catch (IOException | JDOMException io) {
            System.out.println(io.getMessage());
        }
        return null;
    }
}
