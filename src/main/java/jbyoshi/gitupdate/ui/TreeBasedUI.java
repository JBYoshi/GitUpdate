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
package jbyoshi.gitupdate.ui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.tree.*;

public final class TreeBasedUI implements UI {
	private final JFrame frame;
	private final JTree tree;
	private final DefaultTreeModel model;
	private final GUIReportData root;

	TreeBasedUI() {
		frame = new JFrame("GitUpdate - Working");
		root = new GUIReportData("Updates");
		model = new DefaultTreeModel(root.node);
		tree = new JTree(model);
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		frame.getContentPane().add(new JScrollPane(tree));
		frame.setMinimumSize(new Dimension(400, 600));
		frame.pack();
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}

	@Override
	public UsernamePasswordPair promptLogin(String prompt) {
		JTextField user = new JTextField();
		JPasswordField pass = new JPasswordField();
		JOptionPane pane = new JOptionPane(new Object[] { prompt, user, pass }, JOptionPane.QUESTION_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION);
		final JDialog dialog = pane.createDialog(frame, "Input");
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
			return null;
		}
		return new UsernamePasswordPair(user.getText(), pass.getPassword());
	}

	@Override
	public char[] promptPassword(String prompt) {
		JPasswordField pass = new JPasswordField();
		JOptionPane pane = new JOptionPane(new Object[] { prompt, pass }, JOptionPane.QUESTION_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION);
		final JDialog dialog = pane.createDialog(frame, "Input");
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

	@Override
	public ReportData newRootReportData(String text) {
		ReportData child = root.newChild(text);
		tree.expandPath(new TreePath(root.node.getPath()));
		return child;
	}

	private final class GUIReportData implements ReportData {
		private final DefaultMutableTreeNode node;

		private GUIReportData(String text) {
			node = new DefaultMutableTreeNode(text);
		}

		@Override
		public ReportData newChild(String text) {
			GUIReportData child = new GUIReportData(text);
			model.insertNodeInto(child.node, node, node.getChildCount());
			model.nodeChanged(node);
			return child;
		}

		@Override
		public ReportData newErrorChild(String text) {
			// TODO Maybe add a separate icon?
			return newChild(text);
		}
	}

	static {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void finish() {
		frame.setTitle("GitUpdate - Finished");
	}

}
