package net.nawaman.script;

import java.util.Vector;

/** Collection of compile problems */
public class ProblemContainer {
	
	protected Vector<Problem> Problems = null;
	
	/** Report a problem */
	public void reportProblem(Problem pProblem) {
		if(this.Problems == null) this.Problems = new Vector<Problem>();
		this.Problems.add(pProblem);
	}
	/** Checks if there are compile problems reported */
	final public boolean hasProblem() {
		return ((this.Problems != null) && (this.Problems.size() != 0)); 
	}
	
	/** Checks if there are compile errors reported */
	final public boolean hasError() {
		if(!this.hasProblem()) return false;
		for(Problem P : this.Problems) {
			if(P == null) continue;
			if(P.getKind() == Problem.Kind.Error)  return true;
		}
		return false;
	}
	
	/** Returns the number of the problem reported */
	final public int getProblemCount() {
		return (this.Problems != null)?this.Problems.size():0;
	}
	
	/** Returns the problem at the index */
	final public Problem getProblem(int I) {
		return ((I < 0) || (this.Problems == null) || (I >= this.Problems.size()))?null:this.Problems.get(I);
	}
	
	/** {@inheritDoc} */
	@Override public String toString() {
		StringBuffer SB = new StringBuffer();
		
		for(int i = 0; i < this.getProblemCount(); i++) {
			Problem P = this.getProblem(i);			
			
			if(i != 0) SB.append("\n");
			SB.append("Problem #");
			SB.append(i + 1);
			if(P == null) {
				SB.append(": null;\n");
				continue;
			}
			
			SB.append(" -----------------------------------------------------------------------------------\n");
			SB.append(ProblemContainer.getProblemToString(P));
		}

		if(SB.length() != 0)
			SB.append("----------------------------------------------------------------------------------------------\n");
		
		return SB.toString();
	}
	
	/** Convert problem in to string */
	static public String getProblemToString(Problem pProblem) {
		StringBuffer SB = new StringBuffer();
		
		Problem P = pProblem;
		
		String CodeName = P.getCodeName();
		String Code     = P.getCode();
		int    Col      = P.getColumnNumber();
		int    Row      = P.getLineNumber();
		String Kind     = P.getKind().toString();
		int    Start    = P.getStartPosition();
		String Message  = P.getMessage();
		
		// Print Code Name
		if((CodeName != null) && !Problem.NoCodeName.equals(CodeName)) SB.append("Code: ").append(CodeName).append("\n");
		
		// Print Message
		SB.append(Kind).append(" Message: ").append(Message).append("\n");
		
		// Print position
		if((Col != Problem.NoPosition) && (Row != Problem.NoPosition)) {
			SB.append("Found at (").append(Col).append(", ").append(Row).append(")\n");
		} else if(Row != Problem.NoPosition) {
			SB.append("Found on line# ").append(Row).append("\n");
		}
		
		// Print the position
		if((Code != null) && (Start != Problem.NoPosition)) {
			String LineNum = (Row != Problem.NoPosition)?"Line #"+Row+": ":"";
			
			int StartLineBegin = Code.lastIndexOf("\n", Start);
			int StartLineEnd   = Code.indexOf(    "\n", Start);
			if(StartLineBegin == -1) StartLineBegin = 0; else StartLineBegin++;
			if(StartLineEnd   == -1) StartLineEnd = Code.length();
			
			if(StartLineBegin > StartLineEnd) {
				StartLineBegin = Code.lastIndexOf("\n", Start - 1);
				if(StartLineBegin == -1) StartLineBegin = 0; else StartLineBegin++;
			}
			
			// Show the line
			String Line = Code.substring(StartLineBegin, StartLineEnd);
			SB.append(LineNum);
			SB.append(Line);
			SB.append("\n");
			
			// Show the cursor
			SB.append(LineNum);
			int StartInLine = (Start - StartLineBegin);
			if(StartInLine > Line.length()) StartInLine = Line.length();
			// Show start
			for(int c = 0; c < StartInLine; c++) {
				char C = Line.charAt(c);
				switch(C) {  
					case '\t': SB.append('\t'); continue;
					default:   SB.append(' ');  continue;
				}
			}
			SB.append("^");
		}

		SB.append("\n");
		return SB.toString();
	}

}
