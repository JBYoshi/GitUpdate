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
import java.util.*;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.*;

import jbyoshi.gitupdate.*;
import jbyoshi.gitupdate.ui.*;

public abstract class BranchProcessor extends Processor<Map.Entry<String, Ref>> {

	@Override
	public final Iterable<Map.Entry<String, Ref>> getKeys(Repository repo) throws IOException {
		return Utils.getLocalBranches(repo).entrySet();
	}

	@Override
	public final void process(Repository repo, Git git, Map.Entry<String, Ref> branch, ReportData report)
			throws GitAPIException, IOException {
		process(repo, git, branch.getKey(), branch.getValue(), report);
	}

	public abstract void process(Repository repo, Git git, String branch, Ref ref, ReportData report)
			throws GitAPIException, IOException;
}
