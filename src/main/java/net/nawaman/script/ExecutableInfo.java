package net.nawaman.script;

/** Informations about Executable */
public class ExecutableInfo {

	static public final ExecutableInfo DefaultExecutableInfo = new ExecutableInfo(null, "main", null, null);
	
	static public final Signature MAIN_SIGNATURE = new Signature.Simple("main", Void.class, true, String.class);

	// Main ExecutableInfo
	static public final ExecutableInfo MainExecutableInfo =
		new ExecutableInfo(
			null,
			"function",
			MAIN_SIGNATURE,
			"($Args:String ...):Void ",
			new String[] { "$Args" });
	
	static public ExecutableInfo getDefaultExecutableInfo(String Name) {
		if((Name == null) || (Name.length() == 0)) return DefaultExecutableInfo;
		return new ExecutableInfo(null, Name, null, null);
	}
	static public ExecutableInfo getMainExecutableInfo(String Name) {
		if((Name == null) || (Name.length() == 0)) return MainExecutableInfo;
		return new ExecutableInfo("function",
				null,
				new Signature.Simple(Name, Void.class, true, String.class),
				Name + "($Args:String ...):Void ",
				new String[] { "$Args" }
		);
	}
	
	/** Constructs an ExecutableInfo */
	public ExecutableInfo(String pFileName, String pKind, Signature pSignature, String pSignatureText, String[] pParamNames) {
		this(pFileName, pKind, pSignature, pSignatureText);
		this.setParameterNames(pParamNames);
	}
	/** Constructs an ExecutableInfo */
	public ExecutableInfo(String pFileName, String pKind, Signature pSignature, String pSignatureText) {
		this.FileName      = pFileName;
		this.Kind          = pKind;
		this.Signature     = pSignature;
		this.SignatureText = pSignatureText;
	}
	public final String    FileName;
	public final String    Kind;
	public final Signature Signature;
	public final String    SignatureText;
	
	private String[] ParamNames = null;
	
	/**
	 * Try to set the parameter names. If the previous one is null, the new one will replace and true is return.
	 * Otherwise, false will be returned and nothing is changed.
	 **/
	public boolean setParameterNames(String[] pParamNames) {
		if(this.ParamNames != null) return false;
		if((pParamNames == null) && ((this.Signature == null) || (this.Signature.getParamCount() == 0)))
			return true;
				
		if(pParamNames.length != this.Signature.getParamCount())
			throw new IllegalArgumentException();

		this.ParamNames = pParamNames.clone();
		return true;
	}
	
	/** Returns the parameter name */
	public String[] getParameterNames() {
		return (this.ParamNames == null) ? null : this.ParamNames.clone();
	}

	/** Returns the parameter name */
	public String getParameterName(int I) {
		return ((I < 0) || (this.ParamNames == null) || (I >= this.ParamNames.length))
		          ? null
		          : this.ParamNames[I];
	}
	
	/** Returns the string represetation of this Signature */ @Override
	public String toString() {
		StringBuilder SB = new StringBuilder();
		
		// The Kind
		if((this.Kind != null) && (this.Kind.length() != 0))
			SB.append(this.Kind).append(" ");
		
		// The signature
		if(this.Signature != null) {
			if(this.ParamNames == null)
				this.ParamNames = new String[this.Signature.getParamCount()];
			
			SB.append("(");
			for(int i = 0; i < this.Signature.getParamCount(); i++) {
				if(i != 0) SB.append(", ");
				String PName = this.getParameterName(i);
				if(PName == null) {
					PName = "Param" + i;
					this.ParamNames[i] = PName;
				} 
				
				SB.append(this.ParamNames[i]);
				SB.append(":");
				SB.append(Utils.getClassSimpleName(this.Signature.getParamType(i)));
			}
			if(this.Signature.isVarArgs()) SB.append(" ...");
			SB.append("):");
			SB.append(Utils.getClassSimpleName(this.Signature.getReturnType()));
			SB.append(" ");
		}
		
		return (SB.length() == 0) ? "" : "{ " + SB.append("}").toString();
	}
}