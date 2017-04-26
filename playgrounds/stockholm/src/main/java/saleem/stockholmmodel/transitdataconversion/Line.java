package saleem.stockholmmodel.transitdataconversion;

/** A Line object used in converting Excel based data into MatSim based transit schedule 
 * data structure, consisting of neccessary attributes for stops and lines.
 * 
 * @author Mohammad Saleem
 */
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

	public class Line {
	private String lineId;
	private ArrayList<TransitRoute> troutes = new ArrayList<TransitRoute>();
	private ArrayList<String> routeIds = new ArrayList<String>();
	public String getLineId(){
		return lineId;
	}
	public void addRouteID(String routeid){
		 routeIds.add(routeid);
	}
	public String countRouteIds(String routeid){
		int count=-1;//added before ID is set thats why always have one instance
		 Iterator iter = routeIds.iterator();
		 while(iter.hasNext()){
			 String str = (String)iter.next();
			 if(str.equals(routeid)){
				 count++;
			 }
		 }
		 if(count==0)return "";
		 return "_"+count;
	}
	public void addTransitRoute(TransitRoute troute){
		troutes.add(troute);
	}
	public List<TransitRoute> getTransitRoutes(){
		return troutes;
	}
	public void addTransitRoute(int index, TransitRoute troute){
		troutes.add(index, troute);
	} 
	public void setLineId(String id){
		lineId=id;
	}
	public TransitRoute getRouteAtIndex(int index){
		return troutes.get(index);
	}
	public TransitRoute removeRouteAtIndex(int index){
		return troutes.remove(index);
	}
	public int indexOfTransitRoute(TransitRoute route){//checks is the line contains the passed route, and if it does, returns its index
		String rstr = route.toString();
		Iterator iter = troutes.iterator();
		while(iter.hasNext()){
			TransitRoute troute = (TransitRoute)iter.next();
			String tstr = troute.toString();
			if(rstr.equals(tstr)){
				return troutes.indexOf(troute);
			}
		}
	return -1;
	}
}
