/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Graph.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
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

package org.matsim.guice;

import com.google.inject.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class Graph {

    public static void main(String[] args) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        String configFileToGraph = args[0];
        String moduleToGraph = args[1];
        String outputDirectory = args[2];

        Config configToGraph = ConfigUtils.loadConfig(configFileToGraph);
        configToGraph.controler().setOutputDirectory(outputDirectory);
        configToGraph.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        Class<?> moduleClass = Class.forName(moduleToGraph);
        Object module = moduleClass.newInstance();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintWriter out = new PrintWriter(baos);
        com.google.inject.Injector matsimInjector = Injector.createInjector(configToGraph, (AbstractModule) module);
        matsimInjector.getInstance(ControlerI.class);
        try {
            MyGrapher renderer = new MyGrapher();
            renderer.setRankdir("LR");
            renderer.setOut(out);
            renderer.graph(matsimInjector.getInstance(com.google.inject.Injector.class));

            File file = new File(outputDirectory +"/guice.dot");
            PrintWriter fileOut = new PrintWriter(file);
            String s = baos.toString("UTF-8");
            s = fixGrapherBug(s);
            s = hideClassPaths(s);
            fileOut.write(s);
            fileOut.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String hideClassPaths(String s) {
        s = s.replaceAll("\\w[a-z\\d_\\.]+\\.([A-Z][A-Za-z\\d_]*)", "");
        return s;
    }

    public static String fixGrapherBug(String s) {
        s = s.replaceAll("style=invis", "style=solid");
        return s;
    }

}
