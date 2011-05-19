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
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.qnetsimengine.QNetwork;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.fileio.OTFFileWriter;
import org.matsim.vis.otfvis.data.fileio.qsim.OTFQSimServerQuadBuilder;
import org.matsim.vis.otfvis.opengl.drawer.SimpleBackgroundDrawer;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;

import playground.gregor.otf.readerwriter.InundationDataFromBinaryFileReader;
import playground.gregor.snapshots.OTFSnapshotGenerator;


public class MVISnapshotWriter extends OTFFileWriter{
	//private final   String netFileName = "";
//	private  String vehFileName = "";
	//private  String outFileName = "";
//	public static final String CVSROOT = "../../../../../workspace/vsp-cvs";
//	private static final String BG_IMG_ROOT = CVSROOT + "/studies/padang/imagery/sliced/";
	//	final String BUILDINGS_FILE =  CVSROOT + "/studies/padang/imagery/GIS/convex_buildings.shp";
//		final String LINKS_FILE =  CVSROOT + "/studies/padang/gis/network_v20080618/links.shp";
		
	//	final String NODES_FILE =  CVSROOT + "/studies/padang/imagery/GIS/convex_nodes.shp";
	
	final String BUILDINGS_FILE =  OTFSnapshotGenerator.SHARED_SVN + "/countries/id/padang/gis/vis/shelters.shp";
	
	
//	final String BUILDINGS_FILE =  "/home/laemmel/devel/allocation/data/buildings.shp";
	
//	final String LINKS_FILE = OTFSnapshotGenerator.SHARED_SVN + "/countries/id/padang/gis/vis/links.shp";
//	final String NODES_FILE = OTFSnapshotGenerator.SHARED_SVN + "/countries/id/padang/gis/vis/nodes.shp";
	
	final String LINKS_FILE = "/home/laemmel/devel/sim2d/data/duisburg/links.shp";
	final String NODES_FILE = "/home/laemmel/devel/sim2d/data/duisburg/nodes.shp";
	
	final String REGION_FILE =  OTFSnapshotGenerator.SHARED_SVN + "/countries/id/padang/gis/vis/region.shp";
	final String SAFE_FILE =  OTFSnapshotGenerator.SHARED_SVN + "/countries/id/padang/gis/vis/safe.shp";
	final private static float [] regionColor = new float [] {.9f,.82f,.82f,1.f};
	final private static float [] safeColor = new float [] {.1f,.9f,.1f,1.f};
	final private static float [] buildingsColor = new float [] {1.f,.5f,.0f,.8f};
	final private static float [] linksColor = new float [] {.5f,.5f,.5f,.7f};
	final private static float [] nodesColor = new float [] {.4f,.4f,.4f,.7f};

	private final AgentWriterXYAzimuth writer = new AgentWriterXYAzimuth();
	private final boolean insertWave = false;
	
	private String label = "run1006: Nash approach";
	
	private double startTime = 0;

	public MVISnapshotWriter(final QNetwork net, final String vehFileName, final String outFileName, final double intervall_s) {
		super(intervall_s, new OTFQSimServerQuadBuilder(net), outFileName, new OTFFileWriterConnectionManagerFactory());
		((NetworkImpl) net.getNetwork()).createAndAddNode(new IdImpl("minXY"), new CoordImpl(643000,9880000));//HACK to get the bounding box big enough; 
		//otherwise we could get negative openGL coords since we calculating offsetEast, offsetNorth based on this bounding box
//		this.vehFileName = vehFileName;
		//this.outFileName = outFileName;
	}


	public MVISnapshotWriter(Scenario sc) {
//		super(sc.getConfig().simulation().getSnapshotPeriod(),new OTFQSimServerQuadBuilder(new QNetwork(new QSim(sc,new EventsManagerFactoryImpl().createEventsManager()))),OTFSnapshotGenerator.MVI_FILE, new OTFFileWriterConnectionManagerFactory());
		super(sc.getConfig().simulation().getSnapshotPeriod(),new OTFQSimServerQuadBuilder(new QSim(sc,(EventsUtils.createEventsManager())).getNetsimNetwork()),OTFSnapshotGenerator.MVI_FILE, new OTFFileWriterConnectionManagerFactory());
	}


	public MVISnapshotWriter(Scenario sc, String mVIFILE) {
		super(sc.getConfig().simulation().getSnapshotPeriod(),new OTFQSimServerQuadBuilder(new QSim(sc,(EventsUtils.createEventsManager())).getNetsimNetwork()),mVIFILE, new OTFFileWriterConnectionManagerFactory());
	}


	public void setLabel(String label) {
		this.label = label;
	}
	
	@Override
	protected void onAdditionalQuadData(OTFConnectionManager connect) {
		this.quad.addAdditionalElement(this.writer);
		if (this.insertWave ){
			this.quad.addAdditionalElement(new InundationDataWriter_v2(new InundationDataFromBinaryFileReader().readData(),this.startTime));
//			this.quad.addAdditionalElement(new InundationDataWriter_v2(new InundationDataFromNetcdfReaderII(OTFServerQuad2.offsetNorth,OTFServerQuad2.offsetEast).createData(),this.startTime));
		}
		

		
		//			this.quad.addAdditionalElement(new PolygonDataWriter(ShapeFileReader.readDataFile(this.REGION_FILE),regionColor));
		//			this.quad.addAdditionalElement(new PolygonDataWriter(ShapeFileReader.readDataFile(this.SAFE_FILE),safeColor));
					this.quad.addAdditionalElement(new TileDrawerDataWriter());
					this.quad.addAdditionalElement(new PolygonDataWriter(ShapeFileReader.readDataFile(this.LINKS_FILE),linksColor));
					this.quad.addAdditionalElement(new PolygonDataWriter(ShapeFileReader.readDataFile(this.NODES_FILE),nodesColor));
		//			this.quad.addAdditionalElement(new PolygonDataWriter(ShapeFileReader.readDataFile(this.BUILDINGS_FILE),buildingsColor));
		
		for (OTFBackgroundTexturesDrawer sbg : this.sbgs){
			this.quad.addAdditionalElement(new TextureDataWriter(sbg));
		}
		
		if (this.label != null) {
			this.quad.addAdditionalElement(new LabelWriter(this.label));
			connect.connectWriterToReader(LabelWriter.class,LabelReader.class);
			connect.connectReaderToReceiver(LabelReader.class,OTFLabelDrawer.class);
			connect.connectReceiverToLayer(OTFLabelDrawer.class,LabelLayer.class);
		}
		
		connect.connectWriterToReader(PolygonDataWriter.class,PolygonDataReader.class);
		
		
//		connect.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
		connect.connectReceiverToLayer(SimpleBackgroundDrawer.class, OGLSimpleBackgroundLayer.class);
		

		
		
		connect.connectWriterToReader(AgentWriterXYAzimuth.class,  AgentReaderXYAzimuth.class);
		connect.connectReaderToReceiver(AgentReaderXYAzimuth.class,AgentDrawerXYAzimuth.class);
		connect.connectReceiverToLayer(AgentDrawerXYAzimuth.class,AgentLayer.class);
	
		
		
//		connect.add(SimpleStaticNetLayer.NoQuadDrawer.class, SimpleStaticNetLayer.class);
//		connect.add(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
//		connect.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
//		connect.add(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.NoQuadDrawer.class);
		
		
		
		
		connect.connectWriterToReader(TileDrawerDataWriter.class,TileDrawerDataReader.class);
		//		connect.add(InundationDataWriter.class,InundationDataReader.class);
		//		connect.add(InundationDataReader.class,Dummy.class);
//		connect.connectWriterToReader(PolygonDataWriter.class,PolygonDataReader.class);
		connect.connectWriterToReader(TextureDataWriter.class,TextutreDataReader.class);
		if (this.occMap != null) {
			FeatureSource fs = ShapeFileReader.readDataFile(this.BUILDINGS_FILE);
			OTFSheltersDrawer sd = new OTFSheltersDrawer(fs,this.occMap,OTFServerQuad2.offsetNorth,OTFServerQuad2.offsetEast);
			this.quad.addAdditionalElement(new SheltersWriter(sd));
			connect.connectWriterToReader(SheltersWriter.class,SheltersReader.class);
			connect.connectReaderToReceiver(SheltersReader.class,TimeDependentTrigger.class);
		}
		
		if (this.insertWave) {
			connect.connectWriterToReader(InundationDataWriter_v2.class,InundationDataReader_v2.class);
			connect.connectReaderToReceiver(InundationDataReader_v2.class,TimeDependentTrigger.class);
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


	public void addVehicle(final double time, final AgentSnapshotInfo position) {
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
	
	public void setStartTime(double time) {
		this.startTime = time;
	}


}
