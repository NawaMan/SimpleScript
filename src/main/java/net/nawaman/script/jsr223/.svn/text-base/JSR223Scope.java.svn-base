package net.nawaman.script.jsr223;

import java.io.Reader;
import java.io.Writer;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import net.nawaman.script.Scope;
import net.nawaman.script.ScriptManager;

/** Scope used for the execution of JSR223 script */
public class JSR223Scope  implements Scope {
	
	static final int ScopeNumber = ScriptContext.ENGINE_SCOPE;
	
	/** Constructs a JSR223 scope */
	protected JSR223Scope() {
		this.Context = new SimpleScriptContext();
		this.Scope   = Context.getBindings(ScopeNumber);
	}
	
	ScriptContext Context;
    Bindings      Scope;

	/** Returns a variable and constant names */
	public Set<String> getVariableNames() {
		return this.Scope.keySet();
	}
	
	/** Returns the variable count */
	public int getVarCount() {
		return this.Scope.size();
	}
	
	/** Returns the variable value */
	public Object getValue(String pName) {
		return this.Context.getAttribute(pName, ScopeNumber);
	}
	
	/** Change the variable value and return if success */
	public Object setValue(String pName, Object pValue) {
		this.Context.setAttribute(pName, pValue, ScopeNumber);
		return pValue;
	}
	
	/** Create a new variable and return if success */
	public boolean newVariable(String pName, Class<?> pType, Object pValue) {
		this.Context.setAttribute(pName, pValue, ScopeNumber);
		return true;
	}
	
	/** Create a new constant and return if success */
	public boolean newConstant(String pName, Class<?> pType, Object pValue) {
		// Constant does not support
		throw new RuntimeException("Constant declaration is not support in JavaScript Scope.");
	}
	
	/** Removes a variable or a constant and return if success */
	public boolean removeVariable(String pName) {
		this.Context.removeAttribute(pName, ScopeNumber);
		return true;
	}
	
	/** Returns the variable value */
	public Class<?> getTypeOf(String pName) {
		return Object.class;
	}
	
	/** Checks if the variable of the given name is writable */
	public boolean isExist(String pName)  {
		return (Boolean)ScriptManager.Instance.getDefaultEngineOf(JSEngine.Name).eval(
				String.format("(typeof(%s)!=\"undefined\")", pName), this, null);
	}
	
	/** Checks if the variable of the given name is writable */
	public boolean isWritable(String pName) {
		return true;
	} 
	
	/** Checks if this scope support constant declaration */
	public boolean isConstantSupport() {
		return false;
	}

    /** Returns the <code>Writer</code> for scripts to use when displaying output. */
    public Writer getWriter() {
    	return this.Context.getWriter();
    }
    
    /** Returns the <code>Writer</code> used to display error output. */
    public Writer getErrorWriter() {
    	return this.Context.getErrorWriter();
    }
    
    /** Sets the <code>Writer</code> for scripts to use when displaying output. */
    public void setWriter(Writer writer) {
    	this.Context.setWriter(writer);
    }
    
    /** Sets the <code>Writer</code> used to display error output. */
    public void setErrorWriter(Writer writer) {
    	this.Context.setErrorWriter(writer);
    }
    
    /** Returns a <code>Reader</code> to be used by the script to read input. */
    public Reader getReader() {
    	return this.Context.getReader();
    }
    
    /** Sets the <code>Reader</code> for scripts to read input */
    public void setReader(Reader reader) {
    	this.Context.setReader(reader);
    }
}
