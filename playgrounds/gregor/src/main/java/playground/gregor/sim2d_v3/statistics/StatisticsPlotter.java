package playground.gregor.sim2d_v3.statistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;

import playground.gregor.microcalibratedqsim.SVMClassifier;
import playground.gregor.sim2d_v3.events.XYVxVyEventsFileReader;
import playground.gregor.sim2d_v3.statistics.OnLinkStatistics.AgentInfo;

public class StatisticsPlotter {

	private final OnLinkStatistics le;

	public StatisticsPlotter(OnLinkStatistics le) {
		this.le = le;
	}

	public static void main(String [] args) {
		String eventsFile = "/Users/laemmel/devel/crossing/output/ITERS/it.0/0.events.xml.gz";
		//		String eventsFile = "/Users/laemmel/devel/gr90/input/events.xml";

		EventsManager mng = EventsUtils.createEventsManager();
		OnLinkStatistics le = new OnLinkStatistics();

		Id id1 = new IdImpl(2);
		Id id2 = new IdImpl(8);

		Id id3 = new IdImpl(-1);
		Id id4 = new IdImpl(-2);
		le.addObservationLinkId(id1);
		le.addObservationLinkId(id2);
		le.addObservationLinkId(id3);
		le.addObservationLinkId(id4);
		
		mng.addHandler(le);
		//		new EventsReaderXMLv1(mng).parse(eventsFile);
		new XYVxVyEventsFileReader(mng).parse(eventsFile);

		try {
			new StatisticsPlotter(le).mkPlots(id1,id2);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Testing only
	public void mkPlots(Id id1, Id id2) throws IOException {
		int steps = 1;




		BufferedWriter writer = new BufferedWriter(new FileWriter(new File("/Users/laemmel/devel/crossing/dbg/plot/data.arff")));
		writer.append("@relation test\n");
		for (int i = 0; i < 2*steps; i++) {
			String attrib = null; 
			if (i < steps) {
				attrib = "a"+i;
			} else {
				attrib = "b"+(i-steps);
			}
			writer.append("@attribute " + attrib + " numeric\n");
		}
		writer.append("@attribute class numeric\n");

		writer.append("@data\n");

		SVMClassifier clz = new SVMClassifier();

		List<AgentInfo> agents = this.le.getAndRemoveAgentInfos(id1);

		for (AgentInfo ai : agents) {
			double t = ai.enterTime;
			double cl = 5./ai.travelTime;


			if (Double.isNaN(cl) || Double.isInfinite(cl)) {
				continue;
			}
			double [] inst = new double[2];
			int attr = 0;
			for (int i : new int[]{2,8}) {
				Id id = new IdImpl(i);
				double[] c = this.le.getOnLinkHistory(id, t, steps);
				inst[attr++] = c[0];
//				c = this.le.getEnterHistory(id, t, 4);
//				inst[attr++] = c[0];
//				inst[attr++] = c[1];
//				inst[attr++] = c[2];
//				inst[attr++] = c[3];
//				c = this.le.getLeaveHistory(id, t, 4);
//				inst[attr++] = c[0];
//				inst[attr++] = c[1];
//				inst[attr++] = c[2];
//				inst[attr++] = c[3];
			}
			clz.addInstance(inst, cl);

			addLinkStats(id1,t,steps,writer);
			addLinkStats(id2,t,steps,writer);
			writer.append(cl+"\n");
		}
		writer.close();
		clz.validateAndBuild();
	}

	private void addLinkStats(Id id, double t, int steps, BufferedWriter writer) throws IOException {
		double[] counts = this.le.getOnLinkHistory(id, t, steps);
		for (int i = 0; i < steps; i++) {
			writer.append(counts[i]+",");
		}

	}
}
