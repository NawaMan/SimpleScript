package net.nawaman.script;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** The scope for the script execution */
public interface Scope {

	/** Returns a variable and constant names */
	public Set<String> getVariableNames();
	
	/** Returns the variable count */
	public int getVarCount();
	
	/** Returns the variable value */
	public Object getValue(String pName);
	
	/** Change the variable value and return if success */
	public Object setValue(String pName, Object pValue);
	
	/** Create a new variable and return if success */
	public boolean newVariable(String pName, Class<?> pType, Object pValue);
	
	/** Create a new constant and return if success */
	public boolean newConstant(String pName, Class<?> pType, Object pValue);
	
	/** Removes a variable or a constant and return if success */
	public boolean removeVariable(String pName);
	
	/** Returns the variable value */
	public Class<?> getTypeOf(String pName);
	
	/** Checks if the variable of the given name is writable */
	public boolean isExist(String pName); 
	
	/** Checks if the variable of the given name is writable */
	public boolean isWritable(String pName);
	
	/** Checks if this scope support constant declaration */
	public boolean isConstantSupport();

    /** Returns the <code>Writer</code> for scripts to use when displaying output. */
    public Writer getWriter();
    
    /** Returns the <code>Writer</code> used to display error output. */
    public Writer getErrorWriter();
    
    /** Sets the <code>Writer</code> for scripts to use when displaying output. */
    public void setWriter(Writer writer);
    
    /** Sets the <code>Writer</code> used to display error output. */
    public void setErrorWriter(Writer writer);
    
    /** Returns a <code>Reader</code> to be used by the script to read input. */
    public Reader getReader();
    
    /** Sets the <code>Reader</code> for scripts to read input */
    public void setReader(Reader reader);
	
	// Sub classes ------------------------------------------------------------
	
    /** An empty scope that does not allow new variable and use default writers/reader */
    static public class Empty implements Scope {
		
    	/** An only instance of the empty scope */
    	static public final Empty Instance = new Empty();
		
    	/** Empty names */
		static private Set<String> EmptyNames = null;
		
		/** Returns a variable and constant names */
		static public Set<String> getEmptyNames() {
			if(EmptyNames == null) {
				EmptyNames = new AbstractSet<String>() {
					@Override public Iterator<String> iterator() {
					    return new Iterator<String>() {
								public boolean hasNext() { return false; }
								public String  next()    { return  null; }
								public void    remove()  {}
					    };
					}
					@Override public int     size() { return 0; }
					@Override public boolean contains(Object k) { return false; }
				};
			}
			return EmptyNames;
		}
    	
		Empty() {}
		
		/** Returns a variable and constant names */
		public Set<String> getVariableNames() { return getEmptyNames(); }
		
		/** Returns the variable count */
		public int getVarCount() { return 0; }
		
		/** Returns the variable value */
		public Object getValue(String pName) { return null; }
		
		/** Change the variable value and return if success */
		public Object setValue(String pName, Object pValue) { return null; }
		
		/** Create a new variable and return if success */
		public boolean newVariable(String pName, Class<?> pType, Object pValue) { return false; }
		
		/** Create a new constant and return if success */
		public boolean newConstant(String pName, Class<?> pType, Object pValue) { return false; }
		
		/** Removes a variable or a constant and return if success */
		public boolean removeVariable(String pName) { return false; }
		
		/** Returns the variable value */
		public Class<?> getTypeOf(String pName) { return null; }
		
		/** Checks if the variable of the given name is writable */
		public boolean isExist(String pName)  { return false; }
		
		/** Checks if the variable of the given name is writable */
		public boolean isWritable(String pName)  { return false; } 
		
		/** Checks if this scope support constant declaration */
		public boolean isConstantSupport() { return false; }
		
		static Writer DOut = new OutputStreamWriter(System.out);
		static Writer DErr = new OutputStreamWriter(System.err);
		static Reader DIn  = new InputStreamReader( System.in);

	    /** Returns the <code>Writer</code> for scripts to use when displaying output. */
	    public Writer getWriter() { return DOut; }
	    
	    /** Returns the <code>Writer</code> used to display error output. */
	    public Writer getErrorWriter() { return DErr; }
	    
	    /** Sets the <code>Writer</code> for scripts to use when displaying output. */
	    public void setWriter(Writer pWriter) {}
	    
	    /** Sets the <code>Writer</code> used to display error output. */
	    public void setErrorWriter(Writer pWriter) {}
	    
	    /** Returns a <code>Reader</code> to be used by the script to read input. */
	    public Reader getReader() { return DIn; }
	    
	    /** Sets the <code>Reader</code> for scripts to read input */
	    public void setReader(Reader pReader) { }
	}
    
    /** A simple implementation of a scope */
	static public class Simple implements Scope {
		
		public Simple() {}
		
		public Simple(String VName, Class<?> Type, Object VValue, boolean pIsConstant) {
			this();
			if(!pIsConstant) this.newVariable(VName, Type, VValue);
			else             this.newConstant(VName, Type, VValue);
		}
		
		HashSet<String>           Constants = null;
		HashMap<String, Class<?>> VTypes    = null;
		HashMap<String, Object>   VValues   = null;
		
		/** Returns a variable and constant names */
		public Set<String> getVariableNames() {
			if(this.VTypes != null) return this.VTypes.keySet();
			return Empty.getEmptyNames();
		}
		
		// Services as Scope -------------------------------------------------------------------------------------------
		
		/** Returns the variable count */
		public int getVarCount() {
			return (this.VTypes == null)?0:this.VTypes.size();
		}
		
		/** Returns the variable value */
		public Object getValue(String pName) {
			return (this.VTypes == null)?null:this.VValues.get(pName);
		}
		
		/** Change the variable value and return if success */
		public Object setValue(String pName, Object pValue) {
			if(this.VTypes == null)
				throw new RuntimeException("Variable `"+pName+"` does not exist.");
			if((this.Constants != null) && this.Constants.contains(pName))
				throw new RuntimeException("Variable `"+pName+"` is a constant.");
			
			Class<?> C = this.VTypes.get(pName);
			if(C == null)
				throw new RuntimeException("Variable `"+pName+"` is a void.");
			if((pValue != null) && !C.isInstance(pValue))
				throw new RuntimeException("Invalid assign value '"+pValue+"' for the variable `"+pName+"`:"+C.toString()+".");
			
			this.VValues.put(pName, pValue);
			return true;
		}
		
		/** Create a new variable and return if success */
		public boolean newVariable(String pName, Class<?> pType, Object pValue) {
			if((pType != null) && (pValue != null) && !pType.isInstance(pValue)) {
				if(pType.isPrimitive()) throw new RuntimeException("Primitive type cannot be used in scope.");
				return false;
			}
			
			if(pType == null) pType = Object.class;
			if((this.VTypes != null) && this.VTypes.containsKey(pName)) return false;
			if(this.VTypes == null) {
				this.VTypes  = new HashMap<String, Class<?>>();
				this.VValues = new HashMap<String, Object>();
			}
			this.VTypes.put( pName, pType);
			this.VValues.put(pName, pValue);
			return true;
		}
		
		/** Create a new constant and return if success */
		public boolean newConstant(String pName, Class<?> pType, Object pValue) {
			if(this.newVariable(pName, pType, pValue)) {
				if(this.Constants == null) this.Constants = new HashSet<String>();
				this.Constants.add(pName);
				return true;
			}
			return false;
		}
		
		/** Removes a variable or a constant and return if success */
		public boolean removeVariable(String pName) {
			if(this.VTypes == null)             return false;
			if(!this.VTypes.containsKey(pName)) return false;
			this.VTypes.remove(pName);
			this.VValues.remove(pName);
			return true;
		}
		
		/** Returns the variable value */
		public Class<?> getTypeOf(String pName) {
			return (this.VTypes == null)?null:this.VTypes.get(pName);
		}
		
		/** Checks if the variable of the given name is writable */
		public boolean isExist(String pName) {
			return (this.VTypes == null)?false:this.VTypes.containsKey(pName);
		}
		
		/** Checks if the variable of the given name is writable */
		public boolean isWritable(String pName) {
			return (this.Constants == null)?(this.getTypeOf(pName) != null):!this.Constants.contains(pName);
		}
		
		/** Checks if this scope support constant declaration */
		public boolean isConstantSupport() { return true; }
		
		static public final Writer DOut = new OutputStreamWriter(System.out);
		static public final Writer DErr = new OutputStreamWriter(System.err);
		static public final Reader DIn  = new InputStreamReader( System.in);
		
		Writer Out = new OutputStreamWriter(System.out);
		Writer Err = new OutputStreamWriter(System.err);
		Reader In  = new InputStreamReader( System.in);

	    /** Returns the <code>Writer</code> for scripts to use when displaying output. */
	    public Writer getWriter() {
	    	return this.Out;
	    }
	    
	    /** Returns the <code>Writer</code> used to display error output. */
	    public Writer getErrorWriter() {
	    	return this.Err;
	    }
	    
	    /** Sets the <code>Writer</code> for scripts to use when displaying output. */
	    public void setWriter(Writer pWriter) {
	    	this.Out = (pWriter != null)?pWriter:DOut;
	    }
	    
	    /** Sets the <code>Writer</code> used to display error output. */
	    public void setErrorWriter(Writer pWriter) {
	    	this.Err = (pWriter != null)?pWriter:DErr;
	    }
	    
	    /** Returns a <code>Reader</code> to be used by the script to read input. */
	    public Reader getReader() {
	    	return this.In;
	    }
	    
	    /** Sets the <code>Reader</code> for scripts to read input */
	    public void setReader(Reader pReader) {
	    	this.In = (pReader != null)?pReader:DIn;
	    }
	    
	    /** Creates duplicate scope of another scope */ 
	    static public Scope.Simple getDuplicateOf(Scope S) {
	    	if(S == null) return null;
	    	return getDuplicateOf(S.getVariableNames(), S);
	    }
	    /** Creates duplicate scope of another scope */ 
	    static public Scope.Simple getDuplicateOf(Set<String> pNamesToCopy, Scope S) {
	    	if(S == null) return null;
	    	Scope.Simple New = new Scope.Simple();
	    	duplicate(pNamesToCopy, S, New);
			return New;
	    }
	    /** Creates duplicate scope of another scope */ 
	    static public Scope getDuplicateOf(ScriptEngine E, Scope S) {
	    	if(S == null) return null;
	    	return getDuplicateOf(E, S.getVariableNames(), S);
	    	
	    }
	    /** Creates duplicate scope of another scope */ 
	    static public Scope getDuplicateOf(ScriptEngine E, Set<String> pNamesToCopy, Scope S) {
	    	if(S == null) return null;
	    	Scope New = E.newScope();
	    	duplicate(pNamesToCopy, S, New);
			return New;
	    }
	    /** Duplicate all variable of a source scope to the target scope */
	    static public void duplicate(Scope Source, Scope Target) {
	    	if(Source == null) return;
	    	duplicate(Source.getVariableNames(), Source, Target);
	    }
	    /**
	     * Creates duplicate scope of another scope
	     * If the target already have a variable that can be written or the variable type is not compatible, the variable
	     *     will be ignored.
	     **/ 
	    static public void duplicate(Set<String> pNamesToCopy, Scope Source, Scope Target) {
	    	if((Source == null) || (pNamesToCopy == null)) return;
	    	
			for(String Name : pNamesToCopy) {
				if(!Source.isExist(Name)) continue;
				
				if(!Target.isExist(Name)) {
					if(Source.isWritable(Name) || !Target.isConstantSupport())
						 Target.newVariable (Name, Source.getTypeOf(Name), Source.getValue(Name));
					else Target.newConstant(Name, Source.getTypeOf(Name), Source.getValue(Name));
				} else {
					if(!Target.isWritable(Name)) continue;
					Class<?> T = Target.getTypeOf(Name);
					if(T == null) continue;
					Object O = Source.getValue(Name);
					if((T == Object.class) || T.isInstance(O)) Target.setValue(Name, O);
				}
			}
	    }
	    
	    /** Returns the default value of the given type */
	    static public Object getDefauleValueOf(Class<?> pClass) {
	    	if(pClass == null) return null;
			if(Number.class.isAssignableFrom(pClass)) {
				if((pClass == Integer.class) || (pClass == int   .class)) return 0;
				if((pClass == Double .class) || (pClass == double.class)) return 0.0;
				if((pClass == Long   .class) || (pClass == long  .class)) return 0L;
				if((pClass == Byte   .class) || (pClass == byte  .class)) return (byte)0;
				if((pClass == Float  .class) || (pClass == float .class)) return 0.0f;
				if((pClass == Short  .class) || (pClass == short .class)) return (short)0;
			}
			if(Character.class.isAssignableFrom(pClass)) return '0';
			if(Boolean  .class.isAssignableFrom(pClass)) return false;
			return null;
	    }
	}

}
