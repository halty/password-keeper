package com.lee.password.cmdline;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.lee.password.cmdline.Environment.Name;
import com.lee.password.cmdline.commands.AddWebCommand;
import com.lee.password.cmdline.commands.GenerateKeyCommand;
import com.lee.password.cmdline.commands.HelpCommand;
import com.lee.password.cmdline.commands.ListEnvCommand;
import com.lee.password.cmdline.commands.RemoveWebCommand;
import com.lee.password.cmdline.commands.SetCommand;
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
				primaryName = cmdArgsList.get(0);
				Cmd cmd = Cmd.match(primaryName, secondaryName);
				return cmd == null ? Cmd.incorrectCommand("no matched command for 'help' command")
						: new HelpCommand(cmd);
			case 0: return new HelpCommand();
			default:
				return Cmd.incorrectCommand("incorrect command arguments for 'help' command");
			}
		}
	},
	
	LIST_ENV_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			return size == 0 ? new ListEnvCommand()
					: Cmd.incorrectCommand("incorrect command arguments for 'list env' command");
		}
	},
	
	SET_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			switch(size) {
			case 0: return new SetCommand();
			case 1:
				String args = cmdArgsList.get(0);
				Name name = Environment.nameOf(args);
				if(name == null) {
					return Cmd.incorrectCommand("no program environment variable named with '" + args + "'");
				}
				return new SetCommand(name);
			case 2:
				String first = cmdArgsList.get(0);
				String value = cmdArgsList.get(1);
				Name variableName = Environment.nameOf(first);
				if(variableName == null) {
					return Cmd.incorrectCommand("no program environment variable named with '" + first + "'");
				}
				return new SetCommand(variableName, value);
			default:
				return Cmd.incorrectCommand("incorrect command arguments for 'set' command");
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
			Triple<Boolean, String, Map<String, Object>> triple = parseArgs(Cmd.REMOVE_WEB, cmdArgsList);
			if(!triple.first) { return Cmd.incorrectCommand(triple.second); }
			Triple<Boolean, String, String> keywordTriple = parseArgsValue(Cmd.REMOVE_WEB, triple.third, "-k", String.class);
			String keyword = keywordTriple.third;
			Triple<Boolean, String, Long> webisteIdTriple = parseArgsValue(Cmd.REMOVE_WEB, triple.third, "-i", Long.class);
			Long websiteId = webisteIdTriple.third;
			if(keyword == null && websiteId == null) {
				return Cmd.incorrectCommand("while removing website, no keyword and websiteId are specified");
			}
			return new RemoveWebCommand(websiteId, keyword);
		}
	},
	CHANGE_WEB_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	QUERY_WEB_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	COUNT_WEB_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	LIST_WEB_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	ADD_PWD_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	REMOVE_PWD_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	CHANGE_PWD_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	QUERY_PWD_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	COUNT_PWD_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	LIST_PWD_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	UNDO_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	REDO_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	COMMIT_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	EXIT_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			// TODO Auto-generated method stub
			return null;
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
			return Triple.create(false, "the number of '" + cmd.name() + "' command argument must be an even", null);
		}
		
		Map<String, Object> valueMap = new HashMap<String, Object>(converterMap.size());
		Iterator<String> iterator = cmdArgsList.iterator();
		while(iterator.hasNext()) {
			String option = iterator.next();
			String value = iterator.next();
			Pair<Boolean, Converter<?>> pair = converterMap.get(option);
			if(pair == null) {
				return Triple.create(false, "unrecognized argument option '" + option
					+ "' for '" + cmd.name() + "' command", null);
			}
			Object argsValue = pair.second.convert(value);
			if(argsValue == null) {
				return Triple.create(false, "illeagl argument value '" + value + "' mapped with option '" + option
						+ "' for '" + cmd.name() + "' command", null);
			}
			Object oldArgsValue = valueMap.put(option, argsValue);
			if(oldArgsValue != null) {
				return Triple.create(false, "duplicate argument value '" + value + "' mapped with option '" + option
						+ "' for '" + cmd.name() + "' command", null);
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
					+ cmd.name() + "' command", null);
			}else {
				return Triple.create(true, "'"+ argsOption + "' option is optional for '"
						+ cmd.name() + "' command", null);
			}
		}
		if(!clazz.isInstance(value)) {
			return Triple.create(false, "illegal value format of '"+ argsOption + "' option for '"
					+ cmd.name() + "' command: "+value, null);
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
