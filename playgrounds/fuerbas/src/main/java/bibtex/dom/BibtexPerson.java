/*
 * Created on Mar 27, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package bibtex.dom;

import java.io.PrintWriter;


/**
 * BibtexPerson objects are elements of BibtexPersonLists, which can be
 * used in author or editor fields.
 * 
 * E.g. Charles Louis Xavier Joseph de la Vall{'e}e Poussin Jr:
 * <pre>
 * first = "Charles Louis Xavier Joseph"
 * preLast = "de la"
 * last = "Vall{'e}e Poussin"
 * lineage = "Jr"
 * </pre>
 * 
 * Fields that are not used are set to null. If isAndOthers is true,
 * all fields are ignored (should be null).
 * 
 * @author henkel
 */
public final class BibtexPerson extends BibtexNode {

	BibtexPerson(
		BibtexFile file,
		String first,
		String preLast,
		String last,
		String lineage,
		boolean isOthers) {
		super(file);
		
		this.first = first;
		this.preLast = preLast;
		this.last = last;
		this.lineage = lineage;
		this.isOthers = isOthers;
	}

	private String first, preLast, last, lineage;
	private boolean isOthers;

	/**
	 * @return String
	 */
	public String getFirst() {
		return first;
	}

	/**
	 * @return boolean
	 */
	public boolean isOthers() {
		return isOthers;
	}

	/**
	 * @return String
	 */
	public String getLast() {
		return last;
	}

	/**
	 * @return String
	 */
	public String getLineage() {
		return lineage;
	}

	/**
	 * @return String
	 */
	public String getPreLast() {
		return preLast;
	}

	/**
	 * Sets the first.
	 * @param first The first to set
	 */
	public void setFirst(String first) {
		this.first = first;
	}

	/**
	 * Sets the isAndOthers.
	 * @param isAndOthers The isAndOthers to set
	 */
	public void setOthers(boolean isAndOthers) {
		this.isOthers = isAndOthers;
	}

	/**
	 * Sets the last. May not be null.
	 * @param last The last to set
	 */
	public void setLast(String last) {
    
		this.last = last;
	}

	/**
	 * Sets the lineage. May be null.
	 * @param lineage The lineage to set
	 */
	public void setLineage(String lineage) {
		this.lineage = lineage;
	}

	/**
	 * Sets the preLast.
	 * @param preLast The preLast to set; may be null.
	 */
	public void setPreLast(String preLast) {
		this.preLast = preLast;
	}

	/* (non-Javadoc)
	 * @see bibtex.dom.BibtexNode#printBibtex(java.io.PrintWriter)
	 */
	public void printBibtex(PrintWriter writer) {
//	    System.err.println("DEBUG: first="+getFirst());
//	    System.err.println("DEBUG: last="+getLast());
//	    System.err.println("DEBUG: lineage="+getLineage());
//	    System.err.println("DEBUG: prelast="+getPreLast());
//	    System.err.println("DEBUG: isothers="+isOthers());
	    
	    
		if (this.isOthers) {
			writer.print("others");
		} else {
			if (this.preLast != null) {
				writer.print(this.preLast);
				writer.print(' ');
			}
			if(this.last!=null) writer.print(this.last);
			if(this.lineage!=null || this.first!=null) writer.print(", ");
			if (this.lineage != null) {
				writer.print(this.lineage);
				writer.print(", ");
			}
			if (this.first != null) {
				writer.print(this.first);
			} else if(this.lineage !=null){
			    //this.lineage!=null && this.first==null
			    
			    //We have to print something here - otherwise the lineage would
			    //get parsed as the first name by bibtex.
			    writer.print("{}"); 
			}
		}
	}

}
