package playground.ciarif.models.subtours;

import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.Id;


public class PersonSubtour {
	
	private Id person_id;
	private List<Subtour> subtours;
	
	public PersonSubtour () {
		super();
		this.subtours = new Vector<Subtour>();
	}
	
	//////////////////////////////////////////////////////////////////////
	// Setters methods
	//////////////////////////////////////////////////////////////////////
	
	public void setSubtour (Subtour subtour) {
		subtours.add(subtour); 
	}
	
	public void setPerson_id(Id person_id) {
		this.person_id = person_id;
	}
	
	public void setSubtours(List<Subtour> subtours) {
		this.subtours = subtours;
	}
	
	//////////////////////////////////////////////////////////////////////
	// Getters methods
	//////////////////////////////////////////////////////////////////////
	
	public Id getPerson_id() {
		return person_id;
	}
	
	public List<Subtour> getSubtours() {
		return subtours;
	}

}
