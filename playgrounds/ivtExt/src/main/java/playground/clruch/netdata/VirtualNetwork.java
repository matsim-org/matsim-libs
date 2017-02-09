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

/**
 * Created by Claudio on 2/8/2017.
 */
public class VirtualNetwork {
    private Map<Id<Link>, VirtualNode> linkIdVNodeMap = new HashMap<>();
    private Map<String, VirtualNode> virtualNodeList = new HashMap<>();
    private Map<String, VirtualLink> virtualLinkList = new HashMap<>();


    public VirtualNetwork loadFromXML(Network network, File file) {

        //save all the links from the network in the linkMap and create a string-Id<Link> map for lookup purposes
        HashMap<String, Id<Link>> stringLinkIdMap = new HashMap<>();
        for (Id<Link> linkId : (network.getLinks()).keySet()) {
            stringLinkIdMap.put(linkId.toString(), linkId);
        }


        // from the XML fill the list of virtual nodes and the link to virtual node map
        SAXBuilder builder = new SAXBuilder();
        Map<String, VirtualNode> LinkVNode = new TreeMap<>();
        try {
            // from the XML fill the list of virtual links
            Document document = (Document) builder.build(file);
            Element rootNode = document.getRootElement();
            Element virtualNodesXML = rootNode.getChild("virtualnodes");
            List virtualnode = virtualNodesXML.getChildren("virtualnode");
            for (int i = 0; i < virtualnode.size(); i++) {
                // get the virtual node and save it
                Element virtualNode = (Element) virtualnode.get(i);
                //VirtualNode virtualNode1 = new VirtualNode();
                String virtualNode1id = virtualNode.getAttributeValue("id");

                // get the links associated to the node from the XML
                Element links = virtualNode.getChild("links");
                List linkList = links.getChildren("link");

                Set<Id<Link>> linkIDs = new HashSet<>();
                for (int j = 0; j < linkList.size(); ++j) {
                    Element link = (Element) linkList.get(j);
                    // add the links to the virtual node
                    String linkkey = link.getAttributeValue("id");
                    if (stringLinkIdMap.containsKey(linkkey)) {
                        Id<Link> linkId = stringLinkIdMap.get(linkkey);
                        linkIDs.add(linkId);
                    } else {
                        throw new RuntimeException("link key from  not found in network");
                    }

                }
                virtualNodeList.put(virtualNode1id, new VirtualNode(virtualNode1id,linkIDs));
            }


            // from the XML get a list of all link -> node pairs
            for (int i = 0; i < virtualnode.size(); i++) {
                // get the virtual node and save it
                Element virtualNode = (Element) virtualnode.get(i);

                //VirtualNode virtualNode1 = new VirtualNode();
                String virtualNode1id = virtualNode.getAttributeValue("id");

                // get the links associated to the node from the XML
                Element links = virtualNode.getChild("links");
                List linkList = links.getChildren("link");
                for (int j = 0; j < linkList.size(); ++j) {
                    Element link = (Element) linkList.get(j);
                    // add the links to the virtual node
                    String linkkey = link.getAttributeValue("id");
                    linkIdVNodeMap.put(stringLinkIdMap.get(linkkey),virtualNodeList.get(virtualNode1id));
                }
            }


            //assign to linkMap
            for (Id<Link> linkId : linkIdVNodeMap.keySet()) {
                if (LinkVNode.containsKey(linkId.toString())) {
                    linkIdVNodeMap.put(linkId, LinkVNode.get(linkId.toString()));
                } else {
                    // TODO activate this line as soon as a full network file is available.
                    //throw new RuntimeException("link in network is not assigned to any node");
                }
            }
        } catch (IOException io) {
            System.out.println(io.getMessage());
        } catch (JDOMException jdomex) {
            System.out.println(jdomex.getMessage());
        }

        // From the XML fill the list of virtual links
        try {
            Document document = (Document) builder.build(file);
            Element rootNode = document.getRootElement();
            Element virtualLinksXML = rootNode.getChild("virtuallinks");
            List list = virtualLinksXML.getChildren("virtuallink");
            for (int i = 0; i < list.size(); i++) {
                Element virtualLinkXML = (Element) list.get(i);
                String virtualLinkId = virtualLinkXML.getAttributeValue("id");
                String virtualLinkfrom = virtualLinkXML.getAttributeValue("from");
                String virtualLinkto = virtualLinkXML.getAttributeValue("to");

                virtualLinkList.put(virtualLinkId, new VirtualLink(virtualLinkId, virtualNodeList.get(virtualLinkfrom), virtualNodeList.get(virtualLinkto)));
            }
        } catch (IOException io) {
            System.out.println(io.getMessage());
        } catch (JDOMException jdomex) {
            System.out.println(jdomex.getMessage());
        }



        // TODO delete these printout lines

        /*

        // WORKS FINE
        //private Map<Id<Link>, VirtualNode> linkIdVNodeMap = new HashMap<>();
        for(Id<Link> linkId : linkIdVNodeMap.keySet()){
            System.out.println("link with id " + linkId.toString() + " is on node " + linkIdVNodeMap.get(linkId).getId());
        }


        // WORKS FINE
        //private Map<String, VirtualNode> virtualNodeList = new HashMap<>();
        for(String vnId : virtualNodeList.keySet()){
            System.out.println("virtual node " + vnId + " has the id " + (virtualNodeList.get(vnId)).getId() + " and contains the links");
            for(Id<Link> idlink : virtualNodeList.get(vnId).getLinkIDs()){
                System.out.println(idlink.toString());
            }
        }

        // WORKS FINE

        //private Map<String, VirtualLink> virtualLinkList = new HashMap<>();
        for(String vsId : virtualLinkList.keySet()){
            System.out.println("virtual link " + vsId + " has the id " + (virtualLinkList.get(vsId)).getId() + " and goes from");
            System.out.println("virtual node " + virtualLinkList.get(vsId).getFrom().getId() + " to " + virtualLinkList.get(vsId).getTo().getId());
        }

        */


        return null;
    }

    public VirtualNode getVirtualNode(Id<Link> linkId) {
        return linkIdVNodeMap.get(linkId);
    }


    // TODO implement this in abstract fashion
    public Id<Link> getTargetLink(VirtualNode virtualNode) {
        // returns a network target link for some virtual node
        return null;
    }

}
