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

import jbyoshi.gitupdate.ui.*;

public final class Report {
	boolean error, modified, future, working, done;
	private final Report parent;
	final ReportView view;

	Report(Report parent, String text) {
		this.parent = parent;
		this.view = parent == null ? UI.INSTANCE.getRoot() : parent.view.newChild(text);
	}

	public Report newChild(String text) {
		return new Report(this, text);
	}

	public void newErrorChild(Throwable e) {
		ReportDataUtils.printError(e, this);
	}

	public Report error() {
		if (parent != null) {
			parent.error();
		}
		error = true;
		stateChanged();
		return this;
	}

	public Report modified() {
		if (parent != null) {
			parent.modified();
		}
		modified = true;
		stateChanged();
		return this;
	}

	void stateChanged() {
		view.stateChanged(error, working, future, modified, done);
	}

}
