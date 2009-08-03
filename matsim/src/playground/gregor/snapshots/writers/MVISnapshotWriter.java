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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geotools.data.FeatureSource;
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuad;
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

import playground.gregor.otf.drawer.OTFBackgroundTexturesDrawer;
import playground.gregor.otf.drawer.OTFSheltersDrawer;
import playground.gregor.otf.drawer.TimeDependentTrigger;
import playground.gregor.otf.readerwriter.InundationDataFromBinaryFileReader;
import playground.gregor.otf.readerwriter.InundationDataFromNetcdfReader;
import playground.gregor.otf.readerwriter.InundationDataReader;
import playground.gregor.otf.readerwriter.InundationDataWriter;
import playground.gregor.otf.readerwriter.PolygonDataReader;
import playground.gregor.otf.readerwriter.PolygonDataWriter;
import playground.gregor.otf.readerwriter.SheltersReader;
import playground.gregor.otf.readerwriter.SheltersWriter;
import playground.gregor.otf.readerwriter.TextureDataWriter;
import playground.gregor.otf.readerwriter.TextutreDataReader;
import playground.gregor.otf.readerwriter.TileDrawerDataReader;
import playground.gregor.otf.readerwriter.TileDrawerDataWriter;


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
	final private static float [] regionColor = new float [] {.9f,.92f,.82f,1.f};
	final private static float [] buildingsColor = new float [] {1.f,.5f,.0f,.8f};
	final private static float [] linksColor = new float [] {.5f,.5f,.5f,.7f};
	final private static float [] nodesColor = new float [] {.4f,.4f,.4f,.7f};

	private final OTFAgentsListHandler.Writer writer = new OTFAgentsListHandler.Writer();
	private final boolean insertWave = true;

	public MVISnapshotWriter(final QueueNetwork net, final String vehFileName, final String outFileName, final double intervall_s) {
		super(intervall_s, net, outFileName);
//		this.vehFileName = vehFileName;
		//this.outFileName = outFileName;
	}


	public MVISnapshotWriter(ScenarioImpl sc) {
		super(sc.getConfig().simulation().getSnapshotPeriod(),new QueueNetwork(sc.getNetwork()),"../../outputs/output/movie.mvi");
	}


	@Override
	protected void onAdditionalQuadData(OTFConnectionManager connect) {
		this.quad.addAdditionalElement(this.writer);
		if (this.insertWave ){
			this.quad.addAdditionalElement(new InundationDataWriter(new InundationDataFromBinaryFileReader().readData()));
//			this.quad.addAdditionalElement(new InundationDataWriter(new InundationDataFromNetcdfReader(OTFServerQuad.offsetNorth,OTFServerQuad.offsetEast).createData()));
		}
		

		
		try {
			this.quad.addAdditionalElement(new PolygonDataWriter(ShapeFileReader.readDataFile(this.REGION_FILE),regionColor));
			this.quad.addAdditionalElement(new TileDrawerDataWriter());
			this.quad.addAdditionalElement(new PolygonDataWriter(ShapeFileReader.readDataFile(this.LINKS_FILE),linksColor));
			this.quad.addAdditionalElement(new PolygonDataWriter(ShapeFileReader.readDataFile(this.NODES_FILE),nodesColor));
//			this.quad.addAdditionalElement(new PolygonDataWriter(ShapeFileReader.readDataFile(this.BUILDINGS_FILE),buildingsColor));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for (OTFBackgroundTexturesDrawer sbg : this.sbgs){
			this.quad.addAdditionalElement(new TextureDataWriter(sbg));
		}
		connect.add(PolygonDataWriter.class,PolygonDataReader.class);
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
		if (this.occMap != null) {
			try {
				FeatureSource fs = ShapeFileReader.readDataFile(this.BUILDINGS_FILE);
				OTFSheltersDrawer sd = new OTFSheltersDrawer(fs,this.occMap,OTFServerQuad.offsetNorth,OTFServerQuad.offsetEast);
				this.quad.addAdditionalElement(new SheltersWriter(sd));
				connect.add(SheltersWriter.class,SheltersReader.class);
				connect.add(SheltersReader.class,TimeDependentTrigger.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (this.insertWave) {
			connect.add(InundationDataWriter.class,InundationDataReader.class);
			connect.add(InundationDataReader.class,TimeDependentTrigger.class);
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
//	private SimpleBackgroundTextureDrawer sbg;
	private final List<OTFBackgroundTexturesDrawer> sbgs = new ArrayList<OTFBackgroundTexturesDrawer>();
private Map<String, ArrayList<Tuple<Integer,Double>>> occMap = null;


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

	public void addSimpleBackgroundTextureDrawer(OTFBackgroundTexturesDrawer sbg) {
		this.sbgs.add(sbg);
	}

	@Override
	public void finish() {
		close();
	}


	public void setSheltersOccupancyMap(Map<String, ArrayList<Tuple<Integer,Double>>> occMap) {
		this.occMap  = occMap;
		
	}


}
