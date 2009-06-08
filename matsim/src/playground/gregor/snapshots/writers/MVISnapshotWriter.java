/* *********************************************************************** *
 * project: org.matsim.*
 * OTFTVeh2MVI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.gregor.snapshots.writers;

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler;
import org.matsim.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsHandler;
import org.matsim.vis.otfvis.handler.OTFLinkAgentsNoParkingHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler.ExtendedPositionInfo;
import org.matsim.vis.otfvis.opengl.drawer.SimpleBackgroundDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleBackgroundLayer;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.vis.otfvis.server.OTFQuadFileHandler;

import playground.gregor.otf.Dummy;
import playground.gregor.otf.InundationDataFromBinaryFileReader;
import playground.gregor.otf.InundationDataReader;
import playground.gregor.otf.InundationDataWriter;
import playground.gregor.otf.PolygonDataReader;
import playground.gregor.otf.PolygonDataReaderII;
import playground.gregor.otf.PolygonDataWriter;
import playground.gregor.otf.PolygonDataWriterII;
import playground.gregor.otf.SimpleBackgroundTextureDrawer;
import playground.gregor.otf.TextureDataWriter;
import playground.gregor.otf.TextutreDataReader;
import playground.gregor.otf.TileDrawerDataReader;
import playground.gregor.otf.TileDrawerDataWriter;


public class MVISnapshotWriter extends OTFQuadFileHandler.Writer{
	//private final   String netFileName = "";
//	private  String vehFileName = "";
	//private  String outFileName = "";
	public static final String CVSROOT = "../../../workspace/vsp-cvs";
	private static final String BG_IMG_ROOT = CVSROOT + "/studies/padang/imagery/sliced/";
	//	final String BUILDINGS_FILE =  CVSROOT + "/studies/padang/imagery/GIS/convex_buildings.shp";
//		final String LINKS_FILE =  CVSROOT + "/studies/padang/gis/network_v20080618/links.shp";
		
	//	final String NODES_FILE =  CVSROOT + "/studies/padang/imagery/GIS/convex_nodes.shp";
	final String BUILDINGS_FILE =  "../../inputs/gis/shelters.shp";
	final String LINKS_FILE = "../../inputs/gis/links.shp";
	final String NODES_FILE = "../../inputs/gis/nodes.shp";
	final String REGION_FILE =  "../../inputs/gis/region.shp";
	final private static float [] regionColor = new float [] {.87f,.92f,.87f,1.f};
	final private static float [] buildingsColor = new float [] {1.f,.5f,.0f,.8f};
	final private static float [] linksColor = new float [] {.5f,.5f,.5f,.7f};
	final private static float [] nodesColor = new float [] {.4f,.4f,.4f,.7f};

	private final OTFAgentsListHandler.Writer writer = new OTFAgentsListHandler.Writer();
	private boolean insertWave = true;

	public MVISnapshotWriter(final QueueNetwork net, final String vehFileName, final String outFileName, final double intervall_s) {
		super(intervall_s, net, outFileName);
//		this.vehFileName = vehFileName;
		//this.outFileName = outFileName;
	}


	public MVISnapshotWriter(Scenario sc) {
		super(sc.getConfig().simulation().getSnapshotPeriod(),new QueueNetwork(sc.getNetwork()),"../../outputs/output/movie.mvi");
	}


	@Override
	protected void onAdditionalQuadData(OTFConnectionManager connect) {
		this.quad.addAdditionalElement(this.writer);
		if (insertWave ) this.quad.addAdditionalElement(new InundationDataWriter(new InundationDataFromBinaryFileReader().readData()));
		
		try {
			this.quad.addAdditionalElement(new PolygonDataWriterII(ShapeFileReader.readDataFile(REGION_FILE),regionColor));
			this.quad.addAdditionalElement(new TileDrawerDataWriter());
			this.quad.addAdditionalElement(new PolygonDataWriter(ShapeFileReader.readDataFile(LINKS_FILE),linksColor));
			this.quad.addAdditionalElement(new PolygonDataWriter(ShapeFileReader.readDataFile(NODES_FILE),nodesColor));
			this.quad.addAdditionalElement(new PolygonDataWriter(ShapeFileReader.readDataFile(BUILDINGS_FILE),buildingsColor));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		this.quad.addAdditionalElement(new InundationDataWriter(new InundationDataFromNetcdfReader(OTFServerQuad.offsetNorth,OTFServerQuad.offsetEast).createData()));
		this.quad.addAdditionalElement(new TextureDataWriter(this.sbg));
		connect.add(PolygonDataWriterII.class,PolygonDataReaderII.class);
		connect.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		connect.add(SimpleBackgroundDrawer.class, OGLSimpleBackgroundLayer.class);
		
		connect.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect.add(OTFLinkAgentsHandler.class,  SimpleStaticNetLayer.NoQuadDrawer.class);
		connect.add(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);
		
		connect.add(OTFAgentsListHandler.class, OGLAgentPointLayer.AgentPadangTimeDrawer.class);
		connect.add(OGLAgentPointLayer.AgentPadangTimeDrawer.class, OGLAgentPointLayer.class);
		
		
		connect.add(SimpleStaticNetLayer.NoQuadDrawer.class, SimpleStaticNetLayer.class);
		connect.add(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
		connect.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
		connect.add(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.NoQuadDrawer.class);
		connect.add(TileDrawerDataWriter.class,TileDrawerDataReader.class);
		//		connect.add(InundationDataWriter.class,InundationDataReader.class);
		//		connect.add(InundationDataReader.class,Dummy.class);
		connect.add(PolygonDataWriter.class,PolygonDataReader.class);
		connect.add(TextureDataWriter.class,TextutreDataReader.class);
		if (insertWave) {
			connect.add(InundationDataWriter.class,InundationDataReader.class);
			connect.add(InundationDataReader.class,Dummy.class);
		}
	}

	//	public static double myParseDouble(String rep) {
	//		double result = 0;
	//		int exp = 1;
	//		double factor = 1;
	//		double before= 0;
	//		double after = 0;
	//		String [] parts = StringUtils.explode(rep, 'E');
	//		if(parts.length == 2) {
	//			exp = Integer.parseInt(parts[1]);
	//			while (exp-- >0) factor *=10.;
	//		} else {
	//
	//		}
	//		parts = StringUtils.explode(parts[0], '.');
	//		before = Long.parseLong(parts[0]);
	//		if(parts.length == 2) {
	//			after = Long.parseLong(parts[1]);
	//			double divider = 1;
	//			int stellen = parts[1].length();
	//			while (stellen-- >0) divider *=10.;
	//			after /= divider;
	//		} else {
	//
	//		}
	//		result = (before + after)* factor;
	//		return result;
	//	}

	//ByteBuffer buf = ByteBuffer.allocate(BUFFERSIZE);
	private int cntPositions=0;
	private double lastTime=-1;
	private int cntTimesteps=0;
	private SimpleBackgroundTextureDrawer sbg;


	public void addVehicle(final double time, final ExtendedPositionInfo position) {
		this.cntPositions++;

		// Init lastTime with first occurence of time!
		if (this.lastTime == -1) this.lastTime = time;

		if (time != this.lastTime) {
			this.cntTimesteps++;

			if (time % 600 == 0 ) {
				System.out.println("Parsing T = " + time + " secs");
				Gbl.printElapsedTime();
				Gbl.startMeasurement();
			}
			// the time changes
			// this is a dumpable timestep
			try {
				dump((int)this.lastTime);
				this.writer.positions.clear();
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.lastTime = time;
		}
		// I do not realyy know which second will be written, as it might be any second AFTER nextTime, when NOTHING has happened on "nextTime", as the if-clause will be executed only then
		// still I can collect all vehicles, as to every time change it will get erased...
		//		if (time == nextTime) {
		this.writer.positions.add(position);
		//		}
	}

	public void setSimpleBackgroundTextureDrawer(SimpleBackgroundTextureDrawer sbg) {
		this.sbg = sbg;
	}

	@Override
	public void finish() {
		close();
	}


}
