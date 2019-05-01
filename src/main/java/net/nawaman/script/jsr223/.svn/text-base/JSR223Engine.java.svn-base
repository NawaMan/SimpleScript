package net.nawaman.script.jsr223;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Set;

import javax.script.Compilable;
import javax.script.ScriptEngineManager;

import net.nawaman.script.CompileOption;
import net.nawaman.script.CompiledCode;
import net.nawaman.script.Executable;
import net.nawaman.script.ExecutableInfo;
import net.nawaman.script.FrozenVariableInfos;
import net.nawaman.script.Function;
import net.nawaman.script.Macro;
import net.nawaman.script.ProblemContainer;
import net.nawaman.script.Scope;
import net.nawaman.script.Script;
import net.nawaman.script.ScriptEngine;
import net.nawaman.script.ScriptEngineOption;
import net.nawaman.script.ScriptManager;
import net.nawaman.script.Signature;
import net.nawaman.script.SimpleScriptExecutionExceptionWrapper;
import net.nawaman.script.Utils;
import net.nawaman.usepath.UsableFilter;

/** Script Engine of JSR223 */
abstract public class JSR223Engine implements ScriptEngine {
	
	public JSR223Engine() {}
	
	static public final javax.script.ScriptEngineManager TheFactory = new ScriptEngineManager();
	
	/** Returns the JSR223 Script Engine */
	abstract protected javax.script.ScriptEngine getTheEngine();

	/**{@inheritDoc}*/ @Override
	abstract public String getName();
	
	/**{@inheritDoc}*/ @Override
	abstract public String getShortName();
	
	/**{@inheritDoc}*/ @Override
	public ScriptEngineOption getOption() {
		return null;
	}
	/**{@inheritDoc}*/ @Override
	public String getParameterString() {
		return null;
	}
	/**{@inheritDoc}*/ @Override
	public ScriptEngineOption getOption(String pParam) {
		return null;
	}

	/**{@inheritDoc}*/ @Override
	public ExecutableInfo getReplaceExecutableInfo(ExecutableInfo EInfo) {
		return null;
	}
	
	/**{@inheritDoc}*/ @Override
	public UsableFilter[] getUsableFilters() {
		return null;
	}
	
	/**{@inheritDoc}*/ @Override
	public Scope newScope() {
		return new JSR223Scope();
	}
	
	/**{@inheritDoc}*/ @Override
	public Scope getCompatibleScope(Scope pOrg) {
		if(pOrg instanceof JSR223Scope) return pOrg;
		Scope Target = this.newScope();
		Scope.Simple.duplicate(pOrg, Target);
		return Target;
	}

	/**{@inheritDoc}*/ @Override
	public ProblemContainer newCompileProblemContainer() {
		return new ProblemContainer();
	}
	
	/**{@inheritDoc}*/ @Override
	public Object eval(String pCode, Scope pScope, ProblemContainer pResult) {
		try {
			if((pScope != null) && !(pScope instanceof JSR223Scope)) {
				JSR223Scope NewScope = (JSR223Scope)Scope.Simple.getDuplicateOf(this, pScope);
				Object Result = this.getTheEngine().eval(pCode, NewScope.Context);
				Scope.Simple.duplicate(NewScope, pScope);
				return Result;
				
			} else {
				return this.getTheEngine().eval(pCode, (pScope == null)?null:((JSR223Scope)pScope).Context);
				
			}
		} catch(Exception E) {
			throw new SimpleScriptExecutionExceptionWrapper(E);
		}
	}
	
	/**{@inheritDoc}*/ @Override
	public Object eval(CompiledCode pCode, Scope pScope, ProblemContainer pResult) {
		try {
			// Convert the Scope
			JSR223Scope NewScope = null; 
			if(pScope == null)
				 NewScope = new JSR223Scope();
			else if(!(pScope instanceof JSR223Scope))
				 NewScope = (JSR223Scope)Scope.Simple.getDuplicateOf(this, pScope);
			else NewScope = (JSR223Scope)pScope;
			
			Object Result = null;
			Scope  FSSave = null;

			JSR223CompiledCode JCC    = (JSR223CompiledCode)pCode;
			Scope              FScope = JCC.FrozenScope;
			try {
				// Save the one that already in the scope and add the one that is not
				if(FScope != null) {
					Set<String> FVNames = FScope.getVariableNames();
					for(String FVName : FVNames) {
						if(FVName == null) continue;
						// Save it
						if(NewScope.isExist(FVName)) {
							if(FSSave == null) FSSave = new Scope.Simple();
							FSSave.newVariable(FVName, NewScope.getTypeOf(FVName), NewScope.getValue(FVName));
							NewScope.removeVariable(FVName);
						}
						// Create
						NewScope.newVariable(FVName, FScope.getTypeOf(FVName), FScope.getValue(FVName));
					}
				}
				
				Result = JCC.CompiledScript.eval(NewScope.Context);
			} finally {
				// Remove the variable
				if(FScope != null) {
					Set<String> FVNames = FScope.getVariableNames();
					for(String FVName : FVNames) {
						if(FVName == null) continue;
						if(NewScope.isExist(FVName)) NewScope.removeVariable(FVName);
					}
					
					// Restore the saved
					if(FSSave != null) {
						for(String FVName : FSSave.getVariableNames()) {
							if(FVName == null) continue;
							if(NewScope.isExist(FVName)) NewScope.removeVariable(FVName);
							// Create
							NewScope.newVariable(FVName, FSSave.getTypeOf(FVName), FSSave.getValue(FVName));
						}
					}
				}
			}
			
			// Copy the variable backup (in case there is a new)
			if((pScope != null) && !(pScope instanceof JSR223Scope))
				Scope.Simple.duplicate(NewScope, pScope);
			
			return Result;
		} catch(Exception E) {
			throw new SimpleScriptExecutionExceptionWrapper(E);
		}
	}
	
	/**{@inheritDoc}*/ @Override
	public Object eval(Script pScript, Scope pScope, ProblemContainer pResult) {
		// Ensure that only JSR223 is accepted
		if((pScript == null) || !(pScript.getEngine() instanceof JSR223Engine))
			throw new RuntimeException("JSEngine can execute only JSR223 code.");
		
		// If the engine is not compilable, evaluate it as code
		if(!(pScript.getEngine() instanceof Compilable))
			return this.eval(pScript.getCode(), pScope, pResult);
		
		// Else, evaluate it as compiled script
		return this.eval(pScript.getCompiledCode(), pScope, pResult);
	}
	
	/**{@inheritDoc}*/ @Override
	public CompiledCode compile(String pCode, Scope pFrozen, String[] pFrozenVNames,
			CompileOption pOption, ProblemContainer pResult) {

		if(!(this.getTheEngine() instanceof Compilable))
			throw new RuntimeException("This script engine is not compilable.");
		
		try { return new JSR223CompiledCode(this.getName(), (this.getOption() == null)?null:this.getOption().toString(),
				pFrozen, pFrozenVNames, ((Compilable)this.getTheEngine()).compile(pCode));
		} catch(Exception E) { throw new SimpleScriptExecutionExceptionWrapper(E); }
	}
	
	/**{@inheritDoc}*/ @Override
	public void reCompile(Script pScript, Scope pFrozen, CompileOption pOption, ProblemContainer pResult) {
		if(!(this.getTheEngine() instanceof Compilable))
			throw new RuntimeException("This script engine is not compilable.");
		
		// Ensure that only JSR223 is accepted
		if((pScript == null) || !(pScript.getEngine() instanceof JSR223Engine))
			throw new RuntimeException("JSEngine can execute only JSR223 code.");

		try {
			String EngineOption = (this.getOption() == null)?null:this.getOption().toString();
			
			FrozenVariableInfos TheFVInfos = ((JSR223Script)pScript).getFVInfos();
			String[]            TheFVNames = (TheFVInfos == null)?null:TheFVInfos.getFrozenVariableNames();
			
			((JSR223Script)pScript).CCode =
				new JSR223CompiledCode(this.getName(), EngineOption, pFrozen, TheFVNames,
						((Compilable)this.getTheEngine()).compile(pScript.getCode()));
		} catch(Exception E) {
			throw new SimpleScriptExecutionExceptionWrapper(E);
		}
	}
	
	/**{@inheritDoc}*/ @Override
	public boolean isCompilable() {
		return (this.getTheEngine() instanceof Compilable);
	}

	/**{@inheritDoc}*/ @Override
	public boolean isCompiledCodeSerializable() {
		return false;
	}
	
	/**{@inheritDoc}*/ @Override
	public Script newScript(String pCode, Scope pFrozen, String[] pFrozenVNames,
			CompileOption pOption, ProblemContainer pResult) {
		JSR223CompiledCode CCode = (JSR223CompiledCode)this.compile(pCode, pFrozen, pFrozenVNames, null, null);
		return new JSR223Script.Simple(this.getName(), pCode, FrozenVariableInfos.newFVInfos(pFrozenVNames, pFrozen), CCode);
	}

	// Macro -----------------------------------------------------------------------------------------------------------
	
	static class JSR223SMBody extends JSR223SFBody implements Macro.Simple.Body {
		
		private static final long serialVersionUID = 431891656520101259L;

		JSR223SMBody(Signature pSignature, Script pScript) {
			super(pSignature, pScript);
		}
		
		/** Execute the macro body - The parameter must be a direct map (VarArgs is not applied) */
		public Object run(Macro.Simple pMacro, Scope $Scope, Object[] pParams) {
			Macro.Simple M = pMacro;
			Scope Scope  = $Scope; if(Scope == null) Scope = this.getEngine().newScope();
			Scope FSSave = null;
			try {
				for(int i = this.getSignature().getParamCount(); --i >= 0; ) {
					String FVName = M.getParameterName(i);
					if($Scope != null) {
						// Save it
						if($Scope.isExist(FVName)) {
							if(FSSave == null) FSSave = new Scope.Simple();
							FSSave.newVariable(FVName, $Scope.getTypeOf(FVName), $Scope.getValue(FVName));
							$Scope.removeVariable(FVName);
						}
					}
					// Create
					Scope.newVariable(M.getParameterName(i), this.getSignature().getParamType(i), pParams[i]);
				}
				return this.Script.run(Scope);
			} finally {
				// Remove the variable
				if(FSSave != null) {
					for(String FVName : FSSave.getVariableNames()) {
						if(FVName == null) continue;
						if($Scope.isExist(FVName)) $Scope.removeVariable(FVName);
						// Create
						$Scope.newVariable(FVName, FSSave.getTypeOf(FVName), FSSave.getValue(FVName));
					}
				}
			}
		}
	}
	
	/**{@inheritDoc}*/ @Override
	public Macro newMacro(Signature pSignature, String[] pParamNames, String pCode,
			Scope pFrozen, String[] pFrozenVNames, CompileOption pOption, ProblemContainer pResult) {
		return new JSR223Macro(
					pParamNames,
					new JSR223SMBody(pSignature, this.newScript(pCode, pFrozen, pFrozenVNames, pOption, pResult)),
					FrozenVariableInfos.newFVInfos(pFrozenVNames, pFrozen));
	}

	// Function --------------------------------------------------------------------------------------------------------
	
	static class JSR223SFBody implements Function.Simple.Body, Serializable {
		
		private static final long serialVersionUID = -6452338282555661959L;

		JSR223SFBody(Signature pSignature, Script pScript) {
			this.Signature = pSignature;
			this.Script    = pScript;
		}
		
		Signature Signature;
		Script    Script;
		
		/** Returns the engine name */
		public String getEngineName() {
			return this.Script.getEngineName();
		}
		
		/** Returns the engine for executing this script */
		public ScriptEngine getEngine() {
			return this.Script.getEngine();
		}
		
		/** Returns the code as text */
		public String getCode() {
			return this.Script.getCode();
		}
		
		/** the interface of the function */
		public Signature getSignature() {
			return this.Signature;
		}
		
		/** Execute the function body - The parameter must be a direct map (VarArgs is not applied) */
		public Object run(Function.Simple pFunction, Object[] pParams) {
			Function.Simple F = pFunction;
			Scope S = this.Script.getEngine().newScope();
			for(int i = this.getSignature().getParamCount(); --i >= 0; ) {
				S.newVariable(F.getParameterName(i), this.getSignature().getParamType(i), pParams[i]);
			}
			return this.Script.run(S);
		}
	}
	
	/**{@inheritDoc}*/ @Override
	public Function newFunction(Signature pSignature, String[] pParamNames, String pCode,
			Scope pFrozen, String[] pFrozenVNames, CompileOption pOption, ProblemContainer pResult) {
		return new JSR223Function(
					pParamNames,
					new JSR223SFBody(pSignature, this.newScript(pCode, pFrozen, pFrozenVNames, pOption, pResult)),
					FrozenVariableInfos.newFVInfos(pFrozenVNames, pFrozen));
	}

	/** Create an Executable using the ExecutableInfo and the Code */
	public Executable compileExecutable(ExecutableInfo pExecInfo, String pCode, CompileOption pOption,
			ProblemContainer pResult) {
		return Utils.compileExecutable(this, pExecInfo, pCode, pOption, pResult);
	}

	/** Create a simple JSR223 script */
	final protected Script newSimpleScript(String pCode) {
		JSR223CompiledCode CCode = (JSR223CompiledCode)this.compile(pCode, null, null, null, null);
		return new JSR223Script.Simple(this.getName(), pCode, null, CCode);
	}
	
	/**{@inheritDoc}*/ @Override
	public ObjectOutputStream newExecutableObjectOutputStream(OutputStream OS) {
		return null;
	}
	/**{@inheritDoc}*/ @Override
	public ObjectInputStream newExecutableObjectInputStream(InputStream IS) {
		return null;
	}
	
	// Utilities -------------------------------------------------------------------------------------------------------

	static public void main(String ... Args) {
		String Code = "println(Prefix + \": \" + Text + \": \" + I);";
		System.out.println("Code:-----------------------------------------------------------------------");
		System.out.println(Code);
		System.out.println("----------------------------------------------------------------------------");

		JSEngine JSE = (JSEngine)ScriptManager.Instance.getDefaultEngineOf("JavaScript");

		Scope MainS = JSE.newScope();
		MainS.newVariable("Text", String.class, "Hello from CodeLab!");

		Macro M = JSE.newMacro(new Signature.Simple("fact", Long.class, false, Long.class), new String[] { "I" }, Code, MainS,
				new String[] { "Text" }, null, null);

		MainS.newVariable("Prefix", String.class, "HERE");
		for(int i = 0; i < 21; i++) M.run(MainS, (long)i);
	}
}
