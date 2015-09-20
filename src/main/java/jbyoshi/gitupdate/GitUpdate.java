/*
 * Copyright 2015 JBYoshi
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
import java.nio.file.*;
import java.util.*;

import javax.swing.*;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.submodule.*;

import com.google.common.collect.*;

public class GitUpdate {
	private static final Set<File> updated = new HashSet<File>();
	private static final ImmutableList<Processor<?>> processors = ImmutableList.of(new Fetch(), new FastForward(),
			new Push());

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		File gitDir = new File(System.getProperty("user.home"), "git");
		if (!gitDir.exists()) {
			System.err.println("No such directory: " + gitDir);
			return;
		}
		for (File repoDir : gitDir.listFiles()) {
			update(repoDir);
		}
		System.out.println("========================================");
		System.out.println("Done.");
		for (Processor<?> processor : processors) {
			processor.report();
		}
	}

	public static void update(File repoDir) {
		try {
			if (!repoDir.isDirectory()) {
				return;
			}
			Repository repo = new RepositoryBuilder().setWorkTree(repoDir).setMustExist(true).build();
			update(repo);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void update(Repository repo) {
		File dir = repo.getDirectory();
		if (dir.getName().equals(".git")) {
			dir = dir.getParentFile();
		}
		{
			Path path = dir.toPath();
			if (Files.isSymbolicLink(path)) {
				try {
					dir = Files.readSymbolicLink(path).toFile();
				} catch (IOException e) {
					// Ignore
				}
			}
		}
		if (!updated.add(dir)) {
			return;
		}

		System.out.println("Updating " + dir.getName());
		Git git = Git.wrap(repo);
		for (Processor<?> processor : processors) {
			processor.run(repo, git);
		}

		try {
			if (SubmoduleWalk.containsGitModulesFile(repo)) {
				SubmoduleWalk submodules = SubmoduleWalk.forIndex(repo);
				try {
					while (submodules.next()) {
						update(submodules.getRepository());
					}
				} finally {
					submodules.release();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
