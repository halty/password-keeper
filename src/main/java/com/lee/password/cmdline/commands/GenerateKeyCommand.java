package com.lee.password.cmdline.commands;

import java.io.File;
import com.lee.password.cmdline.Command;
import static com.lee.password.cmdline.Environment.*;
import com.lee.password.keeper.api.Result;
import com.lee.password.keeper.api.crypto.CryptoDriver;
import com.lee.password.keeper.api.crypto.CryptoKey;
import com.lee.password.util.Triple;

public class GenerateKeyCommand implements Command {

	private final int size;
	private final File keyDir;
	
	public GenerateKeyCommand(int size, File keyDir) {
		this.size = size;
		this.keyDir = keyDir;
	}
	
	@Override
	public void execute() {
		Triple<Boolean, String, CryptoDriver> result = current().getCryptoDriver();
		if(!result.first) {
			line(result.second);
		}else {
			CryptoDriver cryptoDriver = result.third;
			Result<CryptoKey[]> keyPairResult = cryptoDriver.generateKeyPair(keyDir.getAbsolutePath(), size);
			if(!keyPairResult.isSuccess()) {
				line("failed to generate key: "+keyPairResult.msg);
			}else {
				line("generate key successful:");
				indent("public key file -- "+keyPairResult.result[0].path());
				indent("private key file -- "+keyPairResult.result[1].path());
			}
		}
		prompt();
	}

}
