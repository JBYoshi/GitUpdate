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

import org.eclipse.jgit.lib.*;

public final class Utils {
	private Utils() {
	}

	public static Map<String, Ref> getLocalBranches(Repository repo) throws IOException {
		return repo.getRefDatabase().getRefs(Constants.R_HEADS);
	}

	public static String getShortBranch(String fullBranch) {
		if (fullBranch.startsWith(Constants.R_HEADS)) {
			return fullBranch.substring(Constants.R_HEADS.length());
		}
		if (fullBranch.startsWith(Constants.R_REMOTES)) {
			return fullBranch.substring(Constants.R_REMOTES.length());
		}
		return fullBranch;
	}

	public static String getPushRemote(Repository repo, String branch) throws IOException {
		String pushDefault = repo.getConfig().getString("branch", branch, "pushremote");
		if (pushDefault == null) {
			pushDefault = repo.getConfig().getString("remote", branch, "pushdefault");
		}
		return pushDefault;
	}
}
