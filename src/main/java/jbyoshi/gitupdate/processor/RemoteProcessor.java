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

import java.io.*;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.*;

import jbyoshi.gitupdate.ui.*;

public abstract class RemoteProcessor extends Processor<String> {

	@Override
	public final Iterable<String> getKeys(Repository repo) {
		return repo.getRemoteNames();
	}

	@Override
	public final void process(Repository repo, Git git, String remote, ReportData data)
			throws GitAPIException, IOException {
		data = data.newChild(remote).working();
		process(repo, git, remote, Constants.R_REMOTES + remote + "/", data);
		data.done();
	}

	public abstract void process(Repository repo, Git git, String remote, String fullRemote, ReportData data)
			throws GitAPIException, IOException;

}
