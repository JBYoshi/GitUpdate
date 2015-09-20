package jbyoshi.gitupdate;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.*;
import org.eclipse.jgit.lib.*;

public abstract class Processor<T> {

	void run(Repository repo, Git git) {
		Iterable<T> keys;
		try {
			keys = getKeys(repo);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		for (T key : keys) {
			try {
				process(repo, git, key);
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

	protected abstract void process(Repository repo, Git git, T key) throws Exception;

	protected void report() {
	}

}
