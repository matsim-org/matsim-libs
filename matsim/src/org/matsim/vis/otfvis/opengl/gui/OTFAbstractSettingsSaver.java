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

package org.matsim.vis.otfvis.opengl.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.gui.OTFVisConfig;
import org.matsim.vis.otfvis.interfaces.OTFSettingsSaver;

import de.schlichtherle.io.ArchiveDetector;
import de.schlichtherle.io.DefaultArchiveDetector;

/**
 * OTFFileSettingsSaver is responsible for saving and 
 * retrieving the OTFVisConfig settings in either binary or XML format.
 * 
 * @author dstrippgen
 *
 */
public abstract class OTFAbstractSettingsSaver implements OTFSettingsSaver {
	String fileName;

	private OTFVisConfig visConfig;
	
	private static final Logger log = Logger.getLogger(OTFAbstractSettingsSaver.class);
	
	private interface Writer {
		void writeObject(Object o) throws IOException;
		void close()throws IOException;
	}
	
	private static class BinWriter extends ObjectOutputStream implements Writer{

		public BinWriter(OutputStream out) throws IOException {
			super(out);
		}
	}

	private static class XMLWriter extends XMLEncoder implements Writer{

		public XMLWriter(OutputStream out) {
			super(out);
		}
	}
		
	public OTFAbstractSettingsSaver(OTFVisConfig visconf, String filename) {
		this.fileName = filename;
		this.visConfig = visconf;
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
	    fc.setFileFilter( new FileFilter() 
	    { 
	      @Override public boolean accept( File f ) 
	      { 
	        return f.isDirectory() || 
	          f.getName().toLowerCase().endsWith( ".vxmlcfg" ); 
	      } 
	      @Override public String getDescription() 
	      { 
	        return "OTFVis XML Config File (*.vxmlcfg)"; 
	      } 
	    } ); 
	 
	    int state = saveIt ? fc.showSaveDialog( null ) : fc.showOpenDialog( null ); 
	 
	    if ( state == JFileChooser.APPROVE_OPTION ) 
	    { 
	    	erg = fc.getSelectedFile(); 
	    }  else {  
	      log.info( "Auswahl abgebrochen" );
	    }
	    
	    return erg;
	}

	public void openAndReadConfigFromFile(File file) {
    	ObjectInputStream inFile;
    	if(file == null) return;
		try {
			inFile = new ObjectInputStream(new FileInputStream(file));
			this.visConfig = (OTFVisConfig)inFile.readObject();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	 }

	

	private void openAndSaveConfig() {
		// We have to use truezip API here as Java does not UPDATE zip files correctly
		OTFVisConfig config = this.visConfig;
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
			out = new de.schlichtherle.io.FileOutputStream(fileName + "/config.xml");
			try {
				  // Create XML encoder.
				XMLEncoder xenc = new XMLEncoder(out);
				xenc.writeObject(config);
				xenc.close();
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
		OTFVisConfig config = this.visConfig;
		File file = chooseFile(true);
		if(file != null){
			OutputStream out;
			try {
				boolean asXML = file.getAbsolutePath().endsWith("vxmlcfg");
				out = new FileOutputStream(file);
				Writer outFile = asXML ? new XMLWriter(out) : new BinWriter(out);
				config.clearModified(); //the saved version should be cleared already
				outFile.writeObject(config);
				outFile.close();
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public void readSettings() {
    	File file = chooseFile(false);
		openAndReadConfigFromFile(file);
	}


}


