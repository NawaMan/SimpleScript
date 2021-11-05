package net.nawaman.script.java;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

import net.nawaman.javacompiler.*;
import net.nawaman.script.*;
import net.nawaman.script.java.JavaCompiledCode.JavaCode;
import net.nawaman.script.java.JavaRoughParser.Statement;
import net.nawaman.usepath.FileExtFilter;
import net.nawaman.usepath.UFFileExtFilter;
import net.nawaman.usepath.UsableFilter;

/**
 * The engine for executing Java code.
 * 
 * CLASSPATH NOTE: To have this engine running tools.jar (a lib included with JDK) is required to be in the classpath.
 * 
 * IMPORTS:
 * Import statments must be put on the top of the code (after the language declaration). Comments can be before the
 *   import statments.
 * 
 * ANNOTATION:
 * Annotation for local methods/fields are allowed but it must be after the accessibility declaration or static keyword.
 *   Annotation for the executable itself can be added by putting each of the annotation after the elements sections but
 *   before any code starts. Each of them MUST ends with ';' to be recognized as a valid annotations.
 * 
 * LOCAL METHODS AND FIELDS:
 * A local method is a method declared in Java code (similar for the field). The method/field will be put in the 
 *   coded-to-be-compiled as a method or field of the class.
 * To declare one, you can simple put its code after imports area. The code must starts with "public", "protected", 
 *   "private" or "static". At least one white space is needed after those prefix. Annotations that are needed should be
 *    put after those words.
 * If the local variable or function is declared to be public, it will be accessible using reflection.
 * 
 * DIRECT ACCESS:
 * A direct access to something is a possibility to access to that things directly. For example, if a variable A can be
 *   direct accessed, the java coded run/compiled by this Engine can use the identifier 'A' directly in the code as if
 *   it is there natively.
 * All local variables and local functions (see above) are all direct-access. $Self method and Frozen variables are also
 *   direct-accessed. For Macro and Function, the paramters are direct access. For Script, the executional paramters 
 *   (Args of Main) will be accessible via scope if the script is run by Main.java.   
 * The scope parameters are only accessible via $Scope variable using $Scope.getValue(`Var Name`) and
 *   $Scope.setValue(`Var Name`, Value).
 *   
 * EXAMPLE CODE:
 * <code>
 * // ## This line will be ignored as it ends with "##"                              ## 
 * // ## This line will be also ignored as it also ends with "##"                    ##
 * // @Java: { main }
 * 
 * /** This is an exmaple code * /
 * 
 * // Import area
 * import java.io.*;
 * import java.util.*;
 * 
 * // Here is a local elements
 * static  int I = 0;
 * private int Factorial(int I) {
 * 	return (I &lt;= 1) ? 1 : I * Factorial(I - 1);
 * }
 * 
 * // Here is the Annotation
 * @SuppressWarnings("unchecked");
 * 
 * System.out.println("Hello World!!!");
 * System.out.println("Main Args = " + Arrays.toString(Args));	// Access Args
 * System.out.println("I = " + I);
 * 
 * for(int i = 0; i &lt; 10; i++) {
 * 	System.out.println(i+"! = " + Factorial(i));
 * }
 * 
 * System.out.println((new File(".")).getAbsolutePath());
 * System.out.println("DONE!!!");
 * 
 * return;
 * </code>
 **/
public class JavaEngine implements ScriptEngine {

	// Debugging - The engine will print the temporary class code while compiling
	static public boolean     DebugMode        = false;
	static public PrintStream DebugPrintStream = System.out;
	
	// Instance --------------------------------------------------------------------------------------------------------
	
	static private JavaEngine Instance = null;
	
	/** Get the only instance of JSEngine */
	static public JavaEngine newInstance(ScriptEngineOption pOption) {
		if(Instance == null) Instance = new JavaEngine();
		return Instance;
	}

	/** Constructs a new JavaEngine */
	JavaEngine() {
		this.JCompiler = JavaCompiler.Instance;
	}

	/**
	 * Constructs a new JavaEngine with a special JavaCompiler - use with care as all serializable might be a problem.
	 **/
	public JavaEngine(JavaCompiler pJavaCompiler) {
		this.JCompiler = (pJavaCompiler == null) ? JavaCompiler.Instance : pJavaCompiler;
	}
	
	/** The name of this class for easy access by the client */
	static public final String Name      = JavaEngine.class.getCanonicalName();
	/** The name of this class for easy access by the client */
	static public final String ShortName = "Java";
	
	/** The name of the object that executing the code */
	static public final String $This = "$This";
	
	/** A regular expression pattern used to parse the import part of the code */
	static public Pattern ImportSearch = Pattern.compile("((\\s)*import[^;]*;)*");
	
	static Pattern ErrorNameExtractorPattern = Pattern.compile("^[^:]*:[0-9]+: ");

	/**{@inheritDoc}*/ @Override public String getName()      { return Name;      }
	/**{@inheritDoc}*/           public String getShortName() { return ShortName; }
	
	/**{@inheritDoc}*/ @Override
	public ScriptEngineOption getOption() {
		return null;
	}
	/**{@inheritDoc}*/ @Override
	public String getParameterString() {
		return null;
	}
	/**{@inheritDoc}*/ @Override
	public ScriptEngineOption getOption(String pParam) {
		return null;
	}

	/**{@inheritDoc}*/ @Override
	public ExecutableInfo getReplaceExecutableInfo(ExecutableInfo EInfo) {
		return null;
	}
	
	// Usable Filters (File filter) ------------------------------------------------------------------------------------

	/** Filter for JASFile only (jas = Java As Script) */
	static class JASFileFilter extends UFFileExtFilter {
		public JASFileFilter() {
			super(new FileExtFilter.FEFExtList("jas"));
		}
	}
	
	static JASFileFilter JASFileFilter = new JASFileFilter();  
	
	/**{@inheritDoc}*/ @Override
	public UsableFilter[] getUsableFilters() {
		return new UsableFilter[] { JASFileFilter };
	}
	
	// Java Specific ---------------------------------------------------------------------------------------------------
	
	JavaCompiler JCompiler;
	
	/** Returns the class loader of this option */
	public JavaCompiler getJavaCompiler() {
		return this.JCompiler;
	}
	
	int Index = Math.abs((new Random()).nextInt());
	
	/** The index for a no-name class to be created */
	protected int nextIndex() {
		return this.Index++;
	}
	
	/** Load a local JarFile into the class path */
	public void addJarFile(String pPath) {
		this.JCompiler.addJarFile(pPath);
	}
	
	/** Load a local JarFile into the class path */
	public void addClasspathURL(String pUrlPath) {
		this.JCompiler.addClasspathURL(pUrlPath);
	}
	
	/** Load a local JarFile into the class path */
	public void removeJarFile(String pPath) {
		this.JCompiler.removeJarFile(pPath);
	}
	
	/** Load a local JarFile into the class path */
	public void removeClasspathURL(String pUrlPath) {
		this.JCompiler.removeClasspathURL(pUrlPath);
	}
	
	/** Returns all the URL to class paths */
	public URL[] getClasspaths() {
		return this.JCompiler.getClasspaths();
	}

	/**
	 * Loads a Serialized ClassData object in a form of an array of bytes to this JavaCompiler and return true if the
	 * byte code is successfully loaded and initialized.
	 **/
	public boolean addClassData(ClassData pClassData) {
		return this.JCompiler.addClassData(pClassData);
	}
	
	/**
	 * Loads a Java class byte-code to this JavaCompiler and return true if the byte code is successfully loaded and
	 * initialized
	 **/
	public boolean addClassByteCode(String pName, String Path, byte[] ByteCode) {
		return this.JCompiler.addClassByteCode(pName, Path, ByteCode);
	}


	/**
	 * Loads a Serialized ClassData object in a form of an array of bytes to this JavaCompiler and return true if the
	 * byte code is successfully loaded and initialized.
	 **/
	public ClassData getCompiledClassData(String pName) {
		return this.JCompiler.getCompiledClassData(pName);
	}
	// Import ----------------------------------------------------------------------------------------------------------
	
	Vector<String> DefaultImports    = null;
	String         DefaultImportsStr = null;

	/**
	 * Add default import name - Returns if the import name is valid. If the import name is in a valid format but it
	 * does not seems to be the right package name or class name, the method will return false but add it in the list
	 * anyway. 
	 **/
	public boolean addDefaultImport(String pImportName) {
		if(this.DefaultImports == null) this.DefaultImports = new Vector<String>();
		String Import = "import " + pImportName + ";";
		
		if(!JavaEngine.ImportSearch.matcher(Import).matches()) return false;
		if(!this.DefaultImports.contains(pImportName)) {
			this.DefaultImports.add(pImportName);
			this.DefaultImportsStr = null;
		}
		
		return true;
	}
	
	/** Get the default imports as string */
	String getDefaultImportStr() {
		if(this.DefaultImportsStr == null) {
			if(this.DefaultImports != null) {
				StringBuffer SB = new StringBuffer();
				for(int i = 0; i < this.DefaultImports.size(); i++)
					SB.append("import ").append(this.DefaultImports.get(i)).append(";\n");
				
				return SB.toString();
			} else this.DefaultImportsStr = "";
		}
		return this.DefaultImportsStr;
	}

	/** Returns the import names */
	public String[] getImportsName() {
		return (this.DefaultImports == null)
				?Signature.Simple.EmptyStringArray
				:this.DefaultImports.toArray(Signature.Simple.EmptyStringArray);
	}
	
	// ScriptEngine Services ------------------------------------------------------------------------------------------- 
	
    // Scope ---------------------------------------------------------------------------------------
    
	/**{@inheritDoc}*/ @Override
	public Scope newScope() {
		return new net.nawaman.script.Scope.Simple();
	}
	
	/**{@inheritDoc}*/ @Override
	public Scope getCompatibleScope(Scope pOrg) {
		if(pOrg instanceof Scope.Simple) return pOrg;
		Scope Target = this.newScope();
		Scope.Simple.duplicate(pOrg, Target);
		return Target;
	}
	
	// Problem container --------------------------------------------------------------------------

	/**{@inheritDoc}*/ @Override
	public ProblemContainer newCompileProblemContainer() {
		return new ProblemContainer();
	}
	
	// Eval ----------------------------------------------------------------------------------------
	
	/**{@inheritDoc}*/ @Override
	public Object eval(String pCode, Scope pScope, ProblemContainer pResult) {
		if(pCode == null) throw new NullPointerException();
		
		CompiledCode CCode = this.compile(pCode, null, null, null, null);
		if(CCode == null) throw new RuntimeException("The engine fails to compile the code.");
		return this.eval(CCode, (pScope == null)?Scope.Empty.Instance:pScope, pResult);
	}
		
	/**{@inheritDoc}*/ @Override
	public Object eval(CompiledCode pCode, Scope pScope, ProblemContainer pResult) {
		if(!(pCode instanceof JavaCompiledCode))
			throw new RuntimeException("Java engine can only execute a Java compiled code.");
		pScope = (pScope == null)?Scope.Empty.Instance:pScope;
		return ((JavaCompiledCode)pCode).JavaCode.run(pScope.getValue($This), pScope);
	}
	
	/**{@inheritDoc}*/ @Override
	public Object eval(Script pScript, Scope pScope, ProblemContainer pResult) {
		return this.eval(((JavaScript)pScript).getCompiledCode(), (pScope == null)?Scope.Empty.Instance:pScope, pResult);
	}
	
	// Compile -------------------------------------------------------------------------------------
	
	static private String ScriptPrefixTemplate = null;
	static private String ScriptMiddleTemplate = null;
	static private String ScriptSuffixTemplate = null;
	
	static void ensureScriptTmeplate() {
		if(ScriptPrefixTemplate != null) return;
		ScriptPrefixTemplate = String.format(
				"public class %s implements %s,%s {"+			// ClassName and JavaCode
				""+
				"	public %s(%s $FScope) {"+
				"		%s"+
				"	}"+
				""+
				"	%s",
				// Params --------------------------------------------------------------
				"%s",																		// ClassName
				JavaCode.class.getCanonicalName(),											// For Extends
				Serializable.class.getCanonicalName(),										// For Extends 

				"%s",																		// Constructor Name (ClassName)
				Scope.class.getCanonicalName(),												// Constructor parameter

				"%s",																		// Constructor body
				
				"%s"																		// Field for frozen
				);
		
		ScriptMiddleTemplate = String.format(
				"	public%s Object run(Object %s, %s $Scope) {",	// $This and Scope class
				"%s",                                   // Annotation    
				$This,									// For Method Parameter
				Scope.class.getCanonicalName());		// For Method Parameter
		
		ScriptSuffixTemplate =
				"	}"+
				"}";
	}
	
	/** Returns the prefix of the template of a java code to to create a script */
	static public String getScriptPrefix(String pClassName, Scope pFrozenScope, String[] pFVNames) {
		String[] FStrs = JavaEngine.getFrozenStrings(pFrozenScope, pFVNames);
		return String.format(ScriptPrefixTemplate, pClassName, pClassName, FStrs[0], FStrs[1]);
	}
	
	/** Returns the prefix of the template of a java code to to create a script */
	static public String getScriptMiddle(String pClassName, String pOtherElements, String pAnnotation) {
		if(pAnnotation == null) pAnnotation = "";
		return String.format(
				ScriptMiddleTemplate,
				pAnnotation,
				((pOtherElements == null)?"":pOtherElements),
				((pAnnotation == null)?"":pAnnotation)
			);
	}
	
	/**{@inheritDoc}*/ @Override
	public CompiledCode compile(String pScriptCode, Scope pFrozen, String[] pFrozenVNames,
			CompileOption pOption, ProblemContainer pResult) {
		return (CompiledCode)this.newExecutable(EKind.CompiledCode, null, null, pScriptCode, pFrozen, pFrozenVNames, pOption, pResult);
	}
	
	/**{@inheritDoc}*/ @Override
	public void reCompile(Script pScript, Scope pFrozen, CompileOption pOption, ProblemContainer pResult) {
		// Ensure that only Java is accepted
		if((pScript == null) || !(pScript.getEngine() instanceof JavaEngine))
			throw new RuntimeException("JEngine can execute only java code.");

		try {
			JavaScript JS = (JavaScript)pScript;

			FrozenVariableInfos TheFVInfos = JS.getFVInfos();
			String[]            TheFVNames = (TheFVInfos == null)?null:TheFVInfos.getFrozenVariableNames();
			
			JS.CCode  = new JavaCompiledCode((JavaCode)this.compile(pScript.getCode(), pFrozen, TheFVNames, pOption, pResult));
			JS.Engine = this;
		} catch(Exception E) {
			throw new RuntimeException("An exception was thrown while compiling a script.", E);
		}
	}
	
	/**{@inheritDoc}*/ @Override
	public boolean isCompilable() {
		return true;
	}

	/**{@inheritDoc}*/ @Override
	public boolean isCompiledCodeSerializable() {
		return true;
	}

	/**{@inheritDoc}*/ @Override
	public Executable compileExecutable(ExecutableInfo pExecInfo, String pCode, CompileOption pOption,
			ProblemContainer pResult) {
		return Utils.compileExecutable(this, pExecInfo, pCode, pOption, pResult);
	}
	
	// Script --------------------------------------------------------------------------------------
	
	/**{@inheritDoc}*/ @Override
	public Script newScript(String pCode, Scope pFrozenScope, String[] pFrozenVNames,
			CompileOption pOption, ProblemContainer pResult) {
		JavaCompiledCode CCode = (JavaCompiledCode)this.compile(pCode, pFrozenScope, pFrozenVNames, pOption, pResult);
		if(CCode == null) return null;
		
		return new JavaScript(this, pCode, FrozenVariableInfos.newFVInfos(pFrozenVNames, pFrozenScope), CCode);
	}

	// Macro ---------------------------------------------------------------------------------------
	
	static private String MacroPrefixTemplate = null;
	static private String MacroMiddleTemplate = null;
	static private String MacroSuffixTemplate = null;
	
	static void ensureMacroTmeplate() {
		if(MacroPrefixTemplate != null) return;
		MacroPrefixTemplate = String.format(
				"public class %s %s implements %s,%s {"+					// ClassName
				"	public %s(%s pSignature, String pCode, %s $FScope) {" +	// ClassName for Constructor
				"		this.$Signature = pSignature;" +
				"		this.$Code      = pCode;" +
				"		%s"+												// Frozen Variable
				"	}"+
				"" +
				"	%s" +													// Frozen fields
				"" +
				"	public String getEngineName() {"+
				"		return \"%s\";"+									// Engine name
				"	}"+
				"" +
				"	public %s getEngine() {"+
				"		return (%s)(%s.Instance.getDefaultEngineOf(%s.getEngineClassNameByName(this.getEngineName())));"+
				"	}"+
				"" +
				"	final String $Code;"+
				"	public String getCode() {"+
				"		return this.$Code;"+
				"	}"+
				""+
				"	final %s $Signature;"+
				"	public %s getSignature() {" +
				"		return this.$Signature;" +
				"	}"+
				""+
				"	public %s run(final %s $Macro, final Object ... $Params) {"+
				"		%sthis.run((%s)$Macro, this.getEngine().newScope(), $Params);%s"+
				"	}"+
				"",
				// Params --------------------------------------------------------------
				"%s",											// ClassName
				"%s",											// "extends " + SuperClassName
				Macro.Simple.Body.class.getCanonicalName(),		// For Implements
				Serializable.class.getCanonicalName(),			// For Implements
				
				"%s",											// ClassName for Construction Name
				Signature.class.getCanonicalName(),				// For Constructor parameter
				Scope.class.getCanonicalName(),					// For Constructor parameter
				
				"%s",											// Constructor Body
				
				"%s",											// Frozen Fields
				
				JavaEngine.Instance.getShortName(),				// Return the engine name
				
				JavaEngine.class.getCanonicalName(),			// Return type of getEngine();
				
				JavaEngine.class.getCanonicalName(),			// Return type of getEngine();
				ScriptManager.class.getCanonicalName(),			// Static access in getEngine()
				ScriptManager.class.getCanonicalName(),			// Static access in getEngine()
				
				Signature.class.getCanonicalName(),				// For getSignature() return type
				
				Signature.class.getCanonicalName(),				// For Field type
		
				"%s",											// Return type
				Function.Simple.class.getCanonicalName(),		// Parameter

				"%s",											// Return statement
				Macro.Simple.class.getCanonicalName(),			// Cast
				"%s");											// Return statement

		MacroMiddleTemplate = String.format(
				"	private %s $Self(final Object ... $Params) {" +
				"		%sthis.run(null, this.getEngine().newScope(), $Params);" +
				"	}" +
				"	public%s %s run(final %s $Macro, final %s $Scope, final Object ... $Params) {",
				// Params --------------------------------------------------------------
				"%s",										// The return type
				"%s",										// Return Statement

				"%s",										// Annotations
				"%s",										// The return type
				Macro.Simple.class.getCanonicalName(),		// For run() parameter
				Scope.class.getCanonicalName());			// For run() parameter
		
		MacroSuffixTemplate =
				"	}"+
				"}";
	}
	
	/** Returns the prefix of the template of a java code to create a macro */
	static public String getMacroPrefix(String pClassName, Class<?> pSuperClass, Signature pSignature,
			Scope pFrozenScope, String[] pFVNames) {
		String[] FStrs = JavaEngine.getFrozenStrings(pFrozenScope, pFVNames);
		String Return1 = "return ";
		String Return2 = "";
		if(pSignature.getReturnType() == Void.class) {
			Return1 = "";
			Return2 = "		return null;";
		}
		return String.format(MacroPrefixTemplate,
				pClassName,
				((pSuperClass == null)?"":"extends " + pSuperClass.getCanonicalName()),
				pClassName,
				FStrs[0],										// Constructor body (about the frozen variables)
				FStrs[1],										// Declaration of frozen variables as fields
				pSignature.getReturnType().getCanonicalName(),	// Return type
				Return1, 
				Return2
			);
	}
	
	/** Returns the prefix of the template of a java code to create a macro */
	static public String getMacroMiddle(Signature Signature, String Annotations) {
		Class<?> C = Signature.getReturnType();
		String CName  = Signature.getReturnType().getCanonicalName();
		String Return = "return ";
		if(C == Void.class) {
			CName   = "void";
			Return = "";
		}
		return String.format(MacroMiddleTemplate,
				CName,		// The return type
				Return,		// Return Statement
				Annotations,// annotation
				CName		// The return type
			);
	}

	// Function ------------------------------------------------------------------------------------
	
	static private String FunctionPrefixTemplate = null;
	static private String FunctionMiddleTemplate = null;
	static private String FunctionSuffixTemplate = null;
	
	static void ensureFunctionTmeplate() {
		if(FunctionPrefixTemplate != null) return;
		FunctionPrefixTemplate = String.format(
				"public class %s %s implements %s,%s {"+					// ClassName
				"	public %s(%s pSignature, String pCode, %s $FScope) {" +	// ClassName for Constructor
				"		this.$Signature = pSignature;" +
				"		this.$Code      = pCode;" +
				"		%s"+												// Frozen Variable
				"	}"+
				"" +
				"	%s" +													// Frozen fields
				"" +
				"	public String getEngineName() {"+
				"		return \"%s\";"+									// Engine name
				"	}"+
				"" +
				"	public %s getEngine() {"+
				"		return %s.Instance.getDefaultEngineOf(%s.getEngineClassNameByName(this.getEngineName()));"+
				"	}"+
				"" +
				"	final String $Code;"+
				"	public String getCode() {"+
				"		return this.$Code;"+
				"	}"+
				""+
				"	final %s $Signature;"+
				"	public %s getSignature() {" +
				"		return this.$Signature;" +
				"	}"+
				"",
				// Params --------------------------------------------------------------
				"%s",											// ClassName
				"%s",											// "extends " + SuperClassName
				Function.Simple.Body.class.getCanonicalName(),	// For Implements
				Serializable.class.getCanonicalName(),			// For Implements
				
				"%s",											// ClassName for Construction Name
				Signature.class.getCanonicalName(),				// For Constructor parameter
				Scope.class.getCanonicalName(),					// For Constructor parameter
				
				"%s",											// Constructor Body
				
				"%s",											// Frozen Fields
				
				JavaEngine.Instance.getShortName(),				// Return the engine name
				
				ScriptEngine.class.getCanonicalName(),			// Return type of getEngine();
				
				ScriptManager.class.getCanonicalName(),			// Static access in getEngine()
				ScriptManager.class.getCanonicalName(),			// Static access in getEngine()
				
				Signature.class.getCanonicalName(),				// For getSignature() return type
				
				Signature.class.getCanonicalName());			// For Field type

		FunctionMiddleTemplate = String.format(
				"	private %s $Self(final Object ... $Params) {" +
				"		%sthis.run(null, $Params);" +
				"	}" +
				""+
				"	public %s run(final %s $Function, final Object ... $Params) {"+
				"		%sthis.run(0, (%s)$Function, $Params);%s"+
				"	}"+
				"	public%s %s run(final int ___JavaEngineDummy___, final %s $Function, final Object ... $Params) {",
				// Params --------------------------------------------------------------
				"%s",											// The function return type
				"%s",											// Return Statement 1

				"%s",											// The function return type
				Function.Simple.class.getCanonicalName(),		// For run() parameter
				"%s",											// Return Statement 2
				Function.Simple.class.getCanonicalName(),		// For Cast
				"%s",											// Return Statement 3

				"%s",											// Annotations
				"%s",											// The function return type
				Function.Simple.class.getCanonicalName()		// For run() parameter
			);
		
		FunctionSuffixTemplate =
				"	}"+
				"}";
	}
	
	/** Returns the prefix of the template of a java code to create a function */
	static public String getFunctionPrefix(String pClassName, Class<?> pSuperClass, Scope pFrozenScope, String[] pFVNames) {
		String[] FStrs = JavaEngine.getFrozenStrings(pFrozenScope, pFVNames);
		return String.format(FunctionPrefixTemplate,
				pClassName,
				((pSuperClass == null)?"":"extends " + pSuperClass.getCanonicalName()),
				pClassName,
				FStrs[0],	// Constructor body (about the frozen variables)
				FStrs[1]	// Declaration of frozen variables as fields
			);
	}
	
	/** Returns the middle part of the template of a java code to create a function */
	static public String getFunctionMiddleMiddle(Signature Signature, String Annotations) {
		Class<?> C = Signature.getReturnType();
		String CName  = Signature.getReturnType().getCanonicalName();
		String Return1 = "return ";
		String Return2 = "return ";
		String Return3 = "";
		if(C == Void.class) {
			CName  = "void";
			Return1 = "";
			Return2 = "";
			Return3 = "		return null;";
		}
		return String.format(FunctionMiddleTemplate,
				CName,		// The return type
				Return1,	// Return Statement
				Signature.getReturnType().getCanonicalName(), // The function return type
				Return2,	// Return Statement
				Return3,	// Return (replace) Statement
				Annotations,// The Annotations
				CName		// The return type
			);
	}

	static final private String  ParameterNamePattern = "[a-zA-Z$_][a-zA-Z0-9$_]*";
	static final private Pattern ParameterNameRegEx   = Pattern.compile(ParameterNamePattern);
	
	/**{@inheritDoc}*/ @Override
	public Macro newMacro(Signature pSignature, String[] pParamNames, String pFunctionCode, Scope pFrozen,
			String[] pFrozenVNames, CompileOption pOption, ProblemContainer pResult) {
		return (Macro)this.newExecutable(EKind.Macro, pSignature, pParamNames, pFunctionCode, pFrozen, pFrozenVNames,
				pOption, pResult);
	}
	
	/**{@inheritDoc}*/ @Override
	public Function newFunction(Signature pSignature, String[] pParamNames, String pFunctionCode, Scope pFrozen,
			String[] pFrozenVNames, CompileOption pOption, ProblemContainer pResult) {
		return (Function)this.newExecutable(EKind.Function, pSignature, pParamNames, pFunctionCode, pFrozen, pFrozenVNames,
				pOption, pResult);
	}

	/**{@inheritDoc}*/ @Override
	public String getLongComments(String Comment, int Width) {
		int WidthMinusOne = Width - 1;
		StringBuilder SB = new StringBuilder();
		while(SB.length() <  WidthMinusOne) SB.append("*");
		return String.format("/%1$s\n%2$s\n%1$s/", SB.toString(), Comment);
	}
	
	/**{@inheritDoc}*/ @Override
	public ObjectOutputStream newExecutableObjectOutputStream(OutputStream OS) {
		return null;
	}
	/**{@inheritDoc}*/ @Override
	public ObjectInputStream newExecutableObjectInputStream(InputStream OS) {
		return null;
	}
	
	// Utilities method for the compilation of executable -----------------------------------------
	
	/**
	 * Find the position in the text where the import statments, elements and annotations ends.
	 * 
	 * The returned value will be an array of [statment end, element end and annotation end, HasLastReturn].
	 * 
	 * The extra integer (HasLastReturn) will indicate if the code ends with a return (so that auto-return-adding may be
	 *     performed if needed). If this last value is -1, the code's last statment is not a return.
	 **/
	static public int[] getImportEndAndElementEndAndAnnotationEnd(String pScriptCode) {
		return JavaEngine.getImportEndAndElementEndAndAnnotationEnd(pScriptCode, "import", true, true);
	}

	/**
	 * Find the position in the text where the import statments, elements and annotations ends.
	 * 
	 * The returned value will be an array of [statment end, element end and annotation end, HasLastReturn].
	 * 
	 * The extra integer (HasLastReturn) will indicate if the code ends with a return (so that auto-return-adding may be
	 *     performed if needed). If this last value is -1, the code's last statment is not a return.
	 **/
	static public int[] getImportEndAndElementEndAndAnnotationEnd(String pScriptCode, String pImportStart,
			boolean pIsAllowElements, boolean pIsAllowAnnotations) {
	
		Vector<Statement> Collector = new Vector<Statement>();
		JavaRoughParser.lengthOfStatements(Collector, pScriptCode, 0);
		Statement.cleanAllIgnoreable(Collector);
		int ImportEnd  = 0;
		int FoundIndex = 0;
		// Find the first that is not import
		for(int i = FoundIndex; i < Collector.size(); i++) {
			Statement Stm = Collector.get(i);
			if(Stm == null) continue;
			String Text = Stm.getText();
			if(Statement.isStartWithWord(Text, pImportStart, 0)) continue;
			if(i != FoundIndex) {
				Stm        = Collector.get(i - 1);
				Text       = Stm.getText();
				ImportEnd  = Stm.getOffset() + Text.length();
				FoundIndex = i;
			}
			break;
		}
		int ElementEnd = ImportEnd;
		if(pIsAllowElements) {
			// Find the first from import that is not private, public nor static
			for(int i = FoundIndex; i < Collector.size(); i++) {
				Statement Stm = Collector.get(i);
				if(Stm == null) continue;
				String Text = Stm.getText();
				if(Statement.isStartWithWord(Text, "private", 0)) continue;
				if(Statement.isStartWithWord(Text, "public",  0)) continue;
				if(Statement.isStartWithWord(Text, "static",  0)) continue;
				if(Text.startsWith(";")                         ) continue;
				if(i != FoundIndex) {
					Stm        = Collector.get(i - 1);
					Text       = Stm.getText();
					ElementEnd = Stm.getOffset() + Text.length();
					FoundIndex = i;
				}
				break;
			}
		}
		int AnnotationEnd = ElementEnd;
		if(pIsAllowAnnotations) {
			// Find the first private, public or static that is not annotation
			for(int i = FoundIndex; i < Collector.size(); i++) {
				Statement Stm = Collector.get(i);
				if(Stm == null) continue;
				String Text = Stm.getText();
				if(Text.startsWith("@")) continue;
				if(Text.startsWith(";")) continue;
				if(i != FoundIndex) {
					Stm           = Collector.get(i - 1);
					Text          = Stm.getText();
					AnnotationEnd = Stm.getOffset() + Text.length();
				}
				break;
			}
		}
		
		int HasLastReturn = -1;
		// Find the first from import that is not private, public nor static
		for(int i = Collector.size(); --i >= 0; ) {
			Statement Stm = Collector.get(i);
			if(Stm == null) continue;
			String Text = Stm.getText();
			if(Text.length() == 0) continue;
			if(Statement.isStartWithWord(Text, "return",  0)) {
				HasLastReturn = 1;
				break;
			}
		}
		
		return new int[] { ImportEnd, ElementEnd, AnnotationEnd, HasLastReturn };
	}
	
	/** Creates the strings of the frozen variable */
	static protected String[] getFrozenStrings(Scope pFrozenScope, String[] pFVNames) {
		if((pFrozenScope == null) || (pFVNames == null)) return new String[] { "", "" };

		StringBuilder ConstructorBody = new StringBuilder();
		StringBuilder FrozenFields    = new StringBuilder();
		if(pFrozenScope != null) {
			for(String VName : pFVNames) {
				if(!pFrozenScope.isExist(VName)) continue;
				Class<?> VType = pFrozenScope.getTypeOf(VName); if(VType == null) VType = Object.class;
				FrozenFields.append(String.format("final %s %s;", VType.getCanonicalName(), VName));
				ConstructorBody.append(
					String.format(
						"this.%s = (($FScope == null)||!$FScope.isExist(\"%s\"))?%s:(%s)$FScope.getValue(\"%s\");",
						VName, VName, String.valueOf(Scope.Simple.getDefauleValueOf(VType)), VType.getCanonicalName(), VName
					)
				);
			}
		}
		
		return new String[] { ConstructorBody.toString(), FrozenFields.toString() };
	}
			
	
	/** Returns the line number of the position */
	static protected int getLineNumberOf(String pCode, Vector<Integer> pNLs, int pPos) {
		if((pPos < 0) || (pPos > pCode.length())) return -1;
		for(int i = pNLs.size(); --i >= 0; ) {
			if(pPos > pNLs.get(i)) return i + 1;
		}
		return 0;
	}
	/** Returns the position of on a line number of the position (i.e., column) */
	static protected int getColOf(String pCode, Vector<Integer> pNLs, int pPos) {
		if((pPos < 0) || (pPos > pCode.length())) return -1;
		for(int i = pNLs.size(); --i >= 0; ) {
			if(pPos > pNLs.get(i)) return pPos - pNLs.get(i) - 1;
		}
		return pPos;
	}
	/**
	 * Report error if the position is not within the given code (so it is in the prefix and suffix). Therefore, there
	 * is a need to debug.
	 **/		
	static protected int ensureGoodPostion(int Pos, Diagnostic<?> Diagnostic,
			int ImportBegin, int ImportEnd, int ElementBegin, int ElementEnd, int BodyBegin, int BodyEnd) {
		if((Pos >=    BodyBegin) && (Pos <=    BodyEnd)) return Pos - (BodyBegin    - ElementEnd) - (ElementBegin - ImportEnd);
		if((Pos >= ElementBegin) && (Pos <= ElementEnd)) return Pos - (ElementBegin - ImportEnd);
		if((Pos >=  ImportBegin) && (Pos <=  ImportEnd)) return Pos;
		
		System.err.println("An error occur in the pre-created code (Contact the developer)");
		System.err.println("START UNEXPECTED ERROR ------------------------------------------------------------------------");
	    System.err.println(Diagnostic.getColumnNumber());
	    System.err.println(Diagnostic.getLineNumber());
		System.err.println(Diagnostic.getCode());
		System.err.println(Diagnostic.getKind());
		System.err.println(Diagnostic.getPosition());
		System.err.println(Diagnostic.getStartPosition());
		System.err.println(Diagnostic.getEndPosition());
		System.err.println(Diagnostic.getSource());
		System.err.println(Diagnostic.getMessage(null));
		System.err.println(Diagnostic.getSource());
		System.err.println("END UNEXPECTED ERROR --------------------------------------------------------------------------");
		return -1;
	}
	
	// Compile executable ----------------------------------------------------------------------------------------------

	static enum EKind { CompiledCode, Macro, Function };
	
	static final String LastReturnNull = "\nreturn null;";

	/**{@inheritDoc}*/ @SuppressWarnings({ "unchecked", "rawtypes" })
	Object newExecutable(EKind pEKind, Signature pSignature, String[] pParamNames, String pCode, Scope pFrozen,
			String[] pFVNames, CompileOption pOption, ProblemContainer pResult) {
	
		if(pOption == null) pOption = JavaCompileOption.DefaultOption;

		int[] IEEnds = JavaEngine.getImportEndAndElementEndAndAnnotationEnd(pCode);
		
		String  Imports     = pCode.substring(0,         IEEnds[0]);
		String  OtherEs     = pCode.substring(IEEnds[0], IEEnds[1]);
		String  Annotations = pCode.substring(IEEnds[1], IEEnds[2]);
		String  Body        = pCode.substring(IEEnds[2]);
		boolean HasReturn   = (IEEnds[3] == -1);
		
		// Prepare Parameters
		String Params = "";
		if(pEKind != EKind.CompiledCode) {
			StringBuilder SB = new StringBuilder();
			for(int i = 0; i < pParamNames.length; i++) {
				String PTypeName = pSignature.getParamType(i).getCanonicalName();
				if(pSignature.isVarArgs() && (i == (pParamNames.length - 1))) {
					// Get the array type 
					PTypeName += "[]";
				}
				String PName = pParamNames[i];
				if((PName == null) || (PName.length() == 0) || !ParameterNameRegEx.matcher(PName = PName.trim()).find())
					throw new IllegalArgumentException("Invalid paramter name '"+PName+"'");
				
				SB.append("		final ");
				SB.append(PTypeName);
				SB.append(" ");
				SB.append(PName);
				SB.append(" = (");
				SB.append(PTypeName);
				SB.append(")$Params[");
				SB.append(i);
				SB.append("];");
			}
			Params = SB.toString();
		}
		
		// Get the ExecName
		String ClassName = null;
		if(pEKind == EKind.CompiledCode) {
			ClassName = ((JavaCompileOption)pOption).getClassNamePrefix() + "_" + this.nextIndex();
			
		} else {
			ClassName = pSignature.getName(); if(ClassName == null) ClassName = "";
			StringBuilder SB = new StringBuilder();
			for(int i = 0; i < ClassName.length(); i++) {
				char Char = ClassName.charAt(i);
				if(Character.isJavaIdentifierPart(Char)) SB.append(Char);
			}
			if(SB.length() == 0) SB.append(((JavaCompileOption)pOption).getClassNamePrefix());
			else                 SB.append("_");
			ClassName = SB.toString() + this.nextIndex();
		}
		
		// Prepare the annotation
		if(Annotations.length() != 0) {
			// Split by ";"
			String[] Anns = Annotations.split("[ \n\t]*;[ \n\t]*");
			// Join
			StringBuilder AnnsSB = new StringBuilder();
			for(int i = 0; i < Anns.length; i++)
				AnnsSB.append(" ").append(Anns[i]);
			// Replace the Annotation value
			Annotations = AnnsSB.toString();
		}
		
		String Prefix    = null;
		String Middle    = null;
		String Suffix    = null;
		
		if(pEKind == EKind.CompiledCode) {
			JavaEngine.ensureScriptTmeplate();
			Prefix = JavaEngine.getScriptPrefix(ClassName, pFrozen, pFVNames);
			Middle = JavaEngine.getScriptMiddle(ClassName, OtherEs, Annotations);
			Suffix = ScriptSuffixTemplate;
			
		} else if(pEKind == EKind.Macro) {
			JavaEngine.ensureMacroTmeplate();
			Prefix = JavaEngine.getMacroPrefix(ClassName, ((JavaCompileOption)pOption).getSuperClass(), pSignature, pFrozen, pFVNames);
			Middle = JavaEngine.getMacroMiddle(pSignature, Annotations);
			Suffix = MacroSuffixTemplate;
			
		} else if(pEKind == EKind.Function) {
			JavaEngine.ensureFunctionTmeplate();
			Prefix = JavaEngine.getFunctionPrefix(ClassName, ((JavaCompileOption)pOption).getSuperClass(), pFrozen, pFVNames);
			Middle = JavaEngine.getFunctionMiddleMiddle(pSignature, Annotations);
			Suffix = FunctionSuffixTemplate;
		}
		
		// Add default import from this engine
		Imports += this.getDefaultImportStr();
		// Add default import from the option
		if(pOption instanceof JavaCompileOption) Imports += ((JavaCompileOption)pOption).getDefaultImportStr();

		// Add a return null at the end of the body because it does not have any return
		if(HasReturn) Body += LastReturnNull;
		// Prepare a flag to help in case there is a need to remove the last return null. (like it ends with 'if' and
		//    'else' that both have return).
		boolean IsRemoveLastReturnNull = false;
		
		TryReturnNull: while(true) {
			// Remove the LastReturnNull (because it becomes the unreachable )
			if(IsRemoveLastReturnNull && HasReturn && Body.endsWith(LastReturnNull))
				Body = Body.substring(0, Body.length() - LastReturnNull.length());
			
			// I---IE---EB---B
			//          ^--IEEnds[1]
			//     ^-------IEEnds[0]
			// I---I[Prefix]E---E[Middle][Params]B---B[Suffix]
			int ImportBegin  = 0;
			int ImportEnd    = IEEnds[0];
			int ElementBegin = ImportEnd    + Prefix.length();
			int ElementEnd   = ElementBegin + (IEEnds[1] - IEEnds[0]);
			int BodyBegin    = ElementEnd   + Middle.length() + Params.length();
			int BodyEnd      = 0;
			
			String TempClassCode = (new StringBuffer())
					.append(Imports)
					.append(Prefix)
					.append(OtherEs)
					.append(Middle)
					.append(Params)
					.append(Body)
					.append(Suffix)
					.toString()
					.replaceAll("->\\$Result", "");
	
			BodyEnd = TempClassCode.length() - Suffix.length();
			
			this.JCompiler.addCode(ClassName+".java", "", TempClassCode);

			if(DebugMode) DebugPrintStream.println(ClassName+".java:\n" + TempClassCode);
			
			DiagnosticCollector<JavaFileObject> Ds = (pResult != null)?new DiagnosticCollector<JavaFileObject>():null;
			
			String Err = this.JCompiler.compile(Ds);
			if(Err != null) {
				// No detail of the error is captured, so just throw an exception
				if((Ds == null) || (pResult == null)) {
					if(!IsRemoveLastReturnNull && Err.contains("unreachable statement\nreturn null;	}}\n^")) {
						IsRemoveLastReturnNull = true;
						continue TryReturnNull;
					}
					
					throw new RuntimeException("An error creating java "+pEKind.toString().toLowerCase()+": Compile Error!\n" + Err);
				}
				
				// Prepare lines
				Vector<Integer> NLs = new Vector<Integer>();
				for(int i = 0; i < pCode.length(); i++) {
					if(pCode.charAt(i) == '\r') {
						if((pCode.length() > i) && (pCode.charAt(i + 1) == '\n')) i++;
					} else if(pCode.charAt(i) == '\n') {
					} else continue;
					NLs.add(i);
				}
				
				for (Diagnostic Diagnostic : Ds.getDiagnostics()) {
					// Get the message kind
					String Kind = Diagnostic.getKind().toString();
					Kind = Kind.charAt(0) + Kind.substring(1).toLowerCase();
					
					// Extract the message (only the important data)
					String   Message = Diagnostic.getMessage(null);
					String[] MLines = Message.split("\n");
					
					Matcher Ma = ErrorNameExtractorPattern.matcher(MLines[0]);
					if(Ma.find()) Message = MLines[0].substring(Ma.end()).trim();
					if(MLines.length > 1) {
						for(int i = 1; i < MLines.length; i++) {
							if(MLines[i].startsWith("location:")) continue;
							Message += " (" + MLines[i] + ")";
						}
					}
					
					// Get the start position - and ensure if it is a good value
					int StartPos = JavaEngine.ensureGoodPostion(
							(int)Diagnostic.getStartPosition(), Diagnostic,
							ImportBegin, ImportEnd, ElementBegin, ElementEnd, BodyBegin, BodyEnd);
					int EndPos = JavaEngine.ensureGoodPostion(
							(int)Diagnostic.getEndPosition(), Diagnostic,
							ImportBegin, ImportEnd, ElementBegin, ElementEnd, BodyBegin, BodyEnd);
					
					if((StartPos == -1) || (EndPos == -1)) {
						throw new RuntimeException(
								"Internal Error: An error happend in an unexpected place.\n" +
								"Please report the error.\n" +
								"Temporary Code: ---------------------------------------------------\n"+
								TempClassCode + "\n" +
								"-------------------------------------------------------------------");
					}
					
					// If the problem is the unreachable code at the end, set the flag to remove the LastReturnNull
					//    and try to compile again.
					if(!IsRemoveLastReturnNull
							&& "unreachable statement".equals(Message)
							&& ((EndPos - StartPos + 1) == LastReturnNull.length())
							&& ((int)Diagnostic.getEndPosition() == BodyEnd)) {
						IsRemoveLastReturnNull = true;
						continue TryReturnNull;
					}
					
					// Get the line and column number
					int StartLineNumber = getLineNumberOf(pCode, NLs, StartPos);
					int StartColumn     = getColOf(       pCode, NLs, StartPos);
					
					pResult.reportProblem(
							new Problem.Detail(
								Problem.Kind.getKind(Kind),
								null,
								pCode,
								Message,
								StartPos, EndPos,
					    		StartLineNumber + 1, StartColumn
					    	)
						);
				}
				
				if(pResult.hasError()) return null;
			}
	
			boolean IsSaveCode = ((JavaCompileOption)pOption).isToSaveCode(); 
			if(pEKind == EKind.CompiledCode) {
				try {
					Class<JavaCode>       Cls  = (Class<JavaCode>)this.JCompiler.getClassByName(ClassName);
					Constructor<JavaCode> Cnst = Cls.getConstructor(Scope.class);
					return new JavaCompiledCode(Cnst.newInstance(pFrozen));
				} catch(Exception E) { throw new RuntimeException("An error creating java compiled code.", E); }
				
			} else if(pEKind == EKind.Macro) {
				try {
					Class<Macro.Simple.Body>       Cls = (Class<Macro.Simple.Body>)this.JCompiler.getClassByName(ClassName);
					Constructor<Macro.Simple.Body> C   = Cls.getConstructor(new Class<?>[] { Signature.class, String.class, Scope.class });
					return new JavaMacro(
								pParamNames,
								C.newInstance(pSignature, IsSaveCode?pCode:null, pFrozen),
								FrozenVariableInfos.newFVInfos(pFVNames, pFrozen));
				} catch(Exception E) { throw new RuntimeException("An error creating java macro.", E); }
				
			} else if(pEKind == EKind.Function) {
				try {
					Class<Function.Simple.Body>       Cls = (Class<Function.Simple.Body>)this.JCompiler.getClassByName(ClassName);
					Constructor<Function.Simple.Body> C   = Cls.getConstructor(new Class<?>[] { Signature.class, String.class, Scope.class });
					return new JavaFunction(
								pParamNames,
								C.newInstance(pSignature, IsSaveCode?pCode:null, pFrozen),
								FrozenVariableInfos.newFVInfos(pFVNames, pFrozen));
				} catch(Exception E) { throw new RuntimeException("An error creating java function.", E); }
				
			}
			return null;
		}
	}
	
	static public void main(String ... Args) {
		String Code = "System.out.println($Scope.getValue(\"Prefix\") + \": \" + Text + \": \" + I); return null;";
		System.out.println("Code:-----------------------------------------------------------------------");
		System.out.println(Code);
		System.out.println("----------------------------------------------------------------------------");

		JavaEngine JE = (JavaEngine)ScriptManager.Instance.getDefaultEngineOf("Java");

		Scope MainS = JE.newScope();
		MainS.newVariable("Text", String.class, "Hello from CodeLab!");

		Macro M = JE.newMacro(new Signature.Simple("fact", Long.class, false, Long.class), new String[] { "I" }, Code, MainS,
				new String[] { "Text" }, null, null);

		MainS.newVariable("Prefix", String.class, "HERE");
		for(int i = 0; i < 21; i++) M.run(MainS, (long)i);
		
		System.out.println();
		Script S = JE.newScript(
				"\n" +
				"import java.io.*;" +
				"\n" +
				"private int factorial(int I) { return (I < 1) ? 1 : I*factorial(I - 1); }\n"+
				"System.out.println(\"Hello World!\");\n" +
				"System.out.println((new File(\".\")).getAbsolutePath());\n"+
				"System.out.println(factorial(5));\n",
				null, null, null, null);
		S.run(MainS);
		
		JavaCompiledCode JCC = (JavaCompiledCode)(S.getCompiledCode());
		
		System.out.println(JCC.getJavaCode().getClass());
		
		System.out.println(JE.getCompiledClassData(JCC.getJavaCode().getClass().getCanonicalName()));
		
		S = ScriptManager.Instance.getDefaultEngineOf("JavaScript").newScript("5;", null, null, null, null);
		System.out.println(S.run(MainS));
		
		System.out.println(ScriptManager.Instance.eval("// @JavaScript:\n 5;", MainS, null));
		System.out.println(ScriptManager.Instance.eval("// @Java:\n return 5;", MainS, null));
	}
}
