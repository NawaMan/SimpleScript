package net.nawaman.script;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/** Compilation problem */
public interface Problem {
	
	/** The kind of the problem */
	static public enum Kind  {
		Note, Warning, Error, Other;
		static public Kind getKind(String pKindName) {
			if(Error  .toString().equals(pKindName)) return Error;
			if(Warning.toString().equals(pKindName)) return Warning;
			if(Note   .toString().equals(pKindName)) return Note;
			return Other;
		}
	}

	/** Used to signal that no position is available. */
	static final int NoPosition = -1;
	
	/** Used to signal that no code name is available. */
	static final String NoCodeName = "<Unknown Code Name>";
	
	/** Used to signal that no code is available. */
	static final String NoCode = "<Unknown Code>";

	/** Return the name of the code of the problem */
	public String getCodeName();

	/** Return the code of the problem */
	public String getCode();

    /** Returns the kind of the problem */
    public Problem.Kind getKind();
	
    /**  Returns a localized message for the given locale. */
    public String getMessage();
	
    /**
     * Returns the character offset from the beginning of the file associated with this diagnostic that indicates the
     * start of the problem.
     **/ 
    public int getStartPosition(); 
	
    /**
     * Returns the character offset from the beginning of the file associated with this diagnostic that indicates the
     * end position of the problem.
     **/ 
    public int getEndPosition();
	
	/** Returns the line number of the starting of the problem */
    public int getLineNumber();

    /** Returns the column number of the starting of the problem */
    public int getColumnNumber();
    
    // Subclasses ------------------------------------------------------------------------------------------------------
    
    /** Simple implementation of Problem */
    static public class Simple implements Problem {
    	
    	/** Constructs a new simple compilation problem */
    	public Simple(Kind pKind, String pCodeName, String pCode, String pMessage) {
    		this.Kind     = (pKind == null)?Problem.Kind.Other:pKind;
    		this.CodeName = (pCodeName == null)?NoCodeName:pCodeName;
    		this.Code     = (pCode     == null)?NoCode:pCode;
    		this.Message  = pMessage;
    	}

    	/** Constructs a new simple compilation problem */
    	public Simple(String pCodeName, String pCode, Exception pException) {
    		this.Kind      = Problem.Kind.Error;
    		this.CodeName  = (pCodeName == null)?NoCodeName:pCodeName;
    		this.Code      = (pCode     == null)?NoCode:pCodeName;
    		this.Exception = pException;
    		
    		ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
    		PrintStream           PS   = new PrintStream(BAOS);
    		Throwable E = pException;
    		while(E != null) {
	    		PS.println(E.getMessage());
	    		this.Exception.printStackTrace(PS);
	    		E = E.getCause();
	    		if(E != null) PS.println("Caused by: ");
    		}
    		this.Message = BAOS.toString();
    	}
    	
    	Kind      Kind      = null;
    	String    CodeName  = null;
    	String    Code      = null;
    	String    Message   = null;
    	Exception Exception = null;

    	/** Return the name of the code of the problem */
    	public String getCodeName() {
    		return this.CodeName;
    	}

    	/** Return the code of the problem */
    	public String getCode() {
    		return this.Code;
    	}

        /** Returns the kind of the problem */
        public Problem.Kind getKind() {
        	return this.Kind;
        }
    	
        /**  Returns a localized message for the given locale. */
        public String getMessage() {
        	return this.Message;
        }
    	
        /**
         * Returns the character offset from the beginning of the file associated with this diagnostic that indicates the
         * start of the problem.
         **/ 
        public int getStartPosition() {
        	return NoPosition;
        }
    	
        /**
         * Returns the character offset from the beginning of the file associated with this diagnostic that indicates the
         * end position of the problem.
         **/ 
        public int getEndPosition() {
        	return NoPosition;
        }
    	
    	/** Returns the line number of the starting of the problem */
        public int getLineNumber() {
        	return NoPosition;
        }

        /** Returns the column number of the starting of the problem */
        public int getColumnNumber() {
        	return NoPosition;
        }
    	
    }

    /** A simple but detailed implementation of problem */
    static public class Detail extends Simple {
    	
    	/** Constructs a detail compilation problem */
    	public Detail(Kind pKind, String pCodeName, String pCode, String pMessage, int pStartPosition, int pEndPosition,
    			int pLineNumber, int pColumnNumber) {
    		super(pKind, pCodeName, pCode, pMessage);
    		this.StartPosition = pStartPosition;
    		this.EndPosition   = pEndPosition;
    		this.LineNumber    = pLineNumber;
    		this.ColumnNumber  = pColumnNumber;
    	}
    	
    	int StartPosition;
    	int EndPosition;
    	int LineNumber;
    	int ColumnNumber;
    	
        /** {@inheritDoc} */ @Override public int getStartPosition() { return this.StartPosition; }
        /** {@inheritDoc} */ @Override public int getEndPosition()   { return this.EndPosition;   }
        /** {@inheritDoc} */ @Override public int getLineNumber()    { return this.LineNumber;    }
        /** {@inheritDoc} */ @Override public int getColumnNumber()  { return this.ColumnNumber;  }
    }
    
}
