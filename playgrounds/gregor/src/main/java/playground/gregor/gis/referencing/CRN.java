/* *********************************************************************** *
 * project: org.matsim.*
 * CRN.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.gregor.gis.referencing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import org.geotools.feature.Feature;

import com.vividsolutions.jts.geom.Coordinate;

public class CRN {

	final private static int MAX_LETTERS = 64;
	private double[][] activationMatrix;
	private final Collection<CaseNode> caseNodes; 
//	private final double [] spreading = new double [] {0.7,0.75,1,0.75,0.7};
	private final double [] spreading = new double [] {0.0,0.75,1,0.75,0.0};
	private final Map<String,String> nT2 = new HashMap<String,String>();
	private final Map<String,String> nT3 = new HashMap<String,String>();
	private final Map<String,String> nT4 = new HashMap<String,String>();
	private final HashSet<String> expr = new HashSet<String>();
	private final HashMap<Integer,SimilarityLink> simLinks = new HashMap<Integer,SimilarityLink>();
	
	public Random rnd = new Random();
	
	
	public CRN(final Collection<Feature> ft) {
		this.caseNodes = new ArrayList<CaseNode>();
		createTranslationTable();
		createMappingTable();
		buildActivationMatrix();
		buildCaseNodes(ft);
//		selfTest(ft);
	}

	
	private void createMappingTable() {
		addSimLink('a','e',0.75);
		addSimLink('s','z',0.75);
		addSimLink('c','k',0.75);
		addSimLink('y','i',0.75);
		addSimLink('u','i',0.75);
		addSimLink('m','n',0.75);
		
	}
	
	private void addSimLink(final int c, final int d, final double e) {
		this.simLinks.put(c, new SimilarityLink(d,e))	;
		this.simLinks.put(d, new SimilarityLink(c,e))	;
	}


	private void createTranslationTable() {
		this.nT3.put(" iii", " 3");
		this.nT2.put(" ii", " 2");
		this.nT2.put(" iv", " 4");
		this.nT4.put(" viii", " 8");
		this.nT3.put(" vii", " 7");
		this.nT2.put(" vi", " 6");
		this.nT2.put(" ix", " 9");
		this.nT2.put(" xi", " 11");
		this.nT3.put("rs ", "rumah sakit ");
		this.nT3.put("jl.", "jalan ");
		this.nT4.put("jl,", "jalan ");
		this.nT4.put("jln ", "jalan ");
		this.nT4.put("jln.", "jalan ");
		this.nT4.put("jal ", "jalan ");
		this.nT4.put("gedung", "");
		this.nT3.put("jl ", "jalan ");
		
	}

//	private void selfTest(Collection<Feature> fts) {
//		for (Feature ft : fts) {
//			String expression = ((String) ft.getAttribute(3)).toLowerCase();
//			CaseNode resp = getCase(expression);
//			if (resp == null) continue; 
//			
//			
//			if (!resp.getExpression().equals(expression)) {
//				System.err.println("Query: " + expression + "  Resp: " + resp.getExpression());
//				
//			}
//			
//		}
//	}

	private void buildCaseNodes(final Collection<Feature> fts) { 
		for (final Feature ft : fts) {
			final String expression = (String) ft.getAttribute(3);
			if (this.expr.contains(expression.toLowerCase())){
				continue;
			}
			this.expr.add(expression.toLowerCase());
//			this.caseNodes.add(new CaseNode(expression,ft.getDefaultGeometry().getCentroid().getCoordinate()));
			this.caseNodes.add(new CaseNode(expression,ft));
		}
		
		
	}
	
	
	public CaseNode getCase(String query) {
		if (query.length() < 2) {
			return null;
		}
		
		restCaseNodes();
		buildActivationMatrix();
		query = query.toLowerCase();
		query = cleanUp(query);
		activateMatrix(query);
		return getCaseWithHighestActivity(query.length());
		
		
	}
	
	
	private void restCaseNodes() {
		for (final CaseNode n : this.caseNodes) {
			n.rest();
		}
	}

	private CaseNode getCaseWithHighestActivity(final int length) {
		
		CaseNode mostSim = null;
		double activation = 0;
		
		for (final CaseNode n : this.caseNodes) {
			final double tmp = n.getActivation(length);
			if (tmp > activation) {
				activation = tmp;
				mostSim = n;
			}
			
			
		}
		return mostSim;
		
	}

	private void activateMatrix(final String query) {
		for (int i = 0; i < query.length(); i++) {
			final int c = query.charAt(i);
			activateZell(i,c);
		}
		for (int i = 0; i < query.length(); i++) {
			final int c = query.charAt(i);
			spreadActivation(i,c);
		}
	}

	private void spreadActivation(final int i, final int c) {
		final SimilarityLink l = this.simLinks.get(c);
		if (l != null) {
			this.activationMatrix[i][l.entry] = l.similarity * this.activationMatrix[i][c]; 
		}
		
		
		
		if (i >= 2) {
			this.activationMatrix[i-2][c] += this.spreading[0] * this.activationMatrix[i][c];
			this.activationMatrix[i-1][c] += this.spreading[1] * this.activationMatrix[i][c];
		} else if ( i >=1) {
			this.activationMatrix[i-1][c] += this.spreading[1] * this.activationMatrix[i][c];			
		}
		
		if ( i <= MAX_LETTERS -3) {
			this.activationMatrix[i+1][c] += this.spreading[3] * this.activationMatrix[i][c];
			this.activationMatrix[i+2][c] += this.spreading[4] * this.activationMatrix[i][c];			
		} else if ( i <= MAX_LETTERS -2) {
			this.activationMatrix[i+1][c] += this.spreading[3] * this.activationMatrix[i][c];
		} 
		
	}

	private void activateZell(final int i, final int c) {
		this.activationMatrix[i][c] = 1;
	}

	private void buildActivationMatrix() {
//		int alphabetSize = 'z' - 'a';
		
		this.activationMatrix = new double [MAX_LETTERS][256]; 
		
	}
	
	public String cleanUp(String expression) {
		
		expression = toArabic(expression);
		
		final ArrayList<Character>  chars = new ArrayList<Character>();
		for (int i = 0; i < expression.length(); i++) {
			final char c = expression.charAt(i);
			if (c >= 'a' && c <='z' || c >= '0' && c <= '9') {
				chars.add(c);
			}
			
		}
		
		final char [] c = new char [chars.size()];
		for (int i = 0; i < chars.size(); i++) {
			c[i] = chars.get(i);
		}

		return new String (c);
	}
	
	private double getWeight(final char charAt) {
		if (charAt >= 'a' && charAt <= 'z') {
			return 1.0;
		}
		if (charAt >= '0' && charAt <= '9') {
			return 0.5;
		}		
		
		return 0;
	}
	
	private String toArabic(String expression) {
		for (final String arab : this.nT4.keySet()) {
			if (expression.contains(arab)) {
				expression = expression.replaceFirst(arab, this.nT4.get(arab));
			}
		}
		for (final String arab : this.nT3.keySet()) {
			if (expression.contains(arab)) {
				expression = expression.replaceFirst(arab, this.nT3.get(arab));
			}
		}
		for (final String arab : this.nT2.keySet()) {
			if (expression.contains(arab)) {
				expression = expression.replaceFirst(arab, this.nT2.get(arab));
			}
		}
		
		return expression;
	}

	
	private class SimilarityLink {
		int entry;
		double similarity;
		public SimilarityLink(final int entry, final double sim) {
			this.entry = entry;
			this.similarity = sim;
		}
	}

	public class CaseNode{
		private final String expression;
		private int length;
		private int[] links;
		private double [] weights;
		private double activation;
		private double sW = 0;
//		private final Coordinate coord;
		private final Feature ft;

		public CaseNode(final String input, final Feature ft) {
//			this.coord = ft;
			this.ft = ft;
			this.expression = input.toLowerCase();
			linkCaseNode();			
			if (!checkIt()) {
				throw new RuntimeException();
			}
		}

		public void rest() {
			this.activation = 0;
			
		}

		public double getActivation(final int ql) {
			double activation = 0;
			for (int i = 0; i < this.length; i++) {
				activation += CRN.this.activationMatrix[i][this.links[i]]*this.weights[i];
				
			}
			
			this.activation = activation / this.sW; 
			return activation / (1+Math.abs(ql-this.length)); 
		}

		public Coordinate getCoordinate() {
			Coordinate [] coords = this.ft.getDefaultGeometry().getCoordinates();
			int idx = (int) (CRN.this.rnd.nextDouble() * (coords.length - 1) + 0.5);
			return coords[idx];
		}
		
		public String getExpression() {
			return this.expression;
		}
		public double getActivation() {
			return this.activation;
		}
		private void linkCaseNode() {
			final String clean = cleanUp(this.expression);
			this.length = clean.length();
			this.links = new int [this.length];
			this.weights = new double[this.length];
			for (int i = 0; i < this.length; i++) {
				this.links[i] = clean.charAt(i) ; //- 'a';
				this.weights[i] = getWeight(clean.charAt(i));
				this.sW += this.weights[i];
			}
		}
		




		private boolean checkIt(){
			
			final char [] couple = new char [this.length];
			for (int i = 0; i < this.length; i++) {
				final char c = (char) (this.links[i] ); //+ 'a');
				couple[i] = c;
			}
			final String test = new String(couple);
			
			return test.equals(cleanUp(this.expression));
		}
		
	}

}
