package com.lee.password.cmdline;

import static com.lee.password.cmdline.Environment.line;
import static com.lee.password.cmdline.Environment.printStackTrace;
import static com.lee.password.cmdline.Environment.prompt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Calendar;

public class CmdMain {
	
	private static final int SUCCESS_EXIT = 0;
	private static final Charset CHARSET = Charset.forName("utf-8");

	private final BufferedReader stdin;
	private final Environment env;
	
	private CmdMain() {
		stdin = new BufferedReader(new InputStreamReader(System.in, CHARSET));
		env = Environment.current();
	}
	
	public static void main(String[] args) {
		System.exit(new CmdMain().run());
	}
	
	private int run() {
		try {
			line(String.format("Good %s %s, welcome to passwod-keeper ^_^", timePeriod(), user()));
			prompt();
			env.signalRunning();
			
			String line = null;
			while((line = stdin.readLine()) != null) {
				Command command = Cmd.parse(line);
				command.execute();
			}
		}catch(Exception e) {
			printStackTrace(e);
			line("exit for unexpected exception: "+e.getMessage());
		}
		env.signalExit();
		return SUCCESS_EXIT;
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
