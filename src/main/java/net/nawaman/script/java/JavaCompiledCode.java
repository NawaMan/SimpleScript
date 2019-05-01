package net.nawaman.script.java;

import java.io.Serializable;

import net.nawaman.script.CompiledCode;
import net.nawaman.script.Scope;

/** The compiled code of a java code */
public class JavaCompiledCode implements CompiledCode, Serializable {
	
	private static final long serialVersionUID = -3921168108496594483L;

	protected JavaCompiledCode(JavaCode pJavaCode) {
		this.JavaCode = pJavaCode;
	}

	JavaCode JavaCode = null;
	
	/** Returns the JavaCode object use in the execution */
	public JavaCode getJavaCode() {
		return this.JavaCode;
	}
	
	/** The interface that every the code is compiled to be */
	static public interface JavaCode {
		
		public Object run(Object $This, Scope $Scope);
		
	}
	
	/** Returns the name of the engine used to execute this code */
	public String getEngineName() {
		return JavaEngine.Name;
	}
	
	/** Returns the string parameter used to create engine for executing this code */
	public String getEngineOptionString() {
		return null;
	}
}
