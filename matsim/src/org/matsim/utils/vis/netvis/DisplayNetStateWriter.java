/* *********************************************************************** *
 * project: org.matsim.*
 * DisplayNetStateWriter.java
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

package org.matsim.utils.vis.netvis;

import java.util.Collection;

import org.matsim.interfaces.basic.v01.BasicLink;
import org.matsim.interfaces.basic.v01.BasicNetwork;
import org.matsim.interfaces.basic.v01.BasicNode;
import org.matsim.utils.vis.netvis.config.IndexationConfig;
import org.matsim.utils.vis.netvis.drawableNet.DrawableLinkI;
import org.matsim.utils.vis.netvis.drawableNet.DrawableNodeI;
import org.matsim.utils.vis.netvis.streaming.BufferedStateA;
import org.matsim.utils.vis.netvis.streaming.NetStateA;
import org.matsim.utils.vis.netvis.streaming.StateI;
import org.matsim.utils.vis.netvis.streaming.StreamWriterA;
import org.matsim.utils.vis.netvis.visNet.DisplayLinkState;
import org.matsim.utils.vis.netvis.visNet.DisplayNodeState;

/**
 * A helper class for writing of network visualizer streams. A subclass should
 * override at least one of the display value getter functions in order to
 * generate useful vis files.
 * <p>
 * <b>How to use this class:</b> (1) write subclass and override e.g.
 * <code>getLinkDisplValue(BasicLinkI)</code>; (2) create subclass; (3) call
 * <code>open()</code>; (4) call <code>dump(int)</code> arbitrarily often;
 * (5) call <code>close()</code>. Results can then be viewed via
 * <code>NetVis</code>'s main function.
 *
 * @author gunnar
 *
 */
public class DisplayNetStateWriter extends StreamWriterA {

    // -------------------- CONSTRUCTION --------------------

    public DisplayNetStateWriter(BasicNetwork network,
            String networkFileName, String filePrefix,
            int timeStepLength_s, int bufferSize) {
        super(network, networkFileName, new IndexationConfig(network),
                filePrefix, NetVis.FILE_SUFFIX, timeStepLength_s, bufferSize);
    }

    public DisplayNetStateWriter(BasicNetwork network,
            String networkFileName, VisConfig visConfig,
            String filePrefix, int timeStepLength_s, int bufferSize) {
        this(network, networkFileName, filePrefix, timeStepLength_s, bufferSize);
        super.addConfig(visConfig);
    }

    /*
     * -------------------- MINIMAL INTERFACE DEFINITION --------------------
     *
     * Overriding this function suffices to paint nodes and links with colors
     * and to write labels on top of them. Only functions of interest need to be
     * overridden.
     */

    /**
     * Is expected to return the value to be displayed for <code>node</code>
     * in color-coding.
     */
    protected double getNodeDisplValue(BasicNode node) {
        return 0;
    }

    /**
     * Is expected to return the single value to be displayed for
     * <code>link</code> in color-coding.
     */
    protected double getLinkDisplValue(BasicLink link) {
        return 0;
    }

    /**
     * Is expected to return a <code>String</code> to be displayed on top of
     * the passed <code>node</code>.
     */
    protected String getNodeDisplLabel(BasicNode node) {
        return "";
    }

    /**
     * Is expected to return a <code>String</code> to be displayed on top of
     * the passed <code>link</code>.
     */
    protected String getLinkDisplLabel(BasicLink link) {
        return "";
    }

    /*
     * -------------------- INTERFACE DEFINITION --------------------
     *
     * Extended functionality that allows for drawing of individual cells.
     * Overriding these functions invalidates the simple link coloring functions
     * given above.
     */

    /**
     * Is expected to return the number of values to be displayed for
     * <code>link</code> in color-coding. Returns <code>1</code> if not
     * overridden.
     */
    protected int getLinkDisplValueCnt(BasicLink link) {
        return 1;
    }

    /**
     * Is expected to return the (<code>index+1</code>)st value to be
     * displayed for <code>link</code> in color-coding.
     */
    protected double getLinkDisplValue(BasicLink link, int index) {
        return getLinkDisplValue(link);
    }

    /**
     * Is expected to return a collection of <code>DrawableAgentI</code>
     * instances or <code>null</code>, if no agents are to be drawn.
     */
    protected Collection<? extends DrawableAgentI> getAgentsOnLink(
            BasicLink link) {
        return null;
    }

    // ----------------- IMPLEMENTATION OF BasicStateWriter -----------------

    @Override
		protected final StateI newState() {
        return new MyNetworkState(getIndexConfig());
    }

    // -------------------- WRAPPERS AROUND STATE CLASSES --------------------

    private class MyNetworkState extends NetStateA {

        private MyNetworkState(IndexationConfig indexConfig) {
            super(indexConfig);
        }

        @Override
				protected BufferedStateA newNodeState(BasicNode node) {
            return new DisplayNodeState(new MyDrawableNode(node));
        }

        @Override
				protected BufferedStateA newLinkState(BasicLink link) {
            return new DisplayLinkState(new MyDrawableLink(link));
        }
    }

    private class MyDrawableNode implements DrawableNodeI {

        private BasicNode node;

        private MyDrawableNode(BasicNode node) {
            this.node = node;
        }

        public double getDisplayValue() {
            return getNodeDisplValue(node);
        }

        public String getDisplayText() {
            return getNodeDisplLabel(node);
        }

    }

    private class MyDrawableLink implements DrawableLinkI {

        private BasicLink link;

        private MyDrawableLink(BasicLink link) {
            this.link = link;
        }

        public int getDisplayValueCount() {
            return getLinkDisplValueCnt(link);
        }

        public double getDisplayValue(int index) {
            return getLinkDisplValue(link, index);
        }

        public String getDisplayText() {
            return getLinkDisplLabel(link);
        }

        public Collection<? extends DrawableAgentI> getMovingAgents() {
            return getAgentsOnLink(link);
        }

    }

}

// ############################################################
// ############################################################
// ############################################################

// --------------- IMPLEMENTATION OF SimWriterI ---------------

/**
 * Vanilla constructor for use without any further configuration.
 *
 * @param network
 *            The network to be displayed. Note that the network elements must
 *            implement interfaces as defined in <code>drawableNet000</code>
 *            package.
 * @param filePrefix
 *            the created binary file prefix (might contain entire path)
 * @param verbose
 *            if the writer is to act verbose
 * @param netFileName
 *            name of the displayed network file (must contain entire path)
 * @param netReader
 *            A class implementing <code>NetReaderI</code> that is able to
 *            read the network file. For MATSIM files, this is
 *            <code>MatsimNetReader_PLAIN</code>.
 * @param startTime_s
 *            the time at which writing starts (in seconds of day)
 * @param bufferSize
 *            The buffer size determines how many dumps() are written into one
 *            file.
 * @param timeStepLength_s
 *            the time interval between two calls to <code>dump()</code>
 */
// public DisplayNetStateWriter(BasicNetI network, String filePrefix,
// boolean verbose, String netFileName, Class netReader,
// int startTime_s, int bufferSize, int timeStepLength_s) {
// super(network, new GeneralConfig(verbose, netFileName, netReader),
// filePrefix, NetVis.FILE_SUFFIX);
//
// super.open(new TemporalConfig(startTime_s, Integer.MAX_VALUE,
// bufferSize, timeStepLength_s), new IndexationConfig(network));
// }
// /**
// * Does not have to be called.
// */
// public final void open() throws IOException {
// // Does nothing, since super.open(..) is already called in the
// // constructor.
// }
// public boolean isDue(int time_s) {
// return (time_s % super.getTemporalConfig().getTimeStepLength_s() == 0);
// }
/**
 * Does not have to be called.
 */
// public final void open(GeneralConfig generalConfig,
// TemporalConfig temporalConfig) {
// // do nothing: super.open(...) is already called in constructor
// }
/**
 *
 */
// public void dump(int time_s) throws IOException {
// super.dump(time_s);
// }
/**
 * To be called when writing is done!
 */
// public final void close() throws IOException {
// super.close();
// }
