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
import org.eclipse.jgit.errors.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.submodule.*;

import com.google.common.collect.*;

import jbyoshi.gitupdate.processor.*;

public class GitUpdate {
	private static final Set<File> updated = new HashSet<>();
	private static final ImmutableList<Processor> processors = ImmutableList.of(new Fetch(), new FastForward(),
			new Rebase(), new Push());

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
			putAboutText(root.report);
			root.start();
		} catch (Throwable t) {
			if (rootReport == null) {
				rootReport = new Report(null, "Error");
			}
			rootReport.newErrorChild(t);
		}
	}

	private static void putAboutText(Report report) {
		report = report.newChild("Licenses");
		for (String name : Arrays.asList("", "JGit", "Guava", "slf4j", "JSch", "JavaEWAH", "Apache_HTTPClient",
				"JDT_Annotations_for_Enhanced_Null_Analysis", "Apache_HTTPCore", "Apache_Commons_Logging",
				"Apache_Commons_Codec")) {
			StringBuilder sb = new StringBuilder();
			String file = name == "" ? "/LICENSE.txt"
 : "/licenses/" + name.toLowerCase() + "-LICENSE.txt";
			if (name == "" && GitUpdate.class.getResource("/LICENSE.txt") == null) {
				report.newChild("GitUpdate").newChild("Could not locate license in development mode!").error();
				continue;
			}
			try (Reader reader = new InputStreamReader(GitUpdate.class.getResourceAsStream(file))) {
				char[] cbuf = new char[1024];
				int read;
				while ((read = reader.read(cbuf)) > 0) {
					sb.append(cbuf, 0, read);
				}
			} catch (IOException e) {
				throw new AssertionError(e);
			}
			Report out = report.newChild(name == "" ? "GitUpdate" : name.replace("_", " "));
			for (String part : sb.toString().split("\n")) {
				out.newChild(part);
			}
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
		} catch (RepositoryNotFoundException e) {
			if (repoDir.getName().equals(Constants.DOT_GIT)) {
				repoDir = repoDir.getParentFile();
			}
			try {
				repoDir = repoDir.toPath().toRealPath().toFile();
			} catch (IOException e1) {
				repoDir = repoDir.toPath().normalize().toFile();
			}
			if (updated.add(repoDir)) {
				root.report.newChild(repoDir.getName() + " - not a Git repository");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void update(Repository repo, Task root) {
		File dir = repo.getDirectory();
		if (dir.getName().equals(Constants.DOT_GIT)) {
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
