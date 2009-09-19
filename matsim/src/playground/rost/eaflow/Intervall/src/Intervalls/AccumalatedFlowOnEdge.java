package playground.rost.eaflow.Intervall.src.Intervalls;


public class AccumalatedFlowOnEdge {
	
	protected AVLTree tree;
	protected AccumalatedFlowOnEdgeIntervall last;
	
	public AccumalatedFlowOnEdge()
	{
		AccumalatedFlowOnEdgeIntervall intervall = new AccumalatedFlowOnEdgeIntervall(0,Integer.MAX_VALUE);
		intervall.setFlow(0);
		tree = new AVLTree();
		tree.insert(intervall);
		last = intervall;
	}
	
	
	public void augmentFlowOverTime(int startTime, int arrivalTime, int additionalFlow)
	{
		AccumalatedFlowOnEdgeIntervall currentIntervall = this.getIntervallAt(startTime);
		while(currentIntervall != null && currentIntervall.getLowBound() < arrivalTime)
		{
			if(currentIntervall.getHighBound() > arrivalTime + 1)
			{
				//split currentIntervall
				this.splitAt(arrivalTime);
				currentIntervall = this.getIntervallAt(currentIntervall.getLowBound());
			}
			currentIntervall.setFlow(currentIntervall.getFlow()+additionalFlow);
			currentIntervall = this.getNext(currentIntervall);
		}
	}
	
	public AccumalatedFlowOnEdgeIntervall getIntervallAt(int t){
		if(t<0){
			throw new IllegalArgumentException("negative time: "+ t);
		}
		AccumalatedFlowOnEdgeIntervall i = (AccumalatedFlowOnEdgeIntervall)tree.contains(t);
		if(i==null)throw new IllegalArgumentException("there is no Intervall containing"+t);
		return i;
	}
	
	/**
	 * gives the last Stored AccumalatedFlowOnEdgeIntervall
	 * @return AccumalatedFlowOnEdgeIntervall with maximal lowbound
	 */
	public AccumalatedFlowOnEdgeIntervall getLast(){
		return last;
	}

	/**
	 * Checks weather the given AccumalatedFlowOnEdgeIntervall is the last
	 * @param o EgeIntervall which it test for 
	 * @return true if getLast.equals(o)
	 */
	public boolean isLast(AccumalatedFlowOnEdgeIntervall o){
		return (last.equals(o));
	}
	
	/**
	 * gives the next AccumalatedFlowOnEdgeIntervall with respect of the order contained 
	 * @param o schould be contained
	 * @return next AccumalatedFlowOnEdgeIntervall iff o is not last and contained. if o is last, null is returned.
	 */
	public AccumalatedFlowOnEdgeIntervall getNext(AccumalatedFlowOnEdgeIntervall o){
		tree.goToNodeAt(o.getLowBound());
		
			AccumalatedFlowOnEdgeIntervall j = (AccumalatedFlowOnEdgeIntervall) tree._curr.obj;
			if(j.equals(o)){
				tree.increment();
				if(!tree.isAtEnd()){
					AccumalatedFlowOnEdgeIntervall i = (AccumalatedFlowOnEdgeIntervall) tree._curr.obj;
					tree.reset();
					return i;
				}
				else 	
					return null;
			}
			else throw new IllegalArgumentException("Intervall was not contained");
	}
	
	/**
	 * gives the previous AccumalatedFlowOnEdgeIntervall with respect to the order contained 
	 * @param o should be contained
	 * @return next AccumalatedFlowOnEdgeIntervall iff o isnot first and contained
	 */
	public AccumalatedFlowOnEdgeIntervall getPrevious(AccumalatedFlowOnEdgeIntervall o){
		if(o.getLowBound() == 0)
			return null;
		
		return this.getIntervallAt(o.getLowBound()-1);
	}

	/**
	 * Finds the EgdeIntervall containing t and splits this at t 
	 * giving it the same flow as the flow as the original 
	 * it inserts the new EdgeInterval after the original
	 * @param t time point to split at
	 * @return the new EdgeIntervall for further modification
 	 */
	public AccumalatedFlowOnEdgeIntervall splitAt(int t){
		
		boolean isLast = false;
		Intervall newIntervall = null;
		
		AccumalatedFlowOnEdgeIntervall foundIntervall = getIntervallAt(t);
			if (foundIntervall != null){
				//update last
				if(foundIntervall == last){
					newIntervall = foundIntervall.splitAt(t);
					isLast = true;
				}else {
					newIntervall = foundIntervall.splitAt(t);
				}
			}
		
		if (foundIntervall != null){
			AccumalatedFlowOnEdgeIntervall newAccIntervall = new AccumalatedFlowOnEdgeIntervall(newIntervall, foundIntervall.getFlow());
			tree.insert(newAccIntervall);
			if(isLast)
			{
				last = newAccIntervall;
			}
			return newAccIntervall;
		}
		else throw new IllegalArgumentException("there is no Intervall that can be split at "+t);
	}

}

