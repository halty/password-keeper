package com.lee.password.cmdline;

import static com.lee.password.cmdline.Environment.indent;
import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.newLine;
import static com.lee.password.cmdline.Environment.printStackTrace;
import static com.lee.password.cmdline.Environment.prompt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Calendar;

import com.lee.password.cmdline.Environment.Name;
import com.lee.password.keeper.impl.crypto.RSACryptoDriver;
import com.lee.password.keeper.impl.store.BinaryStoreDriver;
import com.lee.password.util.Pair;

public class CmdMain {

	private static final Charset CHARSET = Charset.forName("utf-8");

	private final BufferedReader stdin;
	private final Environment env;
	
	private CmdMain() {
		stdin = new BufferedReader(new InputStreamReader(System.in, CHARSET));
		env = Environment.current();
	}
	
	public static void main(String[] args) {
		new CmdMain().useDefault().loadUserPrefs().run();
	}
	
	private CmdMain useDefault() {
		line("using default setting...");
		String cryptoDriver = RSACryptoDriver.class.getName();
		String isDataLock = "true";
		String storeDriver = BinaryStoreDriver.class.getName();
		Pair<Boolean, String> putResult = env.putVariable(Name.CRYPTO_DRIVER, cryptoDriver);
		indent("use '"+cryptoDriver+"' as default setting for variable '"+Name.CRYPTO_DRIVER.name+"' "
				+(putResult.first ? "successful" : "failed: "+putResult.second));
		putResult = env.putVariable(Name.IS_DATA_LOCK, isDataLock);
		indent("use '"+isDataLock+"' as default setting for variable '"+Name.IS_DATA_LOCK.name+"' "
				+(putResult.first ? "successful" : "failed: "+putResult.second));
		putResult = env.putVariable(Name.STORE_DRIVER, storeDriver);
		indent("use '"+storeDriver+"' as default setting for variable '"+Name.STORE_DRIVER.name+"' "
				+(putResult.first ? "successful" : "failed: "+putResult.second));
		line("use default setting finished");
		return this;
	}
	
	private CmdMain loadUserPrefs() {
		env.loadUserPrefs();
		return this;
	}
	
	private void run() {
		try {
			newLine();
			line(String.format("Good %s %s, welcome to passwod-keeper ^_^", timePeriod(), user()));
			prompt();
			env.signalRunning();
			
			String line = null;
			while(!env.needExitSystem() && (line = stdin.readLine()) != null) {
				Command command = Cmd.parse(line);
				command.execute();
			}
			line("exit system successful!");
			newLine();
		}catch(Exception e) {
			printStackTrace(e);
			line("exit for unexpected exception: "+e.getMessage());
		}
	}
	
	private static String timePeriod() {
		int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		switch(hour) {
		case 5: case 6: case 7: case 8: case 9: case 10: case 11: return "morning";
		case 12: case 13: case 14: case 15: case 16: return "afternoon";
		case 17: case 18: case 19: case 20: return "evening";
		case 21: case 22: case 23: case 0: case 1: case 2: case 3: case 4: return "night";
		default: return "time";
		}
	}
	
	private static String user() { return System.getProperty("user.name", "anonymous"); }

}
