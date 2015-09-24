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

import com.google.common.collect.*;

import jbyoshi.gitupdate.*;

public final class ReportDataUtils {

	public static void printError(Throwable e, Report data) {
		new ErrorPrint(null, e, "").print(data);
	}

	private static final class ErrorPrint {
		private final String desc;
		private final StackTraceElement[] stack;
		private final Multimap<Integer, ErrorPrint> causes = HashMultimap.create();
		private final int indexBreak;

		private ErrorPrint(ErrorPrint parent, Throwable e, String desc) {
			this.desc = desc + e;
			stack = e.getStackTrace();
			int i = 0;
			if (parent != null) {
				while (i < stack.length && i < parent.stack.length
						&& parent.stack[parent.stack.length - i - 1].equals(stack[stack.length - i - 1])) {
					i++;
				}
			}
			indexBreak = i;

			if (e.getCause() != null) {
				addCause(new ErrorPrint(this, e.getCause(), "Caused by: "));
			}
			for (Throwable suppressed : e.getSuppressed()) {
				addCause(new ErrorPrint(this, suppressed, "Suppressed: "));
			}
		}

		private void addCause(ErrorPrint print) {
			causes.put(print.indexBreak, print);
		}

		private void print(Report data) {
			data = data.newChild(desc).error();
			for (int i = indexBreak; i < stack.length; i++) {
				data.newChild(stack[i - indexBreak].toString());
				for (ErrorPrint child : causes.removeAll(stack.length - i + indexBreak - 1)) {
					child.print(data);
				}
			}

			// Print any remaining children
			for (ErrorPrint child : causes.values()) {
				child.print(data);
			}
		}
	}

}
