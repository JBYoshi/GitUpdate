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

public abstract class ReportData {
	protected boolean error, working, future, modified, done;
	private final ReportData parent;

	protected ReportData(ReportData parent) {
		this.parent = parent;
	}

	public abstract ReportData newChild(String text);

	public void newErrorChild(Throwable e) {
		ReportDataUtils.printError(e, this);
	}

	public ReportData future() {
		if (parent != null) {
			parent.future();
		}
		if (!working) {
			future = true;
		}
		done = false;
		return this;
	}

	public ReportData error() {
		if (parent != null) {
			parent.error();
		}
		error = true;
		stateChanged();
		return this;
	}

	public ReportData working() {
		if (parent != null) {
			parent.working();
		}
		future = false;
		working = true;
		stateChanged();
		return this;
	}

	public ReportData done() {
		if (parent != null) {
			parent.working();
		}
		future = false;
		working = false;
		done = true;
		stateChanged();
		return this;
	}

	public ReportData modified() {
		if (parent != null) {
			parent.modified();
		}
		modified = true;
		stateChanged();
		return this;
	}

	protected void stateChanged() {
	}
}
