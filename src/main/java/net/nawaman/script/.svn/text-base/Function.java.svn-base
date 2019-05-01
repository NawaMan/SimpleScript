package net.nawaman.script;

import java.io.Serializable;

/** An executable function */
public interface Function extends Executable {
	
	/** Returns the code as text */
	public String getCode();
	
	/** the interface of the function */
	public Signature getSignature();
	
	/** Execute the function */
	public Object run(Object ... pParams);
	
	/** Simple implementation of Function */
	static abstract public class Simple implements Function, Serializable {
		
		private static final long serialVersionUID = 580000574214561598L;

		/** Construct a function */
		public Simple(String[] pParamNames, Body pBody, FrozenVariableInfos pFVInfos) {
			if(pBody       == null) throw new NullPointerException("Function body cannot be null.");
			if(pParamNames == null) throw new NullPointerException("Function parameter names cannot be null (try using Empty String Array).");
			if(pBody.getSignature().getParamCount() != pParamNames.length)
				throw new IllegalArgumentException("The function parameter names and the body interface are not compatible.");
			
			for(int i = pParamNames.length; --i >= 0; ) {
				if(pParamNames[i] == null)
					throw new NullPointerException("Function parameter names cannot be null (Param# "+i+").");
			}
			
			this.ParamNames = pParamNames.clone();
			this.Body       = pBody;
			this.FVInfos    = (pFVInfos == null)?null:pFVInfos.clone();
		}
		
		/** Parameter name of the function */
		protected String[] ParamNames = null;
		
		/** The body of the function */
		transient protected Body Body = null;
	
		/** the interface of the function */
		final public Signature getSignature() {
			return this.Body.getSignature();
		}
		
		/** the interface of the function */
		final public Body getBody() {
			return this.Body;
		}
		/** the interface of the function */
		final public Function.Simple.Body getFunctionBody() {
			return this.Body;
		}
		
		/** Returns the name of the parameter */
		public String getParameterName(int I) {
			if((I < 0) || (I >= this.ParamNames.length)) return null;
			return this.ParamNames[I];
		}
		
		/** Returns the engine name */
		public String getEngineName() {
			return this.Body.getEngineName();
		}
		/** Returns the engine for executing this script */
		public ScriptEngine getEngine() {
			return this.Body.getEngine();
		}
		
		/** Returns the code as text */
		public String getCode() {
			return this.Body.getCode();
		}
		
		private FrozenVariableInfos FVInfos = null;
		
		/**{@inheritDoc}*/ @Override
		public FrozenVariableInfos getFVInfos() {
			if(this.FVInfos == null) return FrozenVariableInfos.Empty;
			return this.FVInfos;
		}
		
		/** Recreate this function for the given frozen scope */
		abstract public Function reCreate(Scope pNewFrozenScope);
		
		/** Execute the function - The parameter is VarArgs */
		public Object run(Object ... pParams) {
			Body     TheBody = this.getFunctionBody();
			Object[] Params  = Signature.Simple.adjustParameters(this.getSignature(), pParams);
			Object   Result  = TheBody.run(this, Params);
			return Signature.Simple.ensureReturnCompatible(this.getSignature(), Result);
		}
		
		// Utilities methods -------------------------------------------------------------------------------------------
		
		/** The body of the simple function */
		static public interface Body {
			
			/** Returns the engine name */
			public String getEngineName();
			
			/** Returns the engine for executing this script */
			public ScriptEngine getEngine();
			
			/** Returns the code as text */
			public String getCode();

			/** the interface of the function */
			public Signature getSignature();
			
			/** Execute the function body - The parameter must be a direct map (VarArgs is not applied) */
			public Object run(Function.Simple pFunction, Object[] pParams);

		}
	}
}
