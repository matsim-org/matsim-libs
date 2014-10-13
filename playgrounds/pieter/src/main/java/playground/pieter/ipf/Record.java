package playground.pieter.ipf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

class Record {
	private String id;
	private Record parent;
	private ArrayList<Record> children;
	//category - value hashmap for attributes, e.g. sex = f, income = 1000
	private HashMap<Category, String> attributes;
	// keep track of the control totals this record belongs to,
	// as well as the expected weight contributions of it in that control
	private HashSet<ControlWeightHistory> controls;
	private double weight = 1;
	
	
	
	private class ControlWeightHistory {
		private final ControlTotal control;
		private final ArrayList<Double> controlSpecificWeights;

		public ControlWeightHistory(ControlTotal control) {
			this.control = control;
			this.controlSpecificWeights = new ArrayList<>();
		}

		protected ControlTotal getControl() {
			return control;
		}

		protected ArrayList<Double> getWeights() {
			return controlSpecificWeights;
		}
		protected void addWeight(double d) {
			controlSpecificWeights.add(d);
		}

	}
}
