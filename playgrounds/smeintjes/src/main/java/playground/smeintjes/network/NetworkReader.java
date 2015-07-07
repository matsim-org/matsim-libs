/* 
 * Copyright (C) 2013 Maarten Houbraken
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Software available at https://github.com/mhoubraken/ISMAGS
 * Author : Maarten Houbraken (maarten.houbraken@intec.ugent.be)
 */
package playground.smeintjes.network;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import playground.smeintjes.datastructures.NodeSet;

/**
 * Handles all network memory operations
 */
public class NetworkReader {

    /**
     * Reads a network
     *
     * @param filenames list of files containing edges and nodes
     * @param linkTypes for each filename, a linktype is specified
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static Network readNetworkFromFiles(ArrayList<String> filenames, ArrayList<LinkType> linkTypes) throws FileNotFoundException, IOException {
        Network network = new Network();
        int nodeID = 0;
        int linkID = 0;
        for (int i = 0; i < filenames.size(); i++) {
            String filename = filenames.get(i);
            LinkType linkType = linkTypes.get(i);

            BufferedReader in = new BufferedReader(new FileReader(filename));

            String line = in.readLine();
            int linksi = 0;
            while (line != null) {
                int t = line.indexOf('\t');
                if (t <= 0 || line.contains("#")) {
                    line = in.readLine();
                    continue;
                }
                String n1 = line.substring(0, t) + linkType.sourceNetwork;
                String n2 = line.substring(t + 1) + linkType.destinationNetwork;
                Node origin = network.getNodeByDescription(n1);

                if (n1.equals(n2)) {
//                if(n1.equalsIgnoreCase(n2)){
                    line = in.readLine();
                    continue;
                }
                if (origin == null) {
                    origin = new Node(nodeID++, n1);
                    network.addNode(origin);
                }
                Node destination = network.getNodeByDescription(n2);
                if (destination == null) {
                    destination = new Node(nodeID++, n2);
                    network.addNode(destination);
                }

                NodeSet nodes = origin.neighboursPerType[linkType.getMotifLink().getMotifLinkID()];
                line = in.readLine();
                if (nodes != null && nodes.nodes.contains(destination)) {
                    continue;
                }
                Link l = new Link(linkID++, origin, destination, linkType);
                linksi++;
                network.addLink(l);
            }
            for (Node node : network.nodesByDescription.values()) {
                for (NodeSet nodeSet : node.neighboursPerType) {
                    if (nodeSet != null) {
                        nodeSet.sort();
                        nodeSet.trimToSize();
                    }
                }
            }
            System.out.println("Read: " + filename + " : links: " + linksi);
        }
        network.finalizeNetworkConstruction();
        System.out.println("Nodes: " + network.nodesByDescription.size());
//        System.out.println("Links: "+network.links.size());
        System.out.println("Links: " + network.nrLinks);
        return network;
    }
}
