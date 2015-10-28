/* *********************************************************************** *
 * project: org.matsim.*
 * RoadClosuresEditor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.evacuation.analysis;

import org.matsim.contrib.evacuation.analysis.control.EventHandler;
import org.matsim.contrib.evacuation.analysis.control.EventReaderThread;
import org.matsim.contrib.evacuation.analysis.data.ColorationMode;
import org.matsim.contrib.evacuation.analysis.data.EventData;
import org.matsim.contrib.evacuation.analysis.gui.AbstractDataPanel;
import org.matsim.contrib.evacuation.analysis.gui.KeyPanel;
import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.model.AbstractModule;
import org.matsim.contrib.evacuation.model.AbstractToolBox;
import org.matsim.contrib.evacuation.model.Constants;
import org.matsim.contrib.evacuation.model.Constants.Mode;
import org.matsim.contrib.evacuation.model.imagecontainer.BufferedImageContainer;
import org.matsim.contrib.evacuation.model.process.*;
import org.matsim.contrib.evacuation.view.DefaultWindow;
import org.matsim.contrib.evacuation.view.renderer.GridRenderer;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.Vehicle2DriverEventHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

public class EvacuationAnalysis extends AbstractModule {

    private ArrayList<File> eventFiles;
    private File currentEventFile;
    private EventHandler eventHandler;
    private ColorationMode colorationMode = ColorationMode.GREEN_YELLOW_RED;
    private float cellTransparency = 0.6f;
    private int k = 5;
    private GridRenderer gridRenderer;
    private int gridRendererID;
    private AbstractDataPanel graphPanel;
    private KeyPanel keyPanel;
    private Thread readerThread;
    private Mode mode;

    private EAToolBox toolBox;

    private double gridSize = 10;
    private boolean useCellCount = true;

    public EvacuationAnalysis(Controller controller) {
        super(controller.getLocale().moduleEvacuationAnalysis(), Constants.ModuleType.ANALYSIS, controller);

        // disable all layers
        this.processList.add(new DisableLayersProcess(controller));

        // initialize Matsim config
        this.processList.add(new InitMatsimConfigProcess(controller));

        // check if the default render panel is set
        this.processList.add(new InitMainPanelProcess(controller));

        // check if there is already a map viewer running, or just (re)set
        // center position
        this.processList.add(new InitMapLayerProcess(controller));

        // set module listeners
        this.processList.add(new SetModuleListenerProcess(controller, this, new EAEventListener(controller)));

        // add grid renderer
        this.processList.add(new BasicProcess(controller) {
            @Override
            public void start() {
                if (!this.controller.hasGridRenderer()) {
                    gridRenderer = new GridRenderer(controller);
                    gridRendererID = gridRenderer.getId();
                    this.controller.addRenderLayer(gridRenderer);
                }
            }

        });

        // add toolbox
        this.processList.add(new SetToolBoxProcess(controller, getToolBox()));

        // enable all layers
        this.processList.add(new EnableLayersProcess(controller));

        // enable grid renderer
        this.processList.add(new BasicProcess(controller) {
            @Override
            public void start() {
                toolBox.setGridRenderer(gridRenderer);
                readEvents();

                // finally: enable all layers
                controller.enableMapRenderer();

                controller.setToolBoxVisible(true);

                gridRenderer.setEnabled(true);
            }

        });

    }

    public static void main(String[] args) {
        // set up controller and image interface
        final Controller controller = new Controller();
        BufferedImage image = new BufferedImage(width - border * 2, height - border * 2, BufferedImage.TYPE_INT_ARGB);
        BufferedImageContainer imageContainer = new BufferedImageContainer(image, border);
        controller.setImageContainer(imageContainer);

        // inform controller that this module is running stand alone
        controller.setStandAlone(true);

        // instantiate evacuation area selector
        EvacuationAnalysis evacAnalysis = new EvacuationAnalysis(controller);

        // create default window for running this module standalone
        DefaultWindow frame = new DefaultWindow(controller);

        // set parent component to forward the (re)paint event
        controller.setParentComponent(frame);
        controller.setMainPanel(frame.getMainPanel(), true);

        // start the process chain
        evacAnalysis.start();
        frame.requestFocus();

    }

    @Override
    public AbstractToolBox getToolBox() {
        if (this.toolBox == null) {
            this.toolBox = new EAToolBox(this, this.controller);
        }
        return this.toolBox;
    }


    public void runCalculation() {
        try {
            this.controller.getParentComponent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            if (currentEventFile != null) {
                readEvents();

                this.controller.paintLayers();

            }
        } finally {
            this.controller.getParentComponent().setCursor(Cursor.getDefaultCursor());
        }
    }

    public void setCellTransparency(float cellTransparency) {
        this.cellTransparency = cellTransparency;
    }

    public void readEvents() {
        if (currentEventFile == null) {
            eventFiles = getAvailableEventFiles(this.controller.getIterationsOutputDirectory());
            currentEventFile = eventFiles.get(0);

            // check if empty
            if (eventFiles.isEmpty()) {
                JOptionPane.showMessageDialog(this.controller.getParentComponent(), "Could not find any event files", "Event files unavailable", JOptionPane.ERROR_MESSAGE);
                return;
            }
            else {
//                this.toolBox.getOpenOTFVisBtn().setEnabled(true);
            }
            ((EAToolBox) getToolBox()).setEventFileItems(eventFiles);
        }
        if ((graphPanel == null) || (keyPanel == null)) {
            graphPanel = ((EAToolBox) getToolBox()).getGraphPanel();
            keyPanel = ((EAToolBox) getToolBox()).getKeyPanel();
        }

        // run event reader
        runEventReader(currentEventFile);

        // get data from eventhandler (if not null)
        if (eventHandler != null) {
            eventHandler.setColorationMode(this.colorationMode);
            eventHandler.setTransparency(this.cellTransparency);
            eventHandler.setK(k);

            // get data
            EventData data = eventHandler.getData();

            // update data in both the map viewer and the graphs

            this.controller.setEventData(data);

            graphPanel.updateData(data);
            keyPanel.updateData(data);
        }

        ((EAToolBox) getToolBox()).setFirstLoad(false);

    }

    public void runEventReader(File eventFile) {

        this.eventHandler = null;
        EventsManager e = EventsUtils.createEventsManager();
        EventsReaderXMLv1 reader = new EventsReaderXMLv1(e);
        this.readerThread = new Thread(new EventReaderThread(reader, eventFile.toString()), "readerthread");
        this.eventHandler = new EventHandler(useCellCount, eventFile.getName(), this.controller.getScenario(), this.gridSize, this.readerThread);
        e.addHandler(this.eventHandler);
        this.readerThread.run();

    }

    public File getEventPathFromName(String selectedItem) {
        for (File eventFile : eventFiles) {
            if (eventFile.getName().equals(selectedItem)) {
                return eventFile;
            }
        }
        return null;
    }

    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public File getCurrentEventFile() {
        return this.currentEventFile;
    }

    public void setCurrentEventFile(File currentEventFile) {
        this.currentEventFile = currentEventFile;
    }

    public GridRenderer getGridRenderer() {
        return gridRenderer;
    }

    public int getGridRendererID() {
        return gridRendererID;
    }

    public ArrayList<File> getAvailableEventFiles(String dirString) {
        File dir = new File(dirString);
        Stack<File> directoriesToScan = new Stack<File>();
        ArrayList<File> files = new ArrayList<File>();

        directoriesToScan.add(dir);

        while (!directoriesToScan.isEmpty()) {
            File currentDir = directoriesToScan.pop();
            File[] filesToCheck = currentDir.listFiles(new EventFileFilter());

            if (filesToCheck.length > 0) {

                for (File currentFile : filesToCheck) {
                    if (currentFile.isDirectory()) {
                        directoriesToScan.push(currentFile);
                    }
                    else {
                        if (!files.contains(currentFile)) {
                            files.add(currentFile);
                        }
                    }
                }
            }

        }

        return files;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;

        this.gridRenderer.setMode(mode);
        if (keyPanel != null) {
            keyPanel.setMode(mode);
        }

    }

    public void setGraphPanel(AbstractDataPanel graphPanel) {
        this.graphPanel = graphPanel;

    }

    public void setKeyPanel(KeyPanel keyPanel) {
        this.keyPanel = keyPanel;

    }

    public void setGridSize(double gridSize) {
        this.gridSize = gridSize;

    }

    class EventFileFilter implements java.io.FileFilter {

        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            return f.getName().endsWith(".events.xml.gz");
        }
    }

}
