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
package jbyoshi.gitupdate.ui;

import java.io.*;

public final class ConsoleUI implements UI {
	private final Console console = System.console();
	private static final NodeView root = new NodeView() {
		@Override
		public NodeView newChild(String text) {
			return new ConsoleNodeView(null, text);
		}

		@Override
		public void stateChanged(boolean error, boolean working, boolean future, boolean modified, boolean done) {
			if (done) {
				System.out.println("========================================");
				if (error) {
					System.err.println("Errored. See the log for details.");
				} else {
					System.out.println("Done.");
				}
				if (modified) {
					System.out.println("Changes have been made. See the log for details.");
				} else {
					System.out.println("No changes have been made.");
				}
			}
		}
	};

	@Override
	public UsernamePasswordPair promptLogin(String prompt) {
		System.out.println(prompt);
		return new UsernamePasswordPair(console.readLine("Username: "), promptPassword("Password: "));
	}

	@Override
	public char[] promptPassword(String prompt) {
		return console.readPassword("%s", prompt);
	}

	@Override
	public NodeView getRoot() {
		return root;
	}

	private static final class ConsoleNodeView implements NodeView {
		private final int indent;
		private final String line;
		private boolean printed;

		private ConsoleNodeView(ConsoleNodeView parent, String text) {
			this.indent = parent == null ? 0 : parent.indent + 1;
			StringBuilder line = new StringBuilder();
			for (int i = 0; i < indent; i++) {
				line.append('\t');
			}
			this.line = line.append(text).toString();
		}

		@Override
		public NodeView newChild(String text) {
			return new ConsoleNodeView(this, text);
		}

		@Override
		public void stateChanged(boolean error, boolean working, boolean future, boolean modified, boolean done) {
			if (future || printed) {
				return;
			}
			printed = true;
			if (error) {
				System.err.println(line);
			} else {
				System.out.println(line);
			}
		}
	}
}
