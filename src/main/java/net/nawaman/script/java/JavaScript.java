package net.nawaman.script.java;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.nawaman.javacompiler.JavaCompilerObjectInputStream;
import net.nawaman.javacompiler.JavaCompilerObjectOutputStream;
import net.nawaman.script.CompiledCode;
import net.nawaman.script.FrozenVariableInfos;
import net.nawaman.script.Scope;
import net.nawaman.script.Script;
import net.nawaman.script.ScriptEngine;
import net.nawaman.script.ScriptManager;
import net.nawaman.script.java.JavaCompiledCode.JavaCode;

/** A script in java language */
public class JavaScript implements Script {
	
	private static final long serialVersionUID = -651862520789734184L;

	/** Constructs a java script */
	JavaScript(JavaEngine pEngine, String pCode, FrozenVariableInfos pFVInfos, JavaCompiledCode pCCode) {
		this(pEngine, pCode, pFVInfos);
		this.CCode = pCCode;
	}

	/** Constructs a java script */
	JavaScript(JavaEngine pEngine, String pCode, FrozenVariableInfos pFVInfos) {
		this.Engine  = pEngine;
		this.Code    = pCode;
		this.FVInfos = (pFVInfos == null)?null:pFVInfos.clone();
	}

	/** Returns the engine name */
	@Override public String getEngineName() {
		return JavaEngine.Name;
	}
	
	/** Returns the engine for executing this script */
	public ScriptEngine getEngine() {
		if(this.Engine == null) this.Engine = (JavaEngine)ScriptManager.Instance.getDefaultEngineOf(this.getEngineName());
		return this.Engine;
	}
	
	transient JavaEngine       Engine = null;
	transient JavaCompiledCode CCode  = null;
	
	String                      Code    = null;
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
	/** Recreate this script for the given frozen scope */
	public Script reCreate(Scope pNewFrozenScope) {
		JavaCode JC = null;
		if((this.CCode == null) || ((JC = this.CCode.getJavaCode()) == null))
			return new JavaScript(this.Engine, this.Code, this.FVInfos);

		try {
			Constructor<? extends JavaCode> CJC = JC.getClass().getConstructor(Scope.class);
			JavaCompiledCode JCC = new JavaCompiledCode((JavaCode)(CJC.newInstance(pNewFrozenScope)));
			return new JavaScript(this.Engine, this.Code, this.FVInfos, JCC);
		}
		catch(NoSuchMethodException     E) {}
		catch(InvocationTargetException E) {}
		catch(IllegalAccessException    E) {}
		catch(InstantiationException    E) {}
		return null;
	}
	
	/** Returns the code as text */
	public String getCode() {
		return this.Code;
	}
	
	/** Returns the compiled code */
	public CompiledCode getCompiledCode() {
		if((this.CCode == null) && ((this.FVInfos == null) || (this.FVInfos.getFrozenVariableCount() == 0)))
			this.CCode = (JavaCompiledCode)this.getEngine().compile(this.getCode(), null, null, null, null);
		return this.CCode;
	}
	
	/**{@inheritDoc}*/ @Override
	public Object run() {
		return this.run(null);
	}
	
	/**{@inheritDoc}*/ @Override
	public Object run(Scope pScope) {
		if(pScope == null) pScope = this.getEngine().newScope();
		return this.getEngine().eval(this.getCompiledCode(), pScope, null);
	}

	// Serializable ----------------------------------------------------------------------------------------------------
		
	/** Custom de-serialization is needed. */
	private void readObject(ObjectInputStream aStream) throws IOException, ClassNotFoundException {
		// Load the rest
		aStream.defaultReadObject();
		
		// Save the data specially
		if(!(aStream instanceof JavaCompilerObjectInputStream)) {
			// Regular OutputStream
			String Word = aStream.readUTF();
			if(!JavaCompilerObjectOutputStream.MAGIC_WORD.equals(Word))
				throw new IOException("Invalid JCO protocol Magic Word: " + Word);
			
			Object[] Data = JavaCompilerObjectOutputStream.DeSerializeObjects((byte[])aStream.readObject());
			this.CCode = ((Data == null) || (Data.length < 1)) ? null : (JavaCompiledCode)Data[0];
		
		} else {
			// JCObjectInputStream
			this.CCode = (JavaCompiledCode)aStream.readObject();
			return;
		}
	}

	/** Custom serialization is needed. */
	private void writeObject(ObjectOutputStream aStream) throws IOException {
		// Save the rest
		aStream.defaultWriteObject();
		
		// Save the data specially
		if(!(aStream instanceof JavaCompilerObjectOutputStream)) {
			// Regular OutputStream
			aStream.writeUTF   (JavaCompilerObjectOutputStream.MAGIC_WORD);
			aStream.writeObject(JavaCompilerObjectOutputStream.SerializeObjects(this.CCode));
		
		} else {
			// JCObjectOutputStream
			aStream.writeObject(this.CCode);
			return;
		}
	}

}
