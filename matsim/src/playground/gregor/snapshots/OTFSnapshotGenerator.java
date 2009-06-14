package playground.gregor.snapshots;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.core.events.Events;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.gregor.MY_STATIC_STUFF;
import playground.gregor.otf.ConfluenceArrowsFromEvents;
import playground.gregor.otf.SimpleBackgroundTextureDrawer;
import playground.gregor.snapshots.postprocessors.DestinationDependentColorizer;
import playground.gregor.snapshots.postprocessors.EvacuationLinksTeleporter;
import playground.gregor.snapshots.postprocessors.TimeDependentColorizer;
import playground.gregor.snapshots.writers.LineStringTree;
import playground.gregor.snapshots.writers.MVISnapshotWriter;
import playground.gregor.snapshots.writers.PositionInfo;
import playground.gregor.snapshots.writers.SnapshotGenerator;

public class OTFSnapshotGenerator {

	private final static String lsFile = "../../../workspace/vsp-cvs/studies/padang/gis/network_v20080618/d_ls.shp";

	private Scenario scenario;
	private String eventsFile;

	public OTFSnapshotGenerator(String[] args) {
		this.eventsFile = MY_STATIC_STUFF.OUTPUTS + "/output/ITERS/it." + args[1] + "/" + args[1] + ".events.txt.gz"; 
		ScenarioLoader sl = new ScenarioLoader(args[0]);
		sl.loadNetwork();
		this.scenario = sl.getScenario();
	}

	public void run() {
		
		PositionInfo.setLANE_WIDTH(this.scenario.getNetwork().getEffectiveLaneWidth());
		
		Events ev = new Events();
		DestinationDependentColorizer d = new DestinationDependentColorizer();
		ev.addHandler(d);
		EvacuationLinksTeleporter e = new EvacuationLinksTeleporter();
		TimeDependentColorizer t = new TimeDependentColorizer();
		ev.addHandler(t);
		
		SimpleBackgroundTextureDrawer sbg = new SimpleBackgroundTextureDrawer("./res/arrow.png");
		ConfluenceArrowsFromEvents c = new ConfluenceArrowsFromEvents(sbg,this.scenario.getNetwork());
		ev.addHandler(c);
		new EventsReaderTXTv1(ev).readFile(this.eventsFile);
		ev.removeHandler(d);
		ev.removeHandler(t);
		ev.removeHandler(c);
		c.createArrows();
		
		PositionInfo.lsTree = new LineStringTree(getFeatures(),(NetworkLayer) this.scenario.getNetwork());
		
		MVISnapshotWriter writer = new MVISnapshotWriter(this.scenario);
		writer.setSimpleBackgroundTextureDrawer(sbg);
		
		SnapshotGenerator sg = new SnapshotGenerator(this.scenario,writer);
		ev.addHandler(sg);
		sg.addColorizer(d);
		sg.addColorizer(e);
		sg.addColorizer(t);
		new EventsReaderTXTv1(ev).readFile(this.eventsFile);
		sg.finish();
	}
	
	private Collection<Feature> getFeatures() {
		
		FeatureSource fts = null;
		try {
			fts = ShapeFileReader.readDataFile(lsFile);
		} catch (final Exception e) {
			// TODO Auto-generated catch block
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

		new OTFSnapshotGenerator(args).run();
	}
}
