package patryk.populationgeneration;

import java.util.ArrayList;
import java.util.Calendar;


public class PendlerRelation {
	
	private County  home = null;
	private County  work = null;
	private int number;
	private ArrayList<Integer> age;
	private ArrayList<Integer> income;
	private ArrayList<Integer> housingType;
	
	public PendlerRelation(County home, County  work) {
		this.work = work;
		this.home = home;
		this.age = new ArrayList<Integer>();
		this.income = new ArrayList<Integer>();
		this.housingType = new ArrayList<Integer>();
		number = 0;
	}
	
	public int getNumber() {
		return number;
	}
	
	public County getHome() {
		return home;
	}
	
	public County getWork() {
		return work;
	}
	
	public String getRelationKey() {
		return getHome().getKey()+"-"+getWork().getKey();
	}

	public void addNumber(int number) {
		this.number += number;
	}
	
	public void addAttributes(int birthYear, int income, int housingType) {
		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		age.add(currentYear - birthYear);
		this.income.add(income);
		this.housingType.add(housingType);
	}
	
	public ArrayList<Integer> getAges() {
		return age;
	}
	
	public ArrayList<Integer> getIncome() {
		return income;
	}
	
	public ArrayList<Integer> getHousingType() {
		return housingType;
	}

}