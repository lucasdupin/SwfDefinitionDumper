package flash.swf.tools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;


public class CustomAbcPrinter extends WideOpenAbcPrinter {

	ClassInfo publicClass;
	MethodInfo publicMethod;
	ArrayList<ClassInfo> classes;
	HashMap<MethodInfo, MethodInfo> unmodifiedMethods;

	public CustomAbcPrinter(byte[] abc) {
		super(abc, new PrintWriter(new StringWriter()), false, 0, false);
		// Initialize
		classes = new ArrayList<ClassInfo>();
		// Evaluate
		print();
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();

		// Add package with external symbol
		appendPackage(result);
		// Print internal functions
		appendInternalDefinitions(result);

		return result.toString();
	}

	private void appendPackage(StringBuffer buffer) {

		for (ClassInfo klass : classes)
			if (klass.modifier.equals("public")) {
				classes.remove(klass);
				publicClass = klass;
				break;
			}

		for (MethodInfo method : methods)
			if (method.className != null
					&& method.className.matches("^script\\d")) {
				publicMethod = method;
				break;
			}

		// Check for public definitions
		String packageName = "";
		if (publicClass != null) {
			packageName = publicClass.packageName;
		} else if (publicMethod != null) {
			packageName = publicMethod.name.split(":")[0];
		} else {
			return;
		}

		// Open package
		buffer.append("package " + packageName + " {\n");

		if (publicClass != null) {
			appendClass(publicClass, buffer);
		} else {
			appendMethod(publicMethod, buffer);
		}

		// Close package
		buffer.append("}\n");
	}

	private void appendInternalDefinitions(StringBuffer buffer) {
		for (ClassInfo klass : classes) {
			appendClass(klass, buffer);
		}
	}

	private void appendClass(ClassInfo klass, StringBuffer buffer) {

		buffer.append("  " + klass.modifier + " class " + klass.name + " {\n");
		// Class methods
		for (MethodInfo method : methods) {
			// Methods form the same class without $cinit
			if (method.className == klass.abcName
					&& method.name.indexOf("$cinit") == -1) {
				appendMethod(method, buffer);
			}
		}
		buffer.append("  }\n");
	}

	private void appendMethod(MethodInfo method, StringBuffer buffer) {

		// TODO check if it's static or not
		String[] nameComponents = method.name.split(":");
		String methodName = nameComponents[nameComponents.length - 1];
		String returnType = sanitizeType(multiNameConstants[method.returnType]
				.toString());
		String modifier = getModifier(method, methodName);

		buffer.append("    " + modifier + " function " + methodName + "(");
		for (int x = 0; x < method.paramCount; x++) {
			// TODO get actual parameter name
			buffer.append("arg"
					+ x
					+ ":"
					+ sanitizeType(multiNameConstants[method.params[x]]
							.toString()));
			if (x < method.paramCount - 1)
				buffer.append(", ");
		}
		buffer.append(")");
		if (returnType.length() > 0)
			buffer.append(":" + returnType);
		buffer.append(";\n");
	}

	/*
	 * Gets a String in the format: flash.display:Movieclip or :String and
	 * converts to an actual AS3 type
	 */
	private String sanitizeType(String type) {
		String returnType = type.replace(':', '.');
		if (returnType.indexOf('.') == 0)
			returnType = returnType.substring(1);
		return returnType;
	}

	private String getModifier(MethodInfo method, String sanitizedName) {

		MethodInfo unmodified = unmodifiedMethods.get(method);

		// public, private, protected...
		String m = "public";
		int nameIndex = unmodified.name.indexOf(":" + sanitizedName);
		int slashIndex = unmodified.name.indexOf('/');
		if (slashIndex > -1 && nameIndex > slashIndex) {
			m = unmodified.name.substring(slashIndex + 1, nameIndex);
		}

		// Check if it is a getter or setter
		int gsIndex = unmodified.name.indexOf(sanitizedName + "/");
		if (gsIndex > -1 && method.name != method.className) {
			m += " " + unmodified.name.substring(gsIndex + sanitizedName.length() + 1);
		}

		return m;

	}

	@Override
	void printMethods() {
		super.printMethods();
		unmodifiedMethods = new HashMap<MethodInfo, MethodInfo>();
		for (int i = 0; i < methods.length; i++) {
			unmodifiedMethods.put(methods[i], (MethodInfo) methods[i].clone());
		}
	}

	@Override
	void printClasses() {
		// Get number of instances in this ABC
		long n = readU32();
		// printOffset();
		// System.out.println(n + " Instance Entries");
		instanceNames = new String[(int) n];
		for (int i = 0; i < n; i++) {
			int start = offset;
			// printOffset();
			String name = multiNameConstants[(int) readU32()].toString();
			instanceNames[i] = name;
			String base = multiNameConstants[(int) readU32()].toString()
					.replace(':', '.');
			// Sanitizing packge name
			if (base.indexOf('.') == 0)
				base = base.substring(1);

			int b = abc[offset++];
			if ((b & 0x8) == 0x8)
				readU32(); // eat protected namespace
			long val = readU32();
			String s = "";
			for (int j = 0; j < val; j++) {
				s += " " + multiNameConstants[(int) readU32()].toString();
			}
			int init = (int) readU32(); // eat init method
			MethodInfo mi = methods[init];
			mi.name = name;
			mi.className = name;
			mi.kind = TRAIT_Method;

			// Compose class info
			ClassInfo classDescription = new ClassInfo(name, base, s);

			// Add to the list
			classes.add(classDescription);

			int numTraits = (int) readU32(); // number of traits
			printOffset();
			// System.out.println(numTraits + " Traits Entries");
			for (int j = 0; j < numTraits; j++) {
				printOffset();
				start = offset;
				s = multiNameConstants[(int) readU32()].toString(); // eat trait
																	// name;
				b = abc[offset++];
				int kind = b & 0xf;
				switch (kind) {
				case 0x00: // slot
				case 0x06: // const
					readU32(); // id
					readU32(); // type
					int index = (int) readU32(); // index;
					if (index != 0)
						offset++; // kind
					break;
				case 0x04: // class
					readU32(); // id
					readU32(); // value;
					break;
				default:
					readU32(); // id
					mi = methods[(int) readU32()]; // method
					mi.name = s;
					mi.className = name;
					mi.kind = kind;
					break;
				}
				if ((b >> 4 & 0x4) == 0x4) {
					val = readU32(); // metadata count
					for (int k = 0; k < val; k++) {
						readU32(); // metadata
					}
				}
				if (showByteCode) {
					for (int x = start; x < offset; x++) {
						// System.out.print(hex(abc[(int)x]) + " ");
					}
				}
				// System.out.println(s);
			}
		}
		printOffset();
		// System.out.println(n + " Class Entries");
		for (int i = 0; i < n; i++) {
			int start = offset;
			printOffset();
			MethodInfo mi = methods[(int) readU32()];
			String name = instanceNames[i];
			mi.name = name + "$cinit";
			mi.className = name;
			mi.kind = TRAIT_Method;
			//String base = "Class";
			if (showByteCode) {
				for (int x = start; x < offset; x++) {
					// System.out.print(hex(abc[(int)x]) + " ");
				}
			}
			// System.out.print(name + " ");
			// if (base.length() > 0)
			// System.out.print("extends " + base + " ");
			// System.out.println("");

			int numTraits = (int) readU32(); // number of traits
			printOffset();
			// System.out.println(numTraits + " Traits Entries");
			for (int j = 0; j < numTraits; j++) {
				printOffset();
				start = offset;
				String s = multiNameConstants[(int) readU32()].toString(); // eat
																			// trait
																			// name;
				int b = abc[offset++];
				int kind = b & 0xf;
				switch (kind) {
				case 0x00: // slot
				case 0x06: // const
					readU32(); // id
					readU32(); // type
					int index = (int) readU32(); // index;
					if (index != 0)
						offset++; // kind
					break;
				case 0x04: // class
					readU32(); // id
					readU32(); // value;
					break;
				default:
					readU32(); // id
					mi = methods[(int) readU32()]; // method
					mi.name = s;
					mi.className = name;
					mi.kind = kind;
					break;
				}
				if ((b >> 4 & 0x4) == 0x4) {
					int val = (int) readU32(); // metadata count
					for (int k = 0; k < val; k++) {
						readU32(); // metadata
					}
				}
				// if (showByteCode)
				// {
				// for (int x = start; x < offset; x++)
				// {
				// System.out.print(hex(abc[(int)x]) + " ");
				// }
				// }
				// System.out.println(s);
			}
		}
	}

	class ClassInfo {
		String modifier;
		String name;
		String abcName;
		String packageName;
		String extendS;
		String implementS;

		public ClassInfo(String abcName, String extendS, String implementS) {
			this.abcName = abcName;
			if (extendS != null & extendS.length() > 0)
				this.extendS = extendS;
			if (implementS != null & implementS.length() > 0)
				this.implementS = implementS;

			modifier = "public";
			packageName = abcName.split(":")[0];
			if (packageName.equals("private")) {
				packageName = "";
				modifier = "internal";
			}
			name = abcName.split(":")[1];
		}

		@Override
		public String toString() {
			String ext = (extendS == null ? "" : "extends " + extendS + " ");
			String impl = (implementS == null ? "" : "implements " + implementS
					+ " ");
			return modifier + " class " + name + " " + ext + impl;
		}
	}

}
