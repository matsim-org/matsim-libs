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

import java.util.HashMap;
import java.util.Map;
import playground.smeintjes.motifs.MotifLink;

/**
 * Represents a link type in the graph
 */
public class LinkType {

    MotifLink motifLink;
    MotifLink inverseMotifLink;
    static Map<Integer, LinkType> linkTypes = new HashMap<Integer, LinkType>();
    boolean directed;
    String description;
    int linkTypeID;
    char abr;
    String sourceNetwork;
    String destinationNetwork;

    public LinkType(boolean directed, String description, int linkTypeID, char abr, String sourceNetwork, String destinationNetwork) {
        this.directed = directed;
        this.description = description;
        this.linkTypeID = linkTypeID;
        this.abr = abr;
        this.sourceNetwork = sourceNetwork;
        this.destinationNetwork = destinationNetwork;
        if (directed) {
            motifLink = new MotifLink(this, true);
            inverseMotifLink = new MotifLink(this, false);
        } else {
            motifLink = new MotifLink(this, true);
            inverseMotifLink = motifLink;
        }
        linkTypes.put(linkTypeID, this);

    }
    public MotifLink getMotifLink() {
        return motifLink;
    }

    public MotifLink getInverseMotifLink() {
        return inverseMotifLink;
    }

    public boolean isDirected() {
        return directed;
    }

    public String getDescription() {
        return description;
    }

    public int getLinkTypeID() {
        return linkTypeID;
    }

    public char getAbr() {
        return abr;
    }

    public String getSourceNetwork() {
        return sourceNetwork;
    }

    public String getDestinationNetwork() {
        return destinationNetwork;
    }
    public static int getNrLinkTypes(){
        return linkTypes.size();
    }
}
