/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ConfigLoader.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.controller;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

class ConfigLoader implements Provider<Config> {

    private String configFileName;

    @Inject
    public ConfigLoader(@Named("configFileName") String configFileName) {
        this.configFileName = configFileName;
    }

    @Override
    public Config get() {
        return ConfigUtils.loadConfig(configFileName);
    }

}
