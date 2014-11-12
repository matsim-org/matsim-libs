package pedCA.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import matsimConnector.agents.Pedestrian;
import pedCA.agents.Agent;
import pedCA.context.Context;
import pedCA.environment.grid.GridPoint;
import pedCA.output.Log;
import pedCA.utility.Constants;
import pedCA.utility.Lottery;
import pedCA.utility.RandomExtractor;

public class ConflictSolver {
	private Context context;
	
	public ConflictSolver(Context context){
		this.context = context;
	}
	
	public void step(){
		solveConflicts();
	}
	
	private void solveConflicts(){
		
//		Ottengo tutti i pedestrian nel context
		Iterable<Agent> pedsIt = getPedestrians();
//		Lista contenente tutti i pedoni con destinazione COMUNE
		ArrayList<Agent> pedsList = new ArrayList<Agent>();
//		Lista che user� per riempire la lista di 
		ArrayList<GridPoint> nextPosList = new ArrayList<GridPoint>();
		
		ArrayList<Agent> listaCompletaPedoni = new ArrayList<Agent>();
//		HashSet per ottenere le destinazioni UNIVOCHE dei pedoni
        HashSet<GridPoint> uniqueGP = new HashSet<GridPoint>();
//		Vecchia dimensione della hashMap
        int oldSize;
        
        HashMap<GridPoint, Agent> multipleGP = new HashMap<GridPoint, Agent>();
        
//      Ottengo una lista di tutte le destinazioni UNICHE dei pedoni
//      Inizializzo la lista di pedoni che avranno un conflitto (anche la lista della destinazioni con conflitti)
		for(Agent p:pedsIt){
			oldSize = uniqueGP.size();
			GridPoint agentNewPosition = getNewAgentPosition(p);
			uniqueGP.add(agentNewPosition);
			
			listaCompletaPedoni.add(p);
			if(oldSize == uniqueGP.size()){
				pedsList.add(p);
				if(!nextPosList.contains(agentNewPosition)){
					nextPosList.add(agentNewPosition);
				}
			}
			else{
				multipleGP.put(agentNewPosition, p);
			}
		}
		
//		Se non esiste alcun pedone con conflitto, posso anche evitare!
		if(pedsList.size() == 0)
			return;
		
//		Per ogni destinazione conflittuale, creo una lista temporanea di pedoni da cui estrarr� un vincitore, gli altri verranno riposizionati
//		Poi elimino dalla lista dei pedoni conflittuali i pedoni con conflitto risolto
		
		for (GridPoint gp:nextPosList) {
        	ArrayList<Agent> sameGPPedList = new ArrayList<Agent>();
        	
        	for(Agent p:pedsList){
        		if(getNewAgentPosition(p).equals(gp)){
        			sameGPPedList.add(p);
        		}
        	}
        	        	
        	sameGPPedList.add(multipleGP.get(gp));	
        	
        	if(!frictionCondition()){
        		int randomWinner = RandomExtractor.nextInt(sameGPPedList.size());
        		sameGPPedList.remove(randomWinner);
        	}
        	
        	for(Agent p:sameGPPedList){
        		p.revertChoice();
        		pedsList.remove(p);
        	}	
        }
		
		for(int i = 0; i < listaCompletaPedoni.size(); i++){
			for(int j = i+1; j < listaCompletaPedoni.size(); j++)
				if(getNewAgentPosition(listaCompletaPedoni.get(i)).equals(getNewAgentPosition(listaCompletaPedoni.get(j)))){
					Log.error("Error in Conflict Solving!!");
			}
		}
	}

	public GridPoint getNewAgentPosition(Agent p) {
		return ((Pedestrian)p).getRealNewPosition();
	}

	private boolean frictionCondition() {
		return Lottery.simpleExtraction(Constants.FRICTION_PROBABILITY);
	}

	private Iterable<Agent> getPedestrians() {
		return context.getPopulation().getPedestrians();
	}
}
