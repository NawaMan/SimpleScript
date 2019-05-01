package net.nawaman.script.jsr223;

import javax.script.Compilable;

import net.nawaman.script.CompiledCode;
import net.nawaman.script.FrozenVariableInfos;
import net.nawaman.script.Scope;
import net.nawaman.script.Script;
import net.nawaman.script.ScriptEngine;
import net.nawaman.script.ScriptManager;

/** Script for JSR223 script */
abstract public class JSR223Script implements Script {

	private static final long serialVersionUID = 3720529499285786142L;

	/** Constructs a JSR223 script */
	protected JSR223Script(String pCode, FrozenVariableInfos pFVInfos, JSR223CompiledCode pCCode) {
		this.CCode   = pCCode;
		this.Code    = (pCode    == null)? "" : pCode;
		this.FVInfos = (pFVInfos == null)?null:pFVInfos.clone();

		FrozenVariableInfos.ensureReCreatable(pFVInfos, pCCode.FrozenScope, true);
	}

	/** Returns the engine name */
	abstract public String getEngineName();

	/** Returns the engine for executing this script */
	public ScriptEngine getEngine() {
		return ScriptManager.Instance.getDefaultEngineOf(this.getEngineName());
	}

	String Code;

	/** Returns the code as text */
	public String getCode() {
		return this.Code;
	}

	private FrozenVariableInfos FVInfos = null;

	/** Returns the frozen variable information */
	public FrozenVariableInfos getFVInfos() {
		return this.FVInfos;
	}
	/** Returns the number if the frozen variable needed in this script */
	public int getFrozenVariableCount() {
		if(this.FVInfos == null) return 0;
		return this.FVInfos.getFrozenVariableCount();
	}
	/** Returns the name of the frozen variable at the index I */
	public String getFrozenVariableName(int I) {
		if(this.FVInfos == null) return null;
		return this.FVInfos.getFrozenVariableName(I);
	}
	/** Returns the type of the frozen variable at the index I */
	public Class<?> getFrozenVariableType(int I) {
		if(this.FVInfos == null) return null;
		return this.FVInfos.getFrozenVariableType(I);
	}

	transient JSR223CompiledCode CCode = null;

	/** Returns the compiled code */
	public CompiledCode getCompiledCode() {
		if((this.CCode == null) && this.getEngine().isCompilable() &&
				((this.FVInfos == null) || (this.FVInfos.getFrozenVariableCount() == 0)))
			this.CCode = (JSR223CompiledCode)this.getEngine().compile(this.getCode(), null, null, null, null);
		return this.CCode;
	}

	/** Run the script using the parameter */
	public Object run() {
		return this.run(null);
	}

	/** Run the script using the parameter */
	public Object run(Scope pScope) {
		if(pScope == null) pScope = this.getEngine().newScope();
		
		if(!(((JSR223Engine)this.getEngine()).getTheEngine() instanceof Compilable))
			return this.getEngine().eval(this.getCode(), pScope, null);
		
		return this.getEngine().eval(this.getCompiledCode(), pScope, null);
	}
	
	/** A simple JSR223 script */
	static public class Simple extends JSR223Script {

		private static final long serialVersionUID = -3490846881786044886L;

		protected Simple(String pEngineName, String pCode, FrozenVariableInfos pFVInfos, JSR223CompiledCode pCCode) {
			super(pCode, pFVInfos, pCCode);
			this.EngineName = pEngineName.toString();
		}
		
		String EngineName;

		/** Returns the engine name */
		@Override public String getEngineName() {
			return this.EngineName;
		}
		
		/** Recreate this script for the given frozen scope */
		public Script reCreate(Scope pNewFrozenScope) {
			FrozenVariableInfos TheFVInfos = this.getFVInfos();
			String[]            TheFVNames = (TheFVInfos == null)?null:TheFVInfos.getFrozenVariableNames();
			
			if(this.getCompiledCode() == null)
				return new Simple(this.getEngineName(), this.getCode(), TheFVInfos, null);
			
			JSR223CompiledCode NewCCode = new JSR223CompiledCode(
												this.getEngineName(),
												this.CCode.getEngineOptionString(),
												pNewFrozenScope,
												TheFVNames,
												this.CCode.CompiledScript);
			return new Simple(this.getEngineName(), this.getCode(), TheFVInfos, NewCCode);
		}
		
	}

}
