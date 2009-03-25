/* *********************************************************************** *
 * project: org.matsim.*
 * OTFFileSettingSaver.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.utils.vis.otfvis.opengl.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.filechooser.FileFilter;

import org.matsim.core.gbl.Gbl;
import org.matsim.utils.vis.otfvis.gui.OTFVisConfig;
import org.matsim.utils.vis.otfvis.interfaces.OTFSettingsSaver;

import de.schlichtherle.io.ArchiveDetector;
import de.schlichtherle.io.DefaultArchiveDetector;

public class OTFFileSettingsSaver implements OTFSettingsSaver {
	String fileName;
	
	public OTFFileSettingsSaver(String filename) {
		this.fileName = filename;
	}

	public File chooseFile(boolean saveIt) {
		File erg = null;
		File selFile = new File(fileName + ".vcfg");
		JFileChooser fc;

		String path = selFile.getParent();
		
		fc = new JFileChooser(path);
		fc.setSelectedFile( selFile );
		if(saveIt)fc.setDialogType(JFileChooser.SAVE_DIALOG);
		
	    fc.setFileFilter( new FileFilter() 
	    { 
	      @Override public boolean accept( File f ) 
	      { 
	        return f.isDirectory() || 
	          f.getName().toLowerCase().endsWith( ".vcfg" ); 
	      } 
	      @Override public String getDescription() 
	      { 
	        return "OTFVis Config File (*.vcfg)"; 
	      } 
	    } ); 
	 
	    int state = saveIt ? fc.showSaveDialog( null ) : fc.showOpenDialog( null ); 
	 
	    if ( state == JFileChooser.APPROVE_OPTION ) 
	    { 
	    	erg = fc.getSelectedFile(); 
	    }  else {  
	      System.out.println( "Auswahl abgebrochen" );
	    }
	    
	    return erg;
	}

	public void openAndReadConfigFromFile(File file) {
    	ObjectInputStream inFile;
    	if(file == null) return;
		try {
			inFile = new ObjectInputStream(new FileInputStream(file));
			Gbl.getConfig().removeModule(OTFVisConfig.GROUP_NAME);
			Gbl.getConfig().addModule(OTFVisConfig.GROUP_NAME, (OTFVisConfig)inFile.readObject());
			//redrwaw
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	 }

	public OTFVisConfig openAndReadConfig() {
		ZipFile zipFile;
		ObjectInputStream inFile;
		// open file
		try {
			File sourceZipFile = new File(fileName);
			// Open Zip file for reading
			zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ);
			ZipEntry infoEntry = zipFile.getEntry("config.bin");
			if(infoEntry != null) {
				//load config settings
				inFile = new ObjectInputStream(zipFile.getInputStream(infoEntry));
				Gbl.getConfig().removeModule(OTFVisConfig.GROUP_NAME);
				Gbl.getConfig().addModule(OTFVisConfig.GROUP_NAME, (OTFVisConfig)inFile.readObject());
			} 
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		// Test if loading worked, otherwise create default
		if(Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME) == null) {
			Gbl.getConfig().addModule(OTFVisConfig.GROUP_NAME, new OTFVisConfig());
		}
		OTFVisConfig conf = (OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);
		conf.clearModified();
		return conf;
	}		

	private void openAndSaveConfig() {
		// We have to use truezip API here as Java does not UPDATE zip files correctly
		OTFVisConfig config = (OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);
		try {
			de.schlichtherle.io.File.setDefaultArchiveDetector(new DefaultArchiveDetector(
			        ArchiveDetector.NULL, // delegate
			        new String[] {
			            "mvi", "de.schlichtherle.io.archive.zip.JarDriver",
			        }));
			de.schlichtherle.io.File ipF = new de.schlichtherle.io.File(fileName);
			// we somehow have to wait here till file is not in use from PRECHACHING anymore
			if(!ipF.canWrite()) {
	    		final JDialog d = new JDialog((JFrame)null,"MVI File is read-only", true);
	    		JLabel field = new JLabel("Can not access .MVI!\n Maybe it is in use by pre-caching.\n Please try again when loading is finished.");
	    		JButton ok = new JButton("Ok");
	    	    ActionListener al =  new ActionListener() { 
	    	        public void actionPerformed( ActionEvent e ) {
	    	        	d.setVisible(false);
	    	        	
	    	      } }; 
	    	      ok.addActionListener(al);
	    	      d.getContentPane().setLayout( new FlowLayout() );
		    		d.getContentPane().add(field);
		    		d.getContentPane().add(ok);
		    		d.doLayout();
	    		d.pack();
	    		d.setVisible(true);
	    		de.schlichtherle.io.File.umount(true);
	    		return;
			}
			OutputStream out = new de.schlichtherle.io.FileOutputStream(fileName + "/config.bin");
			try {
				ObjectOutputStream outFile = new ObjectOutputStream(out);
				config.clearModified(); //the saved version should be cleared already
				outFile.writeObject(config);
			} finally {
			    out.close(); // ALWAYS close the stream!
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void saveSettings() {
		openAndSaveConfig();
	}
	public void saveSettingsAs() {
		OTFVisConfig config = (OTFVisConfig)Gbl.getConfig().getModule(OTFVisConfig.GROUP_NAME);
		File file = chooseFile(true);
		if(file != null){
			OutputStream out;
			try {
				out = new FileOutputStream(file);
				ObjectOutputStream outFile = new ObjectOutputStream(out);
				config.clearModified(); //the saved version should be cleared already
				outFile.writeObject(config);
				out.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public void readSettings() {
    	File file = chooseFile(false);
		openAndReadConfigFromFile(file);
	}

	public void readDefaultSettings() {
		File file = new File(fileName + ".vcfg");
		if(file.exists())openAndReadConfigFromFile(file);
	}
}


