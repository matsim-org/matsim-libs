package playground.anhorni.crossborder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

class Writer {
	
	private BufferedWriter out;
	private boolean fileOpened = false;
	private ArrayList<Plan> allPlans;
	private String file;
	
	public Writer() {}
	
	public Writer(ArrayList<Plan> allPlans) {
		this.allPlans=allPlans;
	}
		
	// <plans ... > ... </plans> --------------------------------------------------------------
	public void startPlans() throws IOException {
		this.out.write("<plans");
		this.out.write(" name=\"" + Config.plansName + "\"");
		this.out.write(">\n\n");
	}

	public void endPlans() throws IOException {
		this.out.write("</plans>\n");
	}

	// <person ... > ... </person> ------------------------------------------------------------
	public void startPerson(int id) throws IOException {
		this.out.write("\t<person");
		this.out.write(" id=\"" + id + "\"");
		this.out.write(" sex=\"" + "m" + "\"");
		this.out.write(" age=\"" + "20" + "\"");
		this.out.write(" license=\"" + "yes" + "\"");
		this.out.write(" car_avail=\"" + "always" + "\"");
		this.out.write(" employed=\"" + "yes" + "\"");
		this.out.write(">\n");
	}

	public void endPerson() throws IOException {
		this.out.write("\t</person>\n\n");
	}
	
	// <plan ... > ... </plan> ----------------------------------------------------------------
	public void startPlan(final Plan plan) throws IOException {
		this.out.write("\t\t<plan");
		this.out.write(" selected=\"" + "yes" + "\"");
		this.out.write(">\n");
		this.writeActLeg(plan);		
	}

	public void endPlan() throws IOException {
		this.out.write("\t\t</plan>\n\n");
	}
	
	// <act ... > ... </act> ------------------------------------------------------------------
	public void writeActLeg(final Plan plan) throws IOException {
		
		// no coords given as link id is provided
		//Home act
		this.out.write("\t\t\t<act");
		this.out.write(" type=\"" + plan.getTempHomeType() + "\"");
		this.out.write(" link=\"" + plan.getStartLink() + "\"");	
		this.out.write(" start_time=\"" + Time.writeTime(0.0) + "\"");
		this.out.write(" dur=\"" + Time.writeTime(Time.UNDEFINED_TIME) + "\"");
		this.out.write(" end_time=\"" + Time.writeTime(plan.getStartTime()) + "\"");
		this.out.write(">\n");
		this.out.write("\t\t\t</act>\n");
		
		/*
		QUESTION: is leg needed or not
		ANSWER: <!ELEMENT plan           (act|leg)*>  => NO!
		//leg:
		this.out.write("\t\t\t<leg");
		this.out.write(" num=\" 0 \"");
		this.out.write(" mode=\" car \"");
		this.out.write(" dep_time=\"" + Time.writeTime(plan.getStartTime()) + "\"");
		this.out.write(" trav_time=\"" + Time.writeTime(Time.UNDEFINED_TIME) + "\"");
		this.out.write(" arr_time=\"" + Time.writeTime(Time.UNDEFINED_TIME) + "\"");
		this.out.write(">\n");
		this.out.write("\t\t\t</leg>\n");
		*/
		
		//act		
		this.out.write("\t\t\t<act");
		this.out.write(" type=\"" + plan.getActivityType() + "\"");
		this.out.write(" link=\"" + plan.getEndLink() + "\"");	
		// check start time
		this.out.write(" start_time=\"" + Time.writeTime(plan.getStartTime()) + "\"");
		this.out.write(" dur=\"" + Time.writeTime(Time.UNDEFINED_TIME) + "\"");
		this.out.write(" end_time=\"" + Time.writeTime(Time.UNDEFINED_TIME) + "\"");
		this.out.write(">\n");
		this.out.write("\t\t\t</act>\n");
		
		// 2 DO: check which share actually returns home on the same day
		
		// add home act if not transit traffic
		if (!plan.getActivityType().equals("tta")) {
			this.out.write("\t\t\t<act");
			this.out.write(" type=\"" + plan.getTempHomeType() + "\"");
			this.out.write(" link=\"" + plan.getEndLink() + "\"");
			// check start time
			this.out.write(" start_time=\"" + Time.writeTime(Time.UNDEFINED_TIME) + "\"");
			this.out.write(" dur=\"" + Time.writeTime(Time.UNDEFINED_TIME) + "\"");
			this.out.write(" end_time=\"" + Time.writeTime(Time.UNDEFINED_TIME) + "\"");
			this.out.write(">\n");
			this.out.write("\t\t\t</act>\n");
		}
		
		this.out.flush();
		
	}


	public void writeSeparator() throws IOException {
		this.out.write("<!-- =================================================" +
							"===================== -->\n\n");
	}

	private void writeXmlHead() throws IOException {
		this.out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
	}

	private void writeDoctype() throws IOException {
		this.out.write("<!DOCTYPE plans SYSTEM \"" + Config.DTD + "\">\n");
	}

	public final void writeStartPlans() {
		try {
			this.out = IOUtils.getBufferedWriter(this.file);
			this.fileOpened = true;
			this.writeXmlHead();
			this.writeDoctype();
			this.out.flush();
			this.startPlans();
			this.writeSeparator();
			this.out.flush();	
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	public final void writePlans() {
		Iterator<Plan> p_it = this.allPlans.iterator();
		int cnt=1000000000;
		while (p_it.hasNext()) {
			Plan plan = p_it.next();
			cnt++;
			try {
				this.startPerson(cnt);
				this.startPlan(plan);
				this.endPlan();
				this.endPerson();
			}
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
	}

	public final void writeEndPlans() {
		if (this.fileOpened) {
			try {
				this.endPlans();
				this.out.flush();
				this.out.close();
			}
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
	}
	
	public void write(String file) {
		this.file=file;
		System.out.println("number of persons to write: "+ this.allPlans.size());
		this.writeStartPlans();
		this.writePlans();
		this.writeEndPlans();
	}
	
}
