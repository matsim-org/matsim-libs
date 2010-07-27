/*
 * Created on Mar 29, 2003
 * 
 * @author henkel@cs.colorado.edu
 *  
 */
package bibtex.expansions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import bibtex.dom.BibtexAbstractEntry;
import bibtex.dom.BibtexAbstractValue;
import bibtex.dom.BibtexConcatenatedValue;
import bibtex.dom.BibtexEntry;
import bibtex.dom.BibtexFile;
import bibtex.dom.BibtexMacroDefinition;
import bibtex.dom.BibtexMacroReference;
import bibtex.dom.BibtexPreamble;
import bibtex.dom.BibtexStandardMacros;
import bibtex.dom.BibtexString;
import bibtex.dom.BibtexToplevelComment;

/**
 * This expander expands macro references into strings - have a look at the
 * options that can be given in the constructor.
 * 
 * @author henkel
 */
public final class MacroReferenceExpander extends AbstractExpander implements Expander {

	/**
	 * 
	 * This is just a convenience / backward compatibility constructor. It is
	 * equivalent to calling MacroReferenceExpander(expandStandardMacros,
	 * expandMonthAbbreviations, removeMacros, true);
	 */

	public MacroReferenceExpander(
		boolean expandStandardMacros,
		boolean expandMonthAbbreviations,
		boolean removeMacros) {
		this(expandStandardMacros, expandMonthAbbreviations, removeMacros, true);
	}

	/**
	 * @param expandStandardMacros
	 *            Expand all standard macros as defined in the bibtex file
	 *            plain.bst
	 * @param expandMonthAbbreviations
	 *            Expand all month abbreviations. This parameter has precedence
	 *            over the first one (note that the month abbreviations are a
	 *            subset of the standard macros).
	 * @param removeMacros
	 *            Remove all macros from the bibtex model.
	 * @param throwAllExpansionExceptions
	 *            Setting this to true means that all exceptions will be thrown
	 *            immediately. Otherwise, the expander will skip over things it
	 *            can't expand and you can use getExceptions to retrieve the
	 *            exceptions later
	 */
	public MacroReferenceExpander(
		boolean expandStandardMacros,
		boolean expandMonthAbbreviations,
		boolean removeMacros,
		boolean throwAllExpansionExceptions) {
		super(throwAllExpansionExceptions);
		this.expandMonthAbbreviations = expandMonthAbbreviations;
		this.expandStandardMacros = expandStandardMacros;
		this.removeMacros = removeMacros;
	}

	private final boolean expandStandardMacros;
	private final boolean expandMonthAbbreviations;
	private final boolean removeMacros;


	/**
	 * This method walks over all entries in a BibtexFile and expands macro
	 * references. Thus, after the execution of this function, all fields
	 * contain BibtexString entries. Exceptions: 1) the crossref fields 2)
	 * 3-letter month abbreviations and standard macros, if specified in the
	 * constructor (MacroReferenceExpander).
	 * 
	 * If you use the flag throwAllExpansionExceptions set to false, you can
	 * retrieve all the exceptions using getExceptions()
	 * 
	 * @param bibtexFile
	 */
	public void expand(BibtexFile bibtexFile) throws ExpansionException {
		HashMap stringKey2StringValue = new HashMap();
		for (Iterator entryIt = bibtexFile.getEntries().iterator(); entryIt.hasNext();) {
			BibtexAbstractEntry abstractEntry = (BibtexAbstractEntry) entryIt.next();
			if (abstractEntry instanceof BibtexMacroDefinition) {
				BibtexMacroDefinition bibtexStringDefinition = (BibtexMacroDefinition) abstractEntry;
				BibtexAbstractValue simplifiedValue =
					simplify(bibtexFile, bibtexStringDefinition.getValue(), stringKey2StringValue);
				bibtexStringDefinition.setValue(simplifiedValue);
				if (removeMacros) {
					bibtexFile.removeEntry(bibtexStringDefinition);
				};
				stringKey2StringValue.put(bibtexStringDefinition.getKey().toLowerCase(), simplifiedValue);

			} else if (abstractEntry instanceof BibtexPreamble) {
				BibtexPreamble preamble = (BibtexPreamble) abstractEntry;
				preamble.setContent(simplify(bibtexFile, preamble.getContent(), stringKey2StringValue));
			} else if (abstractEntry instanceof BibtexEntry) {
				BibtexEntry entry = (BibtexEntry) abstractEntry;
				for (Iterator fieldIt = entry.getFields().entrySet().iterator(); fieldIt.hasNext();) {
					Map.Entry field = (Map.Entry) fieldIt.next();
					if (!(field.getValue() instanceof BibtexString)) {
						entry.setField(
							(String) field.getKey(),
							simplify(bibtexFile, (BibtexAbstractValue) field.getValue(), stringKey2StringValue));
					}
				}
			} else if (abstractEntry instanceof BibtexToplevelComment) {
				// don't do anything here ...
			} else {
				throwExpansionException(
					"MacroReferenceExpander.expand(): I don't support \""
						+ abstractEntry.getClass().getName()
						+ "\". Use the force, read the source!");
			}
		}
		finishExpansion();
	}

	private BibtexAbstractValue simplify(
		BibtexFile factory,
		BibtexAbstractValue compositeValue,
		HashMap stringKey2StringValue)
		throws ExpansionException {
		if (compositeValue instanceof BibtexString)
			return (BibtexString) compositeValue;
		if (compositeValue instanceof BibtexMacroReference) {
			BibtexMacroReference reference = (BibtexMacroReference) compositeValue;
			String key = reference.getKey();

			BibtexString simplifiedValue = (BibtexString) stringKey2StringValue.get(key);
			if (simplifiedValue == null) {

				if (!this.expandMonthAbbreviations && BibtexStandardMacros.isMonthAbbreviation(key))
					return reference;
				if (!this.expandStandardMacros && BibtexStandardMacros.isStandardMacro(key))
					return reference;

				if (BibtexStandardMacros.isStandardMacro(key)) {
					return factory.makeString(BibtexStandardMacros.resolveStandardMacro(key));
				} else {
					throwExpansionException(
						"Invalid macro reference (target does not exist): \"" + reference.getKey() + "\"");
					return factory.makeString(""); // if target does not exist:
					// resolve to empty string.
				}
			}
			return simplifiedValue;
		}
		if (compositeValue instanceof BibtexConcatenatedValue) {
			BibtexConcatenatedValue concatenatedValue = (BibtexConcatenatedValue) compositeValue;
			BibtexAbstractValue left = simplify(factory, concatenatedValue.getLeft(), stringKey2StringValue);
			BibtexAbstractValue right = simplify(factory, concatenatedValue.getRight(), stringKey2StringValue);
			if (left instanceof BibtexString && right instanceof BibtexString)
				return factory.makeString(
					((BibtexString) left).getContent() + ((BibtexString) right).getContent());
			else
				return factory.makeConcatenatedValue(left, right);
		}
		throwExpansionException(
			"MacroReferenceExpander.simplify(): I don't support \""
				+ compositeValue.getClass().getName()
				+ "\". Use the force, read the source!");
		return factory.makeString(""); // so we don't know what to do, let's
		// use the empty string
	}

	


}
