package com.lee.password.cmdline;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.lee.password.util.Pair;
import com.lee.password.util.Triple;

public final class Environment {
	
	private static final PrintStream STDOUT = System.out;
	
	public static final void newLine() { STDOUT.println(); }
	
	public static final void line(String str) { STDOUT.println(str); }

	private static final String INPUT_PROMPT = "pk>";
	
	public static final void prompt() {
		STDOUT.println();
		STDOUT.print(INPUT_PROMPT);
	}
	
	private static final String INDENT = "  ";
	
	public static final void indent(String followingStr) {
		STDOUT.print(INDENT);
		STDOUT.println(followingStr);
	}
	
	public static final void indent(int times, String followingStr) {
		while(times-- > 0) { STDOUT.print(INDENT); }
		STDOUT.println(followingStr);
	}
	
	/** customizable environment variable name **/
	public static enum Name {
		
		PUBBLIC_KEY_DIR("pubKeyDir", "key directory for save/load public key file") {
			@Override
			public Pair<Boolean, String> checkValid(String value) {
				if(value == null) { return Pair.create(false, name + " is null"); }
				File dir = new File(value);
				if(!dir.isDirectory()) { return Pair.create(false, "'" + value + "' is not a directory"); }
				return Pair.create(true, "success");
			}

			@Override
			public <T> Triple<Boolean, String, T> cast(Object value, Class<T> clazz) {
				
				return null;
			}
		},
		PRIVATE_KEY_DIR("priKeyDir", "key directory for save/load private key file") {
			@Override
			public Pair<Boolean, String> checkValid(String value) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> Triple<Boolean, String, T> cast(Object value, Class<T> clazz) {
				// TODO Auto-generated method stub
				return null;
			}
		},
		CRYPTO_DRIVER("cryptoDriver", "crypto driver for generate/load key and encrypt/decrypt data, "
				+ "specify implementation with class name") {
			@Override
			public Pair<Boolean, String> checkValid(String value) {
				
				return null;
			}

			@Override
			public <T> Triple<Boolean, String, T> cast(Object value, Class<T> clazz) {
				// TODO Auto-generated method stub
				return null;
			}
		},
		DATA_DIR("dataDir", "data directory for save/load password data file") {
			@Override
			public Pair<Boolean, String> checkValid(String value) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> Triple<Boolean, String, T> cast(Object value, Class<T> clazz) {
				// TODO Auto-generated method stub
				return null;
			}
		},
		IS_DATA_LOCK("isStoreLock", "lock the password data file or not ") {
			@Override
			public Pair<Boolean, String> checkValid(String value) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> Triple<Boolean, String, T> cast(Object value,
					Class<T> clazz) {
				// TODO Auto-generated method stub
				return null;
			}
		},
		STORE_DRIVER("storeDriver", "store driver for manage website and password data, "
				+ "specify implementation with class name",
			PRIVATE_KEY_DIR, DATA_DIR, CRYPTO_DRIVER) {
			@Override
			public Pair<Boolean, String> checkValid(String value) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> Triple<Boolean, String, T> cast(Object value, Class<T> clazz) {
				// TODO Auto-generated method stub
				return null;
			}
		},
		
		;
		
		public final String name;
		public final String desc;
		private final Name[] dependencies;
		
		private Name(String name, String desc, Name... dependencies) {
			this.name = name;
			this.desc = desc;
			this.dependencies = dependencies;
		}
		
		public abstract Pair<Boolean, String> checkValid(String value);
		
		public abstract <T> Triple<Boolean, String, T> cast(Object value, Class<T> clazz);
	}

	private static final Map<String, Name> NAME_MAP = new HashMap<String, Name>();
	
	static {
		for(Name name : Name.values()) { NAME_MAP.put(name.name, name); }
	}
	
	public static Name nameOf(String name) { return NAME_MAP.get(name); }
	
	private static final Environment ENV = new Environment();
	
	public static Environment current() { return ENV; }
	
	private final Map<Name, Object> variableMap = new HashMap<Name, Object>();
	
	private Environment() {}
	
	/**
	 * <pre>
	 * The return result pair:
	 *   first Boolean value - put variable success or not;
	 *   second String value - error message if put variable failed.
	 * </pre>
	 */
	public Pair<Boolean, String> putVariable(Name name, String value) {
		Pair<Boolean, String> result = name.checkValid(value);
		if(!result.first) { return result; }
		variableMap.put(name, value);
		return new Pair<Boolean, String>(true, "success");
	}
	
	/**
	 * <pre>
	 * The return result triple:
	 *   first Boolean value - get variable success or not;
	 *   second String value - error message if get variable failed;
	 *   third {@code T} value - mapped value if get variable success.
	 * </pre>
	 */
	public <T> Triple<Boolean, String, T> getVariable(Name name, Class<T> clazz) {
		Object value = variableMap.get(name);
		if(value == null) {
			return new Triple<Boolean, String, T>(false, name.name + " variable is not set", null);
		}
		return name.cast(value, clazz);
	}
	
	private boolean existVariable(Name name) { return variableMap.get(name) != null; }
	
	public List<Pair<Name, String>> getAllVariables() {
		List<Pair<Name, String>> list = new ArrayList<Pair<Name, String>>(variableMap.size());
		for(Entry<Name, Object> entry : variableMap.entrySet()) {
			list.add(new Pair<Name, String>(entry.getKey(), String.valueOf(entry.getValue())));
		}
		return list;
	}
}
