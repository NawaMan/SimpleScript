package net.nawaman.script.java;

import java.util.Vector;

import net.nawaman.script.CompileOption;
import net.nawaman.script.Signature;

/** Java Engine */
public class JavaCompileOption implements CompileOption {
	
	static JavaCompileOption DefaultOption = new JavaCompileOption(); 
	
	/** Create a Java script option */
	public JavaCompileOption() {}
	
	// Class name prefix -----------------------------------------------------------------------------------------------

	String ClassNamePrefix = "JC";
	
	/** Set the class name prefix */
	public void setClassNamePrefix(String pClassNamePrefix) {
		if((pClassNamePrefix == null) || (pClassNamePrefix.length() == 0)) return;
		this.ClassNamePrefix = pClassNamePrefix;
	}
	/** Get the class name prefix */
	public String getClassNamePrefix() {
		return this.ClassNamePrefix;
	}
	
	// Super class -----------------------------------------------------------------------------------------------------

	Class<?> SuperClass = null;
	
	/** Set the class name prefix */
	public void setSuperClass(Class<?> pSuperClass) {
		this.SuperClass = pSuperClass;
	}
	/** Get the class name prefix */
	public Class<?> getSuperClass() {
		return this.SuperClass;
	}
	
	// ToSaveCode ------------------------------------------------------------------------------------------------------
	
	boolean IsToSeveCode = false;
	
	public void setToSaveCode(boolean pIsToSeveCode) {
		this.IsToSeveCode = pIsToSeveCode;		
	}
	
	public boolean isToSaveCode() {
		return this.IsToSeveCode;
	}
	
	// Imports ---------------------------------------------------------------------------------------------------------
	
	Vector<String> DefaultImports    = null;
	String         DefaultImportsStr = null;
	
	/** Add default import name - Returns if the import name is valid. **/
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
}
