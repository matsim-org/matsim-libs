package org.matsim.codeexamples.guicewithoutmatsim;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.ImplementedBy;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class GuiceTester {
	public static void main(String[] args) {
		Injector injector = Guice.createInjector(new TextEditorModule());
		TextEditor editor = injector.getInstance(TextEditor.class);
		editor.makeSpellCheck();
	}
}

class TextEditor {
	private SpellChecker spellChecker;

	@Inject
	public TextEditor( SpellChecker spellChecker) {
		this.spellChecker = spellChecker;
	}
	public void makeSpellCheck() {
		spellChecker.checkSpelling();
	}
}

//Binding Module
class TextEditorModule extends AbstractModule {
	@Override protected void configure() {
		bind(String.class).annotatedWith(Names.named("JDBC")).toInstance("jdbc:mysql://localhost:5326/emp");
		bind( SpellChecker.class ).to(  SpellCheckerImpl.class ) ;
	}
}

//@ImplementedBy(SpellCheckerImpl.class)
interface SpellChecker {
	public void checkSpelling();
}

//spell checker implementation
class SpellCheckerImpl implements SpellChecker {
	private String dbUrl;
	public SpellCheckerImpl(){}

	@Inject
	public void setDbUrl(@Named("JDBC") String dbUrl) {
		this.dbUrl = dbUrl;
	}
	@Override
	public void checkSpelling() {
		System.out.println("Inside checkSpelling." );
		System.out.println(dbUrl);
	}
}
