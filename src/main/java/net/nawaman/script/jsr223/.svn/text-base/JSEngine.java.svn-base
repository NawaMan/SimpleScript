package net.nawaman.script.jsr223;

import java.util.regex.Pattern;

import net.nawaman.script.CompileOption;
import net.nawaman.script.FrozenVariableInfos;
import net.nawaman.script.Function;
import net.nawaman.script.Macro;
import net.nawaman.script.ProblemContainer;
import net.nawaman.script.Scope;
import net.nawaman.script.Signature;

/** JavaScript Engine */
public class JSEngine extends JSR223Engine {

	/** The only instance of JSEngine */
	static JSEngine Instance = null;
	
	/** Get the only instance of JSEngine */
	static public JSEngine newInstance() {
		if(Instance == null) Instance = new JSEngine();
		return Instance;
	}
	
	private JSEngine() {}
	
	/** Cache of the JSR223 script engine */
	static final javax.script.ScriptEngine TheEngine = TheFactory.getEngineByName("JavaScript");
	
	/** The name of this class for easy access by the client */
	static public final String Name      = JSEngine.class.getCanonicalName();
	/** The name of this class for easy access by the client */
	static public final String ShortName = "JavaScript";
	
	/**{@inheritDoc}*/ @Override public    String getName()                         { return Name;      }
	/**{@inheritDoc}*/ @Override public    String getShortName()                    { return ShortName; }
	/**{@inheritDoc}*/ @Override protected javax.script.ScriptEngine getTheEngine() { return TheEngine; }
	
	static final private String  ParameterNamePattern = "[a-zA-Z$_][a-zA-Z0-9$_]*";
	static final private Pattern ParameterNameRegEx   = Pattern.compile(ParameterNamePattern);
	
	/** Verify the list of parameter name (that is valid in JavaScript) and return the string of the java script list */
	private String getParameterList(String[] pParamNames) {
		StringBuilder SB = new StringBuilder();
		if(pParamNames != null) {
			for(int i = 0; i < pParamNames.length; i++) {
				String PName = pParamNames[i];
				if((PName == null) || (PName.length() == 0) || !ParameterNameRegEx.matcher(PName = PName.trim()).find())
					throw new IllegalArgumentException("Invalid paramter name '"+PName+"'");
				
				if(SB.length() != 0) SB.append(",");
				SB.append(PName);
			}
		}
		return SB.toString();
	}
	
	/**{@inheritDoc}*/ @Override
	public Macro newMacro(Signature pSignature, String[] pParamNames, String pCode, Scope pFrozen, String[] pFrozenVNames,
			CompileOption pOption, ProblemContainer pResult) {
		String Parameters = getParameterList(pParamNames);
		String NewCode    = String.format(
		                        "function $Macro(%1$s) {%2$s}; function $Self(%1$s) { return $Macro(%1$s) } $Macro(%1$s);",
		                        Parameters, pCode
		                    );
		return new JSR223Macro(
				pParamNames,
				new JSR223SMBody(pSignature, this.newScript(NewCode, pFrozen, pFrozenVNames, pOption, pResult)),
				FrozenVariableInfos.newFVInfos(pFrozenVNames, pFrozen));
	}
	
	/**{@inheritDoc}*/ @Override
	public Function newFunction(Signature pSignature, String[] pParamNames, String pCode, Scope pFrozen, String[] pFrozenVNames,
			CompileOption pOption, ProblemContainer pResult) {
		String Parameters = getParameterList(pParamNames);
		String NewCode    = String.format(
                                "function $Function(%1$s) {%2$s}; function $Self(%1$s) { return $Function(%1$s) } $Function(%1$s);",
                                Parameters, pCode
                            );
		return new JSR223Function(
					pParamNames,
					new JSR223SFBody(pSignature, this.newScript(NewCode, pFrozen, pFrozenVNames, pOption, pResult)),
					FrozenVariableInfos.newFVInfos(pFrozenVNames, pFrozen));
	}

	/** Returns the long comments of the given comment text */
	public String getLongComments(String Comment, int Width) {
		int WidthMinusOne = Width - 1;
		StringBuilder SB = new StringBuilder();
		while(SB.length() <  WidthMinusOne) SB.append("*");
		return String.format("/%1$s\n%2$s\n%1$s/", SB.toString(), Comment);
	}
}
