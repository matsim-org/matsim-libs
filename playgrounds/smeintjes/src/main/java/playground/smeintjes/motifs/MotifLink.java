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
package playground.smeintjes.motifs;

import java.util.HashMap;
import java.util.Map;
import playground.smeintjes.network.LinkType;

/**
 * Represents a motif link with all related attributes
 */
public class MotifLink {

    public LinkType linkType;
    boolean direction;
    public int motifLinkID;
    static Map<Integer, MotifLink> linkIDToMotifLink = new HashMap<Integer, MotifLink>();
    static int nrLinkIDs = 0;

    public MotifLink(LinkType linkType, boolean direction) {
        this.linkType = linkType;
        this.direction = direction;
        motifLinkID = nrLinkIDs++;
        linkIDToMotifLink.put(motifLinkID, this);
    }

    @Override
    public String toString() {
        if (direction) {
            return ("" + linkType.getAbr()).toUpperCase();
        }
        return ("" + linkType.getAbr()).toLowerCase();
    }

    public static void clear() {
        nrLinkIDs = 0;
        linkIDToMotifLink = new HashMap<Integer, MotifLink>();
    }
    
    public static int getNrLinkIDs() {
        return nrLinkIDs;
    }

    public LinkType getLinkType() {
        return linkType;
    }

    public boolean isDirection() {
        return direction;
    }

    public int getMotifLinkID() {
        return motifLinkID;
    }
    
}
