package playground.clruch.netdata;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

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

    // get the collection of virtual nodes
    public Collection<VirtualNode> getVirtualNodes() {
        return virtualNodeList.values();
    }

    // get the collection of virtual links
    public Collection<VirtualLink> getVirtualLinks() {
        return virtualLinkList.values();
    }


    /**
     * Function that loads a virtual network from a virtualNetwork.xml file
     *
     * @param network the regular network used in  the simulation
     * @param file    the file location of the virtualNetwork.xml file
     * @return
     */
    public static VirtualNetwork loadFromXML(Network network, File file) {
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
                    virtualNetwork.virtualNodeList.put(virtualNodeId, new VirtualNode(virtualNodeId, linkIDs));
                }

                // generate a list that associates every network link to a virtual node
                for (VirtualNode virtualNode : virtualNetwork.getVirtualNodes()) {
                    for (Link link : virtualNode.getLinks()) {
                        virtualNetwork.linkIdVNodeMap.put(link, virtualNode);

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

                    virtualNetwork.virtualLinkList.put(virtualLinkId, //
                            new VirtualLink( //
                                    virtualLinkId, //
                                    virtualNetwork.virtualNodeList.get(virtualLinkfrom), //
                                    virtualNetwork.virtualNodeList.get(virtualLinkto)));
                }
            }
            return virtualNetwork;
        } catch (IOException | JDOMException io) {
            System.out.println(io.getMessage());
        }
        return null;
    }

    /**
     * Gets the virtual node belonging to a link
     *
     * @param link of the network
     * @return virtual node belonging to link
     */
    public VirtualNode getVirtualNode(Link link) {
        return linkIdVNodeMap.get(link.getId());
    }

    // TODO don't delete this function but move outside into class e.g. VirtualNetworkHelper
    public void printForTesting() {
        // check the virtualNodeList
        for (String vNodeId : virtualNodeList.keySet()) {
            System.out.println("Node " + vNodeId + " belongs to virtual node" + virtualNodeList.get(vNodeId).getId());
            System.out.println("the following links belong to it: ");
            for (Link link : virtualNodeList.get(vNodeId).getLinks()) {
                System.out.println(link.getId().toString());
            }

        }

        // check the virtualLinkList
        for (String vLinkId : virtualLinkList.keySet()) {
            System.out.println("virtual link " + vLinkId + " has the id " + (virtualLinkList.get(vLinkId)).getId() + " and goes from");
            System.out.println("virtual node " + virtualLinkList.get(vLinkId).getFrom().getId() + " to " + virtualLinkList.get(vLinkId).getTo().getId());
        }


        // check the linkIdVNodeMap
        for (Link link : linkIdVNodeMap.keySet()) {
            System.out.println("Link " + link.getId() + " belongs to virtual node " + linkIdVNodeMap.get(link).getId());
        }

    }
}
