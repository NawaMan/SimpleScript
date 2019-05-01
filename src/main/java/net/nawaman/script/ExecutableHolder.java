package net.nawaman.script;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import net.nawaman.script.Tools.ExtractResult;
import net.nawaman.usepath.USFile;
import net.nawaman.usepath.UsableFactory;
import net.nawaman.usepath.UsableHolder;
import net.nawaman.usepath.UsableStorage;
import net.nawaman.usepath.Usepath;

/** Hold an executable */
public class ExecutableHolder extends UsableHolder<Executable> {
	
	static public class UFExecutable implements UsableFactory {
		
		/** Returns the name of the Factory */
		public String getName() {
			return "ExecutableHolderFactory";
		}
		
		/** Create the UsableHolder from the given paramters */
		public UsableHolder<? extends Object> getUsableHolder(Usepath Path, UsableStorage Storage) {
			return new ExecutableHolder(Path, Storage);
		}
		
	}
	
	ExecutableHolder(Usepath pUsepath, UsableStorage pStorage) {
		super(pUsepath, pStorage.getName());
		
		if(pStorage == null)
			throw new NullPointerException();
		
		this.Storage = pStorage;
	}
	
	UsableStorage Storage;
	
	Executable Exec = null;
	
	/** Force the executable to be reloaded */
	protected void forceReloaded() {
		this.Exec = null;
	}
	
	/**{@inheritDoc}*/ @Override
	public Executable get() {
		// Checks for update at all time
		if(this.Exec != null) return this.Exec;
		
		try {
			// Use it
			if(this.Storage instanceof USFile) {
				this.Exec = Tools.Use(((USFile)this.Storage).getTheFile());
				return this.Exec;
			}
			
			// Load the Code
			String Text = Utils.loadTextFromStream(new ByteArrayInputStream(this.Storage.load()));
			
			boolean isWritable = this.Storage.isWritable();
			
			// Re-compiled			
			ExtractResult EResult = Tools.ExtractExecutableFromCompiledText(this.Storage.getName(), Text, null, null, !isWritable, true, false);
			if(EResult.IsUpdated || EResult.IsAltered) {
				if(isWritable) {
					// Save the compile result back
					String CCode = Tools.MergeCodeAndCompiledExecutable(EResult.Code, EResult.Executable);
					
					ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
					Utils.saveTextToStream(CCode, BAOS);
					this.Storage.save(BAOS.toByteArray());
					BAOS.close();
					
				} else {
					System.err.println("The executable `"+this.getName()+"` is out-of-date or " +
							"improperly altered. SimpleScript can manage to recompile it but unable to write the result back.");
				}
			}
					
			this.Exec = EResult.Executable;
			
		} catch (RuntimeException RE) {
			throw RE;
			
		} catch (Throwable E) {
			throw new RuntimeException("Getting object fail with an exception", E);
			
		}
		
		return this.Exec;
	}

}
