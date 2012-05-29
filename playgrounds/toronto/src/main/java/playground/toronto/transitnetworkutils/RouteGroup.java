package playground.toronto.transitnetworkutils;

import java.util.ArrayList;
import java.util.HashMap;

import playground.toronto.transitnetworkutils.ScheduledRoute;

public class RouteGroup {

		private ArrayList<ScheduledRoute> routes;
		private String name;
		
		public RouteGroup(String name){
			this.routes = new ArrayList<ScheduledRoute>();
			this.name = name;
		}
		
		public void addRoute(ScheduledRoute R){
			this.routes.add(R);
		}
		
		public String getName(){
			return this.name;
		}
		
		/**
		 * Returns an ArrayList of modes used in the route group. 
		 * 
		 * @return
		 */
		public ArrayList<String> getModes(){
			
			ArrayList<String> result = new ArrayList<String>();
			
			for(ScheduledRoute R : this.routes){
				if (! result.contains(R.mode)){
					result.add(R.mode);
				}
			}
			
			return result;
			
		}
		
		
		/**
		 * Returns a HashMap of directions, and the number of branches in each direction.
		 * 
		 * @return
		 */
		public HashMap<String, Integer> getBranches(){
			
			HashMap<String, Integer> result = new HashMap<String, Integer>();
			
			for (ScheduledRoute R : this.routes){
				if (result.get(R.direction) == null){
					result.put(R.direction, 1);
				}
				else{
					result.put(R.direction, result.get(R.direction) + 1);
				}
				
			}
			
			return result;
			
		}
		
		/**
		 * Returns an ArrayList of stops serviced by this RouteGroup
		 * 
		 * @return
		 */
		public ArrayList<String> getStops(){
			
			ArrayList<String> result = new ArrayList<String>();
			
			for (ScheduledRoute R : this.routes){
				for (String S : R.getStopSequence()){
					if (! result.contains(S)){result.add(S);};
				}
			}
			
			return result;
			
		}
		
		/**
		 * Returns a subset of routes which enumerate the set of stops served (ie, 
		 * which in total serve all stops on the route).
		 * 
		 * @return
		 */
		public ArrayList<ScheduledRoute> getEnumeratedSet(){
						
			ArrayList<ScheduledRoute> result = new ArrayList<ScheduledRoute>();
			ArrayList<String> stops = this.getStops();
			ScheduledRoute longestEnumRoute;
		
			while (stops.size() > 0){
				
				//get longest route not already in result set
				longestEnumRoute = new ScheduledRoute();
				int maxEnumStops = 0;
				for (ScheduledRoute R : this.routes){
					int currentEnumStops = 0;
					for (String S : R.getStopSequence()){
						if (stops.contains(S)) currentEnumStops++;
					}
					
					if ((currentEnumStops > maxEnumStops) && ( ! result.contains(R))){
						maxEnumStops = currentEnumStops;
						longestEnumRoute = R;
					}
					
				}
				
				result.add(longestEnumRoute);
				
				//remove served stops
				for (String S : longestEnumRoute.getStopSequence()){
					stops.remove(S);
				}
				
				if(result.size() > this.routes.size()){
					System.err.println("error!");
				}
				
			}
			
			return result;
			
		}
		
		@Override
		public String toString(){
			String S = "";
			
			return S;
			
		}
}
