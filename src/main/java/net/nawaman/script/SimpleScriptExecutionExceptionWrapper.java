package net.nawaman.script;

/** Use this class to wrap around script execution so that there is no need to declare 'throw' for run()*/
public class SimpleScriptExecutionExceptionWrapper extends RuntimeException {
	
	private static final long serialVersionUID = 7388281273522717880L;

	public SimpleScriptExecutionExceptionWrapper(Throwable pCause) {
        super("An exception was thrown while executing a script.", pCause);
    }

}
