package net.nawaman.script;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.nawaman.javacompiler.JavaCompilerObjectInputStream;
import net.nawaman.javacompiler.JavaCompilerObjectOutputStream;

/**
 * This class provide utilities methods for compiled, save and loaded compiled code. It ensures that the required
 *    executabled is uptodate with the code it compiled from. Both the code and the compiled executabled are embeded
 *    together in one file using BASE64 encoding technique. The following are the steps for compiled, save and load.
 */
public class Tools {
	
	Tools() {}

	/** An error message throw when a mal-fomed executable file is loaded. */
	static public final String MAL_FORMED_COMPILED_CODE_EXCEPTION_MESSAGE = "Mal-formed compiled-executable data.";
	
	// NOTE: DO NOTE CHANGE THIS VALUE - EVER!!! OR THE ALREADY COMPILED FILES WILL NOT BE USEABLE.
	/** The width of the comment text to store the compiled code */
	static public final int LONG_COMMENT_LINE_WIDTH = 80;
	
	// UTILITIES -------------------------------------------------------------------------------------------------------
	
	// BASE 64 ENCODING -- Taken from --http://www.wikihow.com/Encode-a-String-to-Base64-With-Java
	// Change from (String):String to (byte[]):String
	// The decoding is all my code (reverse from the one taken, anyway)
	
	public static String base64code   = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	public static int    splitLinesAt = 19 - 1; // 76/4 - 1;
	
	/** Encode the given byte[] with BASE64 ENCODING String */
	static public String base64Encode(byte[] Bytes) {
		if(Bytes == null) return "";
			
		StringBuilder Encoded = new StringBuilder();
		
		// Determine how many padding bytes to add to the output
		int PaddingCount = (3 - (Bytes.length % 3)) % 3;
		
		try {
			ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
			
			// Write the original data
			BAOS.write(Bytes);
			// Write the padding-zero
			for(int i = PaddingCount; --i >= 0; ) BAOS.write(0);

			// Move to the working array
			Bytes = BAOS.toByteArray();
			BAOS.close();
		} catch (Exception E) {}
		
		int c = 0;
		// Process 3 bytes at a time, churning out 4 output bytes
		// worry about CRLF insertions later
		for (int i = 0; i < Bytes.length; i += 3) {
			// I0            I1         I2
			// 0101 01 - 01  0101-0101  01-01 0101
			// F    C    3   F    F     C  3  F   
			// 0101 01 + 01  0101+0101  01+01 0101
			// B0        B1       B2       B3

			int I0 = Bytes[i    ];
			int I1 = Bytes[i + 1];
			int I2 = Bytes[i + 2];
			
			int B0 = ((I0 & 0xFC) >> 2)                     ;
			int B1 = ((I0 & 0x03) << 4) | ((I1 & 0xF0) >> 4);
			int B2 = ((I1 & 0x0F) << 2) | ((I2 & 0xC0) >> 6);
			int B3 =                       (I2 & 0x3F)      ;
			
			Encoded.append(base64code.charAt(B0))
			       .append(base64code.charAt(B1))
			       .append(base64code.charAt(B2))
			       .append(base64code.charAt(B3));
			
			if(c == splitLinesAt) {
				Encoded.append("\n");
				c = 0;
			} else c++;
		}

		// replace encoded padding nulls with "="
		String Result = Encoded.substring(0, Encoded.length() - PaddingCount) + "==".substring(0, PaddingCount); 
		return Result;
	}

	/** Decode the given BASE64-ENCODING String to byte[] */
	static public byte[] Base64Decode(String BASE64) {
		if((BASE64 == null) || (BASE64.length() == 0)) return new byte[0];
		BASE64 = BASE64.trim();
		
		// Remove padding
		int PaddingCount = 0;
		if(     BASE64.charAt(BASE64.length() - 2) == '=') PaddingCount = 2;
		else if(BASE64.charAt(BASE64.length() - 1) == '=') PaddingCount = 1;
		BASE64 = BASE64.substring(0, BASE64.length() - PaddingCount) + "\0\0".substring(2 - PaddingCount);
		
		byte[] Bs = null;
		try {
			ByteArrayOutputStream BAOS = new ByteArrayOutputStream();

			int c = 0;
			for(int i = 0; i < BASE64.length(); i += 4) {
				// B0       B1      B2      B3
				// 010101 + 01 0101 0101 01 01 0101
				// 3 F      3  F    3 C  3  3  F   
				// 010101 + 01 0101-0101 01-01 0101
				// I0          I1        I2
				
				int B0 = base64code.indexOf(BASE64.charAt(i    ));
				int B1 = base64code.indexOf(BASE64.charAt(i + 1));
				int B2 = base64code.indexOf(BASE64.charAt(i + 2));
				int B3 = base64code.indexOf(BASE64.charAt(i + 3));
				
				int I0 = ((B0 & 0x3F) << 2) | ((B1 & 0x30) >> 4);
				int I1 = ((B1 & 0x0F) << 4) | ((B2 & 0x3C) >> 2);
				int I2 = ((B2 & 0x03) << 6) | ((B3 & 0x3F)     );
				
				BAOS.write(I0);
				BAOS.write(I1);
				BAOS.write(I2);
				
				if(c == splitLinesAt) {
					i += 1;
					c = 0;
				} else c++;
			}
			
			Bs = BAOS.toByteArray();
			BAOS.close();
		
		}
		catch (RuntimeException E) { throw E; }
		catch (Exception E)        { throw new RuntimeException(E); }
		
		if(Bs == null) Bs = new byte[0];
		else if(PaddingCount != 0){
			// Cut the padding
			byte[] NBs = new byte[Bs.length - PaddingCount];
			System.arraycopy(Bs, 0, NBs, 0, NBs.length);
			Bs = NBs;
		}
			
		return Bs;
	}
	
	/** Returns a HashValue of a Text */
	static public String GetHashText(String Text) {
		if(Text == null) return null;
		int H = 0;
			
		for(int i = 0; i < Text.length(); i++)
			H += Text.charAt(i);
			
		if(H < 1) H = -H;
		return String.format("%8X", H);
	}
	
	// EXECUTABLE INFO -------------------------------------------------------------------------------------------------
	
	/** Regular expression to extract the signature */
	static public final String SignaturePattternStr = 
		"\\{[ \t]*"+
		"([a-zA-Z0-9$_][a-zA-Z0-9$_]*)[ \t]*"+
		"("+
			"\\("+
				"("+
					"[ \t]*"+
					"[a-zA-Z0-9$_][a-zA-Z0-9$_]*+" +
					"[ \t]*:[ \t]*"+
					"[a-zA-Z0-9$_][a-zA-Z0-9$_]*([ \t]*\\.[ \t]*[a-zA-Z0-9$_][a-zA-Z0-9$_]*)*"+
					"[ \t]*"+
					"(\\[[ \t]*\\][ \t]*)*"+
					"("+
						"("+
							",[ \t]*"+
							"[a-zA-Z0-9$_][a-zA-Z0-9$_]*+" +
							"[ \t]*:[ \t]*"+
							"[a-zA-Z0-9$_][a-zA-Z0-9$_]*([ \t]*\\.[ \t]*[a-zA-Z0-9$_][a-zA-Z0-9$_]*)*" +
							"[ \t]*"+
							"(\\[[ \t]*\\][ \t]*)*"+
						")*"+
						"(\\.\\.\\.[ \t]*)?"+
					")?"+
				")?"+
			"\\)"+
			"[ \t]*"+
			":"+
			"[ \t]*"+
			"[a-zA-Z0-9$_][a-zA-Z0-9$_]*([ \t]*\\.[ \t]*[a-zA-Z0-9$_][a-zA-Z0-9$_]*)*"+
			"[ \t]*"+
		")?"+
		"[ \t]*"+
		"\\}";
	
	static public final Pattern SignaturePatttern = Pattern.compile(SignaturePattternStr);

	/** Returns the signature */
	static public Signature ParseSignature(String SignStr) throws ClassNotFoundException {
		return (Signature)ParseSignatureORExecutableInfo(null, null, SignStr, false);
	}

	/** Returns the signature */
	static public Signature ParseSignature(String pName, String SignStr) throws ClassNotFoundException {
		return (Signature)ParseSignatureORExecutableInfo(null, pName, SignStr, false);
	}

	/** Returns the signature */
	static public Signature ParseSignature(String SignStr, boolean NoException) throws ClassNotFoundException {
		return (Signature)ParseSignatureORExecutableInfo(null, null, SignStr, NoException);
	}

	/** Returns the signature */
	static public Signature ParseSignature(String pName, String SignStr, boolean NoException) throws ClassNotFoundException {
		return (Signature)ParseSignatureORExecutableInfo(null, pName, SignStr, NoException);
	}

	/**
	 * Returns the signature or ExecInfo.
	 * If pKind is given, this method will reutrn ExecutionInfo; otherwise, a Signature will be returned.
	 **/
	static private Object ParseSignatureORExecutableInfo(String pKind, String pName, String SignStr, boolean NoException)
	                        throws ClassNotFoundException {
		if((SignStr == null) || (SignStr.length() == 0)) return null;
		
		String[] ParamsAndReturn = SignStr.split("\\)[ \t]*:");
		if((ParamsAndReturn == null) || (ParamsAndReturn.length == 0)) return null;
		
		String   ParamList = ParamsAndReturn[0].trim().substring(1);	// Eliminate the open bracket
		String   Return    = ParamsAndReturn[1].trim();
		String[] Params    = ParamList.split("[ \t]*,[ \t]*");
		
		if(pName == null) pName = "";
		Signature S = null;
		
		try {
			Class<?>   ReturnType = Utils.getClass(Return);
			Class<?>[] ParamTypes = null;
			boolean    IsVarArgs  = false;
			
			if((Params == null) || (Params.length <= 1) && ("".equals(Params[0]))) {
				Params     = new String[0];
				ParamTypes = new Class<?>[0];
				
			} else {
				ParamTypes = new Class<?>[Params.length];
				
				for(int i = 0; i < ((ParamTypes == null) ? 0: ParamTypes.length); i++) {
					if("".equals(Params[i])) continue;
					
					String[] P = Params[i].split("[ \t]*:[ \t]*");
					if(P.length != 2)
						continue;
					
					String Name = P[0];
					String Type = P[1];
					
					if(i == (Params.length - 1)) {
						IsVarArgs = Type.endsWith("...");
						if(IsVarArgs) Type = Type.substring(0, Type.length() - "...".length()).trim();
					}
					
					// Reuse for Name
					Params[i]     = Name;
					ParamTypes[i] = Utils.getClass(Type);
				}
			}
		
			S = new Signature.Simple(pName, ReturnType, IsVarArgs, ParamTypes);
		} catch (ClassNotFoundException E) {
			if(!NoException) throw E;
		} catch (IllegalArgumentException E) {
			if(!NoException) throw E;
		}
		
		if(pKind == null) return S;
		return new ExecutableInfo(pName, pKind, S, SignStr, Params);
	}

	/** Returns the signature of the given code */
	static public ExecutableInfo ParseExecutableInfoFromCode(String Name, String Code) throws ClassNotFoundException {
		return ParseExecutableInfoFromCode(Name, Code, false);
	}

	/** Returns the signature of the given code */
	static public ExecutableInfo ParseExecutableInfoFromCode(String Name, String Code, boolean NoException)
	                throws ClassNotFoundException {
		if(Code == null) return null;

		Matcher M = SignaturePatttern.matcher(Code);
		if(!M.find(ScriptManager.GetEndOfIgnored(Code))) return null;
		
		String Kind = M.group(1);
		String Sign = M.group(2);
		
		if(Kind == null) Kind = "";
		
		ExecutableInfo ExecInfo;
		if(Sign != null)
			 ExecInfo = (ExecutableInfo)ParseSignatureORExecutableInfo(Kind, Name, Sign, NoException);
		else ExecInfo = new ExecutableInfo(Name, Kind, null, "");
		
		return ExecInfo;
	}
	
	// COMPILE EXECUTABLE ----------------------------------------------------------------------------------------------

	/** Compile a peice of code into an executable. */
	static public Executable CompileExecutable(InputStream IS, CompileOption pOption, ProblemContainer pResult)
					throws IOException{
		return CompileExecutable(Utils.loadTextFromStream(IS), null, pOption, pResult);
	}

	/** Compile a peice of code into an executable. */
	static public Executable CompileExecutable(InputStream IS, String pName, CompileOption pOption, ProblemContainer pResult)
					throws IOException{
		return CompileExecutable(Utils.loadTextFromStream(IS), pName, pOption, pResult);
	}

	/** Compile a peice of code into an executable. */
	static public Executable CompileExecutable(String Name, String Text, CompileOption pOption, ProblemContainer pResult) {
		ScriptEngine SE = ScriptManager.GetEngineFromCode(Text);
		
		// The engine is not found
		if(SE == null)
			throw new IllegalArgumentException("The stream does not contain a code.");
		
		ExecutableInfo ExecInfo = null;
		try { ExecInfo = ParseExecutableInfoFromCode(Name, Text); }
		catch (ClassNotFoundException E) {
			throw new IllegalArgumentException("Error extracting executable information: " + E);
		}
		
		// Set the default one
		if(ExecInfo == null) {
			ExecutableInfo DEI = new ExecutableInfo(Name, null, null, null);
			ExecInfo = SE.getReplaceExecutableInfo(DEI); //Name);
			
			if((ExecInfo == DEI) || (ExecInfo == null)) {
				DEI      = ExecutableInfo.DefaultExecutableInfo;
				ExecInfo = new ExecutableInfo(
								Name,
								DEI.Kind,
								DEI.Signature,
								DEI.SignatureText,
								DEI.getParameterNames()
							);
			}
			
		} else {
			if(ExecInfo.Signature == null)
				ExecInfo = ExecutableInfo.getMainExecutableInfo(Name);
		}
		
		return SE.compileExecutable(ExecInfo, Text, pOption, pResult);
	}
	
	// COMPILE and COMBINE ---------------------------------------------------------------------------------------------
	
	/** Reads a SimpleScript file and compile it. */
	static public String CompileExecutableToCompiledText(String Name, String Text, CompileOption pOption,
			ProblemContainer pResult) {

		ScriptEngine SE = ScriptManager.GetEngineFromCode(Text);
		if(SE == null) return null;
		
		Executable Exec = CompileExecutable(Text, Name, pOption, pResult);
		if(Exec == null) return null;
		
		return MergeCodeAndCompiledExecutable(Text, Exec);
	}
	
	/** Merge the text and the compiled executable into a string. */
	static public String MergeCodeAndCompiledExecutable(String Text, Executable Exec) {
		if(Exec == null) return null;

		// Ensure 3 empty lines before the compiled code data
		String Last3Chars = Text.substring(Text.length() - 3);
		int NLChar;
		for(NLChar = 3; (--NLChar >= 0) && (Last3Chars.charAt(NLChar) == '\n'); );
		if(NLChar >= 0) Text += "\n\n\n".substring(3 - NLChar - 1);

		StringBuilder SB = new StringBuilder();
		SB.append(Text);
		
		// The code is not compilable so no-point of saving it compiled code.
		if((Exec.getEngine() != null) && !Exec.getEngine().isCompilable())
			return SB.toString();
	
		String Hash = GetHashText(Text);
		
		ByteArrayOutputStream BAOS  = null;
		ObjectOutputStream    OOS   = null;
		ByteArrayOutputStream EBAOS = null;
		ObjectOutputStream    EOOS  = null;
		try {
			BAOS = new ByteArrayOutputStream();
			OOS  = new ObjectOutputStream(BAOS);
			
			ScriptEngine SE     = Exec.getEngine();
			String       EName  = SE.getName();
			String       EParam = SE.getParameterString();
			if(EParam == null) EParam = "";
			
			OOS.writeUTF(EName);
			OOS.writeUTF(EParam);
			
			// Engine specific OutputStream
			EBAOS = new ByteArrayOutputStream();
			EOOS  = SE.newExecutableObjectOutputStream(EBAOS);
			if(EOOS == null) EOOS = JavaCompilerObjectOutputStream.NewJavaCompilerObjectOutputStream(EBAOS);
			
			EOOS.writeObject(Exec);		// Add the serialized value of the Executable
			EOOS.writeObject(Hash);		// Add the serialized value of the has value
			
			EOOS.flush();
			EOOS.close();
			EOOS = null;
			
			byte[] Bs = EBAOS.toByteArray();
			OOS.writeObject(Bs);

			EBAOS.close();
			EBAOS = null;
			
		} catch (IOException E) {
		} finally {
			try { if(EBAOS != null) EBAOS.close(); } catch (IOException E) {};
			try { if(EOOS  != null) EOOS .close(); } catch (IOException E) {};
			try { if(OOS   != null) OOS  .close(); } catch (IOException E) {};
			try { if(BAOS  != null) BAOS .close(); } catch (IOException E) {};
		}

		// Compile Code Hash and Length
		StringBuilder Compiled = new StringBuilder();
		Compiled.append("##:COMPILED::BASE64 {\n");
		Compiled.append(base64Encode(BAOS.toByteArray()));
		Compiled.append("\n}");
		
		String CompiledStr = Compiled.toString();
		Compiled.append("\n");
		
		// HASH 1 is the hash of the Compiled Text used just in case we really needed to be sure that no altered is done
		// HASH 2 is the hash of the Original Code used to check if the code has been edited (so recompiled is needed).
		Compiled.append(String.format("##:HASHES:0x%s-0x%s;\n", Hash,          GetHashText(CompiledStr)));
		Compiled.append(String.format("##:COUNTS:0x%8X-0x%8X;", Text.length(), CompiledStr.length() + 1));	// 1 for '\n' <Tools:415>
		
		// Merge
		SB.append("\n").append(Exec.getEngine().getLongComments(Compiled.toString(), LONG_COMMENT_LINE_WIDTH));
		
		return SB.toString();
	}
	
	// EXTRACT EXECUTABLE - Read and get the executable (recompiled if specited or forced) -----------------------------

	/** Compiled-executale extracted result */
	static public class ExtractResult{
		public ExtractResult(Executable pExec, String pCode, boolean pIsUpdated, boolean pIsAltered) {
			this.Executable = pExec;
			this.Code       = pCode;
			this.IsAltered  = pIsAltered;
			this.IsUpdated  = pIsUpdated;
		}
		
		public final Executable Executable;
		public final String     Code;
		public final boolean    IsUpdated;	// The code is updated
		public final boolean    IsAltered;	// The compiled text is alerted
	}
	
	/** Reads a SimpleScript file and compile it. */
	static public ExtractResult ExtractExecutableFromCompiledText(String Name, String Text)
	                               throws ClassNotFoundException, IOException {
		return ExtractExecutableFromCompiledText(Name, Text, null, null, false, true, false);
	}
	
	/** Reads a SimpleScript file and compile it. */
	static public ExtractResult ExtractExecutableFromCompiledText(String Name, String Text, CompileOption pOption,
			ProblemContainer pResult, boolean IsOnlyNeedExecutable, boolean IsToRecompiledIfNeeded,
			boolean IsForceReCompiled) throws ClassNotFoundException, IOException {
		
		if(Text == null) return null;

		boolean    IsUpdated = false;
		boolean    IsAltered = false;
		Executable Exec      =  null;
		String     Code      =  Text;
		
		// Count must be the list before the last line
		int LIndex = Text.lastIndexOf("\n##:COUNTS:");
		if(LIndex == -1) {
			if(!IsToRecompiledIfNeeded && IsForceReCompiled)
				return new ExtractResult(null, Text, true, true);
			
			IsUpdated = true;
			IsAltered = true;
		} else {
		
			int I;
			if(((I = Text.indexOf('\n', LIndex))        == -1) ||
			   ((I = Text.indexOf('\n', I + 1)) == -1) ||
			   ((I = Text.indexOf('\n', I + 1)) != -1)) {
				throw new IllegalArgumentException(MAL_FORMED_COMPILED_CODE_EXCEPTION_MESSAGE);
			}
			
			// Gets both lengths
			String L1 = Text.substring(LIndex + 13, LIndex + 21);
			String L2 = Text.substring(LIndex + 24, LIndex + 32);
			int Length_Code;
			int Length_CStr;
			try {
				Length_Code = Integer.parseInt(L1.trim(), 16);
				Length_CStr = Integer.parseInt(L2.trim(), 16);
			} catch (NumberFormatException NFE) {
				throw new IllegalArgumentException(MAL_FORMED_COMPILED_CODE_EXCEPTION_MESSAGE);
			}
			
			if(Length_Code >= Text.length())
				throw new IllegalArgumentException(MAL_FORMED_COMPILED_CODE_EXCEPTION_MESSAGE);
	
			if(Length_CStr >= Text.length())
				throw new IllegalArgumentException(MAL_FORMED_COMPILED_CODE_EXCEPTION_MESSAGE);
			
			int HIndex = Text.lastIndexOf('\n', LIndex - 1);
			if(HIndex == -1)
				throw new IllegalArgumentException(MAL_FORMED_COMPILED_CODE_EXCEPTION_MESSAGE);
			
			// Get the Compiled code section
			String CCode;
			
			try {		
				Code  = Text.substring(0, Length_Code);
				CCode = Text.substring(HIndex - Length_CStr + 1, HIndex);	// One for an extract '\n'
			} catch (StringIndexOutOfBoundsException e) {
				throw new IllegalArgumentException(MAL_FORMED_COMPILED_CODE_EXCEPTION_MESSAGE);
			}
			
			// Get the hashes and check them
			String HASH_Code = Text.substring(HIndex + 13, HIndex + 21);
			String Hash_CStr = Text.substring(HIndex + 24, HIndex + 32);
			IsUpdated = !HASH_Code.equals(GetHashText(Code));
			IsAltered = !Hash_CStr.equals(GetHashText(CCode));

			// If the hashes are alright, we load the executable
			if(!IsUpdated && !IsAltered) {
				String CCode_BASE64 = CCode.substring(22, CCode.length() - 2);
				
				ByteArrayInputStream BAIS  = null;
				ObjectInputStream    OIS   = null;
				ByteArrayInputStream EBAIS = null;
				ObjectInputStream    EOIS  = null;
				
				try {
					BAIS = new ByteArrayInputStream(Base64Decode(CCode_BASE64));
					OIS  = new ObjectInputStream(BAIS);
					
					String EngineName  = OIS.readUTF();
					String EngineParam = OIS.readUTF();
					
					byte[] Bs = (byte[])OIS.readObject();
					EBAIS = new ByteArrayInputStream(Bs);
					
					ScriptEngine SE = ScriptManager.GetEngineFromCode(String.format("// @%s(%s):", EngineName, EngineParam));
					EOIS = SE.newExecutableObjectInputStream(EBAIS);
					if(EOIS == null) EOIS = JavaCompilerObjectInputStream.NewJavaCompilerObjectInputStream(EBAIS);
					
					// Extract the object
					Exec            = (Executable)EOIS.readObject();
					String TextHash = (String)    EOIS.readObject();

					// Check the hash using the hash value inside the compiled code
					IsAltered = !TextHash.equals(HASH_Code);	
				} catch (Exception E) {
					IsAltered = true;
				} finally {
					// Close the stream
					if(EBAIS != null) try { EBAIS.close(); } catch (Exception E) { }
					if(EOIS  != null) try { EOIS .close(); } catch (Exception E) { }
					if(OIS   != null) try { OIS  .close(); } catch (Exception E) { }
					if(BAIS  != null) try { BAIS .close(); } catch (Exception E) { }
				}
			}
			
			// Get the code to be re-compiled
			if(IsUpdated || IsAltered || IsForceReCompiled) {
				// Use the hold Text as the Leng information is out of date (so the code got from it may be wrong)
				int EOCIndex = Text.lastIndexOf('\n', HIndex - Length_CStr + 1 - LONG_COMMENT_LINE_WIDTH + 2); 
				Code = Text.substring(0, EOCIndex);	                      // ^ is for '\n' after CCode
				                                              // This is one is for the last index '\n' ---^
			}
		}
		
		// RE-Compiled
		if(((IsUpdated || IsAltered) && IsToRecompiledIfNeeded) || IsForceReCompiled)
			Exec = CompileExecutable(Name, Code, pOption, pResult);
		
		// Force recompiled so it will be up-to-date
		if(IsForceReCompiled) {
			IsUpdated = false;
			IsAltered = false;
		}
				
		// Returns
		boolean IsToReturnCode = !IsOnlyNeedExecutable || (IsUpdated || IsAltered);
		return new ExtractResult(Exec, IsToReturnCode ? Code : null, IsUpdated, IsAltered);
	}
	
	// COMPILE AND SAVE - Compile the text and save to the file -------------------------------------------------------- 

	/** Reads a SimpleScript file and compile it. */
	static public Executable CompileAndSave(String Text, String TheTargetFileName, CompileOption pOption,
			ProblemContainer pResult) throws ClassNotFoundException, IOException {
		return CompileAndSave(Text, new File(TheTargetFileName), pOption, pResult);
	}
	
	/** Reads a SimpleScript file and compile it. */
	static public Executable CompileAndSave(String Text, File TheTargetFile, CompileOption pOption,
			ProblemContainer pResult) throws ClassNotFoundException, IOException {
		
		// Unable to create the file.
		if((TheTargetFile.exists() && !TheTargetFile.canWrite()) ||
		   (!TheTargetFile.exists() && !TheTargetFile.getParentFile().canWrite()))
			throw new IOException("Unable to create the file: " + TheTargetFile);
		
		byte[] Bytes = null;
		
		ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
		Executable Exec = CompileAndSave(Text, TheTargetFile.getName(), BAOS, pOption, pResult);
		if(Exec == null) return null;
			
		Bytes = BAOS.toByteArray();
		BAOS.close();
		
		FileOutputStream FOS = null;
		try {
			FOS = new FileOutputStream(TheTargetFile);
			FOS.write(Bytes);
		} finally {
			if(FOS != null) FOS.close();
		}
		
		return Exec;
	}
	
	/** Reads a SimpleScript file and compile it. */
	static public Executable CompileAndSave(String Text, String Name, OutputStream OS, CompileOption pOption,
			ProblemContainer pResult) throws ClassNotFoundException, IOException {
		
		if(OS == null)
			throw new NullPointerException();
		
		Executable Exec = CompileExecutable(Name, Text, pOption, pResult);
		if(Exec == null) return null;
		
		String CompiledCode = MergeCodeAndCompiledExecutable(Text, Exec);
		if(CompiledCode == null) return null;
		
		Utils.saveTextToStream(CompiledCode, OS);
		
		return Exec;
	}
	
	// USE - Load, recompiled if needed (or forced) and save -----------------------------------------------------------
	
	/** Try to load the file and execute it. If the loading found that there is an updated, compile and save it first. */
	static public Executable Use(String FileName) throws FileNotFoundException, IOException, ClassNotFoundException {
		return Use(new File(FileName), false);
	}
	
	/** Try to load the file and execute it. If the loading found that there is an updated, compile and save it first. */
	static public Executable Use(File File) throws FileNotFoundException, IOException, ClassNotFoundException {
		return Use(File, false);
	}
	
	/** Try to load the file and execute it. If the loading found that there is an updated, compile and save it first. */
	static public Executable Use(String FileName, boolean IsForceRecompile)
					throws FileNotFoundException, IOException, ClassNotFoundException {
		return Use(new File(FileName), IsForceRecompile);
	}
	
	/** Try to load the file and execute it. If the loading found that there is an updated, compile and save it first. */
	static public Executable Use(File File, boolean IsForceRecompile)
					throws FileNotFoundException, IOException, ClassNotFoundException {
		// Early returns
		if(File == null) throw new NullPointerException();
		
		if((File == null) || !File.exists())
			throw new FileNotFoundException((File == null)?null:File.getAbsolutePath());
	
		String        Text    = Utils.loadTextFromStream(new FileInputStream(File));
		ExtractResult EResult = ExtractExecutableFromCompiledText(File.getName(), Text, null, null, false, false,
		                           IsForceRecompile);
		if(EResult == null) return null;
		
		if(EResult.IsUpdated || EResult.IsAltered)
			return CompileAndSave(EResult.Code, File, null, null);
		
		return EResult.Executable;
	}

	/** Run the file */
	static public Object Run(String FileName, Object ... Params) {
		File F = new File(FileName);
		if(!F.exists()) throw new RuntimeException("Script not found: " + FileName);

		if(Params == null) Params = new Object[0];
		
		Executable Exec   = null;
		try { Exec = Use(F, false); }
		catch (Exception E) { throw new RuntimeException(E); }

		if(Exec instanceof Script)   return ((Script)  Exec).run();
		if(Exec instanceof Macro)    return ((Macro)   Exec).run((Object[])Params);
		if(Exec instanceof Function) return ((Function)Exec).run((Object[])Params);
		
		throw new RuntimeException("Incomparible parameters: " + Arrays.toString(Params));
	}
	
	// Test ------------------------------------------------------------------------------------------------------------
	
	static public void main(String ... Args) throws ClassNotFoundException, IOException {
		/* */
		Function F = (Function)Use("test.ss");
		
		F.run();
		/* */
		
		System.out.println("END");
	}
}
