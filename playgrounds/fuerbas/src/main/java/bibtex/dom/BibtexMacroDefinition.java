/*
 * A string definition is something like
 * 
 * @string{ cacm = "Communications of the ACM }
 * 
 * Created on Mar 17, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package bibtex.dom;

import java.io.PrintWriter;

/**
 * Bibtex let's you define macros which are essentially just shortcuts for strings.
 * Macros can reference other macros, as long as there's no cycle.
 * 
 * Examples:
 * <ul>
 * <li>&#x0040string(acm = "Association of the Computing Machinery")</li>
 * <li>&#x0040string(acmsigplan = acm # " SIGPLAN")</li>
 * </ul>
 * @author henkel
 */
public final class BibtexMacroDefinition extends BibtexAbstractEntry {

	BibtexMacroDefinition(BibtexFile file,String key, BibtexAbstractValue value){
		super(file);
		this.key = key.toLowerCase();
		this.value = value;
	}

	private String key; private BibtexAbstractValue value;

	/**
	 * @return String
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @return BibtexValue
	 */
	public BibtexAbstractValue getValue() {
		return value;
	}

	/**
	 * Sets the key.
	 * @param key The key to set
	 */
	public void setKey(String key) {
		this.key = key.toLowerCase();
	}

	/**
	 * Sets the value.
	 * @param value The value to set
	 */
	public void setValue(BibtexAbstractValue value) {
	    
	    assert value!=null: "value parameter has to be !=null.";
	    
		this.value = value;
	}



	/* (non-Javadoc)
	 * @see bibtex.dom.BibtexNode#printBibtex(java.io.PrintWriter)
	 */
	public void printBibtex(PrintWriter writer) {
	    
	    assert writer!=null: "writer parameter has to be !=null.";
	    
		writer.print("@string{");
		writer.print(this.key);
		writer.print("=");
		this.value.printBibtex(writer);
		writer.println("}");

	}

}
