package playground.droeder.bvg09;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.transitSchedule.api.TransitRoute;

import playground.droeder.ValueComparator;

public class DaVisum2HafasMapper6 extends AbstractDaVisum2HafasMapper {
	
	TransitRoute visRoute;
	TransitRoute hafRoute;
	Map<Integer, List<Integer>> solutions;

	public static void main(String[] args) {
		DaVisum2HafasMapper6 mapper = new DaVisum2HafasMapper6(150);
		mapper.run();
		
	}
	
	public DaVisum2HafasMapper6(double dist2Match) {
		super(dist2Match);
	}

	@Override
	protected Map<Id, Id> tryToMatchRoute(TransitRoute visRoute, TransitRoute hafRoute) {
		this.visRoute = visRoute;
		this.hafRoute = hafRoute;
		
		if(visRoute.getStops().size() > hafRoute.getStops().size()) return null;
		
		
		Map<Integer, List<Integer>> solutions = this.generateSolutions(visRoute.getStops().size(), hafRoute.getStops().size());
		if(solutions.size() < 1) return null;
		SortedMap<Integer, Double> scores = this.getSolutionsScore(solutions);
		
		return solutions2Id(solutions.get(scores.firstKey()));
	}
	
	private Map<Integer, List<Integer>> generateSolutions(Integer vSize, Integer hSize){
		
		Map<Integer, List<Integer>> possibleValues = new HashMap<Integer, List<Integer>>();
		
		
		//get possible values for position x
		for(int v = 0; v < vSize; v++){
			List<Integer> temp = new ArrayList<Integer>();
			for (int h = v; h < (v + hSize - vSize + 1); h++){
				temp.add(h);
			}
			possibleValues.put(v, temp);
		}
		
		
		HashMap<Integer, List<Integer>> solutions = new HashMap<Integer, List<Integer>>();
		Integer count = 0;
		
		
		for(Entry<Integer, List<Integer>> e: possibleValues.entrySet()){
			
			// add old solutions to tempMap
			Map<Integer, List<Integer>> temp = new HashMap<Integer, List<Integer>>(solutions);
			//initialize solutions
			solutions = new HashMap<Integer, List<Integer>>();
			count = 0;
			if(e.getKey() == 0){
				for(Integer i : e.getValue()){
					List<Integer> l = new ArrayList<Integer>();
					l.add(i);
					solutions.put(count, l);
					count++;
				}
			}else{
				// iterate over old solutions
				for (List<Integer> l : temp.values()){
					
					// iterate over possible  values for position e.getkey()
					for (Integer i : e.getValue()){
						
						// get new solution from old
						System.out.println(l.size());
						if(l.size() == e.getKey() && l.get(l.size()-1) < i){
							List<Integer> s = new ArrayList<Integer>(l);
							s.add(i);
							double score = scoreSolution(s);
							log.error(score);
							if(score < 5000){
								solutions.put(count, s);
								count++;
								for(Integer ii : s){
									System.out.print(ii + "\t");
								}
								System.out.println();
							}
						}
					}
				}
			}
		}
		
		return solutions;
	}
	
	private SortedMap<Integer, Double> getSolutionsScore(Map<Integer, List<Integer>> solutions){
		Map<Integer, Double> solution2scoreTemp = new HashMap<Integer, Double>();
		ValueComparator vc = new ValueComparator(solution2scoreTemp);
		TreeMap<Integer, Double> solution2score = new TreeMap<Integer, Double>(vc);
		
		for(Entry<Integer, List<Integer>> e : solutions.entrySet()){
			solution2scoreTemp.put(e.getKey(), this.scoreSolution(e.getValue()));
		}
		
		solution2score.putAll(solution2scoreTemp);
		return solution2score;
	}
	
	private Double scoreSolution(List<Integer> solution){
		Double score = new Double(0);
		
		for(Integer i = 0; i < solution.size(); i++ ){
			score += this.getDist(this.visRoute.getStops().get(i).getStopFacility().getId(), 
					hafRoute.getStops().get(solution.
							get(i)).getStopFacility().getId());
		}
		
		score = score/solution.size();
		return score;
	}
	
	private Map<Id, Id> solutions2Id(List<Integer> bestSolution){
		HashMap<Id, Id> id2Id = new HashMap<Id, Id>();
		
		for(Integer i = 0; i < this.visRoute.getStops().size(); i++ ){
			id2Id.put(this.visRoute.getStops().get(i).getStopFacility().getId(), 
					hafRoute.getStops().get(bestSolution.get(i)).getStopFacility().getId());
		}
		
		return id2Id;
	}
}
