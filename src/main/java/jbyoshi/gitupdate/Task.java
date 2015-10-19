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
package jbyoshi.gitupdate;

import java.util.*;
import java.util.function.*;

public final class Task {
	private final Consumer<Report> code;
	private final Set<Task> children = new LinkedHashSet<>();
	private final Report report;

	Task(String text) {
		this(null, text, report -> {
		});
	}

	private Task(Report parentReport, String text, Consumer<Report> code) {
		this.report = new Report(parentReport, text);
		this.report.future = true;
		this.report.stateChanged();
		this.code = code;
	}

	public Task newChild(String text) {
		return newChild(text, report -> {
		});
	}

	public Task newChild(String text, Consumer<Report> code) {
		Task child = new Task(report, text, code);
		children.add(child);
		return child;
	}

	void start() {
		report.future = false;
		report.working = true;
		report.stateChanged();
		code.accept(report);
		for (Task child : children) {
			child.start();
		}
		report.working = false;
		report.done = true;
		report.stateChanged();
	}
}
