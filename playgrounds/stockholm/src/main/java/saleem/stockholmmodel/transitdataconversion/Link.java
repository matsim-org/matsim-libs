package saleem.stockholmmodel.transitdataconversion;

/** Link class is used in converting Excel based data to MatSim based transit schedule 
 * data structure. Link class contains neccessary Link attributes.
 * 
 * @author Mohammad Saleem
 */
public class Link {
	private String refID;
	Link(String refID){
		this.refID=refID;
	}
	public String getRefID(){
		return this.refID;
	}
}
