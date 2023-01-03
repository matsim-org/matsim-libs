
/* *********************************************************************** *
 * project: org.matsim.*
 * GUnZipper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.run.gui;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author mrieser / Senozon AG
 */
/*package*/ class GUnZipper {

	private final static Logger log = LogManager.getLogger(GUnZipper.class);

	public static void gzipFile() {
		JFileChooser chooser = new JFileChooser();
		int openResult = chooser.showOpenDialog(null);
		if (openResult == JFileChooser.APPROVE_OPTION) {
			File srcFile = chooser.getSelectedFile();

			chooser = new SaveFileSaver();
			chooser.setSelectedFile(new File(srcFile.getParentFile(), srcFile.getName() + ".gz"));
			int saveResult = chooser.showSaveDialog(null);
			if (saveResult == JFileChooser.APPROVE_OPTION) {
				File destFile = chooser.getSelectedFile();

				doGzip(srcFile, destFile);
			}
		}
	}

	public static void gunzipFile() {
		JFileChooser chooser = new JFileChooser();
		int openResult = chooser.showOpenDialog(null);
		if (openResult == JFileChooser.APPROVE_OPTION) {
			File srcFile = chooser.getSelectedFile();

			chooser = new SaveFileSaver();
			chooser.setSelectedFile(new File(srcFile.getParentFile(), srcFile.getName().replace(".gz", "")));
			int saveResult = chooser.showSaveDialog(null);
			if (saveResult == JFileChooser.APPROVE_OPTION) {
				File destFile = chooser.getSelectedFile();
				doGunzip(srcFile, destFile);
			}
		}
	}

	private static void doGzip(final File srcFile, final File destFile) {
		AsyncFileInputProgressDialog gui = new AsyncFileInputProgressDialog();
		new Thread(() -> {
			try (FileInputStream srcStream = new FileInputStream(srcFile);
					FileOutputStream destStream = new FileOutputStream(destFile);
					BufferedInputStream bSrcStream = new BufferedInputStream(srcStream, 4 * 1024 * 1024);
					BufferedOutputStream bDestStream = new BufferedOutputStream(
							new GZIPOutputStream(destStream, 64 * 1024), 4 * 1024 * 1024)) {
				gui.observeProgress(srcStream);
				doCopy(bSrcStream, bDestStream);
				SwingUtilities.invokeLater(gui::dispose);
			} catch (IOException e) {
				log.error(e.getMessage(), e);
				SwingUtilities.invokeLater(gui::dispose);
				SwingUtilities.invokeLater(
						() -> JOptionPane.showMessageDialog(null, e.getMessage(), "Error while gzipping",
								JOptionPane.ERROR_MESSAGE));
			}
		}, "gzipper").start();
	}

	private static void doGunzip(final File srcFile, final File destFile) {
		AsyncFileInputProgressDialog gui = new AsyncFileInputProgressDialog();
		new Thread(() -> {
			try (FileInputStream srcStream = new FileInputStream(srcFile);
					FileOutputStream destStream = new FileOutputStream(destFile);
					BufferedInputStream bSrcStream = new BufferedInputStream(new GZIPInputStream(srcStream, 64 * 1024),
							4 * 1024 * 1024);
					BufferedOutputStream bDestStream = new BufferedOutputStream(destStream, 4 * 1024 * 1024)) {
				gui.observeProgress(srcStream);
				doCopy(bSrcStream, bDestStream);
				SwingUtilities.invokeLater(gui::dispose);
			} catch (IOException e) {
				log.error(e.getMessage(), e);
				SwingUtilities.invokeLater(gui::dispose);
				SwingUtilities.invokeLater(
						() -> JOptionPane.showMessageDialog(null, e.getMessage(), "Error while gunzipping",
								JOptionPane.ERROR_MESSAGE));
			}
		}, "gunzipper").start();

	}

	private static void doCopy(InputStream src, OutputStream dest) throws IOException {
		byte[] buffer = new byte[64 * 1024];
		int bytesRead;
		while ((bytesRead = src.read(buffer)) != -1) {
			dest.write(buffer, 0, bytesRead);
		}
		dest.flush();
	}

	public static void main(String[] args) throws Throwable {
		SwingUtilities.invokeLater(() -> {
			final JFrame frame = new JFrame();
			frame.setBounds(100, 100, 600, 500);
			frame.setVisible(true);
			System.out.println("let's go");
			gzipFile();
			System.out.println("let's continue");
			gunzipFile();
			System.out.println("and we're done");
			frame.dispose();
		});
	}
}
