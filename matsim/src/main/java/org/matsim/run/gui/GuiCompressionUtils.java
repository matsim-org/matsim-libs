
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author mrieser / Simunto GmbH
 */
/*package*/ class GuiCompressionUtils {

	private final static Logger log = LogManager.getLogger(GuiCompressionUtils.class);

	public static void compressFile() {
		JFileChooser chooser = new JFileChooser();
		int openResult = chooser.showOpenDialog(null);
		if (openResult == JFileChooser.APPROVE_OPTION) {
			File srcFile = chooser.getSelectedFile();

			chooser = new SaveFileSaver();
			chooser.setSelectedFile(new File(srcFile.getParentFile(), srcFile.getName() + ".zst"));
			chooser.addChoosableFileFilter(new FileNameExtensionFilter("GZip compressed (*.gz)", "gz"));
			chooser.addChoosableFileFilter(new FileNameExtensionFilter("ZStandard compressed (*.zst)", "zst"));
			chooser.addChoosableFileFilter(new FileNameExtensionFilter("LZ4 compressed (*.lz4)", "lz4"));
			int saveResult = chooser.showSaveDialog(null);
			if (saveResult == JFileChooser.APPROVE_OPTION) {
				File destFile = chooser.getSelectedFile();

				doFileTransform(srcFile, destFile);
			}
		}
	}

	public static void uncompressFile() {
		JFileChooser chooser = new JFileChooser();
		int openResult = chooser.showOpenDialog(null);
		if (openResult == JFileChooser.APPROVE_OPTION) {
			File srcFile = chooser.getSelectedFile();

			chooser = new SaveFileSaver();
			String destFilename = srcFile.getName()
				.replace(".gz", "")
				.replace(".zst", "")
				.replace(".lz4", "");
			chooser.setSelectedFile(new File(srcFile.getParentFile(), destFilename));
			int saveResult = chooser.showSaveDialog(null);
			if (saveResult == JFileChooser.APPROVE_OPTION) {
				File destFile = chooser.getSelectedFile();
				doFileTransform(srcFile, destFile);
			}
		}
	}

	private static void doFileTransform(final File srcFile, final File destFile) {
		AsyncFileInputProgressDialog gui = new AsyncFileInputProgressDialog();
		new Thread(() -> {
			try (InputStream srcStream = IOUtils.getInputStream(srcFile.toURI().toURL());
			     OutputStream destStream = IOUtils.getOutputStream(destFile.toURI().toURL(), false);
			) {
				srcStream.transferTo(destStream);
				SwingUtilities.invokeLater(gui::dispose);
			} catch (IOException e) {
				log.error(e.getMessage(), e);
				SwingUtilities.invokeLater(gui::dispose);
				SwingUtilities.invokeLater(
						() -> JOptionPane.showMessageDialog(null, e.getMessage(), "Error while (un)compressing file)",
								JOptionPane.ERROR_MESSAGE));
			}
		}, "file-transformer").start();

	}

	public static void main(String[] args) throws Throwable {
		SwingUtilities.invokeLater(() -> {
			final JFrame frame = new JFrame();
			frame.setBounds(100, 100, 600, 500);
			frame.setVisible(true);
			compressFile();
			uncompressFile();
			frame.dispose();
		});
	}
}
