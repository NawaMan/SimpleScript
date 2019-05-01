package net.nawaman.script;

import java.io.Serializable;

/** Information of frozen variables */
public class FrozenVariableInfos implements Serializable, Cloneable {
	
	private static final long serialVersionUID = 4867951054495144871L;
	
	/** Empty frozen-variable info */
	static public final FrozenVariableInfos Empty = new FrozenVariableInfos();
	
	/** Creates a frozen-variable info from the array of the name and the scope */
	static public FrozenVariableInfos newFVInfos(String[] pFVNames, Scope pNewFrozenScope) {
		if((pFVNames == null) || (pFVNames.length == 0) || (pNewFrozenScope == null)) return FrozenVariableInfos.Empty;
		return new FrozenVariableInfos.Simple(pFVNames, pNewFrozenScope);
	}
	/** Checks if the frozen-variable info can be re-created base on the new frozen scope */
	static public boolean ensureReCreatable(FrozenVariableInfos pFVInfos, Scope pNewFrozenScope, boolean pIsToThrowError) {
		if((pFVInfos == null) || (pFVInfos.getFrozenVariableCount() == 0)) return true;
		for(int i = 0; i < pFVInfos.getFrozenVariableCount(); i++) {
			String   N = pFVInfos.getFrozenVariableName(i);
			Class<?> T = pFVInfos.getFrozenVariableType(i);
			
			Class<?> C = pNewFrozenScope.getTypeOf(N);
			if((T != null) && (C != null) && !T.isAssignableFrom(C)) {
				if(!pIsToThrowError) return false;
				
				throw new RuntimeException(String.format(
						"Incompatible frozen variable type '%s' need but '%s' found for '%s'. ",
						T.getName(), C.getName(), N
					));
			}
		}
		return true;
	}
	
	protected FrozenVariableInfos() {}
	
	// Services --------------------------------------------------------------------------------------------------------
	
	/** Returns all the names of variables */
	public String[] getFrozenVariableNames() {
		return null;
	}
	/** Returns the number if the frozen variable needed in this script */
	public int getFrozenVariableCount() {
		return 0;
	}
	/** Returns the name of the frozen variable at the index I */
	public String getFrozenVariableName(int I) {
		return null;
	}
	/** Returns the type of the frozen variable at the index I */
	public Class<?> getFrozenVariableType(int I) {
		return null;
	}
	
	/**{@inheritDoc}*/ @Override
	public FrozenVariableInfos clone() {
		return this;
	}
	
	
	// Simple implementation -------------------------------------------------------------------------------------------
	
	static public class Simple extends FrozenVariableInfos {

		private static final long serialVersionUID = -8019434846047776891L;

		protected Simple(String[] pFVNames, Scope pNewFrozenScope) {
			if(pFVNames == null) return;
			
			this.FVNames = pFVNames.clone();
			this.FVTypes = new Class<?>[this.FVNames.length];
			
			for(int i = 0; i < this.FVNames.length; i++) {
				Class<?> C = pNewFrozenScope.getTypeOf(this.FVNames[i]);
				if(C == null) C = Object.class;
				this.FVTypes[i] = C;
			}
		}
		
		String[]   FVNames;
		Class<?>[] FVTypes;
		
		/**{@inheritDoc}*/ @Override
		public String[] getFrozenVariableNames() {
			return (this.FVNames == null)?null:this.FVNames.clone();
		}
		/**{@inheritDoc}*/ @Override
		public int getFrozenVariableCount() {
			if(this.FVNames == null) return 0;
			return this.FVNames.length;
		}
		/**{@inheritDoc}*/ @Override
		public String getFrozenVariableName(int I) {
			if((I < 0) || (I >= this.getFrozenVariableCount())) return null;
			return this.FVNames[I];
		}
		/**{@inheritDoc}*/ @Override
		public Class<?> getFrozenVariableType(int I) {
			if((I < 0) || (I >= this.getFrozenVariableCount())) return null;
			return this.FVTypes[I];
		}
		
		/**{@inheritDoc}*/ @Override
		public FrozenVariableInfos clone() {
			FrozenVariableInfos FVInfos = new FrozenVariableInfos.Simple(null, null);
			this.FVNames = this.FVNames.clone();
			this.FVTypes = this.FVTypes.clone();
			return FVInfos;
		}
	}
}
