/* *********************************************************************** *
 * project: org.matsim.*
 * VisConfig.java
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

import org.matsim.utils.vis.netvis.config.ConfigModule;


public class VisConfig extends ConfigModule {

    // -------------------- CONSTANTS --------------------

    public static final String MODULE_NAME = "vis";

    public static final String LOGO = "logo";

    public static final String DELAY = "delay";

    public static final String LINK_WIDTH_FACTOR = "linkwidthfactor";

    public static final String SHOW_NODE_LABELS = "shownodelabels";

    public static final String SHOW_LINK_LABELS = "showlinklabels";

    public static final String USE_ANTI_ALIASING = "antialiasing";

    // -------------------- CONSTRUCTION --------------------

    public VisConfig(String fileName) {
        super(MODULE_NAME, fileName);
    }

    public static VisConfig newDefaultConfig() {
        return new VisConfig();
    }

    private VisConfig() {
        super(MODULE_NAME);
        set(LOGO, "MATSim");
        set(DELAY, "500");
        set(LINK_WIDTH_FACTOR, "20");
        set(SHOW_NODE_LABELS, "false");
        set(SHOW_LINK_LABELS, "false");
        set(USE_ANTI_ALIASING, "true");
    }

    @Override
		public boolean isComplete() {
        return true; // don't need anything
    }

    // -------------------- CACHING --------------------

    private int delay_ms;

    private int linkWidthFactor;

    private boolean showNodeLabels;

    private boolean showLinkLabels;

    private boolean useAntiAliasing;

    @Override
    protected void cache(String name, String value) {
        if (DELAY.equals(name))
            delay_ms = Integer.parseInt(value);
        else if (LINK_WIDTH_FACTOR.equals(name))
            linkWidthFactor = Integer.parseInt(value);
        else if (SHOW_NODE_LABELS.equals(name))
            showNodeLabels = Boolean.parseBoolean(value);
        else if (SHOW_LINK_LABELS.equals(name))
            showLinkLabels = Boolean.parseBoolean(value);
        else if (USE_ANTI_ALIASING.equals(name))
            useAntiAliasing = Boolean.parseBoolean(value);
    }

    public int getDelay_ms() {
        return delay_ms;
    }

    public int getLinkWidthFactor() {
        return linkWidthFactor;
    }

    public boolean showNodeLabels() {
        return showNodeLabels;
    }

    public boolean showLinkLabels() {
        return showLinkLabels;
    }

    public boolean useAntiAliasing() {
        return useAntiAliasing;
    }

    // -------------------- GETTERS --------------------

    public String getLogo() {
        return get(LOGO);
    }

}
