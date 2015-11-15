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

import java.io.*;
import java.util.*;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.submodule.*;

import com.google.common.collect.*;

import jbyoshi.gitupdate.processor.*;

public class GitUpdate {
	private static final Set<File> updated = new HashSet<>();
	private static final ImmutableList<Processor> processors = ImmutableList.of(new Fetch(), new FastForward(),
			new Push());

	public static void main(String[] args) {
		Report rootReport = null;
		try {
			File gitDir = new File(System.getProperty("user.home"), "git");
			if (!gitDir.exists()) {
				throw new FileNotFoundException(gitDir.toString());
			}
			if (!gitDir.isDirectory()) {
				throw new IOException("Not a directory: " + gitDir);
			}
			if (gitDir.list().length == 0) {
				throw new IOException("No files in " + gitDir);
			}
			Task root = new Task("GitUpdate");
			rootReport = root.report;
			for (File repoDir : gitDir.listFiles()) {
				update(repoDir, root);
			}
			root.start();
		} catch (Throwable t) {
			if (rootReport == null) {
				rootReport = new Report(null, "Error");
			}
			rootReport.newErrorChild(t);
		}
	}

	public static void update(File repoDir, Task root) {
		try {
			if (!repoDir.isDirectory()) {
				return;
			}
			try (Repository repo = new RepositoryBuilder().setWorkTree(repoDir).setMustExist(true).build()) {
				update(repo, root);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void update(Repository repo, Task root) {
		File dir = repo.getDirectory();
		if (dir.getName().equals(".git")) {
			dir = dir.getParentFile();
		}
		try {
			dir = dir.toPath().toRealPath().toFile();
		} catch (IOException e) {
			dir = dir.toPath().normalize().toFile();
		}
		if (!updated.add(dir)) {
			return;
		}

		try {
			if (SubmoduleWalk.containsGitModulesFile(repo)) {
				try (SubmoduleWalk submodules = SubmoduleWalk.forIndex(repo)) {
					while (submodules.next()) {
						update(submodules.getRepository(), root);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (Git git = Git.wrap(repo)) {
			Task repoTask = root.newChild(dir.getName());
			for (Processor processor : processors) {
				try {
					processor.registerTasks(repo, git, repoTask);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
