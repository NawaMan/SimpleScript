package net.nawaman.script.java;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.nawaman.javacompiler.JavaCompilerObjectInputStream;
import net.nawaman.javacompiler.JavaCompilerObjectOutputStream;
import net.nawaman.script.FrozenVariableInfos;
import net.nawaman.script.Macro;
import net.nawaman.script.Scope;
import net.nawaman.script.Signature;

/** A java macro */
public class JavaMacro  extends Macro.Simple implements Serializable {

	private static final long serialVersionUID = 4795728128653493141L;

	/** Constructs a java macro */
	public JavaMacro(String[] pParamNames, Body pBody, FrozenVariableInfos pFVInfos) {
		super(pParamNames, pBody, pFVInfos);
	}
	
	/**{@inheritDoc}*/ @Override
	public Macro reCreate(Scope pNewFrozenScope) {
		try {
			Constructor<? extends Macro.Simple.Body> CFB = ((Macro.Simple.Body)this.Body).getClass().
										getConstructor(new Class<?>[] { Signature.class, String.class, Scope.class });
			Macro.Simple.Body MSB = (Macro.Simple.Body)CFB.newInstance(this.getSignature(), this.getCode(), pNewFrozenScope);
			return new JavaMacro(this.ParamNames, MSB, this.getFVInfos());
		}
		catch(NoSuchMethodException     E) {}
		catch(InvocationTargetException E) {}
		catch(IllegalAccessException    E) {}
		catch(InstantiationException    E) {}
		return null;
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
			this.Body = ((Data == null) || (Data.length < 1)) ? null : (Body)Data[0];
		
		} else {
			// JCObjectInputStream
			this.Body = (Body)aStream.readObject();
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
			aStream.writeObject(JavaCompilerObjectOutputStream.SerializeObjects((Serializable)this.Body));
		
		} else {
			// JCObjectOutputStream
			aStream.writeObject(this.Body);
			return;
		}
	}
}
