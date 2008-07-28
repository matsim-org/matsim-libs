/* *********************************************************************** *
 * project: org.matsim.*
 * IndexationConfig.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
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

package org.matsim.utils.vis.netvis.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.networks.basicNet.BasicLink;
import org.matsim.interfaces.networks.basicNet.BasicNet;
import org.matsim.interfaces.networks.basicNet.BasicNode;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author gunnar
 *
 */
public class IndexationConfig extends DefaultHandler {

    // -------------------- CLASS VARIABLES --------------------

  /**
   * xml element
   */
  public static final String CONFIG_ELEM = "config";

  /**
   * xml element
   */
  public static final String MODULE_ELEM = "module";

  /**
   * xml attribute
   */
  public static final String MODULE_NAME_ATTR = "name";

  public static final String MODULE_CLASS_ATTR = "class";

    public static final String CONFIG_NAME = "indexation";

    private static final String NETWORK_ELEM = "network";

    private static final String NODES_ELEM = "nodes";

    private static final String LINKS_ELEM = "links";

    private static final String NODE_ELEM = "node";

    private static final String LINK_ELEM = "link";

    private static final String NODE_ID_ATTR = "id";

    private static final String LINK_ID_ATTR = "id";


    // -------------------- MEMBER VARIABLES --------------------

    private BasicNet<BasicNode, BasicLink> network; // only for xml parsing

    private List<BasicNode> nodes;

    private List<BasicLink> links;

    private Map<BasicNode, Integer> node2index;

    private Map<BasicLink, Integer> link2index;

    private Map<Integer, BasicNode> index2node;

    private Map<Integer, BasicLink> index2link;

    // -------------------- CONSTRUCTION --------------------

    /**
     * Constructs this instance by mapping the indexation configuration
     * contained in <code>fileName</code> onto the elements contained in
     * <code>network</code>.
     * @param network
     * @param fileName
     */
    public IndexationConfig(BasicNet<BasicNode, BasicLink> network, String fileName) {
        this.network = network;
        read(fileName);
        this.network = null;
    }

    /**
     * Constructs this instance by internally indexing all elements of
     * <code>network</code>.
     * @param network
     */
    public IndexationConfig(BasicNet<BasicNode, BasicLink> network) {
        createNodeIndices(new ArrayList<BasicNode>(network.getNodes().values()));
        createLinkIndices(new ArrayList<BasicLink>(network.getLinks().values()));
    }

    private void createNodeIndices(List<BasicNode> nodes) {
        this.nodes = Collections.unmodifiableList(nodes);
        node2index = new LinkedHashMap<BasicNode, Integer>();
        index2node = new LinkedHashMap<Integer, BasicNode>();
        for (int i = 0; i < nodes.size(); i++) {
            BasicNode node = nodes.get(i);
            if (node != null) {
                node2index.put(node, i);
                index2node.put(i, node);
            }
        }
        node2index = Collections.unmodifiableMap(node2index);
        index2node = Collections.unmodifiableMap(index2node);
    }

    private void createLinkIndices(List<BasicLink> links) {
        this.links = Collections.unmodifiableList(links);
        link2index = new LinkedHashMap<BasicLink, Integer>();
        index2link = new LinkedHashMap<Integer, BasicLink>();
        for (int i = 0; i < links.size(); i++) {
            BasicLink link = links.get(i);
            if (link != null) {
                link2index.put(link, i);
                index2link.put(i, link);
            }
        }
        link2index = Collections.unmodifiableMap(link2index);
        index2link = Collections.unmodifiableMap(index2link);
    }

    // ---------- IMPLEMENTATION OF SubConfigI --------------------

    public String getName() {
        return CONFIG_NAME;
    }

    // -------------------- CONTENT ACCESS --------------------

    public List<BasicNode> getIndexedNodeView() {
        return nodes;
    }

    public List<BasicLink> getIndexedLinkView() {
        return links;
    }

    public BasicNode getNode(int index) {
        return index2node.get(index);
    }

    public BasicLink getLink(int index) {
        return index2link.get(index);
    }

    /**
     * @param node
     * @return node's (nonnegative) index and -1 if node is not contained or
     *         null
     */
    public int getIndex(BasicNode node) {
        if (node == null)
            return -1;

        Integer indexObj = node2index.get(node);
        if (indexObj != null)
            return indexObj;
        else
            return -1;
    }

    /**
     * @param link
     * @return link's (nonnegative) index and -1 if link is not contained or
     *         null
     */
    public int getIndex(BasicLink link) {
        if (link == null)
            return -1;

        Integer indexObj = link2index.get(link);
        if (indexObj != null)
            return indexObj;
        return -1;
    }

    // -------------------- READING --------------------

    void read(String fileName) {
        nodes = new ArrayList<BasicNode>();
        links = new ArrayList<BasicLink>();

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(new File(fileName), this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        createNodeIndices(nodes);
        createLinkIndices(links);
    }

    @Override
	public void startElement(String uri, String lName, String qName,
            Attributes attrs) {
        if (NODE_ELEM.equals(qName))
            startNode(attrs);
        else if (LINK_ELEM.equals(qName))
            startLink(attrs);
    }

    private void startNode(Attributes attrs) {
        BasicNode node = network.getNodes().get(new IdImpl(attrs.getValue(NODE_ID_ATTR)));
        if (node != null)
            nodes.add(node);
        else
            nodes.add(null);
    }

    private void startLink(Attributes attrs) {
        BasicLink link = network.getLinks().get(new IdImpl(attrs.getValue(LINK_ID_ATTR)));
        if (link != null)
            links.add(link);
        else
            links.add(null);
    }

    // -------------------- MISC --------------------

    public String asXmlSegment(int indentCnt) {
        String indent = ConfigModule.indent(indentCnt);
        String newline = System.getProperty("line.separator");
        String quote = "\"";

        StringBuffer result = new StringBuffer();

        result.append(indent + "<" + MODULE_ELEM + " " + MODULE_NAME_ATTR + "="
                + quote + CONFIG_NAME + quote + ">" + newline);

        result.append(indent + "\t<" + NETWORK_ELEM + ">" + newline);
        result.append(indent + "\t\t<" + NODES_ELEM + ">" + newline);

        for (BasicNode node : getIndexedNodeView()) {
            if (node != null)
                result.append(indent + "\t\t\t<" + NODE_ELEM + " "
                        + NODE_ID_ATTR + "=" + quote + node.getId().toString()
                        + quote + "/>" + newline);
            else
                result.append(indent + "\t\t\t<" + NODE_ELEM + " "
                        + NODE_ID_ATTR + "=" + quote + "UNKNOWN_NODE" + quote
                        + "/>" + newline);
        }

        result.append(indent + "\t\t</" + NODES_ELEM + ">" + newline);
        result.append(indent + "\t\t<" + LINKS_ELEM + ">" + newline);

        for (BasicLink link : getIndexedLinkView()) {
            if (link != null)
                result.append(indent + "\t\t\t<" + LINK_ELEM + " "
                        + LINK_ID_ATTR + "=" + quote + link.getId().toString()
                        + quote + "/>" + newline);
            else
                result.append(indent + "\t\t\t<" + LINK_ELEM + " "
                        + LINK_ID_ATTR + "=" + quote + "UNKNOWN_LINK" + quote
                        + "/>" + newline);
        }

        result.append(indent + "\t\t</" + LINKS_ELEM + ">" + newline);
        result.append(indent + "\t</" + NETWORK_ELEM + ">" + newline);
        result.append(indent + "</" + MODULE_ELEM + ">" + newline);

        return result.toString();
    }

    @Override
	public String toString() {
        return asXmlSegment(0);
    }

    /**
     * Checks if <code>indexConfig</code> indexes all nodes and links of
     * <code>net</code>. If the check is <code>strict</code>, all elements
     * indexed by this instance must also be contained in <code>net</code>.
     * @param net
     * @param strict
     * @return see description
     */
    public boolean indexes(BasicNet<BasicNode, BasicLink> net, boolean strict) {
        if (strict && net.getNodes().size() != nodes.size())
            return false;
        if (strict && net.getLinks().size() != links.size())
            return false;
        for (BasicNode node : net.getNodes().values()) {
            if (getIndex(node) < 0)
                return false;
        }
        for (BasicLink link : net.getLinks().values()) {
            if (getIndex(link) < 0)
                return false;
        }
        return true;
    }

    /**
     * Checks if <code>this</code> and <code>other</coder> represent the same
     * indexation.
     * @param other
     * @return see description
     */
    public boolean equals(IndexationConfig other) {
        if (this.nodes.size() != other.nodes.size())
            return false;
        for (int i = 0; i < this.nodes.size(); i++)
            if (this.nodes.get(i) == null && other.nodes.get(i) != null)
                return false;
            else if (!this.nodes.get(i).equals(other.nodes.get(i)))
                return false;

        if (this.links.size() != other.links.size())
            return false;
        for (int i = 0; i < this.links.size(); i++)
            if (this.links.get(i) == null && other.links.get(i) != null)
                return false;
            else if (!this.links.get(i).equals(other.links.get(i)))
                return false;

        return true;
    }

    @Override
	public boolean equals(Object o) {
        if (o instanceof IndexationConfig)
            return equals((IndexationConfig) o);
        else
            return false;
    }

}
