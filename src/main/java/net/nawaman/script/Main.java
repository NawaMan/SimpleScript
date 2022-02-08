package net.nawaman.script;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/** The main execution of SimpleScript*/
public class Main {
	
	static String USAGE = null;
	
	static public final int NO_SHUTDOWN_CODE = -1025;

	/** Show the usage text */
	static public void ShowUsage() {
		ShowUsage(null, null, NO_SHUTDOWN_CODE);
	}
	/** Show the usage text */
	static public void ShowUsage(String Prefix, int ShutdownCode) {
		ShowUsage(Prefix, null, ShutdownCode);
	}
	/** Show the usage text */
	static public void ShowUsage(String Prefix, String Suffix) {
		ShowUsage(Prefix, Suffix, NO_SHUTDOWN_CODE);
	}
	/** Show the usage text */
	static public void ShowUsage(String Prefix, String Suffix, int ShutdownCode) {
		if(USAGE == null) {
			try                 { USAGE = Utils.loadTextFromStream(new FileInputStream(new File("./USAGE"))); }
			catch (Exception E) { System.err.print("Fail to load 'USAGE' file. Consult: http://blog.nawaman.net/"); }
		}
		
		if(Prefix != null)                   System.out.println(Prefix);
		                                     System.out.println(USAGE);
		if(Suffix != null)                   System.out.println(Suffix);
		if(ShutdownCode != NO_SHUTDOWN_CODE) System.exit(ShutdownCode);
	}
	
	static public void main(String ... $Args) throws ClassNotFoundException, FileNotFoundException, IOException {
		//$Args = new String[] { "--lang", "Java", "--command", "System.out.println(\"Hello World!\");" };
		//$Args = new String[] { "--lang", "Java", "--command", "import java.io.*; System.out.println((new File(\".\")).getAbsolutePath());" };
		//$Args = new String[] { "--lang", "JavaScipt", "--command", "5 + 10;" };
		//$Args = new String[] { "--command", "/* @Java: */ return 5 + 10;" };
		//$Args = new String[] { "--command", "// @Java:"$'\n'"System.out.println(\"Hello\");" };

		// $Args = new String[] { "--run", "test" };
		
		// Show usage
		if(($Args == null) || ($Args.length == 0))
			ShowUsage("Please tell me something ...\n", 0);
		
		// System.out.println("$Args: " + Arrays.toString($Args));
			
		List<String> Args = Arrays.asList($Args);	// ss, sss, ssm, ssf

		// Show usage
		if(Args.contains("--help") || Args.contains("-h"))
			ShowUsage(null, null, 0);
		
		ScriptEngine Engine = null;
		
		int Index;
		if((Index = Args.indexOf("--lang")) != -1) {
			Index++;
			if(Index >= Args.size())
				ShowUsage("Missing the language name.\n", -1);
			
			String LangName = Args.get(Index);
			Engine = ScriptManager.Instance.getDefaultEngineOf(LangName);

			if((LangName != null) && (Engine == null))
				ShowUsage("Unknown language `"+LangName+"`.\n", -1);
		}

		boolean IsToRun = false;
		if((Index = Args.indexOf("--command")) != -1) {
			Index++;
			if(Index >= Args.size())
				ShowUsage("Missing the command code.\n", -1);
			
			String   Command = Args.get(Index);
			String[] EInfo   = ScriptManager.getEngineNameAndParamFromCode(Command);
			if((EInfo != null) && (EInfo[0] != null)) {
				ScriptEngine CEngine = ScriptManager.GetEngineFromCode(Command);	// Engine extracted from Command
				if(CEngine == null)
					ShowUsage("Unknown language specified in the code "+EInfo[0]+".\n", -1);
				
				if(Engine != null) { 
					if((CEngine != null) && (CEngine != Engine))
						ShowUsage("Multiple language name specified.\n", -1);
					
				} else {
					Engine = CEngine;
				}	
			}

			


			if(Engine == null)
				ShowUsage("No language is specified\n", -1);
			
			String[] Params = new String[$Args.length - (Index + 1)];
			if(Params.length != 0)
				System.arraycopy($Args, 0, Params, 0, Params.length);
				
			Scope MainScope = Engine.newScope();
			if(MainScope.isConstantSupport()) MainScope.newConstant(ScriptManager.$MainArgs, String[].class, Params);
			else                              MainScope.newVariable(ScriptManager.$MainArgs, String[].class, Params);
			
			System.out.print(Engine.eval(Command, MainScope, null));
			return;
			
		} else if((IsToRun = ((Index = Args.indexOf("--run")) != -1)) || ((Index = Args.indexOf("--compile")) != -1)) {
			Index++;
			if(Index >= Args.size())
				ShowUsage("Missing the executable name.\n", -1);
			
			String     ExecName = Args.get(Index);
			Executable Exec     = ScriptManager.UseWithException(ExecName);
			if(Exec == null)
				System.err.println("The executable is not found (\""+ExecName+"\").");
			
			// Not run just compiled
			if(!IsToRun) return;
			
			String[] NewArgs = new String[$Args.length - (Index + 1)];
			if(NewArgs.length != 0)
				System.arraycopy($Args, (Index + 1), NewArgs, 0, NewArgs.length);
				
			Scope MainScope = Exec.getEngine().newScope();
			if(MainScope.isConstantSupport()) MainScope.newConstant(ScriptManager.$MainArgs, String[].class, NewArgs);
			else                              MainScope.newVariable(ScriptManager.$MainArgs, String[].class, NewArgs);
			
			// Gets the signature
			Signature S = null;
			if     (Exec instanceof Function) S = ((Function)Exec).getSignature();
			else if(Exec instanceof    Macro) S = ((Macro)   Exec).getSignature();
			else {
				// Execute as sctipt
				System.out.print(((Script)Exec).run(MainScope));
				return;
			}
			
			// Prepare parameters
			Object[] Params = (S.getParamCount() == 0) ? Signature.EmptyStringArray : new Object[S.getParamCount()];
			
			// Main
			if((S.getParamCount() == 1) && S.isVarArgs() && (S.getParamType(0) == String.class))
				Params[0] = NewArgs;

			// Execute as Fuction or Macro
			if(Exec instanceof Function)
				 System.out.print(((Function)Exec).run(           (Object[])Params));
			else System.out.print(((Macro)   Exec).run(MainScope, (Object[])Params));
			
			return;
		}
		
		// Show usage
		ShowUsage("Unkown command ...\n", 0);
	}	
}
