package com.lee.password.cmdline;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.crypto.CryptoDriver;
import com.lee.password.keeper.api.crypto.CryptoKey;
import com.lee.password.keeper.api.store.StoreDriver;
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
	
	public static final void indent() { STDOUT.print(INDENT); }
	
	public static final void indent(String followingStr) {
		STDOUT.print(INDENT);
		STDOUT.println(followingStr);
	}
	
	public static final void indent(int times, String followingStr) {
		while(times-- > 0) { STDOUT.print(INDENT); }
		STDOUT.println(followingStr);
	}
	
	public static final void printStackTrace(Throwable t) {
		if(t != null) { t.printStackTrace(STDOUT); }
	}
	
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static final String format(long timestamp) { return DATE_FORMAT.format(new Date(timestamp)); }
	
	/** customizable environment variable name **/
	public static enum Name {
		
		PUBLIC_KEY_DIR("pubKeyDir", "key directory for save/load public key file") {
			@Override
			protected Pair<Boolean, String> checkValid(String value) { return checkValidDir(this, value); }
			@Override
			protected File convert(String value) { return new File(value); }
		},
		PRIVATE_KEY_DIR("priKeyDir", "key directory for save/load private key file") {
			@Override
			public Pair<Boolean, String> checkValid(String value) { return checkValidDir(this, value); }
			@Override
			protected File convert(String value) { return new File(value); }
		},
		CRYPTO_DRIVER("cryptoDriver", "crypto driver for generate/load key and encrypt/decrypt data, "
				+ "specify implementation with fully qualified class name") {
			@Override
			public Pair<Boolean, String> checkValid(String value) { return checkValidClass(this, value); }
			@Override
			protected Class<? extends CryptoDriver> convert(String value) {
				try {
					@SuppressWarnings("unchecked")
					Class<? extends CryptoDriver> clazz = (Class<? extends CryptoDriver>) Class.forName(value);
					return clazz;
				}catch(Exception e) {
					throw new RuntimeException("can not cast to a 'CryptoDriver' class with fully qualified name: "+value);
				}
			}
		},
		DATA_DIR("dataDir", "data directory for save/load password data file") {
			@Override
			public Pair<Boolean, String> checkValid(String value) { return checkValidDir(this, value); }
			@Override
			protected File convert(String value) { return new File(value); }
		},
		IS_DATA_LOCK("isStoreLock", "lock the password data file or not ") {
			@Override
			public Pair<Boolean, String> checkValid(String value) { return checkValidBoolean(this, value); }
			@Override
			protected Boolean convert(String value) { return Boolean.valueOf(value); }
		},
		STORE_DRIVER("storeDriver", "store driver for manage website and password data, "
				+ "specify implementation with fully qualified class name") {
			@Override
			public Pair<Boolean, String> checkValid(String value) { return checkValidClass(this, value); }
			@Override
			protected Class<? extends StoreDriver> convert(String value) {
				try {
					@SuppressWarnings("unchecked")
					Class<? extends StoreDriver> clazz = (Class<? extends StoreDriver>) Class.forName(value);
					return clazz;
				}catch(Exception e) {
					throw new RuntimeException("can not cast to a 'StoreDriver' class with fully qualified name: "+value);
				}
			}
		},
		;
		
		public final String name;
		public final String desc;
		
		private Name(String name, String desc) {
			this.name = name;
			this.desc = desc;
		}
		
		protected abstract Pair<Boolean, String> checkValid(String value);
		
		static Pair<Boolean, String> checkValidDir(Name name, String value) {
			if(value == null) { return Pair.create(false, "the value of variable '" + name + "' is null"); }
			File dir = new File(value);
			if(!dir.isDirectory()) { return Pair.create(false, "'" + value + "' is not a directory"); }
			return Pair.create(true, "success");
		}
		
		static Pair<Boolean, String> checkValidClass(Name name, String value) {
			if(value == null) { return Pair.create(false, "the value of variable '" + name + "' is null"); }
			try {
				Class<?> clazz = Class.forName(value);
				if(!CryptoDriver.class.isAssignableFrom(clazz)) {
					return Pair.create(false, "the specified type of value of variable '" + name + "' is not compatible with "+CryptoDriver.class);
				}
				return Pair.create(true, "success");
			}catch(Exception e) {
				return Pair.create(false, "the value of variable '" + name + "' is not a valid fully qualified class name");
			}
		}
		
		static Pair<Boolean, String> checkValidBoolean(Name name, String value) {
			if(value == null) { return Pair.create(false, "the value of variable '" + name + "' is null"); }
			if("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
				return Pair.create(true, "success");
			}
			return Pair.create(false, "the value of variable '" + name + "' is not a boolean literal");
		}
		
		protected final Pair<String, Object> cast(String value) { return Pair.create(value, convert(value)); }
		
		protected abstract Object convert(String value);
		
		protected <T> Triple<Boolean, String, T> cast(Pair<String, Object> value, Class<T> clazz) {
			if(clazz.isAssignableFrom(String.class)) {
				return Triple.create(true, "success", clazz.cast(value.first));
			}
			if(clazz.isInstance(value.second)) {
				return Triple.create(true, "success", clazz.cast(value.second));
			}
			return Triple.create(false, "the value of variable '" + name + "' is not compatible with "+clazz, null);
		}
	}

	private static final Map<String, Name> NAME_MAP = new HashMap<String, Name>();
	
	static {
		for(Name name : Name.values()) { NAME_MAP.put(name.name, name); }
	}
	
	public static Name nameOf(String name) { return NAME_MAP.get(name); }
	
	private static final Preferences PREFS = userPreferences();
	
	private static Preferences userPreferences() {
		Preferences prefs = Preferences.userRoot().node("/com/lee/password-keeper");
		try {
			prefs.flush();
			return prefs;
		}catch(BackingStoreException e) {
			throw new IllegalStateException("failed to init user preferences node: /com/lee/password-keeper", e);
		}
	}
	
	public static void putPref(Name name, String value) { PREFS.put(name.name, value); }
	
	public static String getPref(Name name) { return PREFS.get(name.name, null); }
	
	private static final Environment ENV = new Environment();
	
	public static Environment current() { return ENV; }
	
	private static final class Holder<T> {
		private T value;
		private boolean isChanged;
		Holder(T value) {
			this.value = value;
			this.isChanged = false;
		}
		void onChanged() { this.isChanged = true; }
		T reset(T value) {
			T old = this.value;
			this.value = value;
			this.isChanged = false;
			return old;
		}
	}
	
	private static enum State { ENTER, RUNNING, EXIT }
	
	private final Map<Name, Pair<String, Object>> variableMap = new HashMap<Name, Pair<String, Object>>();
	private Holder<CryptoDriver> cryptoDriver;
	private Holder<CryptoKey> encryptionKey;
	private Holder<CryptoKey> decryptionKey;
	private Holder<StoreDriver> storeDriver;
	
	private State state;
	
	private Environment() {
		signalEnter();
		line("init password-keeper environment...");
		for(Name name : Name.values()) {
			String pref = getPref(name);
			if(pref != null) {
				Pair<Boolean, String> result = putVariable(name, pref);
				if(result.first) {
					indent("load preference variable '"+name.name+"' as '"+pref+"' successful");
				}else {
					indent("load preference variable '"+name.name+"' failed: "+result.second);
				}
			}
		}
		line("init password-keeper finished");
	}
	
	public void signalEnter() {
		if(state != null) { throw new IllegalStateException("current state is null, can not enter"); }
		state = State.ENTER;
	}
	public void signalRunning() {
		if(state != State.ENTER) { throw new IllegalStateException("can not shift to 'running' for illegal current state: "+state); }
		state = State.RUNNING;
	}
	public void signalExit() {
		if(state == State.EXIT) { return; }
		if(state != State.RUNNING) { throw new IllegalStateException("can not shift to 'exit' for illegal current state: "+state); }
		state = State.EXIT;
	}
	
	public boolean canExitNow() { return cryptoDriver == null && storeDriver == null; }
	
	public void exitNow() { signalExit(); }
	
	public boolean needExitSystem() { return state == State.EXIT; }
	
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
		Pair<String, Object> old = variableMap.put(name, name.cast(value));
		if(old != null && !old.first.equals(value)) { onVariableChanged(name); }
		return new Pair<Boolean, String>(true, "success");
	}
	
	private void onVariableChanged(Name name) {
		switch(name) {
		case CRYPTO_DRIVER:
			cryptoDriver.onChanged();
			encryptionKey.onChanged();
			decryptionKey.onChanged();
			storeDriver.onChanged();
			break;
		case PUBLIC_KEY_DIR: encryptionKey.onChanged(); break;
		case PRIVATE_KEY_DIR:
			decryptionKey.onChanged();
			storeDriver.onChanged();
			break;
		case DATA_DIR:
		case IS_DATA_LOCK:
		case STORE_DRIVER:
			storeDriver.onChanged();
			break;
		}
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
		Pair<String, Object> value = variableMap.get(name);
		if(value != null) {
			return name.cast(value, clazz);
		}
		return new Triple<Boolean, String, T>(false, name.name + " variable is not set", null);
	}
	
	public List<Pair<Name, String>> getAllVariables() {
		List<Pair<Name, String>> list = new ArrayList<Pair<Name, String>>(variableMap.size());
		for(Entry<Name, Pair<String, Object>> entry : variableMap.entrySet()) {
			list.add(new Pair<Name, String>(entry.getKey(), entry.getValue().first));
		}
		return list;
	}
	
	public Triple<Boolean, String, CryptoDriver> getCryptoDriver() {
		if(cryptoDriver != null) {
			if(!cryptoDriver.isChanged) { return Triple.create(true, "success", cryptoDriver.value); }
		}
		return loadCryptoDriver();
	}
	
	private Triple<Boolean, String, CryptoDriver> loadCryptoDriver() {
		@SuppressWarnings("rawtypes")
		Triple<Boolean, String, Class> triple = getVariable(Name.CRYPTO_DRIVER, Class.class);
		if(!triple.first) { return Triple.create(false, triple.second, null); }
		@SuppressWarnings("unchecked")
		Class<CryptoDriver> clazz = triple.third;
		try {
			CryptoDriver driver = clazz.newInstance();
			if(cryptoDriver == null) {
				cryptoDriver = new Holder<CryptoDriver>(driver);
			}else {
				CryptoDriver oldDriver = cryptoDriver.reset(driver);
				closeQuietly(oldDriver);
			}
			return Triple.create(true, "success", driver);
		}catch(Exception e) {
			return Triple.create(false, "failed to new an '"+CryptoDriver.class.getSimpleName()+"' instance of '"+clazz+"'", null);
		}
	}
	
	private void closeQuietly(CryptoDriver driver) {
		if(driver != null) {
			try {
				driver.close();
			}catch(Exception e) {
				line("failed to close "+driver.getClass().getSimpleName()+" quietly");
				printStackTrace(e);
			}
		}
	}
	
	public Triple<Boolean, String, CryptoKey> getEncryptionKey() {
		if(encryptionKey != null) {
			if(!encryptionKey.isChanged) { return Triple.create(true, "success", encryptionKey.value); }
		}
		return loadEncryptionKey();
	}
	
	public Triple<Boolean, String, CryptoKey> loadEncryptionKey() {
		Triple<Boolean, String, String> publicKeyDirTriple = getVariable(Name.PUBLIC_KEY_DIR, String.class);
		if(!publicKeyDirTriple.first) { return Triple.create(false, publicKeyDirTriple.second, null); }
		String publicKeyDir = publicKeyDirTriple.third;
		Triple<Boolean, String, CryptoDriver> cryptoDriverTriple = getCryptoDriver();
		if(!cryptoDriverTriple.first) {
			return Triple.create(false, "failed to get dependency variable '"+Name.CRYPTO_DRIVER+"': "+cryptoDriverTriple.second, null);
		}
		CryptoDriver cryptoDriver = cryptoDriverTriple.third;
		Result<CryptoKey> publicKeyResult = cryptoDriver.loadPublicKey(publicKeyDir);
		if(!publicKeyResult.isSuccess()) {
			return Triple.create(false, "failed to load public key file from '"+publicKeyDir+"': "+publicKeyResult.msg, null);
		}
		if(encryptionKey == null) {
			encryptionKey = new Holder<CryptoKey>(publicKeyResult.result);
		}else {
			encryptionKey.reset(publicKeyResult.result);
		}
		return Triple.create(true, "success", encryptionKey.value);
	}
	
	public Triple<Boolean, String, CryptoKey> getDecryptionKey() {
		if(decryptionKey != null) {
			if(!decryptionKey.isChanged) { return Triple.create(true, "success", decryptionKey.value); }
		}
		return loadDecryptionKey();
	}
	
	public Triple<Boolean, String, CryptoKey> loadDecryptionKey() {
		Triple<Boolean, String, String> privateKeyDirTriple = getVariable(Name.PRIVATE_KEY_DIR, String.class);
		if(!privateKeyDirTriple.first) { return Triple.create(false, privateKeyDirTriple.second, null); }
		String privateKeyDir = privateKeyDirTriple.third;
		Triple<Boolean, String, CryptoDriver> cryptoDriverTriple = getCryptoDriver();
		if(!cryptoDriverTriple.first) {
			if(!cryptoDriverTriple.first) {
				return Triple.create(false, "failed to get dependency variable '"+Name.CRYPTO_DRIVER+"': "+cryptoDriverTriple.second, null);
			}
		}
		CryptoDriver cryptoDriver = cryptoDriverTriple.third;
		Result<CryptoKey> privateKeyResult = cryptoDriver.loadPrivateKey(privateKeyDir);
		if(!privateKeyResult.isSuccess()) {
			return Triple.create(false, "failed to load private key file from '"+privateKeyDir+"': "+privateKeyResult.msg, null);
		}
		if(decryptionKey == null) {
			decryptionKey = new Holder<CryptoKey>(privateKeyResult.result);
		}else {
			decryptionKey.reset(privateKeyResult.result);
		}
		return Triple.create(true, "success", decryptionKey.value);
	}
	
	public Triple<Boolean, String, StoreDriver> getStoreDriver() {
		if(storeDriver != null) {
			if(!storeDriver.isChanged) { return Triple.create(true, "success", storeDriver.value); }
		}
		return loadStoreDriver();
	}
	
	private Triple<Boolean, String, StoreDriver> loadStoreDriver() {
		@SuppressWarnings("rawtypes")
		Triple<Boolean, String, Class> triple = getVariable(Name.STORE_DRIVER, Class.class);
		if(!triple.first) { return Triple.create(false, triple.second, null); }
		@SuppressWarnings("unchecked")
		Class<StoreDriver> clazz = triple.third;
		
		// load dependency variables
		Triple<Boolean, String, String> dataDirTriple = getVariable(Name.DATA_DIR, String.class);
		if(!dataDirTriple.first) {
			return Triple.create(false, "failed to get dependency variable '"+Name.DATA_DIR+"': "+dataDirTriple.second, null);
		}
		String dataDir = dataDirTriple.third;
		Triple<Boolean, String, CryptoDriver> cryptoDriverTriple = getCryptoDriver();
		if(!cryptoDriverTriple.first) {
			return Triple.create(false, "failed to get dependency variable '"+Name.CRYPTO_DRIVER+"': "+cryptoDriverTriple.second, null);
		}
		CryptoDriver cryptoDriver = cryptoDriverTriple.third;
		Triple<Boolean, String, CryptoKey> encryptionKeyTriple = getEncryptionKey();
		if(!encryptionKeyTriple.first) {
			return Triple.create(false, "failed to get dependency variable '"+Name.PRIVATE_KEY_DIR+"': "+encryptionKeyTriple.second, null);
		}
		int secretBlockSize = encryptionKeyTriple.third.maxBlockSize();
		Triple<Boolean, String, Boolean> isDataLockTriple = getVariable(Name.IS_DATA_LOCK, Boolean.class);
		if(!isDataLockTriple.first) {
			return Triple.create(false, "failed to get dependency variable '"+Name.IS_DATA_LOCK+"': "+isDataLockTriple.second, null);
		}
		boolean isDataLock = isDataLockTriple.third;
		try {
			// StoreDriver constructor convention
			Constructor<StoreDriver> constructor = clazz.getConstructor(String.class, CryptoDriver.class, int.class, boolean.class);
			StoreDriver driver = constructor.newInstance(dataDir, cryptoDriver, secretBlockSize, isDataLock);
			if(storeDriver == null) {
				storeDriver = new Holder<StoreDriver>(driver);
			}else {
				StoreDriver oldDriver = storeDriver.reset(driver);
				closeQuietly(oldDriver);
			}
			return Triple.create(true, "success", driver);
		}catch(Exception e) {
			return Triple.create(false, "failed to new an '"+StoreDriver.class.getSimpleName()+"' instance of '"+clazz+"' with dependencies", null);
		}
	}
	
	private void closeQuietly(StoreDriver driver) {
		if(driver != null) {
			boolean isSuccess = true;
			String msg = "";
			Throwable t = null;
			try {
				Result<Throwable> result = driver.close();
				isSuccess = result.isSuccess();
				if(!isSuccess) { msg = result.msg; }
				t = result.result;
			}catch(Exception e) {
				isSuccess = false;
				msg = e.getMessage();
				t = e;
			}
			if(!isSuccess) {
				line("failed to close "+driver.getClass().getSimpleName()+" quietly: "+msg);
				printStackTrace(t);
			}
		}
	}
}
