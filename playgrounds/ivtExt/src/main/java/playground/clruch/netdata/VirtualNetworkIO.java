// code by clruch
package playground.clruch.netdata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.io.ObjectFormat;

public class VirtualNetworkIO {

    /** Saves virtualNetwork as a bitmap file
     * 
     * @param file
     * @param virtualNetwork
     * @throws IOException */
    // TODO how to remove this warning
    public static void toByte(File file, VirtualNetwork<Link> virtualNetwork) throws IOException {
        virtualNetwork.checkConsistency();
        Files.write(file.toPath(), ObjectFormat.of(virtualNetwork));
    }

    /** loads virtualNetwork from a bitmap file, if possible use {@link VirtualNetworkGet.readDefault}
     * 
     * @param network
     * @param file
     * @return
     * @throws ClassNotFoundException
     * @throws DataFormatException
     * @throws IOException */
    /* package */ static VirtualNetwork<Link> fromByte(Network network, File file) throws ClassNotFoundException, DataFormatException, IOException {
        VirtualNetwork<Link> virtualNetwork = ObjectFormat.parse(Files.readAllBytes(file.toPath()));
        GlobalAssert.that(virtualNetwork != null);

        Map<String, Link> map = new HashMap<>();
        network.getLinks().entrySet().forEach(e -> map.put(e.getKey().toString(), e.getValue()));
        // virtualNodes.stream().forEach(v -> v.setLinksAfterSerialization2(map));

        virtualNetwork.fillSerializationInfo(map);
        virtualNetwork.checkConsistency();
        return virtualNetwork;
    }

}

// TODO delete this.
/// ** Only for debugging purposes (printing)
// *
// * @param fileName
// * @param virtualNetwork
// * @throws IOException */
// public static void toXML(String fileName, VirtualNetwork virtualNetwork) throws IOException {
//
// Element rootElement = new Element("virtualNetwork");
// rootElement.setAttribute(new Attribute("name", "VNName"));
//
// // add virtualNodes to network (sort according to name)
// Comparator<VirtualNode> byString = (e1, e2) -> e1.getId().compareTo(e2.getId());
// Comparator<VirtualNode> byIntID = (e1, e2) -> Integer.compare(e1.getIndex(), e2.getIndex());
// List<VirtualNode> virtualNodesSorted = virtualNetwork.getVirtualNodes().stream().sorted(byIntID).collect(Collectors.toList());
//
// Element virtualNodeselem = new Element(VIRTUALNODENAME + "s");
// for (VirtualNode<Link> virtualNode : virtualNodesSorted) {
// Element virtualNodeelem = new Element(VIRTUALNODENAME);
// // set attributes
// virtualNodeelem.setAttribute(new Attribute(IDNAME, virtualNode.getId()));
// virtualNodeelem.setAttribute(new Attribute(XNAME, Double.toString(virtualNode.getCoord().Get(0).number().doubleValue())));
// virtualNodeelem.setAttribute(new Attribute(YNAME, Double.toString(virtualNode.getCoord().Get(1).number().doubleValue())));
// virtualNodeelem.setAttribute(new Attribute(NUMBERNEIGHBORNAME, Integer.toString(virtualNode.getNeighCount())));
// // add links
// Element linksElement = new Element(LINKNAME + "s");
// for (Link link : virtualNode.getLinks()) {
// Element linkElem = new Element(LINKNAME);
// linkElem.setAttribute(IDNAME, link.getId().toString());
// linksElement.addContent(linkElem);
// }
// virtualNodeelem.addContent(linksElement);
//
// virtualNodeselem.addContent(virtualNodeelem);
// }
// rootElement.addContent(virtualNodeselem);
//
// // add virtualLinks to network
// Element virtualLinkselem = new Element(VIRTUALLINKNAME + "s");
// for (VirtualLink virtualLink : virtualNetwork.getVirtualLinks()) {
// Element virtualLinkelem = new Element(VIRTUALLINKNAME);
// virtualLinkelem.setAttribute(new Attribute(IDNAME, virtualLink.getId()));
// virtualLinkelem.setAttribute(new Attribute(FROMNAME, virtualLink.getFrom().getId()));
// virtualLinkelem.setAttribute(new Attribute(TONAME, virtualLink.getTo().getId()));
// virtualLinkelem.setAttribute(new Attribute(LINKTRAVELTIMENAME, Double.toString(virtualLink.getDistance())));
// virtualLinkselem.addContent(virtualLinkelem);
// }
// rootElement.addContent(virtualLinkselem);
//
// Document doc = new Document(rootElement);
// XMLOutputter xmlOutput = new XMLOutputter();
//
// // display ml
// xmlOutput.setFormat(Format.getPrettyFormat());
// xmlOutput.output(doc, new FileWriter(fileName));
//
// }
