
/* *********************************************************************** *
 * project: org.matsim.*
 * AsyncFileInputProgressDialog.java
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

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A dialog showing the progress on how far a {@link FileInputStream} has been consumed
 * asynchronously.
 *
 * @author mrieser / Senozon AG
 */
/*package*/ class AsyncFileInputProgressDialog extends JDialog {

	private final static Logger log = LogManager.getLogger(AsyncFileInputProgressDialog.class);

	private static final long serialVersionUID = 1L;

	final JProgressBar progressbar;

	public AsyncFileInputProgressDialog() {
		this("Operation in Progress…");
	}

	public AsyncFileInputProgressDialog(final String title) {
		setTitle(title);
		progressbar = new JProgressBar(0, 1000);
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
						.addContainerGap()
						.addComponent(progressbar, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
						.addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
						.addContainerGap()
						.addComponent(progressbar)
						.addContainerGap()));
		getContentPane().setLayout(groupLayout);

		this.setModal(false);
		this.setResizable(false);
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.pack();

		// center on screen
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int w = this.getSize().width;
		int h = this.getSize().height;
		int x = (dim.width - w) / 2;
		int y = (dim.height - h) / 2;
		this.setLocation(x, y);

		this.setVisible(true);
	}

	// safe to call from outside the event dispatch thread
	public void observeProgress(FileInputStream fis) {
		Thread t = new Thread(() -> {
			FileChannel ch = fis.getChannel();
			while (ch.isOpen()) {
				try {
					long size = ch.size();
					long pos = ch.position();
					final int progress = (int)((((double)pos) / ((double)size)) * 1000.0);
					SwingUtilities.invokeLater(() -> progressbar.setValue(progress));
					Thread.sleep(250);
				} catch (InterruptedException | IOException e) {
					log.error(e.getMessage(), e);
				}
			}
		}, "ProgressObserver");
		t.setDaemon(true);
		t.start();
	}
}
