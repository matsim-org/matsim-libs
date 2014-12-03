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

import org.apache.log4j.Logger;

/**
 * A dialog showing the progress on how far a {@link FileInputStream} has been consumed
 * asyncronously.
 * 
 * @author mrieser / Senozon AG
 */
/*package*/ class AsyncFileInputProgressDialog extends JDialog {

	private final static Logger log = Logger.getLogger(AsyncFileInputProgressDialog.class);
	
	private static final long serialVersionUID = 1L;
	
	public AsyncFileInputProgressDialog(final FileInputStream fis) {
		this(fis, "Operation in Progress…");
	}

	public AsyncFileInputProgressDialog(final FileInputStream fis, final String title) {
		setTitle(title);
		final JProgressBar progressbar = new JProgressBar(0, 1000);
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(progressbar, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(progressbar)
					.addContainerGap())
		);
		getContentPane().setLayout(groupLayout);
		
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				FileChannel ch = fis.getChannel();
				while (ch.isOpen()) {
					try {
						long size = ch.size();
						long pos = ch.position();
						final int progress = (int) ((((double) pos) / ((double) size)) * 1000.0);
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								progressbar.setValue(progress);
							}
						});
						Thread.sleep(250);
					} catch (InterruptedException | IOException e) {
						log.error(e.getMessage(), e);
					}
				}
			}
			
		}, "ProgressObserver");
		
		t.setDaemon(true);
		t.start();
		
		this.setModal(false);
		this.setResizable(false);
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.pack();
		
		// center on screen
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int w = this.getSize().width;
		int h = this.getSize().height;
		int x = (dim.width-w)/2;
		int y = (dim.height-h)/2;
		this.setLocation(x, y);
		
		this.setVisible(true);
	}
	
	public void close() {
		this.setVisible(false);
	}
}
