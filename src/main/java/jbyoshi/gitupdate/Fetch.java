package jbyoshi.gitupdate;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.transport.*;

public final class Fetch extends RemoteProcessor {

	private int fetches;

	@Override
	public void process(Repository repo, Git git, String remote, String fullRemote) throws GitAPIException {
		System.out.println("\tFetching remote " + remote);
		FetchResult result = git.fetch().setCredentialsProvider(Prompts.INSTANCE).setRemote(remote).call();
		for (TrackingRefUpdate update : result.getTrackingRefUpdates()) {
			System.out.print("\t\t" + update.getRemoteName() + ": ");
			String old = update.getOldObjectId().name();
			if (update.getOldObjectId().equals(ObjectId.zeroId())) {
				old = "new branch";
			}
			System.out.println(old + " -> " + update.getNewObjectId().name());
			fetches++;
		}
	}

	@Override
	public void report() {
		System.out.println(fetches + " branch" + (fetches == 1 ? "es" : "") + " fetched.");
	}

}
