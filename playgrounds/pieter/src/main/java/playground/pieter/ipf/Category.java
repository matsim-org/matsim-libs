package playground.pieter.ipf;

import java.util.TreeSet;

public class Category {
	private final String name;
	private final TreeSet<String> levels;

	public Category(String name) {
		this.name = name;
		this.levels = new TreeSet<String>();
	}

	protected String getName() {
		return name;
	}

	public TreeSet<String> getLevels() {
		return levels;
	}

	public void addLevel(String s) {
		this.levels.add(s);
	}

	public boolean equals(Category o) {
		if (this.name.equals(o.getName())) {
			return true;
		} else {
			return false;
		}
	}
}
