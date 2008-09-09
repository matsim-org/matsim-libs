/**
 * 
 */
package playground.mfeil;



/**
 * @author Matthias Feil
 * Collection of code no longer used
 *
 */
public class OldStuff {
	
	//ArrayList<Object> al = new ArrayList<Object> (plan.getActsLegs());
	//ArrayList<Object> al = plan.getActsLegs();
	//int i = (al.size()/2)+1;  //size() returns Acts und Legs, therefore dividing by 2 yields number of Legs, +1 yields number of Acts.
	//System.out.println ("Das ist die Länge der Aktivitätenliste "+i);

	//Leg leg;
	//leg = (Leg)(al.get(1));
	//ArrayList<Node> nodes = new ArrayList<Node> ();
	//nodes = leg.getRoute().getRoute();
	
	//if (i>3){
		//System.out.println("Hier gibt es eine lange Liste: "+al);
		//Object oOne = al.get(4);
		//Object oTwo = al.get(2);
		//System.out.println("Das ist Objekt o: "+oOne);
		//System.out.println("Das ist Objekt o: "+oTwo);
		//al.add(2, oOne);
		//al.add(5, oTwo);
		//al.remove(3);
		//al.remove(5);
		//System.out.println("Nun schaut die Liste so aus: "+al);		
		
	//}
	//this.planomatAlgorithm.run (plan); //Calling standard Planomat to optimise start times and mode choice
	//System.out.println("Plan davor: "+plan.getPerson().getId()+" mit dem Leg: "+nodes);	
	
	//this.router.run(plan);
	
	//Leg leg1;
	//leg1 = (Leg)(al.get(1));
	//ArrayList<Node> nodes1 = new ArrayList<Node> ();
	//nodes1 = leg1.getRoute().getRoute();
	//System.out.println("Plan danach: "+plan.getPerson().getId()+" mit dem Leg: "+nodes1);
	

	//////////////////////////////////////////////////////////////////////
	// New section neighbourhood definition (under construction)
	//////////////////////////////////////////////////////////////////////
	/**		
*		PlanomatXPlan [] neighbourhood = new PlanomatXPlan [neighbourhoodSize+1];
*		int z;
*		for (z = 0; z < neighbourhood.length; z++){
*			neighbourhood[z] = new PlanomatXPlan (plan.getPerson());
*			neighbourhood[z].copyPlan(plan);
*		}
*		int x = 2;
*		for (z = 1; z<(int)(neighbourhoodSize*weightChangeOrder); z++){
*			x =this.changeOrder(neighbourhood[z], x);
*		}
*	
*		for (z = (int) (neighbourhoodSize*weightChangeOrder); z<((int)(neighbourhoodSize*weightChangeOrder)+(int)(neighbourhoodSize*weightChangeNumber)); z++){
*			neighbourhood[z]=this.changeNumber(neighbourhood[z]);
*		}
*	
*		for (z = (int) (neighbourhoodSize*weightChangeOrder)+(int)(neighbourhoodSize*weightChangeNumber); z<neighbourhoodSize; z++){
*			neighbourhood[z]=this.changeType(neighbourhood[z]);
*		}
*		//for (z = 0; z<neighbourhoodSize; z++) System.out.println(z+". Scoring davor: "+neighbourhood [z].getScore());
*		
*		java.util.Arrays.sort(neighbourhood,0,5);
*		for (z = 0; z<neighbourhoodSize+1; z++) System.out.println("Person: "+neighbourhood[z].getPerson().getId()+", "+z+". Scoring nach Sortierung: "+neighbourhood [z].getScore());
*		
*		//System.out.println("Neighbourhood [1]: "+neighbourhood[1].getActsLegs());
*		ArrayList<Object> al = plan.getActsLegs();
*		int size = al.size();
*		for (int i = 1; i<size-1;i=i+2){
*			al.remove(i);
*			al.add(i, neighbourhood[4].getActsLegs().get(i));
*			
*		}
*		for (int i =2; i<size-1;i=i+2){
*			al.remove(i);
*			al.add(i, neighbourhood[4].getActsLegs().get(i));
*		}
*		
*		
*		//for (z = 0; z<neighbourhoodSize; z++) System.out.println(z+". Scoring danach: "+neighbourhood [z].getScore());
*		//int size =0;
*		//for (Object o : plan.getActsLegs()){
*			//if (o.getClass().equals(Act.class)){
*				//plan.removeAct(size);
*				//size=size+1;
*			//}
*			//else {
*				//plan.removeLeg(size);
*				//size=size+1;
*			//}
*			
*		//}
*		
*		
*		//System.out.println("Plan nach Umschreiben: "+plan.getActsLegs());
*		
*		//plan.copyPlan(neighbourhood[0]);
*
*			}
*	
*	
*	public int changeOrder (PlanomatXPlan basePlan, int x){
*		
*		System.out.println("x ist: "+x);
*		
*		ArrayList<Object> actslegs = basePlan.getActsLegs();
*		if (actslegs.size()<=5){	//If true the plan has not enough activities to change their order.
*			return x;
*		}
*		else {
*			for (int loopCounter = x; loopCounter <= actslegs.size()-4; loopCounter=loopCounter+2){ //Go through the "inner" acts only
*				x=x+2;
*				
*				//Activity swapping
*				
*				Act act2 = (Act)(actslegs.get(loopCounter));
*				Act act4 = (Act)(actslegs.get(loopCounter+4));
*				if (act2.getType()!=act4.getType()){
*					//System.out.println("Hab was gefunden!");
*					//System.out.println("Plan davor: "+actslegs);
*					Act act1 = (Act)(actslegs.get(loopCounter-2));
*					Act act3 = (Act)(actslegs.get(loopCounter+2));
*					if (act1.getType()!=act3.getType()){
*						double scoreOne = basePlan.getScore();
*						System.out.println("Scoring davor: "+scoreOne);
*						Act actHelp = new Act ((Act)(actslegs.get(loopCounter)));
*						actslegs.set(loopCounter, actslegs.get(loopCounter+2));
*						actslegs.set(loopCounter+2, actHelp);
*						//System.out.println("Plan danach: "+basePlan.getActsLegs());
*						
*						// Routing
*					
*						this.router.run(basePlan);
*						//System.out.println("Neuer Plan :"+actslegs);
*						
*						//Optimizing the start times
*						
*						//this.planomatAlgorithm.run (basePlan); //Calling standard Planomat to optimise start times and mode choice
*						//System.out.println("Neuer Plan nach Planomat: "+actslegs);
*						
*						// Scoring
*						
*						basePlan.setScore(scorer.getScore(basePlan));
*						double scoreTwo = basePlan.getScore();
*						System.out.println("Scoring danach: "+scoreTwo);
*						break;
*					}
*					
*				}
*			}		
*			return x;
*		}
*	}
*/	
	
}
