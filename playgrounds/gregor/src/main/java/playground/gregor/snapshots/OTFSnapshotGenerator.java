package playground.gregor.snapshots;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.evacuation.otfvis.drawer.OTFBackgroundTexturesDrawer;

import playground.gregor.MY_STATIC_STUFF;
import playground.gregor.snapshots.postprocessors.ConfluenceArrowsFromEvents;
import playground.gregor.snapshots.postprocessors.DestinationDependentColorizer;
import playground.gregor.snapshots.postprocessors.EvacuationLinksTeleporter;
import playground.gregor.snapshots.postprocessors.SheltersColorizer;
import playground.gregor.snapshots.postprocessors.TimeDependentColorizer;
import playground.gregor.snapshots.writers.LineStringTree;
import playground.gregor.snapshots.writers.MVISnapshotWriter;
import playground.gregor.snapshots.writers.PositionInfo;
import playground.gregor.snapshots.writers.SnapshotGenerator;

public class OTFSnapshotGenerator {

	public final static String SHARED_SVN = "../../../../../arbeit/svn/shared-svn";
	public final static String RUNS_SVN = "../../../../../arbeit/svn/runs-svn/run1006";
	
	private final static String lsFile = SHARED_SVN + "/studies/countries/id/padang/gis/network_v20080618/d_ls.shp";

	private final static double VIS_OUTPUT_SAMPLE = 1.;
	
	private final ScenarioImpl scenario;
	private final String eventsFile;
	
	private final String txtSnapshotFile = null;
//	private final String txtSnapshotFile = "../../../outputs/output/snapshots.txt.gz";
	
	public OTFSnapshotGenerator() {
		
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(RUNS_SVN + "/output/output_config.xml.gz");
		
		this.scenario = sl.getScenario();
		this.scenario.getConfig().network().setInputFile(RUNS_SVN + "/output/output_network.xml.gz");
		this.scenario.getConfig().simulation().setSnapshotFormat("otfvis");
		this.scenario.getConfig().simulation().setSnapshotPeriod(60);
//		this.scenario.getConfig().simulation().setEndTime(4*3600+30*60);
		this.scenario.getConfig().simulation().setEndTime(5*3600);
		this.scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		
		this.scenario.getConfig().evacuation().setBuildingsFile(SHARED_SVN + "/studies/countries/id/padang/gis/buildings_v20090728/evac_zone_buildings_v20090728.shp");
//		this.scenario.getConfig().evacuation().setSampleSize("0.1");
		this.scenario.getConfig().controler().setLastIteration(0);
		int it = this.scenario.getConfig().controler().getLastIteration();
		sl.loadNetwork();
		this.eventsFile = MY_STATIC_STUFF.OUTPUTS + "/output/ITERS/it." + it + "/" + it + ".events.txt.gz";
		
//		this.txtSnapshotFile = "../../outputs/output/snapshots.txt.gz";
	}

	public OTFSnapshotGenerator(String[] args) {
		this.eventsFile = MY_STATIC_STUFF.OUTPUTS + "/output/ITERS/it." + args[1] + "/" + args[1] + ".events.txt.gz"; 
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(args[0]);
		sl.loadNetwork();
		this.scenario = sl.getScenario();
	}

	public void run() {
		
		PositionInfo.setLANE_WIDTH(this.scenario.getNetwork().getEffectiveLaneWidth());
		
		EventsManagerImpl ev = new EventsManagerImpl();
		DestinationDependentColorizer d = new DestinationDependentColorizer();
		ev.addHandler(d);
		EvacuationLinksTeleporter e = new EvacuationLinksTeleporter();
//		AllAgentsTeleporter aat = new AllAgentsTeleporter();
		TimeDependentColorizer t = new TimeDependentColorizer();
		ev.addHandler(t);
//		EventsImpl evII = new EventsImpl();
//		evII.addHandler(t);
//		new EventsReaderTXTv1(evII).readFile("../../outputs/output_inf_floCap/ITERS/it.0/0.events.txt.gz");
		
		SheltersColorizer s = new SheltersColorizer(this.scenario.getConfig().evacuation().getBuildingsFile(),this.scenario.getConfig().simulation().getSnapshotPeriod(), this.scenario.getConfig().evacuation().getSampleSize());
		ev.addHandler(s);
		
		OTFBackgroundTexturesDrawer sbg = new OTFBackgroundTexturesDrawer("arrow.png");
		ConfluenceArrowsFromEvents c = new ConfluenceArrowsFromEvents(sbg,this.scenario.getNetwork());
		
//		SimpleBackgroundTextureDrawer sbgII = new SimpleBackgroundTextureDrawer("./res/arrow.png");
//		SimpleBackgroundTextureDrawer sbgIII = new SimpleBackgroundTextureDrawer("./res/blocked.png");
//		WrongDirectionArrowsFromEvents w = new WrongDirectionArrowsFromEvents(sbgII,sbgIII,this.scenario.getNetwork());
		
		ev.addHandler(c);
//		ev.addHandler(w);
		new EventsReaderTXTv1(ev).readFile(this.eventsFile);
		ev.removeHandler(d);
		ev.removeHandler(t);
		ev.removeHandler(c);
//		ev.removeHandler(w);
//		c.createArrows();
//		w.createArrows();
		ev.removeHandler(s);
		
		
		PositionInfo.lsTree = new LineStringTree(getFeatures(),this.scenario.getNetwork());
		
		this.scenario.getNetwork().addNode(
				this.scenario.getNetwork().getFactory().createNode(new IdImpl("minXY"), new CoordImpl(643000,9870000)));//HACK to get the bounding box big enough; 
		//otherwise we could get negative openGL coords since we calculating offsetEast, offsetNorth based on this bounding box
		
		MVISnapshotWriter writer = new MVISnapshotWriter(this.scenario);
		writer.addSimpleBackgroundTextureDrawer(sbg);
		writer.setSheltersOccupancyMap(s.getOccMap());
//		writer.addSimpleBackgroundTextureDrawer(sbgII);
//		writer.addSimpleBackgroundTextureDrawer(sbgIII);
		
		SnapshotGenerator sg = new SnapshotGenerator(this.scenario,writer);
		if (this.txtSnapshotFile != null) {
			sg.enableTableWriter(this.txtSnapshotFile);
		}
		
		sg.setVisOutputSample(VIS_OUTPUT_SAMPLE);
		ev.addHandler(sg);
		sg.addColorizer(d);
		sg.addColorizer(e);
		sg.addColorizer(t);
		
//		sg.addColorizer(aat);
		
		new EventsReaderTXTv1(ev).readFile(this.eventsFile);
		sg.finish();
	}
	
	private Collection<Feature> getFeatures() {
		
		FeatureSource fts = null;
		try {
			fts = ShapeFileReader.readDataFile(lsFile);
		} catch (final Exception e) {
			// TODO Auto-generated catch block/run1006
			e.printStackTrace();
		}
		final Collection<Feature> fa = new ArrayList<Feature>();
		Iterator it = null;
		try {
			it = fts.getFeatures().iterator();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (it.hasNext()) {
			fa.add((Feature) it.next());
		}
		
		return fa;
	}

	public static void main(String [] args) {

		new OTFSnapshotGenerator().run();
	}
}

