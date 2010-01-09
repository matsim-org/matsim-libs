package playground.anhorni.crossborder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;


class OnlineWriter {
	
	private BufferedWriter out;
	private boolean fileOpened = false;
	private ArrayList<Plan> allPlans;
	private String fileName;
	private NetworkLayer network;
	
	public OnlineWriter() {}
		
	public void initWriter() {
		try {
			this.out = IOUtils.getBufferedWriter(this.fileName);
			this.fileOpened = true;
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	public void setNetwork(NetworkLayer network){
		this.network=network;
	}
	
	public void setPlans(ArrayList<Plan> allPlans) {
		this.allPlans=allPlans;
	}
	
	public void setFileName(String fileName) {
		this.fileName=fileName;
	}
		
	private void startPerson(int id) throws IOException {
		this.out.write("\t<person");
		this.out.write(" id=\"" + id + "\"");
		this.out.write(" sex=\"" + "m" + "\"");
		this.out.write(" age=\"" + "20" + "\"");
		this.out.write(" license=\"" + "yes" + "\"");
		this.out.write(" car_avail=\"" + "always" + "\"");
		this.out.write(" employed=\"" + "yes" + "\"");
		this.out.write(">\n");
		this.out.flush();
		
		
	}

	private void endPerson() throws IOException {
		this.out.write("\t</person>\n\n");
	}
	
	// <plan ... > ... </plan> ----------------------------------------------------------------
	private void startPlan(final Plan plan) throws IOException {
		this.out.write("\t\t<plan");
		this.out.write(" selected=\"" + "yes" + "\"");
		this.out.write(">\n");
		this.writeActLeg(plan);		
	}

	private void endPlan() throws IOException {
		this.out.write("\t\t</plan>\n\n");
	}
	
	// <act ... > ... </act> ------------------------------------------------------------------
	private void writeActLeg(final Plan plan) throws IOException {
		
		// use coords not links
		//Home act		
		double xHome=this.network.getLinks().get(plan.getStartLink()).getCoord().getX();
		double yHome=this.network.getLinks().get(plan.getStartLink()).getCoord().getY();	
		this.out.write("\t\t\t<act");
		this.out.write(" type=\"" + plan.getTempHomeType() + "\"");
		this.out.write(" x=\"" + xHome + "\" y=\"" +yHome+ "\"");	
		this.out.write(" end_time=\"" + Time.writeTime(plan.getStartTime()) + "\"");
		this.out.write("/>\n");

		//leg:
		this.out.write("\t\t\t<leg");
		this.out.write(" num=\"0\" mode=\"car\"/>\n");

		//act		
		int actDur=0;
		if (plan.getActivityType().equals("w9")) {actDur=9*3600;}
		if (plan.getActivityType().equals("s5")) {actDur=5*3600;}
		if (plan.getActivityType().equals("w3")) {actDur=3*3600;}
		if (plan.getActivityType().equals("l3")) {actDur=3*3600;}
		
		
		double xAct=this.network.getLinks().get(plan.getEndLink()).getCoord().getX();
		double yAct=this.network.getLinks().get(plan.getEndLink()).getCoord().getY();		
		this.out.write("\t\t\t<act");
		this.out.write(" type=\"" + plan.getActivityType() + "\"");
		this.out.write(" x=\"" + xAct + "\" y=\"" +yAct+ "\"");	
		// check start time
		if (!plan.getActivityType().equals("tta")) {
			this.out.write(" start_time=\"" + Time.writeTime(plan.getStartTime()) + "\"");
			this.out.write(" dur=\"" + Time.writeTime(actDur) + "\"");
		}
		this.out.write("/>\n");
		
		// 2 DO: check which share actually returns home on the same day	
		// add home act if not transit traffic
		if (!plan.getActivityType().equals("tta")) {
			
			//leg:
			this.out.write("\t\t\t<leg");
			this.out.write(" num=\"1\" mode=\"car\"/>\n");
			
			this.out.write("\t\t\t<act");
			this.out.write(" type=\"" + plan.getTempHomeType() + "\"");
			this.out.write(" x=\"" + xHome + "\" y=\"" +yHome+ "\"");
			this.out.write("/>\n");
		}		
		this.out.flush();	
	}

	private final void writePlans(int actPersonNumber) {
		Iterator<Plan> p_it = this.allPlans.iterator();
		int i=actPersonNumber;
		i+=1000000000;
		while (p_it.hasNext()) {
			Plan plan = p_it.next();
			i++;
			try {	
				this.startPerson(i);
				this.startPlan(plan);
				this.endPlan();
				this.endPerson();
				this.out.flush();
			}
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
	}

	// called after last iteration
	public void endWrite() {					
		if (this.fileOpened) {
			try {
				this.out.flush();
				this.out.close();
				this.fileOpened=false;
			}
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
	}

	public void write(int actPersonNumber) {
		this.writePlans(actPersonNumber);
	}
}
