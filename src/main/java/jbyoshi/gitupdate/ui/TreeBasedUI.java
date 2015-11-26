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

@SuppressWarnings("serial")
public final class TreeBasedUI implements UI {
	private final JFrame frame;
	private final JTree tree;
	private final DefaultTreeModel model;
	private final GUINodeView root;

	private static final Icon ICON_PLAIN = getIcon("plain");
	private static final Icon ICON_FUTURE = getIcon("future");
	private static final Icon ICON_WORKING = getIcon("working");
	private static final Icon ICON_MODIFIED = getIcon("modified");
	private static final Icon ICON_DONE = getIcon("done");
	private static final Icon ICON_ERROR = getIcon("error");
	private static final Icon ICON_WORKING_MODIFIED = getIcon("working-modified");
	private static final Icon ICON_WORKING_ERROR = getIcon("working-error");
	private static final Icon ICON_MODIFIED_ERROR = getIcon("modified-error");
	private static final Icon ICON_WORKING_MODIFIED_ERROR = getIcon("working-modified-error");

	TreeBasedUI() {
		frame = new JFrame("GitUpdate - Loading");
		root = new GUINodeView("Updates") {
			@Override
			public void stateChanged(boolean error, boolean working, boolean future, boolean modified, boolean done) {
				super.stateChanged(error, working, future, modified, done);
				if (done) {
					if (error) {
						frame.setTitle("GitUpdate - Errored");
					} else if (modified) {
						frame.setTitle("GitUpdate - Updated");
					} else {
						frame.setTitle("GitUpdate - Done");
					}
				} else if (working) {
					frame.setTitle("GitUpdate - Working");
				}
			}
		};
		model = new DefaultTreeModel(root);
		tree = new JTree(model);
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setCellRenderer(new DefaultTreeCellRenderer() {
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
					boolean isLeaf, int row, boolean focused) {
				super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, focused);
				setIcon(((GUINodeView) value).icon);
				return this;
			}
		});
		frame.getContentPane().add(new JScrollPane(tree));
		frame.setMinimumSize(new Dimension(500, 600));
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
	public ReportView getRoot() {
		return root;
	}

	private class GUINodeView extends DefaultMutableTreeNode implements ReportView {
		private Icon icon = ICON_PLAIN;

		private GUINodeView(String text) {
			super(text);
		}

		@Override
		public ReportView newChild(String text) {
			GUINodeView child = new GUINodeView(text);
			EventQueue.invokeLater(() -> {
				model.insertNodeInto(child, this, getChildCount());
				model.nodeChanged(this);
				tree.expandPath(new TreePath(root.getPath()));
			});
			return child;
		}

		@Override
		public void stateChanged(boolean error, boolean working, boolean future, boolean modified, boolean done) {
			if (future) {
				icon = ICON_FUTURE;
			} else if (working) {
				if (error) {
					if (modified) {
						icon = ICON_WORKING_MODIFIED_ERROR;
					} else {
						icon = ICON_WORKING_ERROR;
					}
				} else if (modified) {
					icon = ICON_WORKING_MODIFIED;
				} else {
					icon = ICON_WORKING;
				}
			} else if (error) {
				if (modified) {
					icon = ICON_MODIFIED_ERROR;
				} else {
					icon = ICON_ERROR;
				}
			} else if (modified) {
				icon = ICON_MODIFIED;
			} else if (done) {
				icon = ICON_DONE;
			} else {
				icon = ICON_PLAIN;
			}
			EventQueue.invokeLater(() -> model.nodeChanged(this));
		}
	}

	static {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Icon getIcon(String name) {
		return new ImageIcon(TreeBasedUI.class.getResource("icons/" + name + ".png"));
	}

}
