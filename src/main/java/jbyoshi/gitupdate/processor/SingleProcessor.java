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

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.*;

import jbyoshi.gitupdate.ui.*;

public abstract class SingleProcessor extends Processor<Void> {

	@Override
	public final Iterable<Void> getKeys(Repository repo) throws Exception {
		return Collections.singleton(null);
	}

	@Override
	public final void process(Repository repo, Git git, Void key, ReportData data) throws Exception {
		process(repo, git, data);
	}

	public abstract void process(Repository repo, Git git, ReportData data) throws Exception;

}
