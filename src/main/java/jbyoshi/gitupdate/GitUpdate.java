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

import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import javax.swing.*;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.submodule.*;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.transport.CredentialItem.*;

public class GitUpdate {
	private static final CredentialsProvider cred = new CredentialsProvider() {
		private final Map<String, String> textPrompts = new HashMap<String, String>();

		@Override
		public boolean supports(CredentialItem... items) {
			for (CredentialItem item : items) {
				if (item instanceof Username) {
					continue;
				}
				if (item instanceof Password) {
					continue;
				}
				return false;
			}
			return true;
		}

		@Override
		public boolean isInteractive() {
			return true;
		}

		@Override
		public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
			for (CredentialItem item : items) {
				if (item instanceof StringType) {
					String prompt = item.getPromptText();
					if (item instanceof Username) {
						prompt = "Username for " + uri;
					}
					String value = textPrompts.computeIfAbsent(prompt,
							(prompt0) -> new String(showPasswordDialog(prompt0)));
					if (value == null) {
						return false;
					}
					((StringType) item).setValue(value);
				} else if (item instanceof CharArrayType) {
					String prompt = item.getPromptText();
					if (item instanceof Username) {
						prompt = "Password for " + uri;
					}
					((CharArrayType) item).setValueNoCopy(
							textPrompts.computeIfAbsent(prompt, (prompt0) -> new String(showPasswordDialog(prompt0)))
									.toCharArray());
				} else {
					return false;
				}
			}
			return true;
		}

		private char[] showPasswordDialog(String prompt) {
			JPasswordField pass = new JPasswordField();
			JOptionPane pane = new JOptionPane(new Object[] { prompt, pass }, JOptionPane.QUESTION_MESSAGE,
					JOptionPane.OK_CANCEL_OPTION);
			final JDialog dialog = pane.createDialog("Input");
			pass.addActionListener((e) -> {
				pane.setValue(JOptionPane.OK_OPTION);
				dialog.setVisible(false);
			});
			dialog.addWindowListener(new WindowAdapter() {
				@Override
				public void windowOpened(WindowEvent e) {
					pass.requestFocusInWindow();
				}
			});
			dialog.setVisible(true);
			dialog.dispose();
			if (!Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue())) {
				return null;
			}
			return pass.getPassword();
		}
	};

	private static final Set<File> updated = new HashSet<File>();

	public static void main(String[] args) {
		File gitDir = new File(System.getProperty("user.home"), "git");
		if (!gitDir.exists()) {
			System.err.println("No such directory: " + gitDir);
			return;
		}
		for (File repoDir : gitDir.listFiles()) {
			update(repoDir);
		}
	}

	public static void update(File repoDir) {
		try {
			if (!repoDir.isDirectory()) {
				return;
			}
			Repository repo = new RepositoryBuilder().setWorkTree(repoDir).setMustExist(true).build();
			update(repo);
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
		Git git = Git.wrap(repo);
		boolean hasRemotes = false;
		for (String remote : repo.getRemoteNames()) {
			hasRemotes = true;
			System.out.println("Fetching " + dir.getName() + " remote " + remote);
			try {
				git.fetch().setCredentialsProvider(cred).setRemote(remote).call();
			} catch (InvalidRemoteException e) {
				e.printStackTrace();
			} catch (TransportException e) {
				e.printStackTrace();
			} catch (GitAPIException e) {
				e.printStackTrace();
			}
		}
		if (hasRemotes) {
			System.out.println("Pushing " + dir.getName());
			try {
				git.push().setPushAll().setTimeout(5).call();
			} catch (InvalidRemoteException e) {
				e.printStackTrace();
			} catch (TransportException e) {
				if (e.getCause() instanceof NoRemoteRepositoryException) {
					// Ignore
				} else {
					e.printStackTrace();
				}
			} catch (GitAPIException e) {
				e.printStackTrace();
			}
		}
	}
}
