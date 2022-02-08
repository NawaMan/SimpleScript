package net.nawaman.script;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;

public class Utils {
	
	/** Returns the simple name of a class */
	static public String getClassSimpleName(Class<?> C) {
		if(C == null) return "Object";
		
		String CName = C.getCanonicalName();
		
		if(CName.startsWith("java.lang."))
			CName = CName.substring("java.lang.".length());
		
		return CName;
	}
	
	/** Returns the class from the given name */	
	static public Class<?> getClassFromName(String ClassName) throws ClassNotFoundException {
		if(ClassName == null) return null;
		
		if(ClassName.indexOf('.') != -1)
			ClassName = ClassName.replaceAll("[ \t]*\\.[ \t]*", ".");

		Class<?> C = null;
		
		// Primitive type
		if(     "int"    .equals(ClassName)) C = int    .class;
		else if("boolean".equals(ClassName)) C = boolean.class;
		else if("double" .equals(ClassName)) C = double .class;
		else if("char"   .equals(ClassName)) C = char   .class;
		else if("byte"   .equals(ClassName)) C = byte   .class;
		else if("long"   .equals(ClassName)) C = long   .class;
		else if("float"  .equals(ClassName)) C = float  .class;
		else if("short"  .equals(ClassName)) C = short  .class;
		else if("void"   .equals(ClassName)) C = void   .class;
		else {
			try {
				C = Class.forName(ClassName);
			} catch (ClassNotFoundException CNFE) {
				ClassName = "java.lang." + ClassName;
				try { C = Class.forName(ClassName); }
				catch (ClassNotFoundException E) {
					throw CNFE;
				}
			}
		}
		
		// Just in case
		if(C.isPrimitive())
			throw new java.lang.IllegalArgumentException("Primitive parameter type is not allowed ("+ClassName+")");
		
		return C;
	}

	/** Returns the class from the given name */	
	static public Class<?> getClass(String ClassStr) throws ClassNotFoundException {
		if(ClassStr == null) return Object.class;
		
		Class<?> C = null;
		String[] Ss = ClassStr.split("[ \t]*\\[[ \t]*");
		
		if((Ss == null) || (Ss.length <= 1)) {
			   C = getClassFromName(ClassStr.trim());
		} else C = getClassFromName(Ss[0]   .trim());
		
		for(int i = 1; i < Ss.length; i++)
			C = Array.newInstance(C, 0).getClass();
		
		return C;
	}
	
	/** Loads a text from a file. */
	static public String loadTextFromStream(InputStream IS) throws IOException {
		InputStreamReader IDR = new InputStreamReader(IS);
		BufferedReader    BR  = new BufferedReader(IDR);
	        
		StringBuffer SB  = new StringBuffer();
		String Line;
		while ((Line = BR.readLine()) != null) SB.append(Line).append('\n');
	        
		BR.close();
		if(SB.length() == 0) return "";
		
		return SB.toString().substring(0, SB.length() - 1);
	}
	
	/** Saves a text to. */
	static public void saveTextToStream(String Text, OutputStream IS) throws IOException {
		OutputStreamWriter OSW = new OutputStreamWriter(
		                               (IS instanceof BufferedOutputStream)
		                               ? IS
		                               : new BufferedOutputStream(IS));
		OSW.write(Text);
		OSW.close();
		
		return;
	}
	
	/** Create an Executable using the ExecutableInfo and the Code */
	static public Executable compileExecutable(ScriptEngine Engine, ExecutableInfo pExecInfo, String pCode,
			CompileOption pOption, ProblemContainer pResult) {
		
		// See if there is a need to replace
		pExecInfo = Engine.getReplaceExecutableInfo(pExecInfo);
		
		// Default one in case of null
		if(pExecInfo == null) pExecInfo = ExecutableInfo.DefaultExecutableInfo;
		
		// Script
		if("script".equals(pExecInfo.Kind)) return Engine.newScript(pCode, null, null, pOption, pResult);
		
		// Main
		if("main".equals(pExecInfo.Kind) && (pExecInfo.Signature == null)) {
			pExecInfo = ExecutableInfo.MainExecutableInfo;
		}
		
		boolean IsMacro = false;
		if((IsMacro = "macro" .equals(pExecInfo.Kind)) || "function" .equals(pExecInfo.Kind)) {
			Signature Signature = pExecInfo.Signature;
			if(Signature == null) Signature = net.nawaman.script.Signature.Simple.FreeSignature;
			
			String[] PNames = null;
			try { PNames = pExecInfo.getParameterNames(); }
			catch (NullPointerException E) {
				PNames = (Signature.getParamCount() == 0)
				             ? net.nawaman.script.Signature.EmptyStringArray
				             : new String[Signature.getParamCount()];
				for(int i = 0; i < PNames.length; i++)
					PNames[i] = "Param" + i;
			}
			
			if(IsMacro) return Engine.newMacro(   Signature, PNames, pCode, null, null, pOption, pResult);
			else        return Engine.newFunction(Signature, PNames, pCode, null, null, pOption, pResult);
		} 
		
		throw new ExecutableKindNotSupport(pExecInfo.toString());
	}
}
