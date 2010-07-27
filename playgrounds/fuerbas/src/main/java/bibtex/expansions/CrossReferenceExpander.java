/*
 * Created on Mar 29, 2003
 * 
 * @author henkel@cs.colorado.edu
 *  
 */
package bibtex.expansions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import bibtex.dom.BibtexAbstractEntry;
import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexEntry;
import bibtex.dom.BibtexFile;
import bibtex.dom.BibtexString;

/**
 * This expander expands the crossreferences defined by the crossref fields -
 * you should run the MacroReferenceExpander first.
 * 
 * @author henkel
 */
public final class CrossReferenceExpander extends AbstractExpander implements Expander {

	/** Equivalent to CrossReferenceExpander(true) */
	public CrossReferenceExpander() {
		this(true);
	}
	
	/**
	 * @param throwAllExpansionExceptions
	 *            Setting this to true means that all exceptions will be thrown
	 *            immediately. Otherwise, the expander will skip over things it
	 *            can't expand and you can use getExceptions to retrieve the
	 *            exceptions later
	 */
	public CrossReferenceExpander(boolean throwAllExpansionExceptions) {
		super(throwAllExpansionExceptions);
	}

	/**
	 * Note: If you don't use the MacroReferenceExpander first, this function
	 * may lead to inconsistent macro references.
	 * 
	 * If you use the flag throwAllExpansionExceptions set to false, you can
	 * retrieve all the exceptions using getExceptions()
	 * 
	 * @param bibtexFile
	 */
	public void expand(BibtexFile bibtexFile1, BibtexFile bibtexFile2) throws ExpansionException {
		HashMap entryKey2Entry = new HashMap();
		ArrayList entriesWithCrossReference = new ArrayList();
		
		for (Iterator entryIt1 = bibtexFile1.getEntries().iterator(); entryIt1.hasNext();) {
			BibtexAbstractEntry abstractEntry = (BibtexAbstractEntry) entryIt1.next();
			if (!(abstractEntry instanceof BibtexEntry))
				continue;
			BibtexEntry entry = (BibtexEntry) abstractEntry;
			entryKey2Entry.put(entry.getEntryKey().toLowerCase(), abstractEntry);
			if (entry.getFields().containsKey("crossref")) {
				entriesWithCrossReference.add(entry);
			}
		}
		for (Iterator entryIt2 = bibtexFile2.getEntries().iterator(); entryIt2.hasNext();) {
			BibtexAbstractEntry abstractEntry = (BibtexAbstractEntry) entryIt2.next();
			if (!(abstractEntry instanceof BibtexEntry))
				continue;
			BibtexEntry entry = (BibtexEntry) abstractEntry;
			entryKey2Entry.put(entry.getEntryKey().toLowerCase(), abstractEntry);
			if (entry.getFields().containsKey("crossref")) {
				entriesWithCrossReference.add(entry);
			}
		}
		
		for (Iterator entryIt = entriesWithCrossReference.iterator(); entryIt.hasNext();) {
			BibtexEntry entry = (BibtexEntry) entryIt.next();
			String crossrefKey = ((BibtexString) entry.getFields().get("crossref")).getContent().toLowerCase();
			entry.undefineField("crossref");
			BibtexEntry crossrefEntry = (BibtexEntry) entryKey2Entry.get(crossrefKey);
			if (crossrefEntry == null)
				throwExpansionException("Crossref key not found: \"" + crossrefKey + "\"");
			if (crossrefEntry.getFields().containsKey("crossref"))
				throwExpansionException(
					"Nested crossref: \""
						+ crossrefKey
						+ "\" is crossreferenced but crossreferences itself \""
						+ ((BibtexString) crossrefEntry.getFields().get("crossref")).getContent()
						+ "\"");
			Map entryFields = entry.getFields();
			Map crossrefFields = crossrefEntry.getFields();
			for (Iterator fieldIt = crossrefFields.keySet().iterator(); fieldIt.hasNext();) {
				String key = (String) fieldIt.next();
				if (!entryFields.containsKey(key)) {
					entry.setField(key, (BibtexAbstractValue) crossrefFields.get(key));
				}
			}
		}
		finishExpansion();
	}

	@Override
	public void expand(BibtexFile file) throws ExpansionException {
		// TODO Auto-generated method stub
		
	}
}
