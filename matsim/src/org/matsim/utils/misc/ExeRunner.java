/* *********************************************************************** *
 * project: org.matsim.*
 * ExeRunner.java
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

package org.matsim.utils.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Date;


/**
 * Runs an executable and waits until the executable finishes.
 *
 * @author mrieser
 */
public class ExeRunner {

	private ExeRunner() {
		// this is a static class
	}

	/**
	 * Runs an executable and waits until the executable finishes or until
	 * a certain amount of time has passed (timeout).
	 *
	 * @param cmd The system command to execute. E.g. "/bin/sh some-script.sh arg1 arg2"
	 * @param stdoutFileName specifies the file where stdout of the executable is redirected to.
	 * @param timeout A timeout in seconds. If the executable is still running after
	 * 					<code>timeout</code> seconds, the executable is stopped and this method returns.
	 * @return exit-code of the executable.
	 */
	public static int run(final String cmd, final String stdoutFileName, final int timeout) {
		ExternalExecutor myExecutor = new ExternalExecutor(cmd, stdoutFileName);

		synchronized (myExecutor) {
			try {
				long timeoutMillis = 1000L * timeout;
				long startTime = System.currentTimeMillis();
				myExecutor.start();
				while (System.currentTimeMillis() - startTime < timeoutMillis) {
					// reduce the timeout to the residual of the timeout
					timeoutMillis -= System.currentTimeMillis() - startTime;
					myExecutor.wait(timeoutMillis);
					// wait can return for different reasons.
					if (myExecutor.getState().equals(Thread.State.TERMINATED)) {
						break;
					}
				}
				if (!myExecutor.getState().equals(Thread.State.TERMINATED)) {
					myExecutor.timeout = true;
					myExecutor.interrupt();
					myExecutor.join();
				}
			} catch (InterruptedException e) {
				System.out.println("ExeRunner.run() got interrupted while waiting for timeout");
				e.printStackTrace();
			}
		}

		return myExecutor.erg;
	}

	private static class ExternalExecutor extends Thread {
		final String cmd;
		final String stdoutFileName;
		final String stderrFileName;
		private Process p = null;
		public volatile boolean timeout = false;

		public int erg = -1;

		public ExternalExecutor (final String cmd, final String stdoutFileName) {
			this.cmd = cmd;
			this.stdoutFileName = stdoutFileName;
			if (stdoutFileName.endsWith(".log")) {
				this.stderrFileName = stdoutFileName.substring(0, stdoutFileName.length() - 4) + ".err";
			} else {
				this.stderrFileName = stdoutFileName + ".err";
			}
		}
		public void killProcess() {
			if (this.p != null) {
				this.p.destroy();
			}
		}

		@Override
		public void run()  {
			try {
				this.p = Runtime.getRuntime().exec(this.cmd);
				BufferedReader in = new BufferedReader(new InputStreamReader(this.p.getInputStream()));
				BufferedReader err = new BufferedReader(new InputStreamReader(this.p.getErrorStream()));
				BufferedWriter writerIn = new BufferedWriter(new FileWriter(this.stdoutFileName));
				BufferedWriter writerErr = new BufferedWriter(new FileWriter(this.stderrFileName));
				StreamHandler outputHandler = new StreamHandler(in, writerIn);
				StreamHandler errorHandler = new StreamHandler(err, writerErr);
				outputHandler.start();
				errorHandler.start();
				System.out.println("  Starting external exe with command: " + this.cmd);
				System.out.println("  Output of the externel exe is written to: " + this.stdoutFileName);
				boolean processRunning = true;
				while (processRunning && !this.timeout) {
					try {
						this.p.waitFor();
						this.erg = this.p.exitValue();
						System.out.println("  external exe returned " + this.erg);
						processRunning = false;
					} catch (InterruptedException e) {
						System.err.println("Thread waiting for external exe to finish was interrupted at " + (new Date()));
						this.erg = -3;
					}
				}
				if (this.timeout) {
					System.out.println("Timeout reached, killing process...");
					killProcess();
				}
				writerIn.flush();
				writerIn.close();

				writerErr.flush();
				writerErr.close();

			} catch (IOException e) {
				e.printStackTrace();
				this.erg = -2;
			}
		}
	}

	static class StreamHandler extends Thread {
		private final BufferedReader in;
		private final BufferedWriter out;

		public StreamHandler(final BufferedReader in) {
			this(in, new BufferedWriter(new OutputStreamWriter(System.out)));
		}

		public StreamHandler(final BufferedReader in, final PrintStream out) {
			this(in, new BufferedWriter(new OutputStreamWriter(out)));
		}

		public StreamHandler(final BufferedReader in, final BufferedWriter out) {
			this.in = in;
			this.out = out;
		}

		@Override
		public void run() {
			try {
				String line = null;
				while ((line = this.in.readLine()) != null) {
					this.out.write(line);
					this.out.write("\n");
				}
				this.out.flush();
			} catch (IOException e) {
				System.out.println("StreamHandler got interrupted at " + (new Date()));
				e.printStackTrace();
			}
		}
	}

}