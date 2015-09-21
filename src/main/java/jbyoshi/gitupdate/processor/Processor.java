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

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.*;
import org.eclipse.jgit.lib.*;

import jbyoshi.gitupdate.ui.*;

public abstract class Processor<T> {

	public void run(Repository repo, Git git, ReportData report) {
		Iterable<T> keys;
		try {
			keys = getKeys(repo);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		for (T key : keys) {
			try {
				process(repo, git, key, report);
			} catch (TransportException e) {
				if (e.getCause() instanceof NoRemoteRepositoryException) {
					System.err.println(e.getCause());
				} else if (e.getMessage().contains("Auth cancel")) {
					System.exit(0);
				} else {
					e.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected abstract Iterable<T> getKeys(Repository repo) throws Exception;

	protected abstract void process(Repository repo, Git git, T key, ReportData report) throws Exception;

}
