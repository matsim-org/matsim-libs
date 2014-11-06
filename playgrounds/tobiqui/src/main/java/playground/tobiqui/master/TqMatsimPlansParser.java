package playground.tobiqui.master;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;


/**
 * 
 */

/**
 * @author tquick
 *
 */

public class TqMatsimPlansParser extends MatsimXmlParser{

		@SuppressWarnings("unused")
		private final CoordinateTransformation transform;
		private Id<Person> currentId;
		private boolean selected;
		protected Map<Id<Person>, Integer> firstDepartures = new HashMap<>();
		
		protected Map<Id, String> routes = new HashMap<Id, String>();
		
		protected Map<Id, HashMap<Integer, Trip>> persons = new HashMap<Id, HashMap<Integer, Trip>>(); //Id, trips
//		protected String[] personData = new String[5];			//departure,fromLink,toLink,actType,route,...
//		protected HashMap<Integer,String[]> trips = new HashMap<Integer,String[]>();
		protected int tripCount = 0;
		protected int selectedCount = 0;
		protected int carCount = 0;
		@SuppressWarnings("deprecation")
		private Network network = NetworkImpl.createNetwork();

		
		public TqMatsimPlansParser(CoordinateTransformation transform) {
			this.transform = transform;
			this.setValidating(false);
		}


		public void writeToFile(String string) {
			((NetworkImpl)network).setCapacityPeriod(60);
			NetworkWriter writer = new NetworkWriter(network);
			writer.write(string);
		}



		@Override
		public void startTag(String name, Attributes atts, Stack<String> context) {
			if ("person".equals(name)){
				this.tripCount = 0;
				currentId = Id.create(atts.getValue("id"), Person.class);
				this.persons.put(this.currentId, new HashMap<Integer,Trip>());
			}
			
			else if ("plan".equals(name)){
				this.selected = false;
				if (atts.getValue("selected").equals("yes")){
					this.selected = true;
					this.selectedCount++;
				}
			}
			else if (("act".equals(name))&& (this.selected)) {
				if (atts.getValue("end_time") != null){
//					 newId = new IdImpl(currentId.toString() + "_" + atts.getValue("type"));
					this.tripCount++;
					Trip trip = new Trip();			//departure,fromLink,toLink,actType,route,...
					this.persons.get(this.currentId).put(this.tripCount, trip);
					this.persons.get(this.currentId).get(this.tripCount).fromLink = atts.getValue("link");
					this.persons.get(this.currentId).get(this.tripCount).actType = atts.getValue("type");
					if (this.tripCount > 1){
						this.persons.get(this.currentId).get((this.tripCount-1)).toLink = atts.getValue("link");
					}
					
					String[] departure = atts.getValue("end_time").split(":");
					int dep = (int) (Double.valueOf(departure[0])*3600 + Double.valueOf(departure[1])*60 +
									Double.valueOf(departure[2]));
					this.persons.get(this.currentId).get(this.tripCount).departure = dep;
					this.persons.get(this.currentId).get(this.tripCount).mode = "car";
					if (this.tripCount == 1){
						this.firstDepartures.put(this.currentId, dep);
					}
				}else
					this.persons.get(this.currentId).get(this.tripCount).toLink = atts.getValue("link");
			} 
			
			else if (("leg".equals(name))&& (this.selected)) {
				if (atts.getValue("mode").equals("car")){
					this.persons.get(this.currentId).get(this.tripCount).mode = "car";
//					String[] departure = atts.getValue("dep_time").split(":");
//					int dep = (int) (Double.valueOf(departure[0])*3600 + Double.valueOf(departure[1])*60 +
//									Double.valueOf(departure[2]));
//					this.persons.get(this.currentId).get(this.tripCount).departure = dep;
//					if (this.tripCount == 1){
//						this.firstDepartures.put(this.currentId, dep);
//						this.carCount++;
//					}
				}else if (atts.getValue("mode").contains("walk")){
					this.persons.get(this.currentId).get(this.tripCount).mode = "walk";
//					String[] departure = atts.getValue("dep_time").split(":");
//					int dep = (int) (Double.valueOf(departure[0])*3600 + Double.valueOf(departure[1])*60 +
//									Double.valueOf(departure[2]));
//					this.persons.get(this.currentId).get(this.tripCount).departure = dep;
//					if (this.tripCount == 1){
//						this.firstDepartures.put(this.currentId, dep);
//					}
				}
			}
		}



		@Override
		public void endTag(String name, String content, Stack<String> context) {
			if (("route".equals(name))&& (this.selected)){
				routes.put(this.currentId, content);
				this.persons.get(this.currentId).get(this.tripCount).route = content;
			}
		}
		
	    public static class Trip{
	    	protected int number;
	    	protected Integer departure;
	    	protected String fromLink;
	    	protected String toLink;
	    	protected String actType;
	    	protected String route;
	    	protected String mode;
	    	
	    	public Trip(){

	    	}
	    }

	}


