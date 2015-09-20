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

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.eclipse.jgit.errors.*;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.transport.CredentialItem.*;

final class Prompts extends CredentialsProvider {

	private final Map<String, String> textPrompts = new HashMap<String, String>();

	static final Prompts INSTANCE = new Prompts();

	private Prompts() {
	}

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
					if (item instanceof Username) {
						prompt = "Username for " + uri;
					}
					String value = textPrompts.computeIfAbsent(prompt, (prompt0) -> {
						char[] val = showPasswordDialog(prompt0);
						if (val == null) {
							return null;
						}
						return new String(val);
					});
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
				((CharArrayType) item).setValueNoCopy(textPrompts
						.computeIfAbsent(prompt, (prompt0) -> new String(showPasswordDialog(prompt0))).toCharArray());
			} else {
				return false;
			}
		}
		return true;
	}

	private static char[] showPasswordDialog(String prompt) {
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

}
