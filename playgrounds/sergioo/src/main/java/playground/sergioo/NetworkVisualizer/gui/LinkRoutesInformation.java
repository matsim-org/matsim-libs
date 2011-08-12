package playground.sergioo.NetworkVisualizer.gui;

import java.util.ArrayList;
import java.util.Collection;

public class LinkRoutesInformation {
	
	//Attributes
	private float numTimes;
	private Collection<String> lines;
	private Collection<String> companies;
	
	//Methods
	public LinkRoutesInformation() {
		numTimes = 0;
		lines = new ArrayList<String>();
		companies = new ArrayList<String>();
	}
	public float getNumTimes() {
		return numTimes;
	}
	public void increaseNumTimes(float numTimes) {
		this.numTimes += numTimes;
	}
	public Collection<String> getLines() {
		return lines;
	}
	public void addLine(String line) {
		lines.add(line);
	}
	public Collection<String> getCompanies() {
		return companies;
	}
	public void addCompany(String company) {
		companies.add(company);
	}
	
}
