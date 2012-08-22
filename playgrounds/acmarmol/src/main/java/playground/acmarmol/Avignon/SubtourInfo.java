package playground.acmarmol.Avignon;

import java.util.ArrayList;

public class SubtourInfo {

	private String actSequence;
	private ArrayList<Integer> subtour;
	
	public SubtourInfo(String actSequence, ArrayList<Integer> subtour){
		
		this.actSequence = actSequence;
		this.subtour = subtour;
	}

	public ArrayList<Integer> getSubtour() {
		return subtour;
	}

	public String getActSequence() {
		return actSequence;
	}
	
}
