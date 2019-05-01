package net.nawaman.script;

import java.io.Serializable;

/**
 * Option for ScriptEngine
 * 
 * This option is used to distinguish an Engine instances (of the same Engine class) from another.
 * 
 **/
public interface ScriptEngineOption extends Serializable {
	
	// No method needed
	
	
	// Simple implementation -------------------------------------------------------------------------------------------
	
	/** Simple implementation of ScriptEngineOption holding parameter string */
	static public class Simple implements ScriptEngineOption {
		
		private static final long serialVersionUID = -5777869983744938039L;
		
		protected String Parameter;
		
		/** Creates a Simple ScriptEngine Option from the Paramter String */
		public Simple(String pParameter) {
			this.Parameter = pParameter;
		}
		
		/** Returns the Engine Parameter as string */
		public String getParameter() {
			return this.Parameter;
		}
		
		/**{@inheritDoc}*/ @Override
		public String toString() {
			return this.Parameter;
		}
		
	}
	
}
