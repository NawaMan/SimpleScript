package net.nawaman.script;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import net.nawaman.script.Tools.ExtractResult;
import net.nawaman.usepath.UsableStorageFile;
import net.nawaman.usepath.UsableFactory;
import net.nawaman.usepath.UsableHolder;
import net.nawaman.usepath.UsableStorage;
import net.nawaman.usepath.UsePath;

/** Hold an executable */
public class ExecutableHolder extends UsableHolder<Executable> {
	
	static public class UFExecutable implements UsableFactory {
		
		/** Returns the name of the Factory */
		public String name() {
			return "ExecutableHolderFactory";
		}
		
		/** Create the UsableHolder from the given paramters */
		@SuppressWarnings("unchecked")
		public UsableHolder<Executable> getUsableHolder(UsePath Path, UsableStorage Storage) {
			return new ExecutableHolder(Path, Storage);
		}
		
	}
	
	ExecutableHolder(UsePath pUsepath, UsableStorage pStorage) {
		super(pUsepath, pStorage.name());
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
			if(this.Storage instanceof UsableStorageFile) {
				this.Exec = Tools.Use(((UsableStorageFile)this.Storage).file());
				return this.Exec;
			}
			
			// Load the Code
			String Text = Utils.loadTextFromStream(new ByteArrayInputStream(this.Storage.load()));
			
			boolean isWritable = this.Storage.isWritable();
			
			// Re-compiled			
			ExtractResult EResult = Tools.ExtractExecutableFromCompiledText(this.Storage.name(), Text, null, null, !isWritable, true, false);
			if(EResult.IsUpdated || EResult.IsAltered) {
				if(isWritable) {
					// Save the compile result back
					String CCode = Tools.MergeCodeAndCompiledExecutable(EResult.Code, EResult.Executable);
					
					ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
					Utils.saveTextToStream(CCode, BAOS);
					this.Storage.save(BAOS.toByteArray());
					BAOS.close();
					
				} else {
					System.err.println("The executable `"+this.objectName()+"` is out-of-date or " +
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
