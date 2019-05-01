package net.nawaman.script;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.nawaman.script.java.JavaEngine;
import net.nawaman.script.jsr223.JSEngine;
import net.nawaman.usepath.UsableHolder;

/** Manager to manage script engines */
final public class ScriptManager {
	
	/** The name of the Args for main function that executing the code */
	static public final String $MainArgs = "$MainArgs";

	HashMap<String, String>                        EngineNameAlias = new HashMap<String, String>();
	HashMap<String, ScriptEngine>                  DefaultEngines  = new HashMap<String, ScriptEngine>();
	HashMap<String, Class<? extends ScriptEngine>> EngineClasses   = new HashMap<String, Class<? extends ScriptEngine>>();

	// Singleton
	static public final ScriptManager Instance = new ScriptManager();
	
	// Singleton constructor
	private ScriptManager() {}
	
	/* Load an engine from the class name - The client is an object that its class can see the engine */
	public boolean loadEngine(String pEngineClassName, Object pClient) {
		return this.loadEngine(pEngineClassName, (pClient == null)?null:pClient.getClass().getClassLoader());
	}
	
	@SuppressWarnings("unchecked")
	/* Load an engine from the class name - class loader is the class loader that can load the engine */
	public boolean loadEngine(String pEngineClassName, ClassLoader CL) {
		if(this.DefaultEngines.containsKey(pEngineClassName)) return false;
		try {
			Class<?> Cls = getClassByName(pEngineClassName, CL);
			if(Cls == null) return false;
			if(!ScriptEngine.class.isAssignableFrom(Cls)) return false;
			return this.loadEngine((Class<ScriptEngine>)Cls);
		} catch(Exception E) {
			return false;
		}
	}
	
	/* Load an engine from the class name - The client is an object that its class can see the engine */
	public boolean loadEngine(Class<? extends ScriptEngine> pEngineClass) {
		if(pEngineClass == null) return false;
		if(this.DefaultEngines.containsKey(pEngineClass.getCanonicalName())) return false;
		try {
			if(!ScriptEngine.class.isAssignableFrom(pEngineClass)) return false;
			
			// Create and remember default engine
			ScriptEngine SE = this.newEngine(pEngineClass, null);
			if(SE == null) return false;
			if(SE != null) ScriptManager.Usepaths.registerUsableFilter(SE.getUsableFilters());
			
			this.EngineNameAlias.put(SE.getShortName(), pEngineClass.getCanonicalName());
			this.DefaultEngines .put(pEngineClass.getCanonicalName(), SE);
			this.EngineClasses  .put( pEngineClass.getCanonicalName(), pEngineClass);
			return true;
		} catch(Exception E) {
			throw new RuntimeException("Error loading engine of '"+pEngineClass+"': " + E, E);
		}
	}
	
	/** Register a new engine into the script manager */
	public void registerEngine(ScriptEngine pEngine) {
		this.EngineNameAlias.put(pEngine.getShortName(), pEngine.getClass().getCanonicalName());
		
		// If there is no default engine for the engine class name 
		if(this.DefaultEngines.get(pEngine.getClass().getCanonicalName()) == null) {
			this.DefaultEngines.put(pEngine.getShortName(),                pEngine);
			this.DefaultEngines.put(pEngine.getClass().getCanonicalName(), pEngine);
			this.EngineClasses.put(pEngine.getClass().getCanonicalName(), pEngine.getClass());
			
		} else {
			ScriptEngine SE = this.DefaultEngines.get(pEngine.getClass().getCanonicalName());
			if((SE.getOption() != null) && (pEngine.getOption() == null)) {
				// If the default engine has non-null option and the this new one has null option, use this one as a default
				this.DefaultEngines.put(pEngine.getClass().getCanonicalName(), pEngine);
			}
		}
	}
	
	/**
	 * Returns the engine class name from the given name - In case, the name does not seems to match any engine
	 * 	short name, this method will returns the given name itself.
	 **/
	static public String getEngineClassNameByName(String pName) {
		// Get class name from alias
		if(Instance.EngineNameAlias.containsKey(pName)) return Instance.EngineNameAlias.get(pName);
		if(Instance.DefaultEngines.containsKey(pName))  return pName;

		// Pre-load JavaScript
		if(JSEngine.ShortName.equals(pName) || JSEngine.Name.equals(pName)) {
			Instance.loadEngine(JSEngine.class);
			return JSEngine.class.getCanonicalName();
		}
		// Pre-load Java
		if(JavaEngine.ShortName.equals(pName) || JavaEngine.Name.equals(pName)) {
			Instance.loadEngine(JavaEngine.class);
			return JavaEngine.class.getCanonicalName();
		}
		
		try {
			Class<?> C = Class.forName(pName);
			if((C == null) || !(ScriptEngine.class.isAssignableFrom(C))) return null;
			
			Instance.loadEngine(C.asSubclass(ScriptEngine.class));
			return JSEngine.class.getCanonicalName();
			
		} catch (ClassNotFoundException CCE) {}
		
		return pName;
	}
	
	/** Create a new engine from the engine class name and the option */
	public ScriptEngine newEngine(String pEngineClassName, ScriptEngineOption pOption) {
		pEngineClassName = getEngineClassNameByName(pEngineClassName);
		
		// See if it already loaded or load it
		if((pOption == null) && this.DefaultEngines.containsKey(pEngineClassName))
			return this.DefaultEngines.get(pEngineClassName);
		
		try {
			Class<? extends ScriptEngine> ECls = this.EngineClasses.get(pEngineClassName);
			ScriptEngine SE = this.newEngine(ECls, pOption);
			if(SE != null) ScriptManager.Usepaths.registerUsableFilter(SE.getUsableFilters());
			return SE;
		} catch(Exception E) {
			return null;
		}
	}

	/** Create a new engine from the engine class and the option */
	ScriptEngine newEngine(Class<? extends ScriptEngine> pEngineClass, ScriptEngineOption pOption) {
		try {
			Class<? extends ScriptEngine> ECls = pEngineClass;
			if(ECls == null)
				throw new RuntimeException("The script engine is not found");

			// Option is null, find Empty Static method 
			if(pOption == null) {
				Method M = null;
				try { M = ECls.getMethod(ScriptEngine.FactoryMethodName, EmptyClassArray); }
				catch(NoSuchMethodException E) {}
				
				// If the empty constructor is found and it is a public, so run it 
				if((M != null) && Modifier.isPublic(M.getModifiers()) && Modifier.isStatic(M.getModifiers()))
					return (ScriptEngine)M.invoke(null, EmptyObjectArray);
				
			}
			// Find Non-Empty Static method
			Method M = null;
			try { M = ECls.getMethod(ScriptEngine.FactoryMethodName, new Class<?>[] { ScriptEngineOption.class }); }
			catch(NoSuchMethodException E) {}
			
			// If the non-empty constructor is found and it is a public, so run it 
			if((M != null) && Modifier.isPublic(M.getModifiers()) && Modifier.isStatic(M.getModifiers()))
				return (ScriptEngine)M.invoke(pEngineClass, pOption);
			
			// Option is null, try to find an empty constructor
			if(pOption == null) { 
				Constructor<? extends ScriptEngine> Conts = null;
				try { Conts = ECls.getConstructor(EmptyClassArray); }
				catch(NoSuchMethodException E) {}
				
				// If the empty constructor is found and it is a public, so run it 
				if((Conts != null) && Modifier.isPublic(Conts.getModifiers()))
					return Conts.newInstance(EmptyObjectArray);
			}
			
			// In case, that the option is not null or the empty constructur is not found, find the constructor with
			//    option
			Constructor<? extends ScriptEngine> Conts = null;
			try { Conts = ECls.getConstructor(new Class<?>[] { ScriptEngineOption.class }); }
			catch(NoSuchMethodException E) {}

			// It the constructor is found and is a public 
			if((Conts != null) && Modifier.isPublic(Conts.getModifiers()))
				return Conts.newInstance(pOption);
			
		} catch(Exception E) {
			String OptionStr = (pOption == null) ? "" : " with option '" + pOption + "'";
			throw new RuntimeException("Unable to initialize the ScriptEngine from '"+pEngineClass+"'" + OptionStr + ".", E);
		}

		// Cannot create the engine
		throw new RuntimeException(
				"Invalid script engine `"+pEngineClass.getCanonicalName()+"`, the engine does not have appropriate " +
				"factory method or constructor.");
	}

	/** Returns the default engine from the engine class name */
	public ScriptEngine getDefaultEngineOf(String pEngineClassName) {
		pEngineClassName = getEngineClassNameByName(pEngineClassName);
		return this.DefaultEngines.get(pEngineClassName);
	}
	
	static final Class<?>[] EmptyClassArray  = new Class<?>[0];
	static final Object[]   EmptyObjectArray = new Object[0];
	
	/** Returns the class by its name and throws exception if not found. */
	static Class<?> getClassByName(String Name, ClassLoader CL) {
		if(CL == null) CL = ScriptManager.class.getClassLoader();
		// Try to extract the class name from the signature
		String SubClassName = "";
		while(true) {
			Class<?> Cls = null;
			try {
				// Get class by the normal mean
				Cls = (CL == null)?Class.forName(Name):Class.forName(Name, true, CL);
			} catch(ClassNotFoundException  E) {
				// When not found, try to see if the class is declared in side other class.
				// By trimming the string after the last "."
				int Ind = Name.lastIndexOf('.');
				// If there is no more ".", the class is not found.
				if(Ind == -1) return null;
				String S = Name.substring(Ind + 1);
				if(SubClassName.length() == 0) SubClassName = S;
				else                           SubClassName = S + "." + SubClassName; 
				Name = Name.substring(0, Ind);
				// The continue, until forName(String) return a class
				continue;
			}

			// After get the base class, get the sub class if any
			if(SubClassName.length() == 0) return Cls;
			String[] SNs = SubClassName.split("\\.");
			// Loop to get each level of sub-class.
			for(int i = 0; i < SNs.length; i++) {
				Class<?>[] SubClasses = Cls.getDeclaredClasses();
				boolean IsFound = false;
				for(int j = SubClasses.length; --j >= 0; ) {
					if(SubClasses[j].getCanonicalName().equals(Cls.getCanonicalName() + "." + SNs[i])) {
						IsFound = true;
						Cls = SubClasses[j];
						break;
					}
				}
				// Throw an error if not found
				if(!IsFound) { return null; }
			} 
			return Cls;
		}
	}
	
	// Evaluate and compile --------------------------------------------------------------------------------------------
	
	/** Evaluate the code */
	public Object eval(String pCode, Scope pScope, ProblemContainer pResult) {
		if(pCode == null) return null;
		ScriptEngine SE = GetEngineFromCode(pCode);
		if(SE == null)
			throw new RuntimeException("Unknown Script Engine: Unable to extract script name from the code: " + pCode);
		return SE.eval(pCode, pScope, pResult);
	}
	
	/** Evaluate the compiled code */
	public Object eval(CompiledCode pCode, Scope pScope, ProblemContainer pResult) {
		if(pCode == null) return null;
		ScriptEngine SE = this.getDefaultEngineOf(pCode.getEngineName());
		if(SE == null) throw new RuntimeException("Unknown Script Error: " + pCode.getEngineName());
		String Option = pCode.getEngineOptionString();
		if(Option != null) SE = this.newEngine(SE.getName(), SE.getOption(Option));
		if(SE == null) throw new RuntimeException("Unknown Script Error: " + pCode.getEngineName() + "(" + Option + ")");
		return SE.eval(pCode, pScope, pResult);
	}
	
	/** Evaluate the script */
	public Object eval(Script pScript, Scope pScope, ProblemContainer pResult) {
		if(pScript == null) return null;
		return pScript.run(pScope);
	}
	
	/** Compile a code */
	public CompiledCode compile(String pCode, Scope pFrozen, String[] pFrozenVNames,
			CompileOption pOption, ProblemContainer pResult) {
		if(pCode == null) return null;
		ScriptEngine SE = GetEngineFromCode(pCode);
		if(SE == null)
			throw new RuntimeException(
					"Unknown Script Engine: Unable to extract script name from the code: " + pCode);
		return SE.compile(pCode, pFrozen, pFrozenVNames, pOption, pResult);
	}

	/** Recompile a code (also reassign the engine and compile option) */
	public void reCompile(Script pScript, Scope pFrozen, CompileOption pOption, ProblemContainer pResult) {
		if(pScript == null) return;
		ScriptEngine SE = GetEngineFromCode(pScript.getCode());
		if(SE == null)
			throw new RuntimeException(
					"Unknown Script Engine: Unable to extract script name from the code: " + pScript.getCode());
		SE.reCompile(pScript, pFrozen, pOption, pResult);
	}
	
	/** Create a new Script object from the code */
	public Script newScript(String pCode, Scope pFrozen, String[] pFrozenVNames,
			CompileOption pOption, ProblemContainer pResult) {
		if(pCode == null) return null;
		ScriptEngine SE = GetEngineFromCode(pCode);
		if(SE == null)
			throw new RuntimeException("Unknown Script Engine: Unable to extract script name from the code: " + pCode);
		return SE.newScript(pCode, pFrozen, pFrozenVNames, pOption, pResult);
	}
	
	/** Creates a new function */
	public Function newFunction(Signature pSignature, String[] pParamNames, String pCode, Scope pFrozen, String[] pFrozenVNames,
			CompileOption pOption, ProblemContainer pResult) {
		if(pCode == null) return null;
		ScriptEngine SE = GetEngineFromCode(pCode);
		if(SE == null)
			throw new RuntimeException("Unknown Script Engine: Unable to extract script name from the code: " + pCode);
		return SE.newFunction(pSignature, pParamNames, pCode, pFrozen, pFrozenVNames, pOption, pResult);
	}
	
	// Extract Engine name from Code -----------------------------------------------------------------------------------
	
	static public final String  EngineNameExtractorRegExpr = "[^@\n]*@([a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)*)(\\([^\\)\\n]+\\)|\\(\\))?:";
	/** Extractor for extracting engine name from code, the engine name can be both short name and long name */
	static public final Pattern EngineNameExtractor = Pattern.compile(EngineNameExtractorRegExpr);

	
	/**
	 * The frist lines of codes that ends with "##" will be ignored.
	 **/
	static public int GetEndOfIgnored(String pCode) {
		// Ignore all first lines that ends with ##
		int Index = pCode.indexOf('\n');
		int PrevI = 0;
		while(((pCode.indexOf("##", PrevI)) == (PrevI + 1)) || (pCode.lastIndexOf("##\n", Index) == (Index - 2))) {
			PrevI = Index;
			Index = pCode.indexOf('\n', Index + 1);
		}
		return (PrevI == 0) ? 0 : PrevI + 1; // +1 for the '\n'
	}
	
	/**
	 * Extract the engine name from the code.
	 * 
	 * All firsts lines that ends with "##" will be ignored.
	 * After that, the code MUST starts with anything but '@' then '@' followed by the name of an engine name (Identifier)
	 * After that it may follow by Parameter - '[:(:]($Param:~[^[:):]]*~)[:):]'
	 * Then it must ends with a  colon.
	 * To conclude: The Extractor is
	 * 		~ ^[^@]*@([a-zA-Z_$][a-zA-Z_$0-9]*)(\([^\)]*\))?:
	 * 		or in RegParser
	 * 		~ [^[:@:]]*[:@:]($EngineName:~[a-zA-Z_$][a-zA-Z_$0-9]*~)([:(:]($Param:~[^[:):]]*~)[:):])?[:::]
	 * For example:
	 * // @Java(6.0) \n
	 * 
	 * This allows the engine mark to be comment out.
	 **/
	static public String[] GetEngineNameAndParamFromCode(String pCode) {
		Matcher M = EngineNameExtractor.matcher(pCode);
		
		if(!M.find(GetEndOfIgnored(pCode))) return null;
		
		String EName = M.group(1);
		if(EName == null) return null;
		
		String EParam = M.group(M.groupCount());
		if(EParam != null) {
			EParam = EParam.trim();
			EParam = (EParam.length() >= 2)?EParam.substring(1, EParam.length() - 1).trim():null;
		}
		
		return new String[] { EName, EParam };
	}
	
	/**
	 * Extract the engine name from the code.
	 * 
	 * All firsts lines that ends with "##" will be ignored.
	 * After that, the code MUST starts with anything but '@' then '@' followed by the name of an engine name (Identifier)
	 * After that it may follow by Parameter - '[:(:]($Param:~[^[:):]]*~)[:):]'
	 * Then it must ends with a  colon.
	 * To conclude: The Extractor is
	 * 		~ ^[^@]*@([a-zA-Z_$][a-zA-Z_$0-9]*)(\([^\)]*\))?:
	 * 		or in RegParser
	 * 		~ [^[:@:]]*[:@:]($EngineName:~[a-zA-Z_$][a-zA-Z_$0-9]*~)([:(:]($Param:~[^[:):]]*~)[:):])?[:::]
	 * For example:
	 * // @Java(6.0) \n
	 * 
	 * This allows the engine mark to be comment out.
	 **/
	@SuppressWarnings("finally")
	static public ScriptEngine GetEngineFromCode(String pCode) {
		
		String[] EngineNameAndParam = GetEngineNameAndParamFromCode(pCode);
		ScriptEngine SE     = null;
		String       EName  = null;
		String       EParam = null;
		
		try {
			if((EngineNameAndParam == null) || (EngineNameAndParam.length != 2)) return null;
			
			EName  = EngineNameAndParam[0];
			EParam = EngineNameAndParam[1];
			
			SE = ScriptManager.Instance.DefaultEngines.get(EName);
			if(SE == null) {
				EName = getEngineClassNameByName(EName);
				
				SE = ScriptManager.Instance.getDefaultEngineOf(EName);
				if(SE == null) return null;
				
				ScriptEngineOption SEOption = SE.getOption(EParam);
				if(SEOption != null) SE = ScriptManager.Instance.newEngine(EName, SEOption); 
			}
			
		} finally{
			if(SE == null) {
				if(EName == null) return null;
				else throw new RuntimeException("Unknown ScriptEngine: " + EName + ".");
			}
			return SE;
			
		}
	}
	
	// Usepath and namespaces ------------------------------------------------------------------------------------------

	static public final Usepaths Usepaths = new Usepaths();
	
	/** Searchs and Returns the executable. */
	static public Executable Use(String Name) {
		try { return UseWithException(Name); }
		catch (FileNotFoundException  E) {}
		catch (IOException            E) {}
		catch (ClassNotFoundException E) {}
		return null;
	}
	
	/** Searchs and Returns the executable. */
	@SuppressWarnings("unchecked")
	static public Executable UseWithException(String Name) throws FileNotFoundException, IOException,
								ClassNotFoundException {
		UsableHolder<Executable> UH = (UsableHolder<Executable>)Usepaths.getUsableHolder(Name);
		if(UH != null) return UH.get();
		
		return null;
	}
	
}
