/*
 * Copyright (c) 2015 JBYoshi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jbyoshi.gitupdate.processor;

import java.util.*;

import org.eclipse.jgit.errors.*;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.transport.CredentialItem.*;

import jbyoshi.gitupdate.ui.*;

final class Prompts extends CredentialsProvider {

	private final Map<String, Optional<String>> textPrompts = new HashMap<>();

	static final Prompts INSTANCE = new Prompts();

	private Prompts() {
	}

	@Override
	public boolean supports(CredentialItem... items) {
		for (CredentialItem item : items) {
			if (item instanceof Username) {
				continue;
			}
			if (item instanceof Password) {
				continue;
			}
			return false;
		}
		return true;
	}

	@Override
	public boolean isInteractive() {
		return true;
	}

	@Override
	public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
		for (int i = 0; i < items.length; i++) {
			CredentialItem item = items[i];
			if (item instanceof StringType) {
				if (item instanceof Username && i < items.length - 1 && items[i + 1] instanceof Password) {
					Password password = (Password) items[i + 1];
					// TODO cache this?
					UsernamePasswordPair login = UI.INSTANCE.promptLogin("Login for " + uri);
					if (login == null) {
						return false;
					}
					((StringType) item).setValue(login.getUsername());
					password.setValue(login.getPassword());
					login.clobber();
					i++;
				} else {
					String prompt = item.getPromptText();
					if (item instanceof Username) {
						prompt = "Username for " + uri;
					}
					Optional<String> value = textPrompts.computeIfAbsent(prompt, (prompt0) -> {
						char[] val = UI.INSTANCE.promptPassword(prompt0);
						if (val == null) {
							return Optional.empty();
						}
						return Optional.of(new String(val));
					});
					if (!value.isPresent()) {
						return false;
					}
					((StringType) item).setValue(value.get());
				}
			} else if (item instanceof CharArrayType) {
				String prompt = item.getPromptText();
				if (item instanceof Password) {
					prompt = "Password for " + uri;
				}
				Optional<String> value = textPrompts.computeIfAbsent(prompt, (prompt0) -> {
					char[] password = UI.INSTANCE.promptPassword(prompt0);
					if (password == null) {
						return Optional.empty();
					}
					return Optional.of(new String(password));
				});
				if (!value.isPresent()) {
					return false;
				}
				((CharArrayType) item).setValueNoCopy(value.get().toCharArray());
			} else {
				return false;
			}
		}
		return true;
	}

}
