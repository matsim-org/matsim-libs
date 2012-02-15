package playground.gregor.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.evacuation.base.Building;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.gregor.gis.buildinglinkmapping.Loader;
import playground.gregor.gis.buildinglinkmapping.PersonBuildingMappingParser;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class EvacCurve implements AgentArrivalEventHandler, AgentDepartureEventHandler {

	private final String events;
	private double time = 0;
	private int evacuated = 0;
	private ArrayList<Integer> outflow;
	private BufferedWriter out = null;
	private int offset;
    private  final Envelope ev;

    private final Set<Id> excl = new HashSet<Id>();
	private final Map<Id, Id> pbm;
	private final Map<Id, BuildingInfo> bldM;

	public EvacCurve(String input, String output, Map<Id, Id> pbm,
			Map<Id, BuildingInfo> bldM) {
		this.events = input;
		try {
			this.out = IOUtils.getBufferedWriter(output, false);
		} catch (Exception e) {
			e.printStackTrace();
		}

        		Coordinate c0 = new Coordinate(649694.00-100,9894872.00+100);
		Coordinate c1 = new Coordinate(653053.00+100,9892897.00-100);
		this.ev = new Envelope(c0, c1);
		this.pbm = pbm;
		this.bldM = bldM;
	}


	private void run() {
		this.outflow = new ArrayList<Integer>();
	EventsManager e = EventsUtils.createEventsManager();
		e.addHandler(this);
		new EventsReaderTXTv1(e).readFile(this.events);
		finish();
	}

    @Override
	public void handleEvent(final AgentDepartureEvent event) {
    	Id id = this.pbm.get(event.getPersonId());
    	BuildingInfo b = this.bldM.get(id);
    	if (!this.ev.contains(b.b.getGeo().getCentroid().getCoordinate())) {
    		this.excl.add(event.getPersonId());
    	}
    }

	@Override
	public void handleEvent(final AgentArrivalEvent event) {
        if (this.excl.contains(event.getPersonId())) {
            return;
        }
		this.evacuated++;
		if (this.time == 0) {
			this.offset = (int) event.getTime();
			this.time = 1;
			writeLine(new String[] { this.time + "", this.evacuated + "" });
		}

		if ((event.getTime() - this.offset) > this.time) { // && ((int)
			// (event.getTime()
			// - this.offset) %
			// 180 == 0)) {
			this.time =  (event.getTime() - this.offset)/60;
			writeLine(new String[] { this.time + "", this.evacuated + "" });
		}
	}

	public void writeLine(final String[] line) {
		StringBuffer buff = new StringBuffer();
		buff.append(line[0]);
		for (int i = 1; i < line.length; i++) {
			buff.append("\t");
			buff.append(line[i]);
		}
		buff.append("\n");
		try {
			this.out.write(buff.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void finish() {
		if (this.out != null) {
			try {
				this.out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(final String[] args) {

        int run = 1394;

		String conf = "/Users/laemmel/arbeit/papers/2012/lastMile/matsim/config.xml";
		String runBase = "/Users/laemmel/svn/runs-svn/run1394/";
		Config c = ConfigUtils.loadConfig(conf);
		
//		c.plans().setInputFile(runBase + "/output/output_plans.xml.gz"); 
		
		Scenario sc = ScenarioUtils.loadScenario(c);
		Loader loader = new Loader(sc);
		loader.loadData();
		List<Building> buildings = loader.getBuildings();
		System.out.println(buildings.size());
		Map<Id,BuildingInfo> bldM = new HashMap<Id,BuildingInfo>();
		for (Building b : buildings) {
			BuildingInfo bi = new BuildingInfo();
			bi.b = b;
			bldM.put(b.getId(),bi);
		}

		Map<Id, Id> pbm = new PersonBuildingMappingParser().getPersonBuildingMappingFromFile(runBase + "/output/person_buildings_mapping");

		String baseDir = "/Users/laemmel/svn/runs-svn/run" + run + "/output/ITERS";
		String baseOutput = "/Users/laemmel/svn/runs-svn/run" + run + "/analysis/";
		ArrayList<Integer> its = new ArrayList<Integer>();
		// its.add(0); its.add(1); its.add(10); its.add(50); its.add(100);
		// its.add(200);
//		its.add(0);
		its.add(1000);
		for (int it : its) {
			String input = baseDir + "/it." + it + "/" + it + ".events.txt.gz";
			String output = baseOutput + "/" + run + "it." + it + ".outflow.txt";
			new EvacCurve(input, output,pbm,bldM).run();

		}

	}

	@Override
	public void reset(final int iteration) {
		// TODO Auto-generated method stub

	}
	private static final class BuildingInfo {
		Building b;
		List<Double> times = new ArrayList<Double>();
		Id linkId = null;
	}
}