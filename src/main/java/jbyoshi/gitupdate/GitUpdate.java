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
import java.text.*;
import java.util.*;

import javax.swing.*;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.*;
import org.eclipse.jgit.internal.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.lib.RefUpdate.*;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.submodule.*;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.transport.CredentialItem.*;

import com.google.common.collect.*;

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
			for (int i = 0; i < items.length; i++) {
				CredentialItem item = items[i];
				if (item instanceof StringType) {
					if (item instanceof Username && i < items.length - 1 && items[i + 1] instanceof Password) {
						Password password = (Password) items[i + 1];
						JTextField user = new JTextField();
						JPasswordField pass = new JPasswordField();
						JOptionPane pane = new JOptionPane(new Object[] { "Login for " + uri, user, pass },
								JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
						final JDialog dialog = pane.createDialog("Input");
						pass.addActionListener((e) -> {
							pane.setValue(JOptionPane.OK_OPTION);
							dialog.setVisible(false);
						});
						dialog.addWindowListener(new WindowAdapter() {
							@Override
							public void windowOpened(WindowEvent e) {
								user.requestFocusInWindow();
							}
						});
						dialog.setVisible(true);
						dialog.dispose();
						if (!Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue())) {
							return false;
						}
						((StringType) item).setValue(user.getText());
						password.setValue(pass.getPassword());
						i++;
					} else {
						String prompt = item.getPromptText();
						String value;
						if (item instanceof Username) {
							prompt = "Username for " + uri;
							value = textPrompts.computeIfAbsent(prompt,
									(prompt0) -> new String(showPasswordDialog(prompt0)));
						} else {
							value = textPrompts.computeIfAbsent(prompt,
									(prompt0) -> new String(showPasswordDialog(prompt0)));
						}
						if (value == null) {
							return false;
						}
						((StringType) item).setValue(value);
					}
				} else if (item instanceof CharArrayType) {
					String prompt = item.getPromptText();
					if (item instanceof Password) {
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
	private static int fetches = 0;
	private static int fastForwards = 0;
	private static int pushes = 0;

	public static void main(String[] args) {
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
		System.out.println(fetches + " branch" + (fetches == 1 ? "" : "es") + " fetched.");
		System.out.println(fastForwards + " branch" + (fastForwards == 1 ? "" : "es") + " fast forwarded.");
		System.out.println(pushes + " branch" + (pushes == 1 ? "" : "es") + " pushed.");
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
		Git git = Git.wrap(repo);
		for (String remote : repo.getRemoteNames()) {
			System.out.println("Fetching " + dir.getName() + " remote " + remote);
			try {
				FetchResult result = git.fetch().setCredentialsProvider(cred).setRemote(remote).call();
				for (TrackingRefUpdate update : result.getTrackingRefUpdates()) {
					System.out.print("\t" + update.getRemoteName() + ": ");
					String old = update.getOldObjectId().name();
					if (update.getOldObjectId().equals(ObjectId.zeroId())) {
						old = "new branch";
					}
					System.out.println(old + " -> " + update.getNewObjectId().name());
					fetches++;
				}
			} catch (InvalidRemoteException e) {
				e.printStackTrace();
			} catch (TransportException e) {
				e.printStackTrace();
			} catch (GitAPIException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Fast-forwarding local branches to their tracking branches");
		try {
			boolean check;
			do {
				check = false;
				for (Map.Entry<String, Ref> localBranch : repo.getRefDatabase().getRefs("refs/heads/").entrySet()) {
					check |= tryFastForward(repo, localBranch.getValue(),
							repo.getRef(new BranchConfig(repo.getConfig(), localBranch.getKey()).getTrackingBranch()));
					check |= tryFastForward(repo, localBranch.getValue(),
							repo.getRef("refs/remotes/upstream/" + localBranch.getKey()));
				}
			} while (check);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Map<String, ObjectId> pushBranches;
		try {
			Map<String, Ref> pushRefs = repo.getRefDatabase().getRefs("refs/heads/");
			pushRefs = Maps.filterKeys(pushRefs, (k) -> new BranchConfig(repo.getConfig(), k).getRemote() != null);
			pushBranches = Maps.transformValues(pushRefs, Ref::getObjectId);
			pushBranches = Maps.filterValues(pushBranches, (v) -> v != null);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		if (!pushBranches.isEmpty()) {
			PushCommand push = git.push().setCredentialsProvider(cred).setTimeout(5);
			for (String branch : pushBranches.keySet()) {
				push.add("refs/heads/" + branch);
			}
			try {
				for (PushResult result : push.call()) {
					for (RemoteRefUpdate update : result.getRemoteUpdates()) {
						if (update.getStatus() == RemoteRefUpdate.Status.OK) {
							String branchName = update.getSrcRef().substring("refs/heads/".length());
							ObjectId oldId = pushBranches.get(branchName);
							String old = oldId == null || oldId.equals(ObjectId.zeroId()) ? "new branch" : oldId.name();
							System.out
									.println("\t" + branchName + ": " + old + " -> " + update.getNewObjectId().name());
							pushes++;
						}
					}
				}
			} catch (InvalidRemoteException e) {
				e.printStackTrace();
			} catch (TransportException e) {
				if (e.getCause() instanceof NoRemoteRepositoryException) {
					System.err.println(e.getCause());
				} else {
					e.printStackTrace();
				}
			} catch (GitAPIException e) {
				e.printStackTrace();
			}
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

	private static boolean tryFastForward(Repository repo, Ref ref, Ref target) {
		if (ref == null || target == null) {
			return false;
		}
		target = repo.peel(target);
		try {
			if (!ref.equals(repo.getRef(Constants.HEAD).getTarget())) {
				RevWalk revWalk = new RevWalk(repo);

				ObjectId targetId = target.getPeeledObjectId();
				if (targetId == null) {
					targetId = target.getObjectId();
				}

				RevCommit targetCommit = revWalk.lookupCommit(targetId);
				ObjectId sourceId = ref.getObjectId();
				RevCommit sourceCommit = revWalk.lookupCommit(sourceId);
				if (revWalk.isMergedInto(sourceCommit, targetCommit)) {
					RefUpdate refUpdate = repo.updateRef(ref.getName());
					refUpdate.setNewObjectId(targetCommit);
					refUpdate.setRefLogMessage("Fast forward", false);
					refUpdate.setExpectedOldObjectId(sourceId);
					Result rc = refUpdate.update();
					switch (rc) {
					case NEW:
					case FAST_FORWARD:
						System.out.println("Fast-forwarded " + ref.getName() + " to " + target.getName());
						fastForwards++;
						return true;
					case REJECTED:
					case LOCK_FAILURE:
						System.err.println(new ConcurrentRefUpdateException(JGitText.get().couldNotLockHEAD,
								refUpdate.getRef(), rc));
						break;
					case NO_CHANGE:
						break;
					default:
						System.err.println(new JGitInternalException(MessageFormat
								.format(JGitText.get().updatingRefFailed, ref.getName(), targetId.toString(), rc)));
						break;
					}
				}
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		try {
			MergeResult result = Git.wrap(repo).merge().setFastForward(MergeCommand.FastForwardMode.FF_ONLY)
					.include(target.getTarget()).call();
			if (result.getMergeStatus() == MergeResult.MergeStatus.ALREADY_UP_TO_DATE) {
				// Ignore
			} else if (result.getMergeStatus() == MergeResult.MergeStatus.FAST_FORWARD) {
				System.out.println("Fast-forwarded " + ref.getName() + " to " + target.getName());
				fastForwards++;
				return true;
			} else {
				System.err.println("Fast-forward failed: status " + result.getMergeStatus());
			}
		} catch (NoHeadException e) {
			// Ignore
		} catch (ConcurrentRefUpdateException e) {
			System.err.println(e);
		} catch (CheckoutConflictException e) {
			System.err.println(e);
		} catch (InvalidMergeHeadsException e) {
			e.printStackTrace();
		} catch (WrongRepositoryStateException e) {
			System.err.println(e);
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
		return false;
	}
}
