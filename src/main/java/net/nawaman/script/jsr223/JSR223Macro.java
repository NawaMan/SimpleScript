package net.nawaman.script.jsr223;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.nawaman.script.FrozenVariableInfos;
import net.nawaman.script.Macro;
import net.nawaman.script.Scope;
import net.nawaman.script.ScriptEngineOption;
import net.nawaman.script.jsr223.JSR223Engine.JSR223SFBody;
import net.nawaman.script.jsr223.JSR223Engine.JSR223SMBody;

/** Macro of a JSR223 script */
public class JSR223Macro extends Macro.Simple {
	
	private static final long serialVersionUID = -3028885037235142822L;

	/** Construct a function */
	public JSR223Macro(String[] pParamNames, Macro.Simple.Body pBody, FrozenVariableInfos pFVInfos) {
		super(pParamNames, pBody, pFVInfos);
	}
	
	/**{@inheritDoc}*/ @Override
	public Macro reCreate(Scope pNewFrozenScope) {
		JSR223SMBody       JSRB  = (JSR223SMBody)this.Body;
		JSR223Script       JSRS  = (JSR223Script)JSRB.Script;
		JSR223CompiledCode JSRCC = JSRS.CCode;
		
		ScriptEngineOption SEOption     = ((JSR223Engine)this.getEngine()).getOption();
		String             EngineOption = SEOption.toString();
		
		FrozenVariableInfos TheFVInfos = JSRS.getFVInfos();
		String[]            TheFVNames = (TheFVInfos == null)?null:TheFVInfos.getFrozenVariableNames();
		
		JSR223CompiledCode NewCC = new JSR223CompiledCode(
										this.getEngineName(),
										EngineOption,
										pNewFrozenScope,
										TheFVNames,
										JSRCC.CompiledScript);
		
		JSR223Script NewJSRS = new JSR223Script.Simple(this.getEngineName(), JSRS.getCode(), TheFVInfos, NewCC);
		JSR223SMBody NewJSRB = new JSR223SMBody(JSRB.getSignature(), NewJSRS);
		return new JSR223Macro(this.ParamNames, NewJSRB, TheFVInfos);
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
