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
	public ReportData newRootReportData(String text) {
		return new ConsoleReportData(System.out, text, 0);
	}

	static final class ConsoleReportData implements ReportData {
		private final int indent;

		private ConsoleReportData(PrintStream stream, String text, int indent) {
			this.indent = indent;
			StringBuilder line = new StringBuilder();
			for (int i = 0; i < indent; i++) {
				line.append('\t');
			}
			System.out.println(line.append(text));
		}

		@Override
		public ReportData newChild(String text) {
			return new ConsoleReportData(System.out, text, indent + 1);
		}

		@Override
		public ReportData newErrorChild(String text) {
			return new ConsoleReportData(System.err, text, indent + 1);
		}
	}

	@Override
	public void finish() {
		System.out.println("========================================");
		System.out.println("Done.");
	}
}
