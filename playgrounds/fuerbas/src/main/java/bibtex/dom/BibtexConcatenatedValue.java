/*
 * E. g. "asdf " # 19
 * 
 * Created on Mar 17, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package bibtex.dom;

import java.io.PrintWriter;

/**
 * 
 * Two abstract values concatenated by the hash-operator (#).
 * 
 * Examples:
 * <ul>
 * 	<li>acm # " SIGPLAN"</li>
 * 	<li>"10th " # pldi</li>
 * </ul>
 * 
 * 
 * @author henkel
 */
public final class BibtexConcatenatedValue extends BibtexAbstractValue {

	BibtexConcatenatedValue(BibtexFile file,BibtexAbstractValue left, BibtexAbstractValue right){
		super(file);
		this.left=left;
		this.right=right;
	}

	private BibtexAbstractValue left, right;

	/**
	 * @return BibtexValue
	 */
	public BibtexAbstractValue getLeft() {
		return left;
	}

	/**
	 * @return BibtexValue
	 */
	public BibtexAbstractValue getRight() {
		return right;
	}

	/**
	 * Sets the left.
	 * @param left The left to set
	 */
	public void setLeft(BibtexAbstractValue left) {
	    
	    assert !(left instanceof BibtexMultipleValues): "left parameter may not be an instance of BibtexMultipleValues."; 
	    
		this.left = left;
	}

	/**
	 * Sets the right.
	 * @param right The right to set
	 */
	public void setRight(BibtexAbstractValue right) {
	    
	    assert !(right instanceof BibtexMultipleValues): "right parameter may not be an instance of BibtexMultipleValues.";
	    
		this.right = right;
	}

	/* (non-Javadoc)
	 * @see bibtex.dom.BibtexNode#printBibtex(java.io.PrintWriter)
	 */
	public void printBibtex(PrintWriter writer) {
	    
	    assert writer!=null: "writer paramter may not be null.";
	    
		this.left.printBibtex(writer);
		writer.print('#');
		this.right.printBibtex(writer);
	}

}
