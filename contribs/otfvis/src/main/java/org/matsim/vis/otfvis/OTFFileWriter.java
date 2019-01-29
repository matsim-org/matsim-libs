/* *********************************************************************** *
 * project: org.matsim.*
 * Writer
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
package org.matsim.vis.otfvis;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.SnapshotWriter;
import org.matsim.vis.snapshotwriters.VisData;
/**
 * The OTF has a file Reader and a file Writer part.
 * The writer is in charge of writing mvi data into a file.
 * 
 * @author dstrippgen 
 * @author dgrether
 */
public final class OTFFileWriter implements SnapshotWriter {

	private static final int BUFFERSIZE = 300000000; // ~300MB
	private static final int FILE_BUFFERSIZE = 50000000; // ~50MB

	private OTFServerQuadTree quad;
	private ZipOutputStream zos;
	private final ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);
	private final OTFConnectionManager connect;
	private String outFileName;
	private Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();

	private double lastTime=-1;

	private final OTFAgentsListHandler.Writer writer;

	public OTFFileWriter(Scenario scenario, String outfilename) {
		this.connect = new OTFConnectionManager();
		this.connect.connectLinkToWriter(OTFLinkAgentsHandler.Writer.class);
		this.connect.connectWriterToReader(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		this.outFileName = outfilename;
		this.quad = new SnapshotWriterQuadTree(scenario.getNetwork());
		this.quad.initQuadTree(connect);
		this.writer = new OTFAgentsListHandler.Writer();
		this.writer.setSrc(new VisData() {

			@Override
			public Collection<AgentSnapshotInfo> addAgentSnapshotInfo(Collection<AgentSnapshotInfo> inPositions) {
				inPositions.addAll(positions);
				return inPositions;
			}
			
		});
		if (ConfigUtils.addOrGetModule(scenario.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class) != null) {
			ConfigUtils.addOrGetModule(scenario.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class).setEffectiveLaneWidth(scenario.getNetwork().getEffectiveLaneWidth());
		}
		OTFClientControl.getInstance().setOTFVisConfig(ConfigUtils.addOrGetModule(scenario.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class));
		try {
			this.zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(this.outFileName), FILE_BUFFERSIZE));
			this.writeQuad();
			this.writeConstData();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeQuad() {
		try {
			this.zos.putNextEntry(new ZipEntry("quad.bin"));
			this.quad.addAdditionalElement(this.writer);
			new ObjectOutputStream(this.zos).writeObject(this.quad);
			this.zos.closeEntry();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeConstData() {
		try {
			this.zos.putNextEntry(new ZipEntry("const.bin"));
			DataOutputStream outFile = new DataOutputStream(this.zos);
			this.buf.position(0);
			outFile.writeDouble(-1.);
			this.quad.writeConstData(this.buf);
			outFile.writeInt(this.buf.position());
			outFile.write(this.buf.array(), 0, this.buf.position());
			this.zos.closeEntry();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void beginSnapshot(final double time) {
		this.positions.clear();
		this.lastTime = time;
	}

	private void writeDynData(final int time_s) {
		try {
			this.zos.putNextEntry(new ZipEntry("step." + time_s + ".bin"));
			DataOutputStream outFile = new DataOutputStream(this.zos);
			this.buf.position(0);
			outFile.writeDouble(time_s);
			this.quad.writeDynData(null, this.buf);
			outFile.writeInt(this.buf.position());
			outFile.write(this.buf.array(), 0, this.buf.position());
			this.zos.closeEntry();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void addAgent(final AgentSnapshotInfo position) {
		if (position.getAgentState() != AgentSnapshotInfo.AgentState.PERSON_AT_ACTIVITY) {
			this.positions.add(position);
		}
	}

	@Override
	public void endSnapshot() {
		writeDynData((int)this.lastTime);
	}

	@Override
	public void finish() {
		try {
			this.zos.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}