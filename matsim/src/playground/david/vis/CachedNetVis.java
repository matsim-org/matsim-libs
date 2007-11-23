/* *********************************************************************** *
 * project: org.matsim.*
 * CachedNetVis.java
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

package playground.david.vis;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.matsim.utils.vis.netvis.NetVis;
import org.matsim.utils.vis.netvis.VisConfig;
import org.matsim.utils.vis.netvis.config.GeneralConfig;
import org.matsim.utils.vis.netvis.streaming.StreamConfig;
import org.matsim.utils.vis.netvis.visNet.DisplayNet;
import org.matsim.utils.vis.netvis.visNet.DisplayNetStateReader;

public class CachedNetVis extends NetVis {

	protected CachedNetVis(GeneralConfig generalConfig, VisConfig visConfig, String filePrefix) {
		super(generalConfig, visConfig, filePrefix);
		// TODO Auto-generated constructor stub
	}

	public CachedNetVis(String generalConfigFile, String visConfigFile, String filePrefix) {
		super(generalConfigFile, visConfigFile, filePrefix);
		// TODO Auto-generated constructor stub
	}

	@Override
    protected DisplayNetStateReader openNetVisReader(DisplayNet network, String filePrefix) {
        DisplayNetStateReader reader = null;

        if (filePrefix != null)
       	 try {
       		 reader = new DisplayCachedNetStateReader(network, filePrefix);
       		 reader.open();
       		 reader.toStart();
       	 } catch (Exception e) {
       		 System.err.println("There is a problem with the movie files: " + e);
       		 System.err.println("I will only display the network.");
       		 reader = null;
       	 }
       	 return reader;
    }
	   public static void main(String[] args) {
	    	if (args.length == 1) {
	   			String configFile = StreamConfig.getConfigFileName(args[0], FILE_SUFFIX);
	   			(new CachedNetVis(configFile, configFile, args[0])).run();
	    	} else if (args.length == 2)
	    		(new CachedNetVis(args[0], args[1], null)).run();
	    	else if (args.length == 3)
	    		(new CachedNetVis(args[0], args[1], args[2])).run();
	    	else if (args.length == 0) {
	    		chooseFileName2();
	    	} else {
	    		tellParameters();
	    	}
	    }
	   protected static void chooseFileName2() {
			JFileChooser fileDialog = new JFileChooser(".");
			String filename = "";
	    	fileDialog.addChoosableFileFilter(
	    		new FileFilter() {
	    	    @Override
						public boolean accept(File f) {
	    	      if (f.isDirectory()) return true;
	    	      return f.getName().endsWith("CONFIG.vis");
	    	    }
	    	    @Override
						public String getDescription () { return "CONFIG.vis"; }
	    	  });

	    	  fileDialog.setMultiSelectionEnabled(false);

			int ret = fileDialog.showOpenDialog(null);
			while (ret != JFileChooser.CANCEL_OPTION) {
				filename = fileDialog.getSelectedFile().getAbsolutePath();
				if (filename.endsWith("CONFIG.vis")) {
					String nameauxsuffix = filename.substring(0, filename.length() - 10);
					(new CachedNetVis(filename, filename, nameauxsuffix)).run();
					return;
				} else if (filename.endsWith(".xml")) {
					GeneralConfig gconf = new GeneralConfig(true,	filename);
					(new CachedNetVis(gconf, VisConfig.newDefaultConfig(), null)).run();
					return;
				} else {
					JOptionPane.showMessageDialog(null,
							"Das scheint kein CONFIG.vis oder XML-Netzwerk zu sein!\n Bitte erneut wählen!");
					ret = fileDialog.showOpenDialog(null);
				}
			}
	    }

}
