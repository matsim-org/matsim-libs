/* *********************************************************************** *
 * project: org.matsim.*
 * QSimOTFFileWriter
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
package org.matsim.vis.otfvis.mobsim;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.matsim.core.gbl.Gbl;
import org.matsim.ptproject.qsim.QueueLink;
import org.matsim.ptproject.qsim.QueueNetwork;
import org.matsim.ptproject.qsim.QueueNode;
import org.matsim.vis.netvis.streaming.SimStateWriterI;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadQSim;
import org.matsim.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.server.OTFQuadFileHandler;
import org.matsim.vis.snapshots.writers.PositionInfo;
import org.matsim.vis.snapshots.writers.SnapshotWriter;


public class QSimOTFFileWriter implements SimStateWriterI, SnapshotWriter {
		private static final int BUFFERSIZE = 300000000;
		protected final QueueNetwork net;
		protected OTFServerQuadQSim quad = null;
		private final String fileName;
		protected final double interval_s;
		protected double nextTime = -1;

		private ZipOutputStream zos = null;
		private DataOutputStream outFile;
		private boolean isOpen = false;

		private final ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);
		private final OTFConnectionManager connect;

		public QSimOTFFileWriter(final double intervall_s, final QueueNetwork network,
				final String fileName) {
			this.net = network;
			this.interval_s = intervall_s;
			this.fileName = fileName;
			this.connect = initConnectionManager();
		}

		private OTFConnectionManager initConnectionManager() {
			OTFConnectionManager c = new OTFConnectionManager();
			c.add(QueueLink.class,
					OTFLinkLanesAgentsNoParkingHandler.Writer.class);
			c.add(QueueNode.class, OTFDefaultNodeHandler.Writer.class);
			return c;
		}

		public boolean dump(final int time_s) throws IOException {
			if (time_s >= this.nextTime) {
				// dump time
				writeDynData(time_s);
				this.nextTime = time_s + this.interval_s;
				return true;
			}
			return false;
		}

		private void writeInfos() throws IOException {
			// Add ZIP entry to output stream.
			this.zos.putNextEntry(new ZipEntry("info.bin"));
			this.outFile = new DataOutputStream(this.zos);
			this.outFile.writeInt(OTFQuadFileHandler.VERSION);
			this.outFile.writeInt(OTFQuadFileHandler.MINORVERSION);

			this.outFile.writeDouble(this.interval_s);
			// outFile.writeUTF("fromFile");
			this.zos.closeEntry();
		}

		protected void onAdditionalQuadData(OTFConnectionManager connect) {
			// intentionally left blank for inheriting classes to overwrite
		}

		private void writeQuad() throws IOException {
			this.zos.putNextEntry(new ZipEntry("quad.bin"));
			Gbl.startMeasurement();
			this.quad = new OTFServerQuadQSim(this.net);
			System.out.print("build Quad on Server: ");
			Gbl.printElapsedTime();

			Gbl.startMeasurement();

			onAdditionalQuadData(this.connect);

			this.quad.fillQuadTree(this.connect);
			System.out.print("fill writer Quad on Server: ");
			Gbl.printElapsedTime();
			Gbl.startMeasurement();
			new ObjectOutputStream(this.zos).writeObject(this.quad);
			this.zos.closeEntry();
			// this is new, write connect into the mvi as well
			this.zos.putNextEntry(new ZipEntry("connect.bin"));
			new ObjectOutputStream(this.zos).writeObject(this.connect);
			this.zos.closeEntry();
		}

		private void writeConstData() throws IOException {
			this.zos.putNextEntry(new ZipEntry("const.bin"));
			this.outFile = new DataOutputStream(this.zos);
			this.buf.position(0);
			this.outFile.writeDouble(-1.);

			this.quad.writeConstData(this.buf);

			this.outFile.writeInt(this.buf.position());
			this.outFile.write(this.buf.array(), 0, this.buf.position());
			this.zos.closeEntry();
		}

		private void writeDynData(final int time_s) throws IOException {
			this.zos.putNextEntry(new ZipEntry("step." + time_s + ".bin"));
			this.outFile = new DataOutputStream(this.zos);
			this.buf.position(0);
			this.outFile.writeDouble(time_s);
			// get State
			this.quad.writeDynData(null, this.buf);
			this.outFile.writeInt(this.buf.position());
			this.outFile.write(this.buf.array(), 0, this.buf.position());
			// dump State
			this.zos.closeEntry();
		}

		public void open() {
			// open zip file
			this.isOpen = true;

			try {
				this.zos = new ZipOutputStream(new BufferedOutputStream(
						new FileOutputStream(this.fileName), BUFFERSIZE));
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			try {
				// dump some infos
				writeInfos();
				writeQuad();
				writeConstData();
				System.out.print("write to file  Quad on Server: ");
				Gbl.printElapsedTime();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// dump the network out
		}

		public void close() {
			try {
				this.zos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void addAgent(final PositionInfo position) {
		}

		public void beginSnapshot(final double time) {
			if (!this.isOpen)
				open();
			try {
				dump((int) time);
			} catch (IOException e) {

				e.printStackTrace();
			}
		}

		public void endSnapshot() {
			// TODO Auto-generated method stub
		}

		public void finish() {
			close();
		}

		public OTFConnectionManager getConnectionManager() {
			return this.connect;
		}
}
