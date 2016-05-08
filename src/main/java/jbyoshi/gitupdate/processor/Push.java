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

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TooLargePackException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.transport.*;

import com.google.common.collect.*;

import jbyoshi.gitupdate.*;

public final class Push extends Processor {
	private static final Map<Repository, Set<String>> forcePushBranches = new WeakHashMap<>();

	static void forcePush(Repository repo, String branch) {
		if (branch.startsWith(Constants.R_HEADS)) branch = branch.substring(Constants.R_HEADS.length());
		forcePushBranches.computeIfAbsent(repo, k -> new HashSet<>()).add(branch);
	}

	@Override
	public void registerTasks(Repository repo, Git git, Task root) throws Exception {
		Task me = root.newChild(getClass().getSimpleName());
		// Group the branches by their remotes.
		Multimap<String, String> branchList = HashMultimap.create();
		for (String branch : Utils.getLocalBranches(repo).keySet()) {
			String remote = new BranchConfig(repo.getConfig(), branch).getRemote();
			if (remote != null) {
				branchList.put(remote, branch);
			}
		}
		for (Map.Entry<String, Collection<String>> remote : branchList.asMap().entrySet()) {
			me.newChild(remote.getKey(), report -> {
				try {
					process(repo, remote.getKey(), remote.getValue(), report);
				} catch (Exception e) {
					report.newErrorChild(e);
				}
			});
		}
	}

	private static void process(Repository repo, String remote, Collection<String> branches,
			Report report) throws Exception {
		// Figure out if anything needs to be pushed.
		Map<String, ObjectId> oldIds = new HashMap<>();
		boolean canPush = false;
		for (String branch : branches) {
			BranchConfig config = new BranchConfig(repo.getConfig(), branch);
			ObjectId target = repo.getRef(branch).getObjectId();

			Ref remoteRef = repo.getRef(config.getRemoteTrackingBranch());
			if (remoteRef == null || !target.equals(remoteRef.getObjectId())) {
				canPush = true;
			}
			oldIds.put(branch, remoteRef == null ? ObjectId.zeroId() : remoteRef.getObjectId());
		}

		if (!canPush) {
			return;
		}

		ArrayList<RefSpec> refSpecs = new ArrayList<>();
		for (String branch : branches) {
			RefSpec spec = new RefSpec(Constants.R_HEADS + branch);
			refSpecs.add(spec);
		}

		try {
			final List<Transport> transports;
			transports = Transport.openAll(repo, remote, Transport.Operation.PUSH);
			for (final Transport transport : transports) {
				transport.setCredentialsProvider(Prompts.INSTANCE);

				List<RefSpec> fetchRefSpecs = new RemoteConfig(repo.getConfig(), remote).getFetchRefSpecs();
				final List<RemoteRefUpdate> toPush = new ArrayList<>(
						Transport.findRemoteRefUpdatesFor(repo, refSpecs, fetchRefSpecs));
				for (int i = 0; i < toPush.size(); i++) {
					final RemoteRefUpdate update = toPush.get(i);
					if (update.isForceUpdate()) {
						toPush.set(i, new RemoteRefUpdate(update, repo.resolve(
								update.getTrackingRefUpdate().getLocalName())));
					}
				}

				try {
					PushResult result = transport.push(null, toPush, null);

					for (RemoteRefUpdate update : result.getRemoteUpdates()) {
						if (update.getStatus() == RemoteRefUpdate.Status.OK) {
							String branchName = Utils.getShortBranch(update.getSrcRef());
							ObjectId oldId = oldIds.get(branchName);
							String old = oldId.equals(ObjectId.zeroId()) ? "new branch" : oldId.name();
							report.newChild(branchName + ": " + old + " -> " + update.getNewObjectId().name())
									.modified();
						}
					}
				} catch (TooLargePackException e) {
					throw new org.eclipse.jgit.api.errors.TooLargePackException(e.getMessage(), e);
				} catch (TransportException e) {
					throw new org.eclipse.jgit.api.errors.TransportException(e.getMessage(), e);
				} finally {
					transport.close();
				}
			}
		} catch (URISyntaxException e) {
			throw new InvalidRemoteException(MessageFormat.format(JGitText.get().invalidRemote, remote));
		} catch (TransportException e) {
			throw new org.eclipse.jgit.api.errors.TransportException(e.getMessage(), e);
		} catch (IOException e) {
			throw new JGitInternalException(JGitText.get().exceptionCaughtDuringExecutionOfPushCommand,e);
		}
	}

}
