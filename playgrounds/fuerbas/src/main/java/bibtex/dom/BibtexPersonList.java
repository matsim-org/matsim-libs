/*
 * Created on Mar 27, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package bibtex.dom;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A list of BibtexPerson objects that can be used for author or editor fields - use
 * the PersonListExpander to convert all editor/author field values of a particular
 * BibtexFile to BibtexPersonLists.
 * 
 * @author henkel
 */
public final class BibtexPersonList extends BibtexAbstractValue {

	BibtexPersonList(BibtexFile file){
		super(file);
	}

	private LinkedList list = new LinkedList();

	/**
	 * Returns a read-only list which members are instances of BibtexPerson.
	 * 
	 * @return BibtexPerson
	 */
	public List getList() {
		return Collections.unmodifiableList(list);
	}
	
	public void add(BibtexPerson bibtexPerson){
	    
	    assert bibtexPerson!=null: "bibtexPerson parameter may not be null.";
	    
		this.list.add(bibtexPerson);
	}


	/* (non-Javadoc)
	 * @see bibtex.dom.BibtexNode#printBibtex(java.io.PrintWriter)
	 */
	public void printBibtex(PrintWriter writer) {
	    
	    assert writer!=null: "writer parameter may not be null.";
	    
		boolean isFirst = true;
		writer.print('{');
		for(Iterator it = list.iterator();it.hasNext();){
			if(isFirst){
				isFirst = false;
			} else writer.print(" and ");
			((BibtexPerson)it.next()).printBibtex(writer);
		}
		writer.print('}');
	}

}
