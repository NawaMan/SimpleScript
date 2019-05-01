package net.nawaman.script.java;

import java.util.Vector;

/** A rough parser for java like language */
public class JavaRoughParser {
	
	/** Parse result of a java like language Statement */
	static public class Statement {
		static public final Statement[] EmptyStatementArray = new Statement[0];
		
		public Statement(String pText, int pOffset) {
			this(pText, pOffset, null);
		}
		
		public Statement(String pText, int pOffset, Vector<Statement> pSubStatements) {
			int L = JavaRoughParser.lengthOfWhileSpace(pText, 0);
			this.Text          = (pText == null)?"":pText.trim();
			this.Offset        = pOffset + L;
			this.SubStatements = pSubStatements;
		}
		
		String            Text          = null;
		int               Offset        =    0;
		Vector<Statement> SubStatements = null;
		
		public String      getText()   { return this.Text;   }
		public int         getOffset() { return this.Offset; }
		public Statement[] getSubStatements() {
			if(this.SubStatements == null) return Statement.EmptyStatementArray;
			return this.SubStatements.toArray(Statement.EmptyStatementArray);
		}
		
		@Override public String toString() {
			StringBuilder SB = new StringBuilder();
			SB.append("[").append(this.Offset).append("]").append(this.Text);
			return SB.toString();
		}
		
		public String toDetail() {
			StringBuilder SB = new StringBuilder();
			SB.append("[").append(this.Offset).append("]").append(this.Text);
			if(this.SubStatements != null) SB.append("{").append(this.SubStatements).append("}");
			return SB.toString();
		}
		
		/** Returns a statement similar to the one given but remove the head part that are ignoreable */
		static public Statement getStatementWithoutIgnoreableHead(Statement pStm) {
			if(pStm == null) return null;
			String T = pStm.getText();
			// Ignore the Comment
			int i = 0;
			int I = i;
			do {
				I = i;
				i += JavaRoughParser.lengthOfLineComment(T, i);
				i += JavaRoughParser.lengthOfLongComment(T, i);
				i += JavaRoughParser.lengthOfWhileSpace( T, i);
			} while(i != I);
			if(i == 0) return pStm;
			return new Statement(pStm.getText().substring(i), pStm.getOffset() + i, pStm.SubStatements);
		}
		
		/** Remove the ignorable that begin each statement */
		static public void cleanAllIgnoreable(Vector<Statement> pStms) {
			if(pStms == null) return;
			for(int i = 0; i < pStms.size(); i++) {
				Statement Stm = Statement.getStatementWithoutIgnoreableHead(pStms.get(i)); 
				pStms.set(i, Stm);
				Statement.cleanAllIgnoreable(Stm.SubStatements);
			}
		}
		
		/**
		 * Check if the given text (should remove the head ignorable first) starts with the given word (ends with word
		 * boundary).
		 **/
		static public boolean isStartWithWord(Statement Stm, String pWord) {
			String T;
			int    WLen;
			if((pWord == null) || ((WLen = pWord.length()) == 0))                                 return true;
			if((Stm == null) || ((T = Stm.getText()) == null) || (Stm.getText().length() < WLen)) return false;
			return Statement.isStartWithWord(T, pWord, 0);
		}
		
		/**
		 * Check if a Statement (should remove the head ignorable first) starts with the given word (ends with word
		 * boundary).
		 **/
		static public boolean isStartWithWord(String pText, String pWord, int pOffset) {
			int TLen;
			int WLen;
			int WEnd;
			if((pWord == null) || ((WLen = pWord.length()) == 0))                        return true;
			if((pText == null) || ((TLen = pText.length()) < (WEnd = (pOffset + WLen)))) return false;
			if(pText.indexOf(pWord, pOffset) != pOffset) return false;
			if(TLen == WEnd) return true;
			
			char c = pText.charAt(WEnd);
			return !(((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')) || (c == '_') || (c == '$') || (c == '@'));
		}
	}
	
	/** Returns the length of the statement found at the offset position of the text */
	static public int lengthOfStatement(Vector<Statement> Statements, String Text, int Offset) {
		if((Text == null) || (Text.length() == 0)) return 0;
		
		boolean IsStarted     = false;
		boolean ArrayDetectOn = false;
		boolean HasJustWhite  = false;
		
		int i = Offset;
		for(; i < Text.length(); i++) {
			// Ignore the Comment
			int I = i;
			do {
				I = i;
				i += JavaRoughParser.lengthOfLineComment(Text, i);
				i += JavaRoughParser.lengthOfLongComment(Text, i);
				i += JavaRoughParser.lengthOfWhileSpace( Text, i);
				HasJustWhite = (i != I);
			} while(HasJustWhite);
			
			if(i >= Text.length()) break;
			
			if(!IsStarted && Statement.isStartWithWord(Text, "for", i)) {
				// For loop has at least 3 body in it
				int L1 = JavaRoughParser.lengthOfStatement(null, Text, i + 3);
				int L2 = JavaRoughParser.lengthOfStatement(null, Text, i + 3 + L1);
				int L3 = JavaRoughParser.lengthOfStatement(null, Text, i + 3 + L1 + L2);
				if((L1 + L2 + L3) != 0) {
					if(Statements != null)
						Statements.add(new Statement(Text.substring(Offset, i + 3 + L1 + L2 + L3), Offset));
					return i + 3 + L1 + L2 + L3 - Offset;
				}
			}
			
			char c = Text.charAt(i);
			
			switch(c) {
			
				case '\"': {
					i += JavaRoughParser.lengthOfStringLiteral(Text, i);
					IsStarted = true;
					break;
				}
				
				case '\'': {
					i += JavaRoughParser.lengthOfCharLiteral(Text, i);
					IsStarted = true;
					break;
				}
				
				case '{': {
					IsStarted = true;
					
					Vector<Statement> SubStatements = null;
					if(Statements != null)  SubStatements = new Vector<Statement>();
					i += JavaRoughParser.lengthOfStatements(SubStatements, Text, i + 1);
					int End = i;
					if((i < Text.length()) && Text.charAt(i) == '}') End++;
					
					if(!ArrayDetectOn) {
						if(Statements != null) Statements.add(new Statement(Text.substring(Offset, End), Offset, SubStatements));
						return End - Offset;
					}
					
					ArrayDetectOn = false;
					continue;
				}
				
				case '}': {
					if(IsStarted && (Statements != null))
						Statements.add(new Statement(Text.substring(Offset, i), Offset));
					
					return i - Offset;
				}
				case ';': {
					int Length = i + 1 - Offset;
					if((Statements != null))
						Statements.add(new Statement(Text.substring(Offset, Offset + Length), Offset));
					
					return Length;
				}
				
				default: {

					ArrayDetectOn = false;
					if(c == '[') {
						i += 1;
						// There must be a white space after new 
						int     HereI            = i;
						do {
							HereI = i;
							i += JavaRoughParser.lengthOfLineComment(Text, i);
							i += JavaRoughParser.lengthOfLongComment(Text, i);
							i += JavaRoughParser.lengthOfWhileSpace( Text, i);
						} while(i != HereI);
						
						if((i < Text.length()) && (Text.charAt(i) == ']')) ArrayDetectOn = true;
					}
					
					IsStarted = true;
				}
			}
		}
		
		// Ignore the Comment
		int I = i;
		do {
			I = i;
			i += JavaRoughParser.lengthOfLineComment(Text, i);
			i += JavaRoughParser.lengthOfLongComment(Text, i);
			i += JavaRoughParser.lengthOfWhileSpace( Text, i);
		} while(i != I);
		
		int Length = i - Offset - 1;
		if(Statements != null) {
			String Str = Text.substring(Offset, Offset + Length);
			if(!"".equals(Str.trim())) Statements.add(new Statement(Str, Offset));
		}
		return Length;
	}
	
	/** Returns the length of the statements found at the offset position of the text */
	static public int lengthOfStatements(String Text, int Offset) {
		return JavaRoughParser.lengthOfStatements(null, Text, Offset);
	}
	/** Returns the length of the statements found at the offset position of the text */
	static public int lengthOfStatements(Vector<Statement> Statements, String Text, int Offset) {
		if((Text == null) || (Text.length() == 0)) return 0;
		for(int i = Offset; i < Text.length();) {
			int L = JavaRoughParser.lengthOfStatement(Statements, Text, i);
			if(L == 0) return i + 1 - Offset;
			i += L;
		}
		return Text.length() - Offset;
	}

	/** Returns the length of the string literal found at the offset position of the text */
	static public int lengthOfStringLiteral(String Text, int Offset) {
		if((Text == null) || (Text.length()  <= (Offset + 2)) || (Text.charAt(Offset) != '\"')) return 0;
		for(int i = Offset + 1; i < Text.length(); i++) {
			switch(Text.charAt(i)) {
				case '\"': return i + 1 - Offset;
				case '\\': i++;
			}
		}
		return 0;
	}
	
	/** Returns the length of the character literal found at the offset position of the text */
	static public int lengthOfCharLiteral(String Text, int Offset) {
		if((Text == null) || (Text.length()  <= (Offset + 2)) || (Text.charAt(Offset) != '\'')) return 0;
		for(int i = Offset + 1; i < Text.length(); i++) {
			switch(Text.charAt(i)) {
				case '\'': return i + 1 - Offset;
				case '\\': i++;
			}
		}
		return 0;
	}
	
	/** Returns the length of the line comment found at the offset position of the text */
	static public int lengthOfLineComment(String Text, int Offset) {
		if((Text == null) || (Text.length()  <= (Offset + 2)) ||
			(Text.charAt(Offset) != '/') || (Text.charAt(Offset + 1) != '/')) return 0;
		for(int i = Offset + 2; i < Text.length(); i++) {
			if(Text.charAt(i) == '\n') return i + 1 - Offset;
		}
		return Text.length() - Offset;
	}
	
	/** Returns the length of the long comment found at the offset position of the text */
	static public int lengthOfLongComment(String Text, int Offset) {
		if((Text == null) || (Text.length() <= (Offset + 2)) ||
			(Text.charAt(Offset) != '/') || (Text.charAt(Offset + 1) != '*')) return 0;
		for(int i = Offset + 2; i < (Text.length() - 1); i++) {
			if((Text.charAt(i) == '*') && (Text.charAt(i + 1) == '/')) return i + 2 - Offset;
		}
		return Text.length() - Offset;
	}
	
	/** Returns the length of the white spaces found at the offset position of the text */
	static public int lengthOfWhileSpace(String Text, int Offset) {
		if((Text == null) || (Text.length() == 0)) return 0;
		for(int i = Offset; i < Text.length(); i++) {
			switch(Text.charAt(i)) {
				// While Space
				case '\n': 
				case ' ' : 
				case '\t': 
				case '\r': 
				case '\u000B': 
				case '\u000C': continue;
				default: return i - Offset;
			}
		}
		return Text.length() - Offset;
	}
	
	static public void main(String ... Args) {
		StringBuilder SB = new StringBuilder();
		SB.append("import java.io.*;\n");
		SB.append("import /* A comment { */ java.util.*;\n");
		SB.append("static int /* A comment { */ i = 10;\n");
		SB.append("static int[] /* A comment { */ Is = new int[ /* Some note */ ] /* Some note */ { 1 2 3}[5];\n");
		SB.append("private /* A comment } */ int j = 20;\n");
		SB.append("for(int x = 0; x < 10; x++) {\n");
		SB.append("	System.out.println(x);\n");
		SB.append("	System.out.println(x + 5 + (new Is[] {1, 5, 6})[1]);\n");
		SB.append("}\n");
		SB.append("return i+j;\n");
		
		Vector<Statement> Stms = new Vector<Statement>();
		
		System.out.println(JavaRoughParser.lengthOfStatements(Stms, SB.toString(), 0));
		
		for(int i = 0; i < Stms.size(); i++)
			System.out.println(Stms.get(i));
	} 
}
