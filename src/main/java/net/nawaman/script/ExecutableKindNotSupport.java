package net.nawaman.script;

/** Thrown when executable kind is not supported */
public class ExecutableKindNotSupport extends RuntimeException {
    
	private static final long serialVersionUID = -7796543631184137300L;

	/** Constructs a new runtime exception with <code>null</code> as its detail message.*/
	public ExecutableKindNotSupport() {
		super();
	}

	/** Constructs a new runtime exception with the specified detail message. */
	public ExecutableKindNotSupport(String pMessage) {
		super(pMessage);
	}

	/** Constructs a new runtime exception with the specified detail message and cause. */
	public ExecutableKindNotSupport(String pMessage, Throwable pCause) {
		super(pMessage, pCause);
	}

	/**
	 * Constructs a new runtime exception with the specified cause and a detail message of
	 *   <tt>(cause==null ? null : cause.toString())</tt> (which typically contains the class and detail message of
     *   <tt>cause</tt>).
     **/
	public ExecutableKindNotSupport(Throwable pCause) {
		super(pCause);
	}
}
