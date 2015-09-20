package jbyoshi.gitupdate;

import java.io.*;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.*;

public abstract class RemoteProcessor extends Processor<String> {

	@Override
	public final Iterable<String> getKeys(Repository repo) {
		return repo.getRemoteNames();
	}

	@Override
	public final void process(Repository repo, Git git, String remote) throws GitAPIException, IOException {
		process(repo, git, remote, "refs/remotes/" + remote + "/");
	}

	public abstract void process(Repository repo, Git git, String remote, String fullRemote)
			throws GitAPIException, IOException;

}
