package net.nawaman.script;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import net.nawaman.usepath.UsableFilter;

/**
 * The implementation of ScriptEngine must have one of the following:
 * 	1. A public static method named newInstance() without parameter
 * 	2. A public static method named newInstance(CompileOption pOption) with CompileOption as an only parameter
 * 	3. A public empty constructor - if the script engien can be create without option
 * 	4. A public constructor with CompileOption as an only parameter 
 */

/** The engine of a script */
public interface ScriptEngine {
	
	/** A static method for creating a script engine */
	static public final String FactoryMethodName = "newInstance";
	
	/** The name of the script (class name of the engine) */
	public String getName();

	/** The name short of the script engine */
	public String getShortName();
	
	/** Returns the options of this engine */
	public ScriptEngineOption getOption();
	
	/** Returns the parameter of this engine */
	public String getParameterString();
	
	/** Replace the ExecutableInfo if needed */
	public ExecutableInfo getReplaceExecutableInfo(ExecutableInfo EInfo);
	
	/**
	 * Returns the options of this engine using the parameter - this method is for the ScriptManager to consult the
	 * Default Engine of the same kind for the Option form the Parameter.
	 **/
	public ScriptEngineOption getOption(String pParam);
	
	/** Returns the UsableFilter to be used for this script Engine */
	public UsableFilter[] getUsableFilters();
	
	/** Create a new scope that is compatible when using with script of this engine */
	public Scope newScope();
	
	/** Returns the scope that can be used with the code of this engine but contains all variables of the given Scope */
	public Scope getCompatibleScope(Scope pOrg);
	
	/** Create a new compile problem container  that is compatible when using with script of this engine */
	public ProblemContainer newCompileProblemContainer();
	
	/** Evaluate the code */
	public Object eval(String pCode, Scope pScope, ProblemContainer pResult);
	
	/** Evaluate the compiled code */
	public Object eval(CompiledCode pCode, Scope pScope, ProblemContainer pResult);
	
	/** Evaluate the script */
	public Object eval(Script pScript, Scope pScope, ProblemContainer pResult);
	
	/** Compile a code */
	public CompiledCode compile(String pCode, Scope pFrozen, String[] pFrozenVNames, CompileOption pOption,
			ProblemContainer pResult);

	/** Recompile a code (also reassign the engine and compile option) */
	public void reCompile(Script pScript, Scope pFrozen, CompileOption pOption, ProblemContainer pResult);
	
	/** Checks if the engine is compilable */
	public boolean isCompilable();

	/**
	 * Checks if compiled code of this engine is serializable (and not that the original code is neded).
	 * If the original code is needed to run the code after it is de-serialized, this method should return false.
	 **/
	public boolean isCompiledCodeSerializable();
	
	/** Create a new Script object from the code */
	public Script newScript(String pCode, Scope pFrozen, String[] pFrozenVNames,
			CompileOption pOption, ProblemContainer pResult);
	
	/** Creates a new macro */
	public Macro newMacro(Signature pSignature, String[] pParamNames, String pCode, Scope pFrozen,
			String[] pFrozenVNames, CompileOption pOption, ProblemContainer pResult);
	
	/** Creates a new function */
	public Function newFunction(Signature pSignature, String[] pParamNames, String pCode, Scope pFrozen,
			String[] pFrozenVNames, CompileOption pOption, ProblemContainer pResult);

	/** Create an Executable using the ExecutableInfo and the Code */
	public Executable compileExecutable(ExecutableInfo pExecInfo, String pCode, CompileOption pOption,
			ProblemContainer pResult);

	/** Returns the long comments of the given comment text */
	public String getLongComments(String Comment, int Width);
	
	/**
	 * Returns an new executable to be used to save executable of this engine or null if a regular JCObjectOutputStream
	 *    one should be used.
	 **/
	public ObjectOutputStream newExecutableObjectOutputStream(OutputStream OS);
	
	/**
	 * Returns an new executable to be used to load executable of this engine or null if a regular JCObjectInputStream
	 * one should be used.
	 **/
	public ObjectInputStream newExecutableObjectInputStream(InputStream IS);

	// Implementation --------------------------------------------------------------------------------------------------
	
	/** Simple Implementation of ScriptEngine */
	static abstract public class Simple implements ScriptEngine {

		/** The name of the script (class name of the engine) */
		abstract public String getName();

		/** The name short of the script engine */
		abstract public String getShortName();

		/** Returns the long comments of the given comment text */
		abstract public String getLongComments(String Comment, int Width);
		
		/**{@inheritDoc}*/ @Override
		public ScriptEngineOption getOption() {
			// No special option
			return null;
		}
		/**{@inheritDoc}*/ @Override
		public String getParameterString() {
			// No special option
			return null;
		}
		
		/**{@inheritDoc}*/ @Override
		public ScriptEngineOption getOption(String pParam) {
			// No special option
			return null;
		}
		
		/**{@inheritDoc}*/ @Override
		public UsableFilter[] getUsableFilters() {
			return null;
		}
		
		/**{@inheritDoc}*/ @Override
		public Scope newScope() {
			// No special scope needed
			return new Scope.Simple();
		}
		
		/**{@inheritDoc}*/ @Override
		public Scope getCompatibleScope(Scope pOrg) {
			// No special scope needed
			return pOrg;
		}
		
		/**{@inheritDoc}*/ @Override
		public ProblemContainer newCompileProblemContainer() {
			// No special containher needed
			return new ProblemContainer();
		}
		
		/**{@inheritDoc}*/ @Override
		public Object eval(String pCode, Scope pScope, ProblemContainer pResult) {
			throw new RuntimeException("Eval is not suported by this Engine `"+this.getShortName()+"`.");
		}
		
		/**{@inheritDoc}*/ @Override
		public Object eval(CompiledCode pCode, Scope pScope, ProblemContainer pResult) {
			throw new RuntimeException("Eval is not suported by this Engine `"+this.getShortName()+"`.");
		}
		
		/**{@inheritDoc}*/ @Override
		public Object eval(Script pScript, Scope pScope, ProblemContainer pResult) {
			throw new RuntimeException("Eval is not suported by this Engine `"+this.getShortName()+"`.");
		}
		
		/**{@inheritDoc}*/ @Override
		public CompiledCode compile(String pCode, Scope pFrozen, String[] pFrozenVNames, CompileOption pOption,
				ProblemContainer pResult) {
			throw new RuntimeException("CompiledCode is not suported by this Engine `"+this.getShortName()+"`.");
		}

		/**{@inheritDoc}*/ @Override
		public void reCompile(Script pScript, Scope pFrozen, CompileOption pOption, ProblemContainer pResult) {
			throw new RuntimeException("Script recompiling is not suported by this Engine `"+this.getShortName()+"`.");
		}
		
		/**{@inheritDoc}*/ @Override
		public boolean isCompilable() {
			return false;
		}

		/**{@inheritDoc}*/ @Override
		public boolean isCompiledCodeSerializable() {
			return false;
		}
		
		/**{@inheritDoc}*/ @Override
		public Script newScript(String pCode, Scope pFrozen, String[] pFrozenVNames,
				CompileOption pOption, ProblemContainer pResult) {
			throw new RuntimeException("Script is not suported by this Engine `"+this.getShortName()+"`.");
		}
		
		/**{@inheritDoc}*/ @Override
		public Macro newMacro(Signature pSignature, String[] pParamNames, String pCode, Scope pFrozen,
				String[] pFrozenVNames, CompileOption pOption, ProblemContainer pResult) {
			throw new RuntimeException("Script is not suported by this Engine `"+this.getShortName()+"`.");
		}
		
		/**{@inheritDoc}*/ @Override
		public Function newFunction(Signature pSignature, String[] pParamNames, String pCode, Scope pFrozen,
				String[] pFrozenVNames, CompileOption pOption, ProblemContainer pResult) {
			throw new RuntimeException("Function is not suported by this Engine `"+this.getShortName()+"`.");
		}

		/**{@inheritDoc}*/ @Override
		public Executable compileExecutable(ExecutableInfo pExecInfo, String pCode, CompileOption pOption,
				ProblemContainer pResult) {
			return Utils.compileExecutable(this, pExecInfo, pCode, pOption, pResult);
		}
		
		/**{@inheritDoc}*/ @Override
		public ObjectOutputStream newExecutableObjectOutputStream(OutputStream OS) {
			return null;
		}
		/**{@inheritDoc}*/ @Override
		public ObjectInputStream newExecutableObjectInputStream(InputStream IS) {
			return null;
		}
	}
	
}
