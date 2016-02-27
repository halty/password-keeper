package com.lee.password.cmdline;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.lee.password.cmdline.Environment.Name;
import com.lee.password.cmdline.commands.AddPwdCommand;
import com.lee.password.cmdline.commands.AddWebCommand;
import com.lee.password.cmdline.commands.ChangePwdCommand;
import com.lee.password.cmdline.commands.ChangeWebCommand;
import com.lee.password.cmdline.commands.CommitCommand;
import com.lee.password.cmdline.commands.CountPwdCommand;
import com.lee.password.cmdline.commands.CountWebCommand;
import com.lee.password.cmdline.commands.ExitCommand;
import com.lee.password.cmdline.commands.GenerateKeyCommand;
import com.lee.password.cmdline.commands.HelpCommand;
import com.lee.password.cmdline.commands.ListEnvCommand;
import com.lee.password.cmdline.commands.ListPwdCommand;
import com.lee.password.cmdline.commands.ListWebCommand;
import com.lee.password.cmdline.commands.QueryPwdCommand;
import com.lee.password.cmdline.commands.QueryWebCommand;
import com.lee.password.cmdline.commands.RedoCommand;
import com.lee.password.cmdline.commands.RemovePwdCommand;
import com.lee.password.cmdline.commands.RemoveWebCommand;
import com.lee.password.cmdline.commands.SetCommand;
import com.lee.password.cmdline.commands.UndoCommand;
import com.lee.password.util.Converter;
import com.lee.password.util.Triple;

import static com.lee.password.util.Converter.*;

import com.lee.password.util.Pair;

public enum CmdArgs {

	HELP_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			String primaryName = null, secondaryName = null;
			switch(size) {
			case 2:
				secondaryName = cmdArgsList.get(1);
			case 1:
				String field = cmdArgsList.get(0);
				if("-h".equals(field)) {
					return new HelpCommand(Cmd.HELP);
				}else if("-s".equals(field)) {
					return new HelpCommand(true);
				}else {
					primaryName = field;
					Cmd cmd = Cmd.match(primaryName, secondaryName);
					return cmd == null ? Cmd.incorrectCommand("no matched command for '" + Cmd.HELP.cmd() + "' command")
							: new HelpCommand(cmd);
				}
			case 0: return new HelpCommand(false);
			default:
				return Cmd.incorrectCommand("incorrect command arguments for '" + Cmd.HELP.cmd() + "' command");
			}
		}
	},
	
	LIST_ENV_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			switch(size) {
			case 0:
				return new ListEnvCommand();
			case 1:
				if("-h".equals(cmdArgsList.get(0))) {
					return new HelpCommand(Cmd.LIST_ENV);
				}
			default:
				return Cmd.incorrectCommand("incorrect command arguments for '" + Cmd.LIST_ENV.cmd() + "' command");
			}
		}
	},
	
	SET_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			switch(size) {
			case 0: return new SetCommand();
			case 1:
				if("-h".equals(cmdArgsList.get(0))) {
					return new HelpCommand(Cmd.SET);
				}
				String first = cmdArgsList.get(0);
				Name name = Environment.nameOf(first);
				if(name == null) {
					return Cmd.incorrectCommand("no program environment variable named with '" + first + "'");
				}
				return new SetCommand(name);
			case 2:
				first = cmdArgsList.get(0);
				String second = cmdArgsList.get(1);
				Name variableName = Environment.nameOf(first);
				if(variableName == null) {
					return Cmd.incorrectCommand("no program environment variable named with '" + first + "'");
				}
				return new SetCommand(variableName, second, false);
			case 3:
				first = cmdArgsList.get(0);
				second = cmdArgsList.get(1);
				String third = cmdArgsList.get(2);
				if("-p".equals(first)) {
					variableName = Environment.nameOf(second);
					if(variableName == null) {
						return Cmd.incorrectCommand("no program environment variable named with '" + second + "'");
					}
					return new SetCommand(variableName, third, true);
				}else if("-p".equals(third)) {
					variableName = Environment.nameOf(first);
					if(variableName == null) {
						return Cmd.incorrectCommand("no program environment variable named with '" + first + "'");
					}
					return new SetCommand(variableName, second, true);
				}
			default:
				return Cmd.incorrectCommand("incorrect command arguments for '" + Cmd.SET.cmd() + "' command");
			}
		}
	},
	
	@SuppressWarnings("unchecked")
	GENERATE_KEY_ARGS(
		triple("-s", true, INTEGER_CONVERTER),
		triple("-p", true, DIR_CONVERTER)
	) {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			if(size == 1 && "-h".equals(cmdArgsList.get(0))) { return new HelpCommand(Cmd.GENERATE_KEY); }
			Triple<Boolean, String, Map<String, Object>> triple = parseArgs(Cmd.GENERATE_KEY, cmdArgsList);
			if(!triple.first) { return Cmd.incorrectCommand(triple.second); }
			Triple<Boolean, String, Integer> keyTriple = parseArgsValue(Cmd.GENERATE_KEY, triple.third, "-s", Integer.class);
			if(!keyTriple.first) { return Cmd.incorrectCommand(keyTriple.second); }
			Triple<Boolean, String, File> dirTriple = parseArgsValue(Cmd.GENERATE_KEY, triple.third, "-p", File.class);
			if(!dirTriple.first) { return Cmd.incorrectCommand(dirTriple.second); }
			return new GenerateKeyCommand(keyTriple.third, dirTriple.third);
		}
	},
	
	@SuppressWarnings("unchecked")
	ADD_WEB_ARGS(
		triple("-k", true, STRING_CONVERTER),
		triple("-u", true, STRING_CONVERTER)
	) {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			if(size == 1 && "-h".equals(cmdArgsList.get(0))) { return new HelpCommand(Cmd.ADD_WEB); }
			Triple<Boolean, String, Map<String, Object>> triple = parseArgs(Cmd.ADD_WEB, cmdArgsList);
			if(!triple.first) { return Cmd.incorrectCommand(triple.second); }
			Triple<Boolean, String, String> keywordTriple = parseArgsValue(Cmd.ADD_WEB, triple.third, "-k", String.class);
			if(!keywordTriple.first) { return Cmd.incorrectCommand(keywordTriple.second); }
			Triple<Boolean, String, String> urlTriple = parseArgsValue(Cmd.ADD_WEB, triple.third, "-u", String.class);
			if(!urlTriple.first) { return Cmd.incorrectCommand(urlTriple.second); }
			return new AddWebCommand(keywordTriple.third, urlTriple.third);
		}
	},
	
	@SuppressWarnings("unchecked")
	REMOVE_WEB_ARGS(
		triple("-k", false, STRING_CONVERTER),
		triple("-i", false, LONG_CONVERTER)
	) {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			if(size == 1 && "-h".equals(cmdArgsList.get(0))) { return new HelpCommand(Cmd.REMOVE_WEB); }
			Triple<Boolean, String, Map<String, Object>> triple = parseArgs(Cmd.REMOVE_WEB, cmdArgsList);
			if(!triple.first) { return Cmd.incorrectCommand(triple.second); }
			Triple<Boolean, String, String> keywordTriple = parseArgsValue(Cmd.REMOVE_WEB, triple.third, "-k", String.class);
			String keyword = keywordTriple.third;
			Triple<Boolean, String, Long> webisteIdTriple = parseArgsValue(Cmd.REMOVE_WEB, triple.third, "-i", Long.class);
			Long websiteId = webisteIdTriple.third;
			if(keyword == null && websiteId == null) {
				return Cmd.incorrectCommand("while remove website, keyword or websiteId don't specified, "
						+ "please run 'help remove web' command to check the use examples");
			}
			return new RemoveWebCommand(websiteId, keyword);
		}
	},
	
	@SuppressWarnings("unchecked")
	CHANGE_WEB_ARGS(
		triple("-i", false, LONG_CONVERTER),
		triple("-k", false, STRING_CONVERTER),
		triple("-u", false, STRING_CONVERTER)
	) {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			if(size == 1 && "-h".equals(cmdArgsList.get(0))) { return new HelpCommand(Cmd.CHANGE_WEB); }
			Triple<Boolean, String, Map<String, Object>> triple = parseArgs(Cmd.CHANGE_WEB, cmdArgsList);
			if(!triple.first) { return Cmd.incorrectCommand(triple.second); }
			Map<String, Object> valueMap = triple.third;
			int argsCount = 0;
			Triple<Boolean, String, Long> webisteIdTriple = parseArgsValue(Cmd.CHANGE_WEB, valueMap, "-i", Long.class);
			Long websiteId = webisteIdTriple.third;
			if(websiteId != null) { argsCount++; }
			Triple<Boolean, String, String> keywordTriple = parseArgsValue(Cmd.CHANGE_WEB, valueMap, "-k", String.class);
			String keyword = keywordTriple.third;
			if(keyword != null && !keyword.isEmpty()) { argsCount++; }
			Triple<Boolean, String, String> urlTriple = parseArgsValue(Cmd.CHANGE_WEB, valueMap, "-u", String.class);
			String url = urlTriple.third;
			if(url != null && !url.isEmpty()) { argsCount++; }
			if(argsCount <= 1) {
				return Cmd.incorrectCommand("while change website, not enough arguments, please run 'help change web' command "
						+ "to check the use examples");
			}
			return new ChangeWebCommand(websiteId, keyword, url);
		}
	},
	
	@SuppressWarnings("unchecked")
	QUERY_WEB_ARGS(
		triple("-k", false, STRING_CONVERTER),
		triple("-i", false, LONG_CONVERTER)
	) {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			if(size == 1 && "-h".equals(cmdArgsList.get(0))) { return new HelpCommand(Cmd.QUERY_WEB); }
			Triple<Boolean, String, Map<String, Object>> triple = parseArgs(Cmd.QUERY_WEB, cmdArgsList);
			if(!triple.first) { return Cmd.incorrectCommand(triple.second); }
			Triple<Boolean, String, String> keywordTriple = parseArgsValue(Cmd.QUERY_WEB, triple.third, "-k", String.class);
			String keyword = keywordTriple.third;
			Triple<Boolean, String, Long> webisteIdTriple = parseArgsValue(Cmd.QUERY_WEB, triple.third, "-i", Long.class);
			Long websiteId = webisteIdTriple.third;
			if(keyword == null && websiteId == null) {
				return Cmd.incorrectCommand("while query website, keyword or websiteId don't specified, "
						+ "please run 'help query web' command to check the use examples");
			}
			return new QueryWebCommand(websiteId, keyword);
		}
	},
	
	COUNT_WEB_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			switch(size) {
			case 0:
				return new CountWebCommand();
			case 1:
				if("-h".equals(cmdArgsList.get(0))) {
					return new HelpCommand(Cmd.COUNT_WEB);
				}
			default:
				return Cmd.incorrectCommand("incorrect command arguments for '" + Cmd.COUNT_WEB.cmd() + "' command");
			}
		}
	},
	
	LIST_WEB_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			switch(size) {
			case 0:
				return new ListWebCommand();
			case 1:
				if("-h".equals(cmdArgsList.get(0))) {
					return new HelpCommand(Cmd.LIST_WEB);
				}
			default:
				return Cmd.incorrectCommand("incorrect command arguments for '" + Cmd.LIST_WEB.cmd() + "' command");
			}
		}
	},
	
	@SuppressWarnings("unchecked")
	ADD_PWD_ARGS(
		triple("-i", false, LONG_CONVERTER),
		triple("-k", false, STRING_CONVERTER),
		triple("-n", true, STRING_CONVERTER),
		triple("-p", true, STRING_CONVERTER),
		triple("-m", false, STRING_CONVERTER)
	) {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			if(size == 1 && "-h".equals(cmdArgsList.get(0))) { return new HelpCommand(Cmd.ADD_PWD); }
			Triple<Boolean, String, Map<String, Object>> triple = parseArgs(Cmd.ADD_PWD, cmdArgsList);
			if(!triple.first) { return Cmd.incorrectCommand(triple.second); }
			Triple<Boolean, String, Long> websiteIdTriple = parseArgsValue(Cmd.ADD_PWD, triple.third, "-i", Long.class);
			Triple<Boolean, String, String> websiteKeywordTriple = parseArgsValue(Cmd.ADD_PWD, triple.third, "-k", String.class);
			if(!websiteIdTriple.first && !websiteKeywordTriple.first) {
				return Cmd.incorrectCommand("while add password, websiteId or websiteKeyword don't specified, "
						+ "please run 'help add pwd' command to check the use examples");
			}
			Triple<Boolean, String, String> usernameTriple = parseArgsValue(Cmd.ADD_PWD, triple.third, "-n", String.class);
			if(!usernameTriple.first) { return Cmd.incorrectCommand(usernameTriple.second); }
			Triple<Boolean, String, String> passwordTriple = parseArgsValue(Cmd.ADD_PWD, triple.third, "-p", String.class);
			if(!passwordTriple.first) { return Cmd.incorrectCommand(passwordTriple.second); }
			Triple<Boolean, String, String> memoTriple = parseArgsValue(Cmd.ADD_PWD, triple.third, "-m", String.class);
			return new AddPwdCommand(websiteKeywordTriple.third, websiteIdTriple.third, usernameTriple.third, passwordTriple.third, memoTriple.third);
		}
	},
	
	@SuppressWarnings("unchecked")
	REMOVE_PWD_ARGS(
		triple("-i", false, LONG_CONVERTER),
		triple("-k", false, STRING_CONVERTER),
		triple("-n", true, STRING_CONVERTER)
	) {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			if(size == 1 && "-h".equals(cmdArgsList.get(0))) { return new HelpCommand(Cmd.REMOVE_PWD); }
			Triple<Boolean, String, Map<String, Object>> triple = parseArgs(Cmd.REMOVE_PWD, cmdArgsList);
			if(!triple.first) { return Cmd.incorrectCommand(triple.second); }
			Triple<Boolean, String, Long> websiteIdTriple = parseArgsValue(Cmd.REMOVE_PWD, triple.third, "-i", Long.class);
			Triple<Boolean, String, String> websiteKeywordTriple = parseArgsValue(Cmd.REMOVE_PWD, triple.third, "-k", String.class);
			if(!websiteIdTriple.first && !websiteKeywordTriple.first) {
				return Cmd.incorrectCommand("while remove password, websiteId or websiteKeyword don't specified, "
						+ "please run 'help remove pwd' command to check the use examples");
			}
			Triple<Boolean, String, String> usernameTriple = parseArgsValue(Cmd.REMOVE_PWD, triple.third, "-n", String.class);
			if(!usernameTriple.first) { return Cmd.incorrectCommand(usernameTriple.second); }
			return new RemovePwdCommand(websiteKeywordTriple.third, websiteIdTriple.third, usernameTriple.third);
		}
	},
	
	@SuppressWarnings("unchecked")
	CHANGE_PWD_ARGS(
		triple("-i", false, LONG_CONVERTER),
		triple("-k", false, STRING_CONVERTER),
		triple("-n", true, STRING_CONVERTER),
		triple("-p", false, STRING_CONVERTER),
		triple("-m", false, STRING_CONVERTER)
	) {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			if(size == 1 && "-h".equals(cmdArgsList.get(0))) { return new HelpCommand(Cmd.CHANGE_PWD); }
			Triple<Boolean, String, Map<String, Object>> triple = parseArgs(Cmd.CHANGE_PWD, cmdArgsList);
			if(!triple.first) { return Cmd.incorrectCommand(triple.second); }
			Triple<Boolean, String, Long> websiteIdTriple = parseArgsValue(Cmd.CHANGE_PWD, triple.third, "-i", Long.class);
			Triple<Boolean, String, String> websiteKeywordTriple = parseArgsValue(Cmd.CHANGE_PWD, triple.third, "-k", String.class);
			if(!websiteIdTriple.first && !websiteKeywordTriple.first) {
				return Cmd.incorrectCommand("while change password, websiteId or websiteKeyword don't specified, "
						+ "please run 'help change pwd' command to check the use examples");
			}
			Triple<Boolean, String, String> usernameTriple = parseArgsValue(Cmd.CHANGE_PWD, triple.third, "-n", String.class);
			if(!usernameTriple.first) { return Cmd.incorrectCommand(usernameTriple.second); }
			Triple<Boolean, String, String> passwordTriple = parseArgsValue(Cmd.CHANGE_PWD, triple.third, "-p", String.class);
			Triple<Boolean, String, String> memoTriple = parseArgsValue(Cmd.CHANGE_PWD, triple.third, "-m", String.class);
			if(!passwordTriple.first && !memoTriple.first) {
				return Cmd.incorrectCommand("while change password, not enough arguments, please run 'help change pwd' command "
						+ "to check the use examples");
			}
			return new ChangePwdCommand(websiteKeywordTriple.third, websiteIdTriple.third, usernameTriple.third, passwordTriple.third, memoTriple.third);
		}
	},
	
	@SuppressWarnings("unchecked")
	QUERY_PWD_ARGS(
		triple("-i", false, LONG_CONVERTER),
		triple("-k", false, STRING_CONVERTER),
		triple("-n", true, STRING_CONVERTER)
	) {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			if(size == 1 && "-h".equals(cmdArgsList.get(0))) { return new HelpCommand(Cmd.QUERY_PWD); }
			Triple<Boolean, String, Map<String, Object>> triple = parseArgs(Cmd.QUERY_PWD, cmdArgsList);
			if(!triple.first) { return Cmd.incorrectCommand(triple.second); }
			Triple<Boolean, String, Long> websiteIdTriple = parseArgsValue(Cmd.QUERY_PWD, triple.third, "-i", Long.class);
			Triple<Boolean, String, String> websiteKeywordTriple = parseArgsValue(Cmd.QUERY_PWD, triple.third, "-k", String.class);
			if(!websiteIdTriple.first && !websiteKeywordTriple.first) {
				return Cmd.incorrectCommand("while query password, websiteId or websiteKeyword don't specified, "
						+ "please run 'help query pwd' command to check the use examples");
			}
			Triple<Boolean, String, String> usernameTriple = parseArgsValue(Cmd.QUERY_PWD, triple.third, "-n", String.class);
			if(!usernameTriple.first) { return Cmd.incorrectCommand(usernameTriple.second); }
			return new QueryPwdCommand(websiteKeywordTriple.third, websiteIdTriple.third, usernameTriple.third);
		}
	},
	
	@SuppressWarnings("unchecked")
	COUNT_PWD_ARGS(
		triple("-i", false, LONG_CONVERTER),
		triple("-k", false, STRING_CONVERTER),
		triple("-n", false, STRING_CONVERTER)
	) {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			if(size == 1 && "-h".equals(cmdArgsList.get(0))) { return new HelpCommand(Cmd.COUNT_PWD); }
			Triple<Boolean, String, Map<String, Object>> triple = parseArgs(Cmd.COUNT_PWD, cmdArgsList);
			if(!triple.first) { return Cmd.incorrectCommand(triple.second); }
			Triple<Boolean, String, Long> websiteIdTriple = parseArgsValue(Cmd.COUNT_PWD, triple.third, "-i", Long.class);
			Triple<Boolean, String, String> websiteKeywordTriple = parseArgsValue(Cmd.COUNT_PWD, triple.third, "-k", String.class);
			Triple<Boolean, String, String> usernameTriple = parseArgsValue(Cmd.COUNT_PWD, triple.third, "-n", String.class);
			return new CountPwdCommand(websiteKeywordTriple.third, websiteIdTriple.third, usernameTriple.third);
		}
	},
	
	@SuppressWarnings("unchecked")
	LIST_PWD_ARGS(
		triple("-i", false, LONG_CONVERTER),
		triple("-k", false, STRING_CONVERTER),
		triple("-n", false, STRING_CONVERTER)
	) {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			if(size == 1 && "-h".equals(cmdArgsList.get(0))) { return new HelpCommand(Cmd.LIST_PWD); }
			Triple<Boolean, String, Map<String, Object>> triple = parseArgs(Cmd.LIST_PWD, cmdArgsList);
			if(!triple.first) { return Cmd.incorrectCommand(triple.second); }
			Triple<Boolean, String, Long> websiteIdTriple = parseArgsValue(Cmd.LIST_PWD, triple.third, "-i", Long.class);
			Triple<Boolean, String, String> websiteKeywordTriple = parseArgsValue(Cmd.LIST_PWD, triple.third, "-k", String.class);
			Triple<Boolean, String, String> usernameTriple = parseArgsValue(Cmd.LIST_PWD, triple.third, "-n", String.class);
			if(!websiteIdTriple.first && !websiteKeywordTriple.first && !usernameTriple.first) {
				return Cmd.incorrectCommand("while list password, not enough arguments, please run 'help list pwd' command "
						+ "to check the use examples");
			}
			return new ListPwdCommand(websiteKeywordTriple.third, websiteIdTriple.third, usernameTriple.third);
		}
	},
	UNDO_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			switch(size) {
			case 0:
				return new UndoCommand();
			case 1:
				if("-h".equals(cmdArgsList.get(0))) {
					return new HelpCommand(Cmd.UNDO);
				}
			default:
				return Cmd.incorrectCommand("incorrect command arguments for '" + Cmd.UNDO.cmd() + "' command");
			}
		}
	},
	REDO_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			switch(size) {
			case 0:
				return new RedoCommand();
			case 1:
				if("-h".equals(cmdArgsList.get(0))) {
					return new HelpCommand(Cmd.REDO);
				}
			default:
				return Cmd.incorrectCommand("incorrect command arguments for '" + Cmd.REDO.cmd() + "' command");
			}
		}
	},
	COMMIT_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			switch(size) {
			case 0:
				return new CommitCommand();
			case 1:
				if("-h".equals(cmdArgsList.get(0))) {
					return new HelpCommand(Cmd.COMMIT);
				}
			default:
				return Cmd.incorrectCommand("incorrect command arguments for '" + Cmd.COMMIT.cmd() + "' command");
			}
		}
	},
	EXIT_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			switch(size) {
			case 0:
				return new ExitCommand();
			case 1:
				if("-h".equals(cmdArgsList.get(0))) {
					return new HelpCommand(Cmd.EXIT);
				}
			default:
				return Cmd.incorrectCommand("incorrect command arguments for '" + Cmd.EXIT.cmd() + "' command");
			}
		}
	},
	;
	
	private final Map<String, Pair<Boolean, Converter<?>>> converterMap = new HashMap<String, Pair<Boolean, Converter<?>>>();
	
	private CmdArgs(Triple<String, Boolean, Converter<?>>... argsConverterTriples) {
		for(Triple<String, Boolean, Converter<?>> triple : argsConverterTriples) {
			converterMap.put(triple.first, pair(triple.second, triple.third));
		}
	}
	
	public abstract Command parse(List<String> cmdArgsList);
	
	protected Triple<Boolean, String, Map<String, Object>> parseArgs(Cmd cmd, List<String> cmdArgsList) {
		int size = cmdArgsList.size();
		if(size == 0) {
			Map<String, Object> emptyValueMap = Collections.emptyMap();
			return Triple.create(true, "success", emptyValueMap);
		}
		if(size % 2 != 0) {
			return Triple.create(false, "the number of '" + cmd.cmd() + "' command argument must be an even", null);
		}
		
		Map<String, Object> valueMap = new HashMap<String, Object>(converterMap.size());
		Iterator<String> iterator = cmdArgsList.iterator();
		while(iterator.hasNext()) {
			String option = iterator.next();
			String value = iterator.next();
			Pair<Boolean, Converter<?>> pair = converterMap.get(option);
			if(pair == null) {
				return Triple.create(false, "unrecognized argument option '" + option
					+ "' for '" + cmd.cmd() + "' command", null);
			}
			Object argsValue = pair.second.convert(value);
			if(argsValue == null) {
				return Triple.create(false, "illeagl argument value '" + value + "' mapped with option '" + option
						+ "' for '" + cmd.cmd() + "' command", null);
			}
			Object oldArgsValue = valueMap.put(option, argsValue);
			if(oldArgsValue != null) {
				return Triple.create(false, "duplicate argument value '" + value + "' mapped with option '" + option
						+ "' for '" + cmd.cmd() + "' command", null);
			}
		}
		return Triple.create(true, "success", valueMap);
	}
	
	protected <T> Triple<Boolean, String, T> parseArgsValue(Cmd cmd, Map<String, Object> argsValueMap, String argsOption, Class<T> clazz) {
		Object value = argsValueMap.get(argsOption);
		if(value == null) {
			Pair<Boolean, Converter<?>> pair = converterMap.get(argsOption);
			if(pair.first) {
				return Triple.create(false, "no mapping value of required '"+ argsOption + "' option for '"
					+ cmd.cmd() + "' command", null);
			}else {
				return Triple.create(true, "'"+ argsOption + "' option is optional for '"
						+ cmd.cmd() + "' command", null);
			}
		}
		if(!clazz.isInstance(value)) {
			return Triple.create(false, "illegal value format of '"+ argsOption + "' option for '"
					+ cmd.cmd() + "' command: "+value, null);
		}
		return Triple.create(true, "success", clazz.cast(value));
	}
	
	protected boolean exist(Map<String, Object> argsValueMap, String argsOption) {
		return argsValueMap.get(argsOption) != null;
	}
	
	/**
	 * build a command argument triple tuple
	 * @param args	the first string element denoted argument option name
	 * @param isRequired the second boolean element denoted whether argument option is required or not 
	 * @param converter the third {@link Converter} element denoted the value converter
	 * @return a command argument triple tuple
	 */
	private static Triple<String, Boolean, Converter<?>> triple(String args, Boolean isRequired, Converter<?> converter) {
		return new Triple<String, Boolean, Converter<?>>(args, isRequired, converter);
	}
	
	private static Pair<Boolean, Converter<?>> pair(Boolean isOptional, Converter<?> converter) {
		return new Pair<Boolean, Converter<?>>(isOptional, converter);
	}
}
