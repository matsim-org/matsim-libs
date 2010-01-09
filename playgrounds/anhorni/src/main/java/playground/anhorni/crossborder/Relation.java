package playground.anhorni.crossborder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;

public class Relation {
	
	private Zone fromZone;
	private Zone toZone;
	private double volume;
	private String type;
	// type means input type of: Tageszeitliche Fahrtenmatrizen:
	//P (Pendler), E (Einkauf), N (Nutzfahrt), S (Sonstiges)
	private int startTime;
	private ArrayList<Plan> plans;
	private List<MyLink> inLinkVolumes;
	private List<MyLink> outLinkVolumes;
	
	//private Hashtable<IdI, Double> inLinkVolumes = new Hashtable<IdI, Double>();
	//private Hashtable<IdI, Double> outLinkVolumes = new Hashtable<IdI, Double>();
	private double totalInLinkCapacity=0.0;
	private double totalOutLinkCapacity=0.0;
	
	
	public Relation() {
		this.plans=new ArrayList<Plan>();
		this.inLinkVolumes=new Vector<MyLink>();
		this.outLinkVolumes=new Vector<MyLink>();
	}
	
	public List<MyLink> getInLinkVolumes() {
		return inLinkVolumes;
	}

	public void setInLinkVolumes(List<MyLink> inLinkVolumes) {
		this.inLinkVolumes = inLinkVolumes;
	}

	public List<MyLink> getOutLinkVolumes() {
		return outLinkVolumes;
	}

	public void setOutLinkVolumes(List<MyLink> outLinkVolumes) {
		this.outLinkVolumes = outLinkVolumes;
	}
	
	public Zone getFromZone() {
		return this.fromZone;
	}
	
	public void setFromZone(Zone fromZone) {
		this.fromZone=fromZone;
	}
	
	public Zone getToZone() {
		return this.toZone;
	}
	
	public void setToZone(Zone toZone) {
		this.toZone=toZone;
	}

	public double getVolume() {
		return volume;
	}

	public void setVolume(double volume) {
		this.volume = volume;
	}
	
	public ArrayList<Plan> getPlans() {
		return plans;
	}

	public void setPlans(ArrayList<Plan> plans) {
		this.plans = plans;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public int getStartTime() {
		return startTime;
	}

	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}
	
	private void assignVolumesToLinks(final NetworkLayer network) {
		computeTotalLinkCapacities(network);
		
		Iterator<Integer> n_it = this.fromZone.getNodes().iterator();
		while (n_it.hasNext()) {
			Integer n_i=n_it.next();
			NodeImpl node=network.getNodes().get(new IdImpl(Integer.toString(n_i)));
					
			for (Link l : node.getOutLinks().values()) {
				if (this.totalOutLinkCapacity>0.0) {
					this.outLinkVolumes.add(new MyLink(l.getId(), this.volume*l.getCapacity(org.matsim.core.utils.misc.Time.UNDEFINED_TIME)/this.totalOutLinkCapacity));					
				}
				else {
					this.outLinkVolumes.add(new MyLink(l.getId(), 0.0));
				}
			}
		}
		
		n_it = this.toZone.getNodes().iterator();
		while (n_it.hasNext()) {
			Integer n_i=n_it.next();
			NodeImpl node=network.getNodes().get(new IdImpl(Integer.toString(n_i)));
		
			for (Link l : node.getInLinks().values()) {
				if (this.totalInLinkCapacity>0.0) {
					this.inLinkVolumes.add(new MyLink(l.getId(), this.volume*l.getCapacity(org.matsim.core.utils.misc.Time.UNDEFINED_TIME)/this.totalInLinkCapacity));
				}
				else {
					this.inLinkVolumes.add(new MyLink(l.getId(), 0.0));
				}
			}//for
		}//while
	}//assignVolumesToLinks

	private void computeTotalLinkCapacities(final NetworkLayer network) {
		this.totalOutLinkCapacity=0.0;
		this.totalInLinkCapacity=0.0;
				
		Iterator<Integer> n_it = this.fromZone.getNodes().iterator();
		while (n_it.hasNext()) {
			Integer n_i=n_it.next();			
			NodeImpl node=network.getNodes().get(new IdImpl(Integer.toString(n_i)));
			
			for (Link l : node.getOutLinks().values()) {
				this.totalOutLinkCapacity+=l.getCapacity(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
			}
		}
		
		n_it = this.toZone.getNodes().iterator();
		while (n_it.hasNext()) {
			Integer n_i=n_it.next();
			NodeImpl node=network.getNodes().get(new IdImpl(Integer.toString(n_i)));
		
			for (Link l : node.getInLinks().values()) {
				this.totalInLinkCapacity+=l.getCapacity(org.matsim.core.utils.misc.Time.UNDEFINED_TIME);
			}//for
		}
	}
	
	public void assignPlansToRelations(final NetworkLayer network) {
		
		if (!(this.volume>0.0)) return;
		
		assignVolumesToLinks(network);
				
		// We have to ensure the boundary condition that the created number of plans
		// is equal to the total relation volume
		double numberOfPlansLeft=this.volume;
		
		// Have to sort the links, such that the ones with the highest capacity 
		// get the volumes assigned first (rounding!)
		this.doSortingOfLinks();
		
		Iterator<MyLink> mlo_it = this.outLinkVolumes.iterator();
		while (mlo_it.hasNext()) {
			MyLink mlo_i=mlo_it.next();
			
			double volout=mlo_i.getVolume();
			int cnt=0;
			
			Iterator<MyLink> mli_it = this.inLinkVolumes.iterator();
			while (mli_it.hasNext()) {
				MyLink mli_i=mli_it.next();
				cnt++;
				double volin=mli_i.getVolume();
											 
				 int nrPlans=Math.round((float)(volout*(volin/this.volume)));			 
				 for (int i=0; i<nrPlans; i++) {
					 Plan plan=createPlan(mlo_i.getId(), mli_i.getId());
					 //Only add the plan, if number of created plans < total volume
					 if (numberOfPlansLeft>0.0) {
						 numberOfPlansLeft-=1.0;
						 this.plans.add(plan);
					 }
				 }
		     }
			//If the number of plans is smaller than this.volume, we assign all the plans to
			//the first outlink-inlink pair. More sophisticated solutions could be applied!
		}
		if (numberOfPlansLeft>0.0) {			
			for (int j=0; j<Math.round((float)(numberOfPlansLeft)); j++) {
				MyLink first_out=this.outLinkVolumes.get(0);
				MyLink first_in=this.inLinkVolumes.get(0);
				Plan plan=createPlan(first_out.getId(), first_in.getId());
				this.plans.add(plan);
			}//for
		}
	}
	
	// check the real numbers
	public boolean checkTransit() {
		//already checked for this.fromZone.getId()>Config.chNumbers in FMAParser
		if (this.toZone.getId()>Config.chNumbers) return true;
		else return false;	}
	
	public void assignStartingTime(int nrPlans, int recentlyAddedNumberOfPersons) {
		int cnt=recentlyAddedNumberOfPersons;
		Iterator<Plan> n_it = this.plans.iterator();
		while (n_it.hasNext()) {
			Plan plan_i=n_it.next();
			// time has to be given in seconds after midnight
			plan_i.setStartTime(startTime*3600+(int)(cnt*3600/nrPlans));
			cnt++;
		}//while
				
	}

	private void doSortingOfLinks() {
		Collections.sort(this.inLinkVolumes, new LinkComparator());
		Collections.sort(this.outLinkVolumes, new LinkComparator());
	}

	private Plan createPlan(Id out_id, Id in_id){
		
		 Plan plan=new Plan(out_id, in_id);
		 
		 //check if it is transit traffic
		 if (!checkTransit()) {
			 if (this.type.equals("P")) {
				 plan.setTempHomeType("h15");
				 plan.setActivityType("w9");
			 }
			 else if (this.type.equals("E")) {
				 plan.setTempHomeType("h19");
				 plan.setActivityType("s5");
			 }
			 else if (this.type.equals("N")) {
				 plan.setTempHomeType("h21");
				 plan.setActivityType("w3");
			 }
			 else if (this.type.equals("S")) {
				 plan.setTempHomeType("h21");
				 plan.setActivityType("l3");
			 }
		 }
		 else {
			 plan.setTempHomeType("h");
			 plan.setActivityType("tta");
		 }	
		return plan;	
	}
}

