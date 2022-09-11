package org.matsim.codeexamples.guicewithoutmatsim;

import com.google.inject.*;
import com.google.inject.multibindings.MapBinder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MapBinderWithStringExample{
	private static final Logger log = LogManager.getLogger( MapBinderWithStringExample.class ) ;

	public static void main ( String [] args ) {
		new MapBinderWithStringExample().run() ;
	}

//	@Inject Map<Annotation, Set<Provider<MyInterface>>> map ;

	void run() {
		List<com.google.inject.Module> modules = new ArrayList<>() ;
		// (there is also a Module in java.lang, and maven gets confused about that.  kai, jul'19)

		modules.add(  new AbstractModule(){
			@Override
			protected void configure(){
				MapBinder<String, MyInterface> mapBinder = MapBinder.newMapBinder( this.binder(), String.class, MyInterface.class );
//				mapBinder.permitDuplicates() ;
				mapBinder.addBinding("abc" ).to( MyImpl1.class ).in( Singleton.class );
				mapBinder.addBinding("def" ).toInstance( new MyImpl2() ) ;
				mapBinder.addBinding("egh").toProvider( MyImpl2::new  ) ;


			}
		} ) ;
		Injector injector = Guice.createInjector( modules );

		Map<Key<?>, Binding<?>> bindings = injector.getAllBindings();

		for( Map.Entry<Key<?>, Binding<?>> entry : bindings.entrySet() ){
			log.info("") ;
			log.info( "key=" + entry.getKey() ) ;
			log.info( "value=" + entry.getValue() ) ;
		}
		log.info("") ;

		Map<String, Provider<MyInterface>> map = injector.getInstance( Key.get( new TypeLiteral<Map<String, Provider<MyInterface>>>(){} ) );;
		Provider<MyInterface> provider = map.get( "abc" );;

//		for( Provider<MyInterface> provider : set ){
			provider.get() ;
//		}

	}

	private interface MyInterface{

	}

	@Singleton
	private static class MyImpl1 implements MyInterface{
		@Inject MyImpl1() {
			log.info( "ctor 1 called" );
		}
	}

	private static class MyImpl2 implements MyInterface{
		@Inject MyImpl2() {
			log.info( "ctor 2 called" );
		}
	}
}
