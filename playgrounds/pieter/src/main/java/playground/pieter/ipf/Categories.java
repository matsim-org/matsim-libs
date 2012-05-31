package playground.pieter.ipf;

import java.util.HashSet;

public class Categories {
	HashSet<Category> categories;

	public Categories() {
		this.categories = new HashSet<Category>();
	}
	
	public void addCategory(Category category){
		//don't need to check for names, done by comparator
		this.categories.add(category);
	}
}
