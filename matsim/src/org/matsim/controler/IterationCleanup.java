/* *********************************************************************** *
 * project: org.matsim.*
 * IterationCleanup.java
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

package org.matsim.controler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.zip.GZIPOutputStream;

import org.matsim.gbl.Gbl;

/**
 * @author marcel
 *
 * Contains different cleanup-routines.<br />
 * currently included / implemented:<br />
 * <dl>
 * 	<dt>compress events-files using gzip</dt>
 *  <dd>At the end of an iteration, the events-file of the iteration before will be compressed.
 *    The compression takes place in a separate thread. The compression-"jobs" are queued and
 *    handled one after the other. At most 5 jobs can be waiting in the queue. If iterations
 *    end faster then the files are compressed and the queue is thus filling up, subsequent
 *    jobs will be ignored as long as the queue is full (=contains 5 jobs) and the files will
 *    not be compressed.</dd>
 * </dl>
 *
 */
public class IterationCleanup {

	private static final int BUFFER_SIZE = 8192;

	private BlockingQueue<String> gzipQ = new ArrayBlockingQueue<String>(5, true);
	private Thread gzipThread = null;

	public IterationCleanup() {
		GZipThread gzipper = new GZipThread(this.gzipQ);
		this.gzipThread = new Thread(gzipper);
	}
	
	public void start() {
		this.gzipThread.start();
	}

	public void iterationEnd(int iteration) {

		if (iteration > 0) {
			try {
				this.gzipQ.add(Controler.getIterationFilename(Controler.FILENAME_EVENTS, iteration - 1));
				System.out.println("Added job to queue: compress events-file of iteration " + (iteration - 1) + ". " + (new Date()));
			} catch (IllegalStateException e) {
				System.err.println("Could not add job to queue: compress events-file of iteration " + (iteration - 1) + ". Reason: see below. " + (new Date()));
				e.printStackTrace();
			}
		}
	}


	private void gzipFile(final String filename, final boolean deleteOriginal) {

		BufferedOutputStream out = null;
		BufferedInputStream in = null;
		try {
			out = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(filename + ".gz")));
			in = new BufferedInputStream(new FileInputStream(filename));
		} catch (FileNotFoundException e) {
			// FileNotFoundException can happen when opening out or in. At least in will always be null then.
			Gbl.warningMsg(this.getClass(), "gzipFile", "Could not compress file. " + e.getMessage());
			if (out != null) {
				try { out.close(); } catch (IOException ignored) {}
			}
			return;
		} catch (IOException e) {
			// IOException can only happen when opening out. At this point, out and in are still null.
			Gbl.warningMsg(this.getClass(), "gzipFile", "Could not compress file. " + e.getMessage());
			return;
		}

		byte[] buffer = new byte[BUFFER_SIZE];
		boolean hadException = false;

		try {
			int len = in.read(buffer, 0, BUFFER_SIZE);
			while (len != -1) {
				out.write(buffer, 0, len);
				len = in.read(buffer, 0, BUFFER_SIZE);
			}
		} catch (IOException e) {
			Gbl.warningMsg(this.getClass(), "gzipFile", "Could not compress file. " + e.getMessage());
			hadException = true;
		}
		try { out.close(); } catch (IOException ignored) {}
		try { in.close(); } catch (IOException ignored) {}

		if (hadException) {
			// something went wrong. try to delete the partially compressed file
			new File(filename + ".gz").delete();
		}
		if (!hadException && deleteOriginal) {
			new File(filename).delete();
		}
	}

	private class GZipThread implements Runnable {
		private BlockingQueue<String> gzipQ; // reference to the common queue
		
		public GZipThread(BlockingQueue<String> gzipQ) {
			this.gzipQ = gzipQ;
		}
		
		public void run() {
			try {
				while(true) {
					String filename = this.gzipQ.take();
					System.out.println("start compressing file " + filename + " at " + (new Date()));
					gzipFile(filename, true);
					System.out.println("finished compressing file " + filename + " at " + (new Date()));
				}
			} catch (InterruptedException e) {
				Gbl.warningMsg(this.getClass(), "run", "thread execution got interrupted. " + e.getMessage());
			}
		}
	}
}