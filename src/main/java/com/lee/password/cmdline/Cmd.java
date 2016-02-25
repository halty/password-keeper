package com.lee.password.cmdline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lee.password.cmdline.Environment.*;

import com.lee.password.keeper.impl.crypto.RSACryptoDriver;
import com.lee.password.keeper.impl.store.BinaryStoreDriver;
import com.lee.password.util.Triple;

public enum Cmd {

	HELP("help", CmdArgs.HELP_ARGS) {
		@Override
		public void printSynopsis() {
			line("help [-h] [-s] [cmd]");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Use examples:");
			indent("help -- show all the command documents");
			indent("help -s -- show all the command synopsises");
			indent("help -h -- show the help info of 'help' command");
			indent("help set -- show the 'set' command documents");
			indent("help generate key -- show the 'generate key' command documents");
		}
	},
	
	LIST_ENV("list env", CmdArgs.LIST_ENV_ARGS) {
		@Override
		public void printSynopsis() {
			line("list env [-h]");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Use examples:");
			indent("list env -- list all the customizable program environment variable names");
			indent("list env -h -- show the help info of 'list env' command");
		}
	},
	
	SET("set", CmdArgs.SET_ARGS) {
		@Override
		public void printSynopsis() {
			line("set [-h] [key value]");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Use examples:");
			indent("set -- show all the program environment variables");
			indent("set -h -- show the help info of 'set' command");
			indent("set keyDir -- show the program environment variable value named with 'keyDir'");
			indent("set keyDir /User/key -- set the program environment variable 'keyDir' of value '/User/key'");
		}
	},
	
	GENERATE_KEY("generate key", CmdArgs.GENERATE_KEY_ARGS) {
		@Override
		public void printSynopsis() {
			line("generate key [-h] -s keySize -p targetKeyDir");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Note:");
			indent("before generate key, you must specify variable named with '" + Name.CRYPTO_DRIVER.name + "'");
			indent("the generated key depend on specific implementation, default is '"
					+ RSACryptoDriver.class.getName() + "'");
			line("Use examples:");
			indent("generate key -h -- show the help info of 'generate key' command");
			indent("generate key -s 1024 -p '/User/key' -- genearte key with 1024 bits and save them to directory '/User/key'");
		}
	},
	
	ADD_WEB("add web", CmdArgs.ADD_WEB_ARGS) {
		@Override
		public void printSynopsis() {
			line("add web [-h] -k keyword -u url");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Note:");
			indent("before add website, you must specify variables '" + Name.CRYPTO_DRIVER.name + "' and '" + Name.STORE_DRIVER.name + "'");
			indent("default '" + Name.CRYPTO_DRIVER.name + "' implementation is '" + RSACryptoDriver.class.getName() + "'");
			indent("default '" + Name.STORE_DRIVER.name + "' implementation is '" + BinaryStoreDriver.class.getName() + "'");
			line("Use examples:");
			indent("add web -h -- show the help info of 'add web' command");
			indent("add web -k amazon -u 'www.amazon.com' -- add 'amazon' website with url 'www.amazon.com'");
		}
	},
	
	REMOVE_WEB("remove web", CmdArgs.REMOVE_WEB_ARGS) {
		@Override
		public void printSynopsis() {
			line("remove web [-h] [-k keyword] [-i websiteId]");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Note:");
			indent("while removing a website, you must specify the keyword or websiteId, or both for target removing website");
			indent("before remove website, you must specify variables '" + Name.CRYPTO_DRIVER.name + "' and '" + Name.STORE_DRIVER.name + "'");
			indent("default '" + Name.CRYPTO_DRIVER.name + "' implementation is '" + RSACryptoDriver.class.getName() + "'");
			indent("default '" + Name.STORE_DRIVER.name + "' implementation is '" + BinaryStoreDriver.class.getName() + "'");
			line("Use examples:");
			indent("remove web -h -- show the help info of 'remove web' command");
			indent("remove web -k amazon -- remove a website by keyword 'amazon'");
			indent("remove web -i 13457927563219 -- remove a website by websiteId '13457927563219'");
		}
	},
	
	CHANGE_WEB("change web", CmdArgs.CHANGE_WEB_ARGS) {
		@Override
		public void printSynopsis() {
			line("change web [-h] [-i websiteId] [-k keyword] [-u url]");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Note:");
			indent("you can change the url by websiteId or keyword, or change the keyword and url by websiteId");
			indent("before change website, you must specify variables '" + Name.CRYPTO_DRIVER.name + "' and '" + Name.STORE_DRIVER.name + "'");
			indent("default '" + Name.CRYPTO_DRIVER.name + "' implementation is '" + RSACryptoDriver.class.getName() + "'");
			indent("default '" + Name.STORE_DRIVER.name + "' implementation is '" + BinaryStoreDriver.class.getName() + "'");
			line("Use examples:");
			indent("change web -h -- show the help info of 'change web' command");
			indent("change web -i 13457927563219 -u 'www.amazon.cn' -- change a website url to 'www.amazon.cn' by websiteId '13457927563219'");
			indent("change web -i 13457927563219 -k az -u 'www.amazon.cn' -- change a website keyword to 'az' "
					+ "and url to 'www.amazon.cn' by websiteId '13457927563219'");
			indent("change web -k amazon -u 'www.amazon.cn' -- change a website url to 'www.amazon.cn' by keyword 'amazon'");
		}
	},
	
	QUERY_WEB("query web", CmdArgs.QUERY_WEB_ARGS) {
		@Override
		public void printSynopsis() {
			line("query web [-h] [-i websiteId] [-k keyword]");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Note:");
			indent("you can query the website by websiteId or keyword, or both");
			indent("before query website, you must specify variables '" + Name.CRYPTO_DRIVER.name + "' and '" + Name.STORE_DRIVER.name + "'");
			indent("default '" + Name.CRYPTO_DRIVER.name + "' implementation is '" + RSACryptoDriver.class.getName() + "'");
			indent("default '" + Name.STORE_DRIVER.name + "' implementation is '" + BinaryStoreDriver.class.getName() + "'");
			line("Use examples:");
			indent("query web -h -- show the help info of 'query web' command");
			indent("query web -i 13457927563219 -- query the website by websiteId '13457927563219'");
			indent("query web -k amazon -- query the website by keyword 'amazon'");
		}
	},
	
	COUNT_WEB("count web", CmdArgs.COUNT_WEB_ARGS) {
		@Override
		public void printSynopsis() {
			line("count web [-h]");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Note:");
			indent("before count website, you must specify variables '" + Name.CRYPTO_DRIVER.name + "' and '" + Name.STORE_DRIVER.name + "'");
			indent("default '" + Name.CRYPTO_DRIVER.name + "' implementation is '" + RSACryptoDriver.class.getName() + "'");
			indent("default '" + Name.STORE_DRIVER.name + "' implementation is '" + BinaryStoreDriver.class.getName() + "'");
			line("Use examples:");
			indent("count web -- count the number of website");
			indent("count web -h -- show the help info of 'count web' command");
		}
	},
	
	LIST_WEB("list web", CmdArgs.LIST_WEB_ARGS) {
		@Override
		public void printSynopsis() {
			line("list web [-h]");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Note:");
			indent("before list website, you must specify variables '" + Name.CRYPTO_DRIVER.name + "' and '" + Name.STORE_DRIVER.name + "'");
			indent("default '" + Name.CRYPTO_DRIVER.name + "' implementation is '" + RSACryptoDriver.class.getName() + "'");
			indent("default '" + Name.STORE_DRIVER.name + "' implementation is '" + BinaryStoreDriver.class.getName() + "'");
			line("Use examples:");
			indent("list web -- list all the stored websites");
			indent("list web -h -- show the help info of 'list web' command");
		}
	},
	
	ADD_PWD("add pwd", CmdArgs.ADD_PWD_ARGS) {
		@Override
		public void printSynopsis() {
			line("add pwd [-h] -i websiteId -n username -p password [-m memo]");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Note:");
			indent("before add password, you must specify variables '" + Name.CRYPTO_DRIVER.name + "' and '" + Name.STORE_DRIVER.name + "'");
			indent("default '" + Name.CRYPTO_DRIVER.name + "' implementation is '" + RSACryptoDriver.class.getName() + "'");
			indent("default '" + Name.STORE_DRIVER.name + "' implementation is '" + BinaryStoreDriver.class.getName() + "'");
			line("Use examples:");
			indent("add pwd -h -- show the help info of 'add pwd' command");
			indent("add pwd -i 13457927563219 -n 'peter' -p 123456 -- add username 'peter' and password '123456' to website which id is '13457927563219'");
			indent("add pwd -i 13457927563219 -n 'julia' -p 234567 -m 'payCode=love' -- add username 'julia' and password '234567' "
					+ "with memo 'payCode=love' to website which id is '13457927563219'");
		}
	},
	
	REMOVE_PWD("remove pwd", CmdArgs.REMOVE_PWD_ARGS) {
		@Override
		public void printSynopsis() {
			line("remove pwd [-h] -i websiteId -n username");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Note:");
			indent("before remove password, you must specify variables '" + Name.CRYPTO_DRIVER.name + "' and '" + Name.STORE_DRIVER.name + "'");
			indent("default '" + Name.CRYPTO_DRIVER.name + "' implementation is '" + RSACryptoDriver.class.getName() + "'");
			indent("default '" + Name.STORE_DRIVER.name + "' implementation is '" + BinaryStoreDriver.class.getName() + "'");
			line("Use examples:");
			indent("remove pwd -h -- show the help info of 'remove pwd' command");
			indent("remove pwd -i 13457927563219 -n 'peter' -- remove password by username 'peter' from website which id is '13457927563219'");
		}
	},
	
	CHANGE_PWD("change pwd", CmdArgs.CHANGE_PWD_ARGS) {
		@Override
		public void printSynopsis() {
			line("change pwd [-h] -i websiteId -n username [-p password] [-m memo]");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Note:");
			indent("before change password, you must specify variables '" + Name.CRYPTO_DRIVER.name + "' and '" + Name.STORE_DRIVER.name + "'");
			indent("default '" + Name.CRYPTO_DRIVER.name + "' implementation is '" + RSACryptoDriver.class.getName() + "'");
			indent("default '" + Name.STORE_DRIVER.name + "' implementation is '" + BinaryStoreDriver.class.getName() + "'");
			line("Use examples:");
			indent("change pwd -h -- show the help info of 'change pwd' command");
			indent("change pwd -i 13457927563219 -n 'peter' -p 987654 -- change password to '987654' by username 'peter' from website which id is '13457927563219'");
			indent("change pwd -i 13457927563219 -n 'julia' -m 'payCode=hate' -- change password memo to 'payCode=hate' by username 'julia' "
					+ "from website which id is '13457927563219'");
			indent("change pwd -i 13457927563219 -n 'julia' -p 876543 -m 'payCode=hate' -- change password to '987654' and memo to 'payCode=hate' by username 'julia' "
					+ "from website which id is '13457927563219'");
		}
	},
	
	QUERY_PWD("query pwd", CmdArgs.QUERY_PWD_ARGS) {
		@Override
		public void printSynopsis() {
			line("query pwd [-h] -i websiteId -n username");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Note:");
			indent("before query password, you must specify variables '" + Name.CRYPTO_DRIVER.name + "' and '" + Name.STORE_DRIVER.name + "'");
			indent("default '" + Name.CRYPTO_DRIVER.name + "' implementation is '" + RSACryptoDriver.class.getName() + "'");
			indent("default '" + Name.STORE_DRIVER.name + "' implementation is '" + BinaryStoreDriver.class.getName() + "'");
			line("Use examples:");
			indent("query pwd -h -- show the help info of 'query pwd' command");
			indent("query pwd -i 13457927563219 -n 'peter' -- query password by username 'peter' from website which id is '13457927563219'");
		}
	},
	
	COUNT_PWD("count pwd", CmdArgs.COUNT_PWD_ARGS) {
		@Override
		public void printSynopsis() {
			line("count pwd [-h] [-i websiteId] [-n username]");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Note:");
			indent("before count password, you must specify variables '" + Name.CRYPTO_DRIVER.name + "' and '" + Name.STORE_DRIVER.name + "'");
			indent("default '" + Name.CRYPTO_DRIVER.name + "' implementation is '" + RSACryptoDriver.class.getName() + "'");
			indent("default '" + Name.STORE_DRIVER.name + "' implementation is '" + BinaryStoreDriver.class.getName() + "'");
			line("Use examples:");
			indent("count pwd -- count the total number of password");
			indent("count pwd -h -- show the help info of 'count pwd' command");
			indent("count pwd -i 13457927563219 -- count the number of password from website which id is '13457927563219'");
			indent("count pwd -n 'peter' -- count the number of password by username 'peter'");
			indent("count pwd -i 13457927563219 -n 'peter' -- count the number of password by username 'peter' "
					+ "from website which id is '13457927563219'");
		}
	},
	
	LIST_PWD("list pwd", CmdArgs.LIST_PWD_ARGS) {
		@Override
		public void printSynopsis() {
			line("list pwd [-h] [-i websiteId] [-n username]");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Note:");
			indent("before list password, you must specify variables '" + Name.CRYPTO_DRIVER.name + "' and '" + Name.STORE_DRIVER.name + "'");
			indent("default '" + Name.CRYPTO_DRIVER.name + "' implementation is '" + RSACryptoDriver.class.getName() + "'");
			indent("default '" + Name.STORE_DRIVER.name + "' implementation is '" + BinaryStoreDriver.class.getName() + "'");
			line("Use examples:");
			indent("list pwd -h -- show the help info of 'list pwd' command");
			indent("list pwd -i 13457927563219 -- list passwords from website which id is '13457927563219'");
			indent("list pwd -n 'peter' -- list passwords by username 'peter'");
			indent("list pwd -i 13457927563219 -n 'peter' -- list passwords by username 'peter' from website which id is '13457927563219'");
		}
	},
	
	UNDO("undo", CmdArgs.UNDO_ARGS) {
		@Override
		public void printSynopsis() {
			line("undo [-h]");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Note:");
			indent("before undo, you must specify variables '" + Name.CRYPTO_DRIVER.name + "' and '" + Name.STORE_DRIVER.name + "'");
			indent("default '" + Name.CRYPTO_DRIVER.name + "' implementation is '" + RSACryptoDriver.class.getName() + "'");
			indent("default '" + Name.STORE_DRIVER.name + "' implementation is '" + BinaryStoreDriver.class.getName() + "'");
			line("Use examples:");
			indent("undo -- undo the last uncommitted change operation");
			indent("undo -h -- show the help info of 'undo' command");
		}
	},
	
	REDO("redo", CmdArgs.REDO_ARGS) {
		@Override
		public void printSynopsis() {
			line("redo [-h]");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Note:");
			indent("before redo, you must specify variables '" + Name.CRYPTO_DRIVER.name + "' and '" + Name.STORE_DRIVER.name + "'");
			indent("default '" + Name.CRYPTO_DRIVER.name + "' implementation is '" + RSACryptoDriver.class.getName() + "'");
			indent("default '" + Name.STORE_DRIVER.name + "' implementation is '" + BinaryStoreDriver.class.getName() + "'");
			line("Use examples:");
			indent("redo -- redo the last undo change operation");
			indent("redo -h -- show the help info of 'redo' command");
		}
	},
	
	COMMIT("commit", CmdArgs.COMMIT_ARGS) {
		@Override
		public void printSynopsis() {
			line("commit [-h]");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Note:");
			indent("before commit, you must specify variables '" + Name.CRYPTO_DRIVER.name + "' and '" + Name.STORE_DRIVER.name + "'");
			indent("default '" + Name.CRYPTO_DRIVER.name + "' implementation is '" + RSACryptoDriver.class.getName() + "'");
			indent("default '" + Name.STORE_DRIVER.name + "' implementation is '" + BinaryStoreDriver.class.getName() + "'");
			line("Use examples:");
			indent("commit -- commit all the change operation since the last commit operation, if commit success, all the change "
					+ "will be flush to underlying storage, and can not be undo by '"+UNDO.cmd()+"' command");
			indent("commit -h -- show the help info of 'commit' command");
		}
	},
	
	EXIT("exit", CmdArgs.EXIT_ARGS) {
		@Override
		public void printSynopsis() {
			line("exit [-h]");
		}
		@Override
		public void printDoc() {
			printSynopsis();
			line("Use examples:");
			indent("exit -- releases any associated system resources and exit");
			indent("exit -h -- show the help info of 'exit' command");
		}
	},
	;
	
	private final String cmd;
	private final int cmdNameCount;
	private final String primaryName;
	private final String secondaryName;
	private final CmdArgs cmdArgs;
	
	private Cmd(String cmd, CmdArgs cmdArgs) {
		this.cmd = cmd;
		String[] names = cmd.split(" ", 2);
		this.cmdNameCount = names.length;
		this.primaryName = names[0];
		this.secondaryName = cmdNameCount == 2 ? names[1] : null;
		this.cmdArgs = cmdArgs;
	}
	
	public String cmd() { return cmd; }
	
	public Command parse(List<String> cmdArgsList) { return cmdArgs.parse(cmdArgsList); }
	
	/** print command synopsis to standard output stream **/
	public abstract void printSynopsis();
	
	/** print command document to standard output stream **/
	public abstract void printDoc();
	
	public static final Command parse(String commandLine) {
		Triple<Boolean, String, List<String>> result = split(commandLine);
		if(!result.first) {
			return incorrectCommand("incorrect command: "+result.second);
		}
		
		List<String> cmdWordList = result.third;
		int size = cmdWordList.size();
		if(size == 0) { return incorrectCommand("command line is empty"); }
		String primaryName = cmdWordList.get(0);
		String secondaryName = (size == 1 ? null : cmdWordList.get(1));
		Cmd cmd = match(primaryName, secondaryName);
		if(cmd == null) { return incorrectCommand("no matched command"); }
		return cmd.parse(cmdWordList.subList(cmd.cmdNameCount, cmdWordList.size()));
	}
	
	/**
	 * Split command line by whitespace delimiter. whitespace is not considered as delimiter
	 * with surround in single quotes {@code '}.
	 */
	private static Triple<Boolean, String, List<String>> split(String commandLine) {
		if(commandLine == null) {
			List<String> emptyList = Collections.emptyList();
			return new Triple<Boolean, String, List<String>>(false, "command line is null", emptyList);
		}

		int length = commandLine.length();
		int index = 0;
		int begin = 0;
		boolean isBeginWithSingleQuotes = false;
		List<String> wordList = new ArrayList<String>();
		while(index < length) {
			char ch = commandLine.charAt(index);
			if(ch == '\'') {
				if(isBeginWithSingleQuotes) {
					wordList.add(commandLine.substring(begin+1, index));
					begin = index + 1;
					isBeginWithSingleQuotes = false;
				}else {
					if(begin == index) {
						isBeginWithSingleQuotes = true;
					}else {
						List<String> emptyList = Collections.emptyList();
						return new Triple<Boolean, String, List<String>>(
								false, "unexpected end single quotes at index "+(index+1), emptyList);
					}
				}
			}else {
				if(!isBeginWithSingleQuotes && Character.isWhitespace(ch)) {
					if(begin < index) { wordList.add(commandLine.substring(begin, index)); }
					// skip consecutive whitespace
					while(++index < length && Character.isWhitespace(commandLine.charAt(index)));
					begin = index;
					isBeginWithSingleQuotes = index < length && commandLine.charAt(index) == '\'';
				}
			}
			index++;
		}
		if(begin < length) {
			if(isBeginWithSingleQuotes) {
				List<String> emptyList = Collections.emptyList();
				return new Triple<Boolean, String, List<String>>(false, "without end single quotes", emptyList);
			}else {
				wordList.add(commandLine.substring(begin, length));
			}
		}
		return new Triple<Boolean, String, List<String>>(true, "success", wordList);
	}

	static Command incorrectCommand(final String tips) {
		return new Command() {
			@Override
			public void execute() {
				line(tips);
				prompt();
			}
		};
	}
	
	static Cmd match(String primaryName, String secondaryName) {
		Object value = NAME_MAP.get(primaryName);
		if(value == null) { return null; }
		if(value instanceof Cmd) {
			return (Cmd) value;
		}else {
			if(secondaryName == null) { return null; }
			@SuppressWarnings("unchecked")
			Map<String, Cmd> map = (Map<String, Cmd>) value;
			return map.get(secondaryName);
		}
	}
	
	private static final Map<String, Object> NAME_MAP = new HashMap<String, Object>();
	static {
		Cmd[] cmdArray = Cmd.values();
		for(Cmd cmd : cmdArray) {
			if(cmd.cmdNameCount == 1) {
				NAME_MAP.put(cmd.primaryName, cmd);
			}else {
				@SuppressWarnings("unchecked")
				Map<String, Cmd> map = (Map<String, Cmd>) NAME_MAP.get(cmd.primaryName);
				if(map == null) {
					map = new HashMap<String, Cmd>();
					NAME_MAP.put(cmd.primaryName, map);
				}
				map.put(cmd.secondaryName, cmd);
			}
		}
	}
}
