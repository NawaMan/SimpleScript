package net.nawaman.script;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

/** The interface of the invocation. */
public interface Signature extends Serializable {
	
	static public final Class<?>[] EmptyClassArray  = new Class<?>[0];
	static public final String[]   EmptyStringArray = new String[0];
	static public final Object[]   EmptyObjectArray = new Object[0];

	/**
	 * Returns the name of the Signature
	 *   - The name should be null or [a-zA-Z_$][a-zA-Z_$0-9].
	 *   - The name is for reference only, it will not be used to evaluate the compatibility
	 */
	public String getName();
	
	/** Returns the number of parameters */
	public int getParamCount();

	/** Returns the parameter type at the index pPos */
	public Class<?> getParamType(int pPos);

	/** Checks if the interface allow the open end parameter at the last parameter */
	public boolean isVarArgs();

	/** Returns the type reference of the return type */
	public Class<?> getReturnType();
	
	/** A simple implementation of the function interface */
	static final public class Simple implements Signature {
		
		static final private long serialVersionUID = 2768341645623722517L;
		
		/** The signature of a free function (with unlimited params) */
		static public Signature FreeSignature      = new Simple(null, Object.class, true, Object.class);
		/** The signature of a procedure (no param) */
		static public Signature ProcedureSignature = new Simple(null, Object.class, false, EmptyClassArray);

		/** Constructs a new simple function interface from a java method*/
		public Simple(Method pMethod) {
			this(pMethod.getName(), pMethod.getReturnType(), pMethod.isVarArgs(), pMethod.getParameterTypes());
		}
		/** Constructs a new simple function interface */
		public Simple(String pName, Signature pOriginal) {
			this(pName, pOriginal.getReturnType(), pOriginal.isVarArgs(), (Class<?>[])null);
			if(pOriginal.getParamCount() == 0) this.ParamTypes = EmptyClassArray;
			else {
				this.ParamTypes = new Class<?>[pOriginal.getParamCount()];
				for(int i = pOriginal.getParamCount(); --i >= 0; )
					this.ParamTypes[i] = pOriginal.getParamType(i);
			}
		}
		/** Constructs a new simple function interface */
		public Simple(String pName, Class<?> pReturnType) {
			this(pName, pReturnType, false, (Class<?>[])null);
		}
		/** Constructs a new simple function interface */
		public Simple(String pName, Class<?> pReturnType, boolean pIsVarArgs, Class<?> ... pParamTypes) {
			this.Name = pName;
			
			if(pReturnType == null) pReturnType = Void.class;
			if(pParamTypes == null) pParamTypes = EmptyClassArray;
			if(pIsVarArgs && (pParamTypes.length == 0))
				throw new IllegalArgumentException("A function without any parameter cannot be VarArgs.");
			
			for(int i = pParamTypes.length; --i >= 0; ) {
				if(pParamTypes[i] == null) pParamTypes[i] = Void.class;
				else if(pParamTypes[i].isPrimitive()) {
					throw new IllegalArgumentException("Primitive parameter type is not allowed ("+pParamTypes[i].getName()+").");
				}
			}
			
			this.IsVarArgs  = pIsVarArgs;
			this.ReturnType = pReturnType;
			this.ParamTypes = (pParamTypes == EmptyClassArray)?EmptyClassArray:pParamTypes.clone();
		}
		
		String     Name;
		boolean    IsVarArgs;
		Class<?>   ReturnType;
		Class<?>[] ParamTypes;

		/** Returns the name of the Signature */
		public String getName() { return this.Name; }

		/** Returns the number of parameters */
		public int getParamCount() {
			return this.ParamTypes.length;
		}

		/** Returns the parameter type at the index pPos */
		public Class<?> getParamType(int pPos) {
			if((pPos < 0) || (pPos >= this.ParamTypes.length)) return null;
			return this.ParamTypes[pPos];
		}

		/** Checks if the interface allow the open end parameter at the last parameter */
		public boolean isVarArgs() {
			return this.IsVarArgs;
		}

		/** Returns the type reference of the return type */
		public Class<?> getReturnType() {
			return this.ReturnType;
		}
		
		/** {@inheritDoc} */ @Override public int hashCode() {
			return hashCode(this);
		}
		
		/** {@inheritDoc} */ @Override public boolean equals(Object O) {
			if(!(O instanceof Signature)) return false;
			return equals(this, (Signature)O);
		}
		
		/** {@inheritDoc} */ @Override public String toString() {
			return toString(this);
		}
		
		// Utilities methods -------------------------------------------------------------------------------------------

		/** Returns the string representation of the given function interface */
		static public int hashCode(Signature pSignature) {
			int h = (pSignature.isVarArgs()?0:1) + pSignature.getReturnType().hashCode();
			for(int i = pSignature.getParamCount(); --i >= 0; ) {
				Class<?> C = pSignature.getParamType(i);
				h += (C == null)?0:C.hashCode();
			}
			return h;
		}
		
		/** Checks if the given Signatures are equals */
		static public boolean equals(Signature pSignature, Signature pAnotherSignature) {
			if( pSignature ==           pAnotherSignature)          return true;
			if((pSignature == null) || (pAnotherSignature == null)) return false;
			
			if(pSignature.isVarArgs()     != pAnotherSignature.isVarArgs())     return false;
			if(pSignature.getReturnType() != pAnotherSignature.getReturnType()) return false;
			if(pSignature.getParamCount() != pAnotherSignature.getParamCount()) return false;
			for(int i = pSignature.getParamCount(); --i >= 0; ) {
				if(pSignature.getParamType(i) != pAnotherSignature.getParamType(i)) return false;
			}
			return true;
		}
		
		/** Returns the string representation of the given function interface */
		static public String toString(Signature pSignature) {
			StringBuffer SB = new StringBuffer();
			SB.append("function ");
			SB.append("(");
			for(int i = 0; i < pSignature.getParamCount(); i++) {
				if(i != 0) SB.append(", ");
				SB.append(pSignature.getParamType(i).getCanonicalName());
				if(pSignature.isVarArgs() && (i == (pSignature.getParamCount() - 1)))
					SB.append(" ...");
			}
			SB.append("):");
			SB.append(pSignature.getReturnType().getCanonicalName());
			return SB.toString();
		}
		
		/** Checks if the given Signatures are equals */
		static public boolean canAImplementsB(Signature pASignature, Signature pBSignature) {
			if(pASignature == pBSignature)       return true;
			if(equals(pBSignature, pBSignature)) return true;
			if((pASignature == null) || (pBSignature == null)) return false;
			
			if(pASignature.isVarArgs() != pBSignature.isVarArgs()) return false;
			// R(B) >= R(A), so when A return, the object is an instance of B
			if(pBSignature.getReturnType().isAssignableFrom(pASignature.getReturnType())) return false;

			// P(B) <= P(A), so when param is assign via B, it can be sure to fix the one in A
			if(pASignature.getParamCount() != pBSignature.getParamCount()) return false;
			for(int i = pASignature.getParamCount(); --i >= 0; ) {
				if(pASignature.getReturnType().isAssignableFrom(pBSignature.getReturnType())) return false;
			}
			return true;
		}
		
		/** Checks if the given Signatures are equals */
		static public boolean canAImplementsB(Signature pASignature, Method pMethod) {
			if((pASignature == null) || (pMethod == null)) return false;
			
			if(pASignature.isVarArgs() != pMethod.isVarArgs()) return false;
			// R(B) >= R(A), so when A return, the object is an instance of B
			if(pMethod.getReturnType().isAssignableFrom(pASignature.getReturnType())) return false;
			Class<?>[] Ps = pMethod.getParameterTypes();
			// P(B) <= P(A), so when param is assign via B, it can be sure to fix the one in A
			if(pASignature.getParamCount() != Ps.length) return false;
			for(int i = pASignature.getParamCount(); --i >= 0; ) {
				if(pASignature.getReturnType().isAssignableFrom(Ps[i])) return false;
			}
			return true;
		}
		
		/** Adjust the parameters (apply VarArgs) */
		static public Object[] adjustParameters(Signature pSignature, Object[] pParams) {
			if(pParams == null) pParams = EmptyObjectArray;
			if(pSignature.isVarArgs()) { // In case of VarArgs
				Class<?> VAType = pSignature.getParamType(pSignature.getParamCount() - 1);
				int      PCount = pParams.length;
				// The last parameter may be absent to the given param can be one less than the parameter count of the
				//    interface
				if(PCount < (pSignature.getParamCount() - 1))
					throw newIncompatibleParameterException(pSignature, pParams);
				
				if(PCount == (pSignature.getParamCount() - 1)) {
					// The last parameter is omitted so add an empty array as the last parameter,
					Object[] Temp = new Object[pSignature.getParamCount()];
					Temp[Temp.length - 1] =  Array.newInstance(VAType, 0);
					System.arraycopy(pParams, 0, Temp, 0, pSignature.getParamCount() - 1);
					pParams = Temp;
				} else if(PCount == pSignature.getParamCount()) {
					Object LastParam = pParams[PCount - 1];
					if(LastParam != null) {
						// The last parameter is the same type
						if(VAType.isInstance(LastParam)) {
							// Make it an array
							Object O = Array.newInstance(VAType, 1);
							Array.set(O, 0, pParams[PCount - 1]);
							pParams[PCount - 1] = O;
							
						} else if(!LastParam.getClass().isArray()) {
							// Not an array
							throw newIncompatibleParameterException(pSignature, pParams);
							
						} else if(!VAType.isAssignableFrom(LastParam.getClass().getComponentType())){
							// Or the component is not compatible, try to see each one
							int    LPCount      = Array.getLength(LastParam);
							Object NewLastParam = Array.newInstance(VAType, LPCount);
							for(int i = 0; i < LPCount; i++ ) {
								Object LP = Array.get(LastParam, i);
								if((LP != null) && !VAType.isInstance(LP))
									throw newIncompatibleParameterException(pSignature, pParams);
								
								Array.set(NewLastParam, i, LP);
							}
							pParams[PCount - 1] = NewLastParam;
							
						} // If it is an array and the component is compatible, the parameter is already match
					}
				} else {	// Tailing
					// Make the last parameter as an array
					int    FirstIndex   = (pSignature.getParamCount() - 1);
					Object NewLastParam = Array.newInstance(VAType, PCount - FirstIndex);
					
					for(int i = FirstIndex; i < PCount; i++ ) {
						if((pParams[i] != null) && !VAType.isInstance(pParams[i]))
							throw newIncompatibleParameterException(pSignature, pParams);
						
						Array.set(NewLastParam, i - FirstIndex, pParams[i]);
					}
					
					Object[] Temp = new Object[pSignature.getParamCount()];
					Temp[pSignature.getParamCount() - 1] = NewLastParam;	// Assign the last array
					System.arraycopy(pParams, 0, Temp, 0, pSignature.getParamCount() - 1);
					pParams = Temp;
				}
				
			} else if(pSignature.getParamCount() != pParams.length) {
				throw newIncompatibleParameterException(pSignature, pParams);
			}
			
			// Checks each parameter
			for(int i = pSignature.getParamCount(); --i >= 0; ) {
				if(pParams[i] == null) continue;
				Class<?> C = pSignature.getParamType(i);
				// For the last parameter of VarArgs, the last parameter is already checked
				if(pSignature.isVarArgs() && (i == (pSignature.getParamCount() - 1))) continue;
				
				if((pParams[i] != null) && !C.isInstance(pParams[i]))
					throw newIncompatibleParameterException(pSignature, pParams);
			}
			
			return pParams;
		}
		
		/** Checks if the return is compatible with the signature */
		static public Object ensureReturnCompatible(Signature pSignature, Object pReturn) {
			Class<?> ReturnType = pSignature.getReturnType();
			if((pReturn != null) && !ReturnType.isInstance(pReturn)) {
				if(Number.class.isAssignableFrom(ReturnType) && Number.class.isInstance(pReturn)) {
					if(ReturnType == Integer.class) return ((Number)pReturn).intValue();
					if(ReturnType ==  Double.class) return ((Number)pReturn).doubleValue();
					if(ReturnType ==    Byte.class) return ((Number)pReturn).byteValue();
					if(ReturnType ==    Long.class) return ((Number)pReturn).longValue();
					if(ReturnType ==   Float.class) return ((Number)pReturn).floatValue();
					if(ReturnType ==   Short.class) return ((Number)pReturn).shortValue();
				}
				
				throw new RuntimeException("Invalid return value '"+pReturn+"' for "
						+Signature.Simple.toString(pSignature));
			}
			return pReturn;
		}
		
		/** Create a new incompatible parameter exception */
		static public RuntimeException newIncompatibleParameterException(Signature pSignature, Object[] pParams) {
			StringBuffer SB = new StringBuffer();
			SB.append("Incompatible parameter: (");
			if(pParams != null) {
				for(int i = 0; i < pParams.length; i++) {
					if(i != 0) SB.append(", "); 
					if(     pParams[i] == null)              SB.append("null");
					else if(!(pParams[i] instanceof String)) SB.append(pParams[i]);
					else {
						SB.append("\"");
						String S = (String)pParams[i];
						for(int c = 0; c < S.length(); c++) {
							char C = S.charAt(i);
							switch(C) {
								case '\n': SB.append("\\n");  break;
								case '\t': SB.append("\\t");  break;
								case '\f': SB.append("\\f");  break;
								case '\r': SB.append("\\r");  break;
								case '\'': SB.append("\\'");  break;
								case '\"': SB.append("\\\""); break;
								default: SB.append(S.charAt(i));
							}
						}
						SB.append("\"");
					} 
					SB.append(":");
					if(pParams[i] == null) SB.append("Object");
					else {
						String T = pParams[i].getClass().getCanonicalName();
						if(T.startsWith("java.lang.")) T = T.substring("java.lang.".length());
						SB.append(T);
					}
				}
			}
			SB.append(") for ");
			SB.append(Signature.Simple.toString(pSignature));
			return new RuntimeException(SB.toString());
		}
	}

}
