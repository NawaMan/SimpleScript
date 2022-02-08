package net.nawaman.script.jsr223;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.nawaman.script.*;
import net.nawaman.script.jsr223.JSR223Engine.JSR223SFBody;

/** Function of a JSR223 script */
public class JSR223Function extends Function.Simple {
	
	private static final long serialVersionUID = -6981551841320394839L;

	/** Construct a function */
	public JSR223Function(String[] pParamNames, Body pBody, FrozenVariableInfos pFVInfos) {
		super(pParamNames, pBody, pFVInfos);
	}
	
	/**{@inheritDoc}*/ @Override
	public Function reCreate(Scope pNewFrozenScope) {
		JSR223SFBody       JSRB  = (JSR223SFBody)this.Body;
		JSR223Script       JSRS  = (JSR223Script)JSRB.Script;
		JSR223CompiledCode JSRCC = JSRS.CCode;

		ScriptEngineOption SEOption     = ((JSR223Engine)this.getEngine()).getOption();
		String             EngineOption = (SEOption == null)?null:SEOption.toString();
		
		FrozenVariableInfos TheFVInfos = JSRS.getFVInfos();
		String[]            TheFVNames = (TheFVInfos == null)?null:TheFVInfos.getFrozenVariableNames();
		
		JSR223CompiledCode NewCC = new JSR223CompiledCode(this.getEngineName(), EngineOption, pNewFrozenScope,
										TheFVNames, JSRCC.CompiledScript);
		
		JSR223Script NewJSRS = new JSR223Script.Simple(this.getEngineName(), JSRS.getCode(), TheFVInfos, NewCC);
		JSR223SFBody NewJSRB = new JSR223SFBody(JSRB.getSignature(), NewJSRS);
		return new JSR223Function(this.ParamNames, NewJSRB, TheFVInfos);
	}

	// Serializable ----------------------------------------------------------------------------------------------------
		
	/** Custom de-serialization is needed. */
	private void readObject(ObjectInputStream aStream) throws IOException, ClassNotFoundException {
		// Load the rest
		aStream.defaultReadObject();

		this.Body = (JSR223SFBody)aStream.readObject();
	}

	/** Custom serialization is needed. */
	private void writeObject(ObjectOutputStream aStream) throws IOException {
		// Save the rest
		aStream.defaultWriteObject();
		
		try {
			aStream.writeObject(this.Body);
		} catch(Exception E) {}
	}
}
