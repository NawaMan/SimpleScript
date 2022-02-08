package net.nawaman.script;

import java.io.Serializable;

public interface Executable extends Serializable {
	
	/** Returns the engine name */
	public String getEngineName();
	
	/** Returns the engine for executing this executable */
	public ScriptEngine getEngine();
	
	/** Returns the frozen variable informations */
	public FrozenVariableInfos getFVInfos();
	
	/** Recreate this executable for the given frozen scope */
	public Executable reCreate(Scope pNewFrozenScope);

}
