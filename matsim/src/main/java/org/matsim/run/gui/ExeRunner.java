/* *********************************************************************** *
 * project: org.matsim.*
 * ExeRunner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import javax.swing.JTextArea;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;


/**
 * A modified version of ExeRunner which sends the output to a text area
 *
 * @author mrieser
 */
/*package*/ class ExeRunner {

	/*package*/ final static Logger log = LogManager.getLogger(ExeRunner.class);

	private final ExternalExecutor executor;

	public static ExeRunner run(final String[] cmdArgs, final JTextArea stdOut, final JTextArea errOut, final String workingDirectory) {
		final ExternalExecutor myExecutor = new ExternalExecutor(cmdArgs, stdOut, errOut, workingDirectory);
		ExeRunner runner = new ExeRunner(myExecutor);
		myExecutor.start();
		return runner;
	}

	private ExeRunner(ExternalExecutor executor) {
		this.executor = executor;
	}

	public void killProcess() {
		this.executor.killProcess();
	}

	public int waitForFinish() {
		synchronized (this.executor) {
			try {
				this.executor.join();
			} catch (InterruptedException e) {
				log.info("Got interrupted while waiting for external exe to finish.", e);
			}
		}

		return this.executor.erg;
	}

	private static class ExternalExecutor extends Thread {
		final String[] cmdArgs;
		final JTextArea stdOut;
		final JTextArea errOut;
		final String workingDirectory;
		private Process p = null;

		public int erg = -1;

		public ExternalExecutor (final String[] cmdArgs, final JTextArea stdOut, final JTextArea errOut, final String workingDirectory) {
			this.cmdArgs = cmdArgs;
			this.stdOut = stdOut;
			this.errOut = errOut;
			this.workingDirectory = workingDirectory;
		}

		public void killProcess() {
			if (this.p != null) {
				this.p.destroy();
			}
		}

		@Override
		public void run()  {
			var processBuilder = new ProcessBuilder();
			processBuilder.environment().put("MATSIM_GUI", "true"); // add "MATSIM_GUI" to the inherited vars
			if (workingDirectory != null) {
				processBuilder.directory(new File(workingDirectory));
			}
			processBuilder.command(cmdArgs);

			try {
				this.p = processBuilder.start();

				BufferedReader in = new BufferedReader(new InputStreamReader(this.p.getInputStream()));
				BufferedReader err = new BufferedReader(new InputStreamReader(this.p.getErrorStream()));

				StreamHandler outputHandler = new StreamHandler(in, this.stdOut);
				outputHandler.start();

				StreamHandler errorHandler = new StreamHandler(err, this.stdOut, this.errOut);
				errorHandler.start();

				log.info("Starting external exe with command: " + Arrays.toString(this.cmdArgs));
				boolean processRunning = true;
				while (processRunning) {
					try {
						this.p.waitFor();
						this.erg = this.p.exitValue();
						log.info("external exe returned " + this.erg);
						processRunning = false;
					} catch (InterruptedException e) {
						log.info("Thread waiting for external exe to finish was interrupted");
						this.erg = -3;
					}
				}
				try {
					outputHandler.join();
				} catch (InterruptedException e) {
					log.info("got interrupted while waiting for outputHandler to die.", e);
				}
				try {
					errorHandler.join();
				} catch (InterruptedException e) {
					log.info("got interrupted while waiting for errorHandler to die.", e);
				}
			} catch (IOException e) {
				log.error("problem running executable.", e);
				this.erg = -2;
			}
		}
	}

	static class StreamHandler extends Thread {
		private final BufferedReader in;
		private final JTextArea[] textArea;

		public StreamHandler(final BufferedReader in, final JTextArea... textArea) {
			this.in = in;
			this.textArea = textArea;
		}

		@Override
		public void run() {
			try {
				String line = null;
				while ((line = this.in.readLine()) != null) {
					for (JTextArea out : this.textArea) {
						out.append(line);
						out.append(IOUtils.NATIVE_NEWLINE);
						int length = out.getDocument().getLength();
						out.setCaretPosition(length);

						if (length > 512*1024) {
							out.setText(out.getText().substring(256*1024));
						}
					}
				}
			} catch (IOException e) {
				log.info("StreamHandler got interrupted", e);
			}
		}
	}

}
