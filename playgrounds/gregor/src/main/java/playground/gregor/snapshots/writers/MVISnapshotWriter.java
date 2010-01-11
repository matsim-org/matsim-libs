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
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.evacuation.otfvis.drawer.AgentDrawer;
import org.matsim.evacuation.otfvis.drawer.OTFBackgroundTexturesDrawer;
import org.matsim.evacuation.otfvis.drawer.OTFLabelDrawer;
import org.matsim.evacuation.otfvis.drawer.OTFSheltersDrawer;
import org.matsim.evacuation.otfvis.drawer.TimeDependentTrigger;
import org.matsim.evacuation.otfvis.layer.AgentLayer;
import org.matsim.evacuation.otfvis.layer.LabelLayer;
import org.matsim.evacuation.otfvis.readerwriter.AgentReader;
import org.matsim.evacuation.otfvis.readerwriter.AgentWriter;
import org.matsim.evacuation.otfvis.readerwriter.InundationDataReader;
import org.matsim.evacuation.otfvis.readerwriter.InundationDataWriter;
import org.matsim.evacuation.otfvis.readerwriter.LabelReader;
import org.matsim.evacuation.otfvis.readerwriter.LabelWriter;
import org.matsim.evacuation.otfvis.readerwriter.PolygonDataReader;
import org.matsim.evacuation.otfvis.readerwriter.PolygonDataWriter;
import org.matsim.evacuation.otfvis.readerwriter.SheltersReader;
import org.matsim.evacuation.otfvis.readerwriter.SheltersWriter;
import org.matsim.evacuation.otfvis.readerwriter.TextureDataWriter;
import org.matsim.evacuation.otfvis.readerwriter.TextutreDataReader;
import org.matsim.evacuation.otfvis.readerwriter.TileDrawerDataReader;
import org.matsim.evacuation.otfvis.readerwriter.TileDrawerDataWriter;
import org.matsim.ptproject.qsim.QueueNetwork;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.fileio.OTFFileWriter;
import org.matsim.vis.otfvis.data.fileio.qsim.OTFFileWriterQSimConnectionManagerFactory;
import org.matsim.vis.otfvis.data.fileio.qsim.OTFQSimServerQuadBuilder;
import org.matsim.vis.otfvis.handler.OTFAgentsListHandler.ExtendedPositionInfo;
import org.matsim.vis.otfvis.opengl.drawer.SimpleBackgroundDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleBackgroundLayer;

import playground.gregor.otf.readerwriter.InundationDataFromBinaryFileReader;


public class MVISnapshotWriter extends OTFFileWriter{
	//private final   String netFileName = "";
//	private  String vehFileName = "";
	//private  String outFileName = "";
	public static final String CVSROOT = "../../../../../workspace/vsp-cvs";
	private static final String BG_IMG_ROOT = CVSROOT + "/studies/padang/imagery/sliced/";
	//	final String BUILDINGS_FILE =  CVSROOT + "/studies/padang/imagery/GIS/convex_buildings.shp";
//		final String LINKS_FILE =  CVSROOT + "/studies/padang/gis/network_v20080618/links.shp";
		
	//	final String NODES_FILE =  CVSROOT + "/studies/padang/imagery/GIS/convex_nodes.shp";
	final String BUILDINGS_FILE =  "../../../../inputs/gis/shelters.shp";
	final String LINKS_FILE = "../../../../inputs/gis/links.shp";
	final String NODES_FILE = "../../../../inputs/gis/nodes.shp";
	final String REGION_FILE =  "../../../../inputs/gis/region.shp";
	final private static float [] regionColor = new float [] {.9f,.92f,.82f,1.f};
	final private static float [] buildingsColor = new float [] {1.f,.5f,.0f,.8f};
	final private static float [] linksColor = new float [] {.5f,.5f,.5f,.7f};
	final private static float [] nodesColor = new float [] {.4f,.4f,.4f,.7f};

	private final AgentWriter writer = new AgentWriter();
	private final boolean insertWave = true;
	
	private final String label = "run1006: Nash approach";

	public MVISnapshotWriter(final QueueNetwork net, final String vehFileName, final String outFileName, final double intervall_s) {
		super(intervall_s, new OTFQSimServerQuadBuilder(net), outFileName, new OTFFileWriterQSimConnectionManagerFactory());
		((NetworkLayer) net.getNetworkLayer()).createAndAddNode(new IdImpl("minXY"), new CoordImpl(643000,9880000));//HACK to get the bounding box big enough; 
		//otherwise we could get negative openGL coords since we calculating offsetEast, offsetNorth based on this bounding box
//		this.vehFileName = vehFileName;
		//this.outFileName = outFileName;
	}


	public MVISnapshotWriter(ScenarioImpl sc) {
		super(sc.getConfig().simulation().getSnapshotPeriod(),new OTFQSimServerQuadBuilder(new QueueNetwork(sc.getNetwork())),"../../../../outputs/output/movie.mvi", new OTFFileWriterQSimConnectionManagerFactory());
	}


	@Override
	protected void onAdditionalQuadData(OTFConnectionManager connect) {
		this.quad.addAdditionalElement(this.writer);
		if (this.insertWave ){
			this.quad.addAdditionalElement(new InundationDataWriter(new InundationDataFromBinaryFileReader().readData()));
//			this.quad.addAdditionalElement(new InundationDataWriter(new InundationDataFromNetcdfReaderII(OTFServerQuad2.offsetNorth,OTFServerQuad2.offsetEast).createData()));
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
		
		if (this.label != null) {
			this.quad.addAdditionalElement(new LabelWriter(this.label));
			connect.add(LabelWriter.class,LabelReader.class);
			connect.add(LabelReader.class,OTFLabelDrawer.class);
			connect.add(OTFLabelDrawer.class,LabelLayer.class);
		}
		
		connect.add(PolygonDataWriter.class,PolygonDataReader.class);
		
		
//		connect.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		connect.add(SimpleBackgroundDrawer.class, OGLSimpleBackgroundLayer.class);
		

		
		
		connect.add(AgentWriter.class,  AgentReader.class);
		connect.add(AgentReader.class,AgentDrawer.class);
		connect.add(AgentDrawer.class,AgentLayer.class);
	
		
		
//		connect.add(SimpleStaticNetLayer.NoQuadDrawer.class, SimpleStaticNetLayer.class);
//		connect.add(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
//		connect.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
//		connect.add(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.NoQuadDrawer.class);
		
		
		
		
		connect.add(TileDrawerDataWriter.class,TileDrawerDataReader.class);
		//		connect.add(InundationDataWriter.class,InundationDataReader.class);
		//		connect.add(InundationDataReader.class,Dummy.class);
		connect.add(PolygonDataWriter.class,PolygonDataReader.class);
		connect.add(TextureDataWriter.class,TextutreDataReader.class);
		if (this.occMap != null) {
			try {
				FeatureSource fs = ShapeFileReader.readDataFile(this.BUILDINGS_FILE);
				OTFSheltersDrawer sd = new OTFSheltersDrawer(fs,this.occMap,OTFServerQuad2.offsetNorth,OTFServerQuad2.offsetEast);
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
