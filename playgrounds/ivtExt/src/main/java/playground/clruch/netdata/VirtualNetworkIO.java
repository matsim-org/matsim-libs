package playground.clruch.netdata;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class VirtualNetworkIO {
    
    
    private static final String VIRTUALNODENAME = "virtualNode";
    private static final String VIRTUALLINKNAME = "virtualLink";
    private static final String IDNAME = "id";
    private static final String XNAME = "x";
    private static final String YNAME = "y";
    private static final String LINKNAME = "link";
    private static final String FROMNAME = "from";
    private static final String TONAME = "to";
    private static final String NUMBERNEIGHBORNAME = "nNeigh";
    private static final String LINKTRAVELTIMENAME = "Ttime";
    
    
    
    
    
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

        // save all the links from the network in the linkMap and create a string-Id<Link> map for
        // lookup purposes
        Map<String, Link> stringLinkIdMap = new HashMap<>();
        for (Entry<Id<Link>, ? extends Link> linkEntry : network.getLinks().entrySet()) {
            stringLinkIdMap.put(linkEntry.getKey().toString(), linkEntry.getValue());
        }

        // from the XML fill the list of virtual nodes and the link to virtual node map
        SAXBuilder builder = new SAXBuilder();
        try {
            final Map<String, VirtualNode> virtualNodeList = new HashMap<>(); // has unique
                                                                              // references
            {
                // from the XML fill the list of virtual links
                Document document = (Document) builder.build(file);
                Element rootNode = document.getRootElement();
                Element virtualNodesXML = rootNode.getChild(VIRTUALNODENAME+"s");
                List<Element> virtualNodes = virtualNodesXML.getChildren(VIRTUALNODENAME);
                for (Element virtualNodeElement : virtualNodes) {
                    // get the virtual node and save it
                    String virtualNodeId = virtualNodeElement.getAttributeValue(IDNAME);
                    int neighCount = Integer.parseInt(virtualNodeElement.getAttributeValue(NUMBERNEIGHBORNAME));
                    double x = Double.parseDouble(virtualNodeElement.getAttributeValue(XNAME));
                    double y = Double.parseDouble(virtualNodeElement.getAttributeValue(YNAME));
                    Coord coord = new Coord(x,y);
                    // get the links associated to the node from the XML
                    Element links = virtualNodeElement.getChild(LINKNAME+"s");
                    List<Element> linkList = links.getChildren(LINKNAME);
                    Set<Link> linkSet = new LinkedHashSet<>(); // keep the ordering as in the XML
                                                               // file

                    for (Element linkElement : linkList) {
                        // add the links to the virtual node
                        String linkkey = linkElement.getAttributeValue(IDNAME);
                        if (stringLinkIdMap.containsKey(linkkey)) {
                            Link correspondingLink = stringLinkIdMap.get(linkkey);
                            linkSet.add(correspondingLink);
                        } else {
                            throw new RuntimeException("link key from not found in network");
                        }
                    }
                    final VirtualNode virtualNode = virtualNetwork.addVirtualNode(virtualNodeId, linkSet, neighCount, coord);
                    // new VirtualNode();
                    virtualNodeList.put(virtualNodeId, virtualNode);

                }

            }
            // From the XML fill the list of virtual links
            {
                Document document = (Document) builder.build(file);
                Element rootNode = document.getRootElement();
                Element virtualLinksXML = rootNode.getChild(VIRTUALLINKNAME+"s");
                List<Element> list = virtualLinksXML.getChildren(VIRTUALLINKNAME);
                for (Element virtualLinkXML : list) {
                    String virtualLinkId = virtualLinkXML.getAttributeValue(IDNAME);
                    String virtualLinkfrom = virtualLinkXML.getAttributeValue(FROMNAME);
                    String virtualLinkto = virtualLinkXML.getAttributeValue(TONAME);
                    String virtualLinkTtime = virtualLinkXML.getAttributeValue(LINKTRAVELTIMENAME);

                    virtualNetwork.addVirtualLink(virtualLinkId, //
                            virtualNodeList.get(virtualLinkfrom), //
                            virtualNodeList.get(virtualLinkto),
                            Double.parseDouble(virtualLinkTtime)); //
                }
            }
            return virtualNetwork;
        } catch (IOException | JDOMException io) {
            System.out.println(io.getMessage());
        }
        return null;
    }

    public static void toXML(String fileName, VirtualNetwork virtualNetwork) throws IOException {

        Element rootElement = new Element("virtualNetwork");
        rootElement.setAttribute(new Attribute("name", "VNName"));


        // add virtualNodes to network (sort according to name)
        Comparator<VirtualNode> byString = (e1, e2) -> e1.getId().compareTo(e2.getId());
        Comparator<VirtualNode> byIntID = (e1, e2) -> Integer.compare(e1.getIndex(), e2.getIndex());
        List<VirtualNode> virtualNodesSorted = virtualNetwork.getVirtualNodes().stream().sorted(byIntID).collect(Collectors.toList());
        
        Element virtualNodeselem = new Element(VIRTUALNODENAME+"s");
        for (VirtualNode virtualNode : virtualNodesSorted) {
            Element virtualNodeelem = new Element(VIRTUALNODENAME);
            // set attributes
            virtualNodeelem.setAttribute(new Attribute(IDNAME, virtualNode.getId()));
            virtualNodeelem.setAttribute(new Attribute(XNAME, Double.toString(virtualNode.getCoord().getX())));
            virtualNodeelem.setAttribute(new Attribute(YNAME, Double.toString(virtualNode.getCoord().getY())));
            virtualNodeelem.setAttribute(new Attribute(NUMBERNEIGHBORNAME, Integer.toString(virtualNode.getNeighCount())));
            // add links
            Element linksElement = new Element(LINKNAME+"s");
            for(Link link : virtualNode.getLinks()){
                Element linkElem = new Element(LINKNAME);
                linkElem.setAttribute(IDNAME,link.getId().toString());
                linksElement.addContent(linkElem);
            }
            virtualNodeelem.addContent(linksElement);
            
            
            virtualNodeselem.addContent(virtualNodeelem);
        }
        rootElement.addContent(virtualNodeselem);
        
        // add virtualLinks to network
        Element virtualLinkselem = new Element(VIRTUALLINKNAME+"s");
        for (VirtualLink virtualLink : virtualNetwork.getVirtualLinks()) {
            Element virtualLinkelem = new Element(VIRTUALLINKNAME);
            virtualLinkelem.setAttribute(new Attribute(IDNAME, virtualLink.getId()));
            virtualLinkelem.setAttribute(new Attribute(FROMNAME, virtualLink.getFrom().getId()));
            virtualLinkelem.setAttribute(new Attribute(TONAME, virtualLink.getTo().getId()));
            virtualLinkelem.setAttribute(new Attribute(LINKTRAVELTIMENAME, Double.toString(virtualLink.getTtime())));
            virtualLinkselem.addContent(virtualLinkelem);
        }
        rootElement.addContent(virtualLinkselem);

        
        Document doc = new Document(rootElement);
        XMLOutputter xmlOutput = new XMLOutputter();

        // display ml
        xmlOutput.setFormat(Format.getPrettyFormat());
        xmlOutput.output(doc, System.out);
        xmlOutput.output(doc, new FileWriter(fileName));

    }
}
