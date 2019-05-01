package net.nawaman.script.jsr223;

import java.util.HashSet;
import java.util.Arrays;

import javax.script.CompiledScript;

import net.nawaman.script.CompiledCode;
import net.nawaman.script.Scope;

/** Compiled JSR223 code */
public class JSR223CompiledCode implements CompiledCode {
	
	/** Constructs a compiled JSR223 code */
	protected JSR223CompiledCode(String pEngineName, String pEngineOption, Scope pFrozenScope, String[] pFVNames,
			CompiledScript pCompiledScript) {
		
		this.CompiledScript = pCompiledScript;
		if(pEngineName   == null)          throw new NullPointerException();
		if(pEngineName.indexOf(':') != -1) throw new IllegalArgumentException();
		pEngineOption = (pEngineOption != null)?(":" + pEngineOption):"";
		this.EngineName = pEngineName + pEngineOption;
		
		if((pFrozenScope != null) && (pFVNames != null)) {
			this.FrozenScope = Scope.Simple.getDuplicateOf(new HashSet<String>(Arrays.asList(pFVNames)), pFrozenScope);
			for(String FVName : pFVNames) {
				if(FVName == null)                   continue;
				if(this.FrozenScope.isExist(FVName)) continue;
				this.FrozenScope.newConstant(FVName, Object.class, null);
			}
		}
	}
	
	String         EngineName;
	CompiledScript CompiledScript;
	Scope          FrozenScope;
	
	/** Returns the actual compiled code */
	protected CompiledScript getCompiledScript() {
		return this.CompiledScript;
	}
	
	/** Returns the name of the engine used to execute this code */
	public String getEngineName() {
		return this.EngineName.substring(0, this.EngineName.indexOf(':'));
	}
	
	/** Returns the string parameter used to create engine for executing this code */
	public String getEngineOptionString() {
		if(this.EngineName.indexOf(':') == -1) return null;
		return this.EngineName.substring(this.EngineName.indexOf(':') + 1, this.EngineName.length());
	}

}
