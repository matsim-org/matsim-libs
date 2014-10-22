package playground.gregor.casim.monitoring;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.utils.collections.Tuple;

import playground.gregor.casim.simulation.physics.CAAgent;
import playground.gregor.casim.simulation.physics.CALink;

public class CALinkMonitorExact {

	private CALink l;
	private CAAgent[] parts;
	private int from;
	private int to;
	private double area;

	private CAAgent lastDsLv = null;
	private CAAgent lastDsEnt = null;
	private CAAgent lastUsLv = null;
	private CAAgent lastUsEnt = null;


	private LinkedList<AgentInfo> dsl = new LinkedList<AgentInfo>();
	private LinkedList<AgentInfo> usl = new LinkedList<AgentInfo>();
	private double realRange;

	private Measure lastM = new Measure(0);

	private List<Measure> ms = new ArrayList<Measure>();
	
	private double lastTriggered = -1;

	public CALinkMonitorExact(CALink l) {
		this.l = l;
		this.parts = l.getParticles();
		int num = l.getNumOfCells();
		double cellWidth = l.getLink().getLength()/num;
		double range = 10;
		double cells = range/cellWidth;
		this.from = (int) (num/2.-cells/2 +.5);
		this.to = (int) (num/2.+cells/2 +.5);
		this.realRange = (to-from)*cellWidth;
		this.area = realRange*l.getLink().getCapacity();

	}

	public void init() {
		for (int i = this.from; i <= this.to; i++) {
			if (this.parts[i] != null && this.parts[i].getDir() == -1) {
				AgentInfo ai = new AgentInfo();
				ai.enterTime = 0;
				this.usl.add(ai);
			}
		}
		for (int i = this.to; i >= this.from; i--) {
			if (this.parts[i] != null && this.parts[i].getDir() == 1) {
				AgentInfo ai = new AgentInfo();
				ai.enterTime = 0;
				this.dsl.add(ai);
			}
		}
		//6. update
		this.lastUsLv = this.parts[from-1];
		this.lastDsLv = this.parts[to+1];
		this.lastUsEnt = this.parts[to];
		this.lastDsEnt = this.parts[from];
		trigger(0);
	}

	public void trigger(double time) {
		if (time <= lastTriggered) {
			return;
		}
		lastTriggered = time;
		//1. check density
		int cnt = 0;
		int dsCnt = 0;
		int usCnt = 0;
		for (int i = this.from; i <= this.to; i++) {
			if (this.parts[i] != null) {
				cnt++;
				if (this.parts[i].getDir() == 1) {
					dsCnt++;
				} else {
					usCnt++;
				}
			}
		}
		double rho = cnt/this.area;
		double dsRho = dsCnt/this.area;
		double usRho = usCnt/this.area;

		//2. check ds left
		double dsTT = 0;
		double dsSpd = 0;
		if (this.parts[to+1] != null && this.parts[to+1] != lastDsLv && this.parts[to+1].getDir() ==1 ) {
			AgentInfo ai = dsl.pollFirst();
			dsTT = time-ai.enterTime;
			dsSpd = this.realRange/dsTT;
		}
		//3. check us left
		double usTT = 0;
		double usSpd = 0;
		if (this.parts[from-1] != null && this.parts[from-1] != lastUsLv && this.parts[from-1].getDir() == -1 ) {
			AgentInfo ai = usl.pollFirst();
			usTT = time-ai.enterTime;
			usSpd = this.realRange/usTT;
		}

		//4. check ds enter
		if (this.parts[from] != null && this.parts[from].getDir() == 1 && this.parts[from] != lastDsEnt) {
			AgentInfo ai = new AgentInfo();
			ai.enterTime = time;
			this.dsl.add(ai);
		}

		//5. check us enter
		if (this.parts[to] != null && this.parts[to].getDir() == -1 && this.parts[to] != lastUsEnt) {
			AgentInfo ai = new AgentInfo();
			ai.enterTime = time;
			this.usl.add(ai);
		}

		if (dsTT == 0 & usTT == 0) {
			this.lastUsEnt = this.parts[to];
			this.lastDsEnt = this.parts[from];
			return;
		}
		
		if (dsTT == 0) {
			dsSpd = lastM.dsSpd;
			dsRho = lastM.dsRho;
		} 
		if (usTT == 0) {
			usSpd = lastM.usSpd;
			usRho = lastM.usRho;
		}

		Measure m = new Measure(time);
		m.dsRho = dsRho;
		m.usRho = usRho;
		m.dsSpd = dsSpd;
		m.usSpd = usSpd;
		this.ms.add(m);

		lastM = m;
		//6. update
		this.lastUsLv = this.parts[from-1];
		this.lastDsLv = this.parts[to+1];
		this.lastUsEnt = this.parts[to];
		this.lastDsEnt = this.parts[from];

	}

	public void report(BufferedWriter bw) throws IOException {

		double dsRho = -1;
		double usRho = -1;
		double rho = -1;
		double dsSpd = -1;
		double usSpd = -1;
		List<Tuple<Integer,Integer>> ranges = new ArrayList<Tuple<Integer,Integer>>();
		int from = 0;
		int to = 0;
		double mxDiff = 0.05;
		int mnRange = 20;
		for (Measure m : this.ms){
			double usRhoDiff = Math.abs(usRho-m.usRho);
			double dsRhoDiff = Math.abs(dsRho-m.dsRho);
			double rhoDiff = Math.abs(rho-(m.dsRho+m.usRho));
			double dsSpdDiff = Math.abs(dsSpd-m.dsSpd);
			double usSpdDiff = Math.abs(usSpd-m.usSpd);
			if (rhoDiff > rho*mxDiff || usRhoDiff > usRho*mxDiff || dsRhoDiff > dsRho*mxDiff || dsSpdDiff > dsSpd*mxDiff || usSpdDiff > usSpd*mxDiff) {
				int range = to-from;
				if (range >= mnRange) {
					ranges.add(new Tuple<Integer,Integer>(from,to));
				}
				from = to;
				dsRho = m.dsRho;
				usRho = m.usRho;
				rho = (m.usRho+m.dsRho);
				usSpd = m.usSpd;
				dsSpd = m.dsSpd;
			}
			
			to++;
		}
		for (Tuple<Integer, Integer> t : ranges) {
			for (int i = t.getFirst(); i < t.getSecond(); i++) {
				Measure m = this.ms.get(i);
				bw.append(m.time + " " + m.dsRho + " " + m.dsSpd + " " + m.usRho + " " + m.usSpd +"\n");
			}
		}
//		for (Measure m : this.ms){
////			if (m.time<20 || m.time >100) {
//////				return;
////				continue;
////			}
//			bw.append(m.time + " " + m.dsRho + " " + m.dsSpd + " " + m.usRho + " " + m.usSpd +"\n");
//		}


	}



	private static final class AgentInfo {
		double enterTime;
	}

	private final class Measure {
		public Measure(double time) {
			this.time = time;
		}
		double dsSpd;
		double dsRho;
		double usSpd;
		double usRho;
		double time;
	}

}
