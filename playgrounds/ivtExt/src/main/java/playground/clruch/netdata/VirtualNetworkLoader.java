package playground.clruch.netdata;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
        VirtualNetwork virtualNetwork = new VirtualNetwork(network);

        // save all the links from the network in the linkMap and create a string-Id<Link> map for lookup purposes
        Map<String, Link> stringLinkIdMap = new HashMap<>();
        for (Entry<Id<Link>, ? extends Link> linkEntry : network.getLinks().entrySet()) {
            stringLinkIdMap.put(linkEntry.getKey().toString(), linkEntry.getValue());
        }

        // from the XML fill the list of virtual nodes and the link to virtual node map
        SAXBuilder builder = new SAXBuilder();
        try {
            {
                // from the XML fill the list of virtual links
                Document document = (Document) builder.build(file);
                Element rootNode = document.getRootElement();
                Element virtualNodesXML = rootNode.getChild("virtualNodes");
                List<Element> virtualNodes = virtualNodesXML.getChildren("virtualNode");
                for (Element virtualNode : virtualNodes) {
                    // get the virtual node and save it
                    String virtualNodeId = virtualNode.getAttributeValue("id");
                    // get the links associated to the node from the XML
                    Element links = virtualNode.getChild("links");
                    List<Element> linkList = links.getChildren("link");
                    Set<Link> linkIDs = new HashSet<>();

                    for (Element link : linkList) {
                        // add the links to the virtual node
                        String linkkey = link.getAttributeValue("id");
                        if (stringLinkIdMap.containsKey(linkkey)) {
                            Link correspondingLink = stringLinkIdMap.get(linkkey);
                            linkIDs.add(correspondingLink);
                        } else {
                            throw new RuntimeException("link key from not found in network");
                        }
                    }
                    virtualNetwork.virtualNodeList_put(virtualNodeId, new VirtualNode(virtualNodeId, linkIDs));
                }

                // generate a list that associates every network link to a virtual node
                for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
                    for (Link link : virtualNode.getLinks()) {
                        virtualNetwork.linkIdVNodeMap_put(link, virtualNode);
                    }
                }

            }
            // From the XML fill the list of virtual links
            {
                Document document = (Document) builder.build(file);
                Element rootNode = document.getRootElement();
                Element virtualLinksXML = rootNode.getChild("virtualLinks");
                List<Element> list = virtualLinksXML.getChildren("virtuallink");
                for (Element virtualLinkXML : list) {
                    String virtualLinkId = virtualLinkXML.getAttributeValue("id");
                    String virtualLinkfrom = virtualLinkXML.getAttributeValue("from");
                    String virtualLinkto = virtualLinkXML.getAttributeValue("to");

                    virtualNetwork.virtualLinkList_put(virtualLinkId, //
                            new VirtualLink( //
                                    virtualLinkId, //
                                    virtualNetwork.virtualNodeList_get(virtualLinkfrom), //
                                    virtualNetwork.virtualNodeList_get(virtualLinkto)));
                }
            }
            return virtualNetwork;
        } catch (IOException | JDOMException io) {
            System.out.println(io.getMessage());
        }
        return null;
    }
}
