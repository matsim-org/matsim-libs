package playground.lsieber.scenario.reducer;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

public class NetworkActions {

    public NetworkActions() {
        // TODO Auto-generated constructor stub
    }
    
    public static Network modeFilter(Network originalNetwork, HashSet<String> modes) {
        // Filter out modes
           Network modesFilteredNetwork = NetworkUtils.createNetwork();
           for (Node node : originalNetwork.getNodes().values()) {
               modesFilteredNetwork.addNode(modesFilteredNetwork.getFactory().createNode(node.getId(), node.getCoord()));        
           }
           for (Link filteredlink : originalNetwork.getLinks().values()) {
               Node filteredFromNode = modesFilteredNetwork.getNodes().get(filteredlink.getFromNode().getId());
               Node filteredToNode = modesFilteredNetwork.getNodes().get(filteredlink.getToNode().getId());
               if (filteredFromNode != null && filteredToNode != null) {
            
                   Iterator<String> it = modes.iterator();
                   boolean allowedMode = false;
                   while (it.hasNext() && !allowedMode) {
                       allowedMode = filteredlink.getAllowedModes().contains(it.next());
                   }
                   if (allowedMode) {
                       Link newLink = modesFilteredNetwork.getFactory().createLink(filteredlink.getId(), filteredFromNode, filteredToNode);

                       // newLink.setAllowedModes(Collections.singleton("car"));
                       newLink.setAllowedModes(filteredlink.getAllowedModes());

                       newLink.setLength(filteredlink.getLength());
                       newLink.setCapacity(filteredlink.getCapacity());
                       newLink.setFreespeed(filteredlink.getFreespeed());
                       // newLink.setNumberOfLanes(link.getNumberOfLanes());

                       modesFilteredNetwork.addLink(newLink);   
                   }
               }
               
           }
           return modesFilteredNetwork;     
       }

}
