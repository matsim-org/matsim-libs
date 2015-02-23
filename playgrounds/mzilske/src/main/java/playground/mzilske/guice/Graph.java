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

package playground.mzilske.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.grapher.graphviz.GraphvizGrapher;
import com.google.inject.grapher.graphviz.GraphvizModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class Graph {

    public static void main(String[] args) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        final PrintWriter out = new PrintWriter(baos);
        Config config = ConfigUtils.loadConfig("examples/equil/config.xml");
        config.controler().setLastIteration(0);
        final Controler controler = new Controler(config);
        controler.addControlerListener(new ShutdownListener() {
            @Override
            public void notifyShutdown(ShutdownEvent event) {
                try {
                    Injector injector = Guice.createInjector(new GraphvizModule());
                    GraphvizGrapher renderer = injector.getInstance(GraphvizGrapher.class);
                    renderer.setRankdir("TB");
                    renderer.setOut(out);
                    renderer.graph(controler.getInjector().getInstance(Injector.class));

                    File file = new File("output/wurst.dot");
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
        });
        controler.setOverwriteFiles(true);
        controler.run();
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
