package net.nawaman.script;

import java.io.Serializable;

/** Macro */
public interface Macro extends Function {
	
	/** Returns the code as text */
	public String getCode();
	
	/** the interface of the macro */
	public Signature getSignature();
	
	/** Execute the macro */
	public Object run(Scope pScope, Object ... pParams);

	/** Simple implementation of Function */
	static abstract public class Simple extends Function.Simple implements Macro, Serializable {
		
		private static final long serialVersionUID = -906918214935419040L;

		/** Construct a function */
		public Simple(String[] pParamNames, Macro.Simple.Body pBody, FrozenVariableInfos pFVInfos) {
			super(pParamNames, pBody, pFVInfos);
		}
	
		/** the interface of the function */
		final public Macro.Simple.Body getMacroBody() {
			return (Macro.Simple.Body)this.Body;
		}
		
		/** Recreate this function for the given frozen scope */
		@Override abstract public Function reCreate(Scope pNewFrozenScope);
		
		/** Execute the macro - The parameter is VarArgs */
		public Object run(Scope pScope, Object ... pParams) {
			if(pScope == null) pScope = this.getEngine().newScope();
			
			Body     TheBody = this.getMacroBody();
			Object[] Params  = Signature.Simple.adjustParameters(this.getSignature(), pParams);
			Object   Result  = TheBody.run(this, pScope, Params);
			
			return Signature.Simple.ensureReturnCompatible(this.getSignature(), Result);
		}
		
		// Utilities methods -------------------------------------------------------------------------------------------
		
		/** The body of the simple macro */
		static public interface Body extends Function.Simple.Body {
			
			/** Returns the engine name */
			public String getEngineName();
			
			/** Returns the engine for executing this script */
			public ScriptEngine getEngine();
			
			/** Returns the code as text */
			public String getCode();

			/** the interface of the macro */
			public Signature getSignature();
			
			/** Execute the macro body - The parameter must be a direct map (VarArgs is not applied) */
			public Object run(Macro.Simple pMacro, Scope pScope, Object[] pParams);

		}
	}
}
