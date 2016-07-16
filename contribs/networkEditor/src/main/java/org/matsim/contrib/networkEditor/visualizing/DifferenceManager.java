/* *********************************************************************** *
 * project: org.matsim.contrib.networkEditor
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 Daniel Ampuero
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.networkEditor.visualizing;

import java.util.Stack;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

/**
 *
 * @author danielmaxx
 */
public class DifferenceManager {
    
    private Stack<Difference> back, front;
    Network net;
    public enum type{CREATE, EDIT, DELETE, SPLIT};
    
    DifferenceManager() {
        back = new Stack<Difference>();
        front = new Stack<Difference>();
    }
    DifferenceManager(Network net) {
        back = new Stack<Difference>();
        front = new Stack<Difference>();
        this.net = net;
    }


    public void saveState(Link link, type currentType, Node nodeAtStart, Node nodeAtEnd, Link linkAtStartAdded1, Link linkAtStartAdded2, Link linkAtEndAdded1, Link linkAtEndAdded2, Link linkAtStartDeleted, Link linkAtEndDeleted) {
        Difference diff = new Difference(link, currentType, net, nodeAtStart,  nodeAtEnd, linkAtStartAdded1, linkAtStartAdded2, linkAtEndAdded1, linkAtEndAdded2, linkAtStartDeleted, linkAtEndDeleted);
        back.push(diff);
        front.clear();
    }

    public void undo() {
        if(back.empty())
            return;
        back.peek().revert();
        front.push(back.pop());
    }

    public void redo() {
        if(front.empty())
            return;
        front.peek().apply();
        back.push(front.pop());
    }

    public int getBackSize() {
        return back.size();
    }

    public int getFrontSize() {
        return front.size();
    }

    public Link cloneLink(Link link) {
        final Network network = net;
	NetworkFactory r = net.getFactory();
	return NetworkUtils.createLink(link.getId(), link.getFromNode(), link.getToNode(), network, link.getLength(), link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
    }

    class Difference {        
        public Link link;
        public Network net;
        public Node nodeAtStart, nodeAtEnd;
        public Link linkAtStartAdded1, linkAtStartAdded2;
        public Link linkAtEndAdded1, linkAtEndAdded2;
        public Link linkAtStartDeleted, linkAtEndDeleted;
        public type currentType;

        Difference() {}
        Difference(Link link, type currentType, Network net, Node nAS, Node nAE, Link lASA1, Link lASA2, Link lAEA1, Link lAEA2, Link lASD, Link lAED) {
            this.link = link;
            this.net = net;
            this.currentType = currentType;
            nodeAtStart = nAS;
            nodeAtEnd = nAE;
            linkAtStartAdded1 = lASA1;
            linkAtStartAdded2 = lASA2;
            linkAtEndAdded1 = lAEA1;
            linkAtEndAdded2 = lAEA2;
            linkAtStartDeleted = lASD;
            linkAtEndDeleted = lAED;
        }
        
        Difference(Network net) {
            this.net = net;
        }
        public void revert() {
            //System.out.println("revert: " + currentType);
            if(currentType == type.CREATE)
                deleteLink();
            else if(currentType == type.EDIT)
                editLink();
            else if(currentType == type.DELETE)
                createLink();
        }
        public void apply() {
            //System.out.println("apply: " + currentType);
            if(currentType == type.CREATE)
                createLink();
            else if(currentType == type.EDIT)
                editLink();
            else if(currentType == type.DELETE)
                deleteLink();
        }

        private void deleteLink() {
            if(link != null)
                net.removeLink(link.getId());
            if(this.linkAtStartDeleted!=null)
                net.addLink(linkAtStartDeleted);
            if(this.linkAtEndDeleted!=null)
                net.addLink(linkAtEndDeleted);
            if(this.linkAtStartAdded1!=null)
                net.removeLink(linkAtStartAdded1.getId());
            if(this.linkAtStartAdded2!=null)
                net.removeLink(linkAtStartAdded2.getId());
            if(this.linkAtEndAdded1!=null)
                net.removeLink(linkAtEndAdded1.getId());
            if(this.linkAtEndAdded2!=null)
                net.removeLink(linkAtEndAdded2.getId());
            if(this.nodeAtStart!=null)
                net.removeNode(nodeAtStart.getId());
            if(this.nodeAtEnd!=null)
                net.removeNode(nodeAtEnd.getId());
        }

        private void editLink() {
            if(link == null) return;
            Link toEdit = net.getLinks().get(link.getId());
            Link oldLink = cloneLink(toEdit);
            toEdit.setCapacity(link.getCapacity());
            toEdit.setFreespeed(link.getFreespeed());
            toEdit.setLength(link.getLength());
            toEdit.setNumberOfLanes(link.getNumberOfLanes());
            link = oldLink;
        }

        private void createLink() {            
            if(this.linkAtStartDeleted!=null)
                net.removeLink(linkAtStartDeleted.getId());
            if(this.linkAtEndDeleted!=null)
                net.removeLink(linkAtEndDeleted.getId());
            if(this.nodeAtStart!=null)
                net.addNode(nodeAtStart);
            if(this.nodeAtEnd!=null)
                net.addNode(nodeAtEnd);
            if(this.linkAtStartAdded1!=null)
                net.addLink(linkAtStartAdded1);
            if(this.linkAtStartAdded2!=null)
                net.addLink(linkAtStartAdded2);
            if(this.linkAtEndAdded1!=null)
                net.addLink(linkAtEndAdded1);
            if(this.linkAtEndAdded2!=null)
                net.addLink(linkAtEndAdded2);
            if(link != null)
                net.addLink(link);
        }

    }

}
