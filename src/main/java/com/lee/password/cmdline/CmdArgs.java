package com.lee.password.cmdline;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lee.password.cmdline.Environment.Name;
import com.lee.password.cmdline.commands.HelpCommand;
import com.lee.password.cmdline.commands.ListEnvCommand;
import com.lee.password.cmdline.commands.SetCommand;
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
	GENERATE_KEY_ARGS(
		pair("-s", INTEGER_CONVERTER),
		pair("-p", FILE_CONVERTER)
	) {
		@Override
		public Command parse(List<String> cmdArgsList) {
			int size = cmdArgsList.size();
			if(size != 4) { return incorrectCommand("incorrect command arguments for 'generate key' command"); }
			int keySize = -1;
			File targetKeyDir = null;
			String optionOne = cmdArgsList.get(0);
			String valueOne = cmdArgsList.get(1);
			String optionTwo = cmdArgsList.get(2);
			String valueTwo = cmdArgsList.get(3);
			if(matchOption(optionOne, "s")) {
				Pair<Boolean, Integer> keySizeResult = parseInt(valueOne);
				if(!keySizeResult.first || keySizeResult.second <= 0) {
					return Cmd.incorrectCommand("incorrect keySize for 'generate key' command: "+valueOne);
				}
				keySize = keySizeResult.second;
				if(matchOption(optionTwo, "p")) {
					Pair<Boolean, File> keyDirResult = parseDir(valueTwo);
					if(!keyDirResult.first) {
						return Cmd.incorrectCommand("incorrect targetKeyDir for 'generate key' command: "+valueTwo);
					}
					targetKeyDir = keyDirResult.second;
				}else {
					return Cmd.incorrectCommand("incorrect argument option for 'generate key' command: "+optionTwo);
				}
			}else {
				if(matchOption(optionOne, "p")) {
					Pair<Boolean, File> keyDirResult = parseDir(valueOne);
					if(!keyDirResult.first) {
						return Cmd.incorrectCommand("incorrect targetKeyDir for 'generate key' command: "+valueOne);
					}
					targetKeyDir = keyDirResult.second;
					if(matchOption(optionTwo, "s")) {
						Pair<Boolean, Integer> keySizeResult = parseInt(valueTwo);
						if(!keySizeResult.first || keySizeResult.second <= 0) {
							return Cmd.incorrectCommand("incorrect keySize for 'generate key' command: "+valueTwo);
						}
						keySize = keySizeResult.second;
					}else {
						return Cmd.incorrectCommand("incorrect argument option for 'generate key' command: "+optionTwo);
					}
				}else {
					return Cmd.incorrectCommand("incorrect argument option for 'generate key' command: "+optionOne);
				}
			}
		}
	},
	ADD_WEB_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			// TODO Auto-generated method stub
			return null;
		}
	},
	REMOVE_WEB_ARGS() {
		@Override
		public Command parse(List<String> cmdArgsList) {
			// TODO Auto-generated method stub
			return null;
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
	
	private final Map<String, Converter> map = new HashMap<String, Converter>();
	
	private CmdArgs(Pair<String, Converter>... argsConverterPairs) {
		for(Pair<String, Converter> pair : argsConverterPairs) {
			map.put(pair.first, pair.second);
		}
	}
	
	public abstract Command parse(List<String> cmdArgsList);
	
	static interface Converter<T> { public T convert(String value); }
	
	private static final Converter<Integer> INTEGER_CONVERTER = new Converter<Integer>() {
		@Override
		public Integer convert(String value) {
			try {
				return Integer.parseInt(value);
			}catch(Exception e) {
				return null;
			}
		}
	};
	
	private static final Converter<File> FILE_CONVERTER = new Converter<File>() {
		@Override
		public File convert(String value) {
			try {
				File dir = new File(value);
				return dir.isDirectory() ? dir : null;
			}catch(Exception e) {
				return null;
			}
		}
	};
	
	private static <T> Pair<String, Converter> pair(String args, Converter<T> converter) {
		return new Pair<String, Converter>(args, converter);
	}
}
