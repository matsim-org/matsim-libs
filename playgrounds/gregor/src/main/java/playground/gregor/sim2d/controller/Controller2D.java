package playground.gregor.sim2d.controller;


import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.evacuation.otfvis.drawer.OTFBackgroundTexturesDrawer;
import org.matsim.evacuation.otfvis.readerwriter.PolygonDataReader;
import org.matsim.evacuation.otfvis.readerwriter.PolygonDataWriter;
import org.matsim.evacuation.otfvis.readerwriter.TextureDataWriter;
import org.matsim.evacuation.otfvis.readerwriter.TextutreDataReader;
import org.matsim.ptproject.qsim.QNetwork;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.data.DefaultConnectionManagerFactory;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.opengl.drawer.SimpleBackgroundDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleBackgroundLayer;
import org.matsim.vis.otfvis.server.OnTheFlyServer;
import org.matsim.world.algorithms.WorldCheck;

import playground.gregor.sim2d.otfdebug.drawer.Agent2DDrawer;
import playground.gregor.sim2d.otfdebug.drawer.ForceArrowDrawer;
import playground.gregor.sim2d.otfdebug.layer.Agent2DLayer;
import playground.gregor.sim2d.otfdebug.layer.ForceArrowLayer;
import playground.gregor.sim2d.otfdebug.readerwriter.Agent2DReader;
import playground.gregor.sim2d.otfdebug.readerwriter.Agent2DWriter;
import playground.gregor.sim2d.otfdebug.readerwriter.ForceArrowReader;
import playground.gregor.sim2d.otfdebug.readerwriter.ForceArrowWriter;
import playground.gregor.sim2d.scenario.ScenarioLoader2DImpl;
import playground.gregor.sim2d.simulation.Sim2D;
import playground.gregor.sim2d.simulation.StaticForceField;

import com.vividsolutions.jts.geom.MultiPolygon;

public class Controller2D extends Controler {

	private Map<MultiPolygon,List<Link>> mps;

	protected OnTheFlyServer myOTFServer = null;
	private final OTFConnectionManager connectionManager = new DefaultConnectionManagerFactory().createConnectionManager();

	private final Agent2DWriter agentWriter;

	private final ForceArrowWriter forceArrowWriter;

	private StaticForceField sff;



	public Controller2D(String[] args) {
		super(args);
		this.setOverwriteFiles(true);


		NetworkLayer fakeNetwork = new NetworkLayer();
		fakeNetwork.createAndAddNode(new IdImpl(0), new CoordImpl(386008.21f,5820000.04f));
		fakeNetwork.createAndAddNode(new IdImpl(1), new CoordImpl(386241.2f,5820247.05f));
		QNetwork fakeQNetwork = new QNetwork(fakeNetwork);
		UUID idOne = UUID.randomUUID();
		this.myOTFServer = OnTheFlyServer.createInstance("OTFServer_" + idOne.toString(), fakeQNetwork, getEvents(), false);
		OTFBackgroundTexturesDrawer sbg = new OTFBackgroundTexturesDrawer("../../../../sim2d/sg4.png");
//		sbg.addLocation(new CoordImpl(386124.75,5820130.6), 0, 33.33);
		sbg.addLocation(386108.0859f,5820114.04f,386141.2f,5820147.092897f);

		this.myOTFServer.addAdditionalElement(new TextureDataWriter(sbg));
		this.connectionManager.add(SimpleBackgroundDrawer.class, OGLSimpleBackgroundLayer.class);
		this.connectionManager.add(TextureDataWriter.class,TextutreDataReader.class);

		float [] linksColor = new float [] {.5f,.5f,.5f,.7f};
		try {
			this.myOTFServer.addAdditionalElement(new PolygonDataWriter(ShapeFileReader.readDataFile("../../../../sim2d/sg4graph.shp"),linksColor));
			this.connectionManager.add(PolygonDataWriter.class,PolygonDataReader.class);
		} catch (IOException e1) {
			e1.printStackTrace();
		}


		this.agentWriter = new Agent2DWriter();
		this.myOTFServer.addAdditionalElement(this.agentWriter);
		this.connectionManager.add(Agent2DWriter.class,  Agent2DReader.class);
		this.connectionManager.add(Agent2DReader.class,  Agent2DDrawer.class);
		this.connectionManager.add( Agent2DDrawer.class, Agent2DLayer.class);

		this.forceArrowWriter = new ForceArrowWriter();
		this.myOTFServer.addAdditionalElement(this.forceArrowWriter);
		this.connectionManager.add(ForceArrowWriter.class,ForceArrowReader.class);
		this.connectionManager.add(ForceArrowReader.class,ForceArrowDrawer.class);
		this.connectionManager.add(ForceArrowDrawer.class,ForceArrowLayer.class);

		OTFClientLive client = null;
		client = new OTFClientLive("rmi:127.0.0.1:4019:OTFServer_" + idOne.toString(), this.connectionManager);
		client.start();

		try {
			this.myOTFServer.pause();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}




	@Override
	protected void loadData() {
		if (!this.scenarioLoaded) {
			this.loader = new ScenarioLoader2DImpl(this.scenarioData);
			this.loader.loadScenario();
			this.mps = ((ScenarioLoader2DImpl)this.loader).getFloorLinkMapping();
			this.sff = ((ScenarioLoader2DImpl)this.loader).getStaticForceField();
			this.network = this.loader.getScenario().getNetwork();
			this.population = this.loader.getScenario().getPopulation();
			this.scenarioLoaded = true;

			if (this.getScenario().getWorld() != null) {
				new WorldCheck().run(this.getScenario().getWorld());
			}
		}
	}

	@Override
	protected void runMobSim() {
		Sim2D sim = new Sim2D(this.network,this.mps,this.population,this.events,this.sff);
		sim.setOTFStuff(this.myOTFServer,this.agentWriter,this.forceArrowWriter);
		sim.run();
	}

	public static void main(String [] args){
		Controler controller = new Controller2D(args);
		controller.run();

	}

}
