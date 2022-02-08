package net.nawaman.script;

import java.io.File;
import java.util.HashSet;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import net.nawaman.usepath.FileExtFilter;
import net.nawaman.usepath.FileExtUsableFilter;
import net.nawaman.usepath.UsableFilter;
import net.nawaman.usepath.UsePathFileInFolder;

public class Usepaths extends net.nawaman.usepath.AppendableUsepaths {

	/** Filter only the ss[a-zA-Z] files */
	static public final SSFileFilter SSFILEFILTER = new SSFileFilter();
	
	/** Automatically used (if exist in the working folder or any used folder) */
	static public final String AUTO_USED_FOLDER_USES    = "uses";
	static public final String AUTO_USED_FOLDER_LIBS    = "libs";
	static public final String AUTO_USED_FOLDER_SSUSES  = "ssuses";
	static public final String AUTO_USED_FOLDER_SSLIBS  = "sslibs";
	static public final String AUTO_USED_FOLDER_SS_USES = "ss_uses";
	static public final String AUTO_USED_FOLDER_SS_LIBS = "ss_libs";
	
	static HashSet<String> AUTO_USED_FOLDERS = new HashSet<String>();
	static {
		AUTO_USED_FOLDERS.add(AUTO_USED_FOLDER_USES);
		AUTO_USED_FOLDERS.add(AUTO_USED_FOLDER_LIBS);

		AUTO_USED_FOLDERS.add(AUTO_USED_FOLDER_SSUSES);
		AUTO_USED_FOLDERS.add(AUTO_USED_FOLDER_SSLIBS);

		AUTO_USED_FOLDERS.add(AUTO_USED_FOLDER_SS_USES);
		AUTO_USED_FOLDERS.add(AUTO_USED_FOLDER_SS_LIBS);
	}

	/** Filter for SSFile only */
	static public class SSFileFilter extends FileExtUsableFilter {
		public SSFileFilter() {
			super(new FileExtFilter.RegExpFileFilter(Pattern.compile("^ss[a-zA-Z_]*$")));
		}
	}
	
	
	Usepaths() {
		this.registerUsepath(".");
		this.registerAllClassPath();
		
		this.registerFactory(new ExecutableHolder.UFExecutable());
		this.registerUsableFilter(SSFILEFILTER);
	}
	
	// Discovering -----------------------------------------------------------------------------------------------------
	
	/** Registre a usepath from string */
	void registerUsepath(String UPath, boolean ToDig) {
		if((UPath == null) || (UPath.length() == 0)) return;
		
		File Folder = new File(UPath);
		if(!Folder.exists() || !Folder.isDirectory() || !Folder.canRead()) return;
		
		UsePathFileInFolder UPFIF = null;
		try { UPFIF = new UsePathFileInFolder(Folder); }
		catch (Exception e) { return; }
		
		this.registerUsepath(UPFIF.name(), UPFIF);
		
		if(!ToDig) return;
		
		// Dig to the Auto use folder
		String[] Fs = Folder.list();
		for(int i = 0; i < Fs.length; i++) {
			if(!AUTO_USED_FOLDERS.contains(Fs[i])) continue;
			this.registerUsepath(UPath + File.separator + Fs[i], false);
		}
	}
	
	/**{@inheritDoc}*/ @Override
	public void registerUsepath(String UPath) {
		this.registerUsepath(UPath, true);
	}

	// Filter ----------------------------------------------------------------------------------------------------------
	
	Vector<UsableFilter> UsableFilters = new Vector<UsableFilter>();
	
	/** Register a new UsableFilter to the Usepaths */
	void registerUsableFilter(UsableFilter ... pUFilters) {
		if((pUFilters == null) || (pUFilters.length == 0)) return;
		for(int i = 0; i < pUFilters.length; i++) {
			UsableFilter UFilter = pUFilters[i];
			if((UFilter == null) || this.UsableFilters.contains(UFilter)) continue;
			
			this.UsableFilters.add(UFilter);
		}
	}
	
	/** @return  the usable filters that this UsePath uses. */
	public Stream<UsableFilter> usableFilters() {
		return UsableFilters.stream();
	}
	
}
