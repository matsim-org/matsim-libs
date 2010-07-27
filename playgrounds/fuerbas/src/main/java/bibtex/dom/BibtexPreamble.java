/*
 * Created on Mar 19, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package bibtex.dom;

import java.io.PrintWriter;

/**
 * A preamble can be used to include pretty much arbitrary latex/tex at the beginning of a
 * generated bibliography. There is usually only one preamble per bibtex file.
 * 
 * @author henkel
 */
public final class BibtexPreamble extends BibtexAbstractEntry {

	private BibtexAbstractValue content;

	BibtexPreamble(BibtexFile file,BibtexAbstractValue content){
		super(file);
		this.content = content;
	}


	/* (non-Javadoc)
	 * @see bibtex.dom.BibtexNode#printBibtex(java.io.PrintWriter)
	 */
	public void printBibtex(PrintWriter writer) {
	    
	    assert writer!=null: "writer parameter may not be null.";
	    
		writer.println("@preamble{");
		content.printBibtex(writer);
		writer.println("}");
	}

	/**
	 * @return BibtexAbstractValue
	 */
	public BibtexAbstractValue getContent() {
		return content;
	}

	/**
	 * Sets the content.
	 * @param content The content to set
	 */
	public void setContent(BibtexAbstractValue content) {
	    
	    assert content!=null : "content parameter may not be null.";
	    assert !(content instanceof BibtexMultipleValues) : "content parameter may not be an instance of BibtexMultipleValues.";
	    
		this.content = content;
	}

}
