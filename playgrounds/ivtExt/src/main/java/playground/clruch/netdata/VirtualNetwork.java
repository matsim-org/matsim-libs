package playground.clruch.netdata;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * Created by Claudio on 2/8/2017.
 */
public class VirtualNetwork {
    public final Network network;
    private final Map<String, VirtualNode> virtualNodeList = new HashMap<>(); // has unique references
    private final Map<Link, VirtualNode> linkIdVNodeMap = new HashMap<>();
    private final Map<String, VirtualLink> virtualLinkList = new HashMap<>();

    public VirtualNetwork(Network network) {
        this.network = network;
    }

    public Collection<VirtualNode> getVirtualNodes() {
        return virtualNodeList.values();
    }

    // FIXME not sure if still works
    public static VirtualNetwork loadFromXML(Network network, File file) {
        VirtualNetwork virtualNetwork = new VirtualNetwork(network);

        // save all the links from the network in the linkMap and create a string-Id<Link> map for lookup purposes
        Map<String, Link> stringLinkIdMap = new HashMap<>();
        for (Entry<Id<Link>, ? extends Link> linkId : network.getLinks().entrySet()) {
            stringLinkIdMap.put(linkId.toString(), linkId.getValue());
        }

        // from the XML fill the list of virtual nodes and the link to virtual node map
        SAXBuilder builder = new SAXBuilder();
        Map<String, VirtualNode> LinkVNode = new TreeMap<>();
        try {
            {
                // from the XML fill the list of virtual links
                Document document = (Document) builder.build(file);
                Element rootNode = document.getRootElement();
                Element virtualNodesXML = rootNode.getChild("virtualnodes");
                List<Element> virtualNodes = virtualNodesXML.getChildren("virtualnode");
                for (Element virtualNode : virtualNodes) {
                    // get the virtual node and save it
                    String virtualNode1id = virtualNode.getAttributeValue("id");

                    // get the links associated to the node from the XML
                    Element links = virtualNode.getChild("links");
                    List<Element> linkList = links.getChildren("link");

                    Set<Link> linkIDs = new HashSet<>();
                    for (Element link : linkList) {
                        // add the links to the virtual node
                        String linkkey = link.getAttributeValue("id");
                        if (stringLinkIdMap.containsKey(linkkey)) {
                            Link linkId = stringLinkIdMap.get(linkkey); // TODO rename
                            linkIDs.add(linkId);
                        } else {
                            throw new RuntimeException("link key from  not found in network");
                        }
                    }

                    virtualNetwork.virtualNodeList.put(virtualNode1id, new VirtualNode(virtualNode1id, linkIDs));
                }

                // from the XML get a list of all link -> node pairs
                for (int i = 0; i < virtualNodes.size(); i++) {
                    // get the virtual node and save it
                    Element virtualNode = (Element) virtualNodes.get(i);

                    // VirtualNode virtualNode1 = new VirtualNode();
                    String virtualNode1id = virtualNode.getAttributeValue("id");

                    // get the links associated to the node from the XML
                    Element links = virtualNode.getChild("links");
                    List linkList = links.getChildren("link");
                    for (int j = 0; j < linkList.size(); ++j) {
                        Element link = (Element) linkList.get(j);
                        // add the links to the virtual node
                        String linkkey = link.getAttributeValue("id");
                        virtualNetwork.linkIdVNodeMap.put(stringLinkIdMap.get(linkkey), //
                                virtualNetwork.virtualNodeList.get(virtualNode1id));
                    }
                }

                // assign to linkMap
                for (Link linkId : virtualNetwork.linkIdVNodeMap.keySet()) { // TODO rename
                    if (LinkVNode.containsKey(linkId.toString())) {
                        virtualNetwork.linkIdVNodeMap.put(linkId, LinkVNode.get(linkId.toString()));
                    } else {
                        // TODO activate this line as soon as a full network file is available.
                        // throw new RuntimeException("link in network is not assigned to any node");
                    }
                }
            }
            // From the XML fill the list of virtual links
            {
                Document document = (Document) builder.build(file);
                Element rootNode = document.getRootElement();
                Element virtualLinksXML = rootNode.getChild("virtuallinks");
                List<Element> list = virtualLinksXML.getChildren("virtuallink");
                for (int i = 0; i < list.size(); i++) {
                    Element virtualLinkXML = (Element) list.get(i);
                    String virtualLinkId = virtualLinkXML.getAttributeValue("id");
                    String virtualLinkfrom = virtualLinkXML.getAttributeValue("from");
                    String virtualLinkto = virtualLinkXML.getAttributeValue("to");

                    virtualNetwork.virtualLinkList.put(virtualLinkId, //
                            new VirtualLink( //
                                    virtualLinkId, //
                                    virtualNetwork.virtualNodeList.get(virtualLinkfrom), //
                                    virtualNetwork.virtualNodeList.get(virtualLinkto)));
                }
            }
            return virtualNetwork;
        } catch (IOException io) {
            System.out.println(io.getMessage());
        } catch (JDOMException jdomex) {
            System.out.println(jdomex.getMessage());
        }

        // TODO delete these printout lines

        /*
         * 
         * // WORKS FINE
         * //private Map<Id<Link>, VirtualNode> linkIdVNodeMap = new HashMap<>();
         * for(Id<Link> linkId : linkIdVNodeMap.keySet()){
         * System.out.println("link with id " + linkId.toString() + " is on node " + linkIdVNodeMap.get(linkId).getId());
         * }
         * 
         * 
         * // WORKS FINE
         * //private Map<String, VirtualNode> virtualNodeList = new HashMap<>();
         * for(String vnId : virtualNodeList.keySet()){
         * System.out.println("virtual node " + vnId + " has the id " + (virtualNodeList.get(vnId)).getId() + " and contains the links");
         * for(Id<Link> idlink : virtualNodeList.get(vnId).getLinkIDs()){
         * System.out.println(idlink.toString());
         * }
         * }
         * 
         * // WORKS FINE
         * 
         * //private Map<String, VirtualLink> virtualLinkList = new HashMap<>();
         * for(String vsId : virtualLinkList.keySet()){
         * System.out.println("virtual link " + vsId + " has the id " + (virtualLinkList.get(vsId)).getId() + " and goes from");
         * System.out.println("virtual node " + virtualLinkList.get(vsId).getFrom().getId() + " to " + virtualLinkList.get(vsId).getTo().getId());
         * }
         * 
         */

        return null;
    }

    public VirtualNode getVirtualNode(Link link) {
        return linkIdVNodeMap.get(link.getId());
    }

    // TODO implement this in abstract fashion
    // public Id<Link> getTargetLink(VirtualNode virtualNode) {
    // // returns a network target link for some virtual node
    // return null;
    // }

}
