import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JButton;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.BufferedReader;

final class AJIDE
{
	public static void main(final String[] args)
	{
		SwingUtilities.invokeLater(
			() ->
			{
				final var app = new JFrame("AJIDE");
				final var build = new JButton("Build");
				build.setMnemonic(KeyEvent.VK_B);
				final var run = new JButton("Run");
				run.setMnemonic(KeyEvent.VK_R);
				final var output = new JLabel();
				final var file_chooser = new JFileChooser();
				final var tool_bar = new JPanel();
				final var build_file_label = new JLabel();

				final var rb_output = new JTextArea();
				final var rb_pane = new JScrollPane(rb_output);
				rb_pane.setPreferredSize(new Dimension(300, 300));
				app.add(rb_pane, BorderLayout.EAST);

				tool_bar.setLayout(new FlowLayout());
				final var clear_rb_output = new JButton("Clear");
				clear_rb_output.setMnemonic(KeyEvent.VK_C);
				clear_rb_output.addActionListener((ae) -> rb_output.setText(""));
				tool_bar.add(build);
				tool_bar.add(run);
				tool_bar.add(clear_rb_output);

				app.add(tool_bar, BorderLayout.NORTH);

				final var text_area = new JTextArea();
				text_area.setTabSize(2);
				final var text_pane = new JScrollPane(text_area);
				app.add(text_pane, BorderLayout.CENTER);

				final var menubar = new JMenuBar();

				final var file_menu = new JMenu("File");

				final var file_open = new JMenuItem("Open");
				file_open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
				file_open.setMnemonic(KeyEvent.VK_O);
				file_open.addActionListener(
					(ae) ->
					{
						switch (file_chooser.showOpenDialog(app))
						{
							case JFileChooser.APPROVE_OPTION:
								final var ofile_name = file_chooser.getSelectedFile().getAbsolutePath();
								try (final var ofile = new BufferedReader(new FileReader(ofile_name)))
								{
									text_area.setText("");

									String line;
									while ((line = ofile.readLine()) != null)
									{
										text_area.append(line);
										text_area.append("\n");
									}

									output.setText(ofile_name);
									build_file_label.setText(ofile_name);
								}
								catch (final FileNotFoundException e)
								{
									output.setText("Error: Cannot open file");
								}
								catch (final IOException e)
								{
									output.setText("Error: Reading file");
								}
							break;

							case JFileChooser.CANCEL_OPTION:
								output.setText("canceled open file selection");
							break;

							case JFileChooser.ERROR_OPTION:
								output.setText("some error occured opening file");
							break;
						}
					}
				);

				final var save_menu = new JMenuItem("Save");
				save_menu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
				save_menu.setMnemonic(KeyEvent.VK_S);
				save_menu.addActionListener(
					(ae) ->
					{
						switch(file_chooser.showSaveDialog(app))
						{
							case JFileChooser.APPROVE_OPTION:
								final var sfile_name = file_chooser.getSelectedFile().getAbsolutePath();
								try (final var sfile = new BufferedWriter(new FileWriter(sfile_name)))
								{
									final var save_str = text_area.getText();
									sfile.write(save_str, 0, save_str.length());
									sfile.flush();

									output.setText(sfile_name);
									build_file_label.setText(sfile_name);
								}
								catch (final IOException e)
								{
									output.setText("IO Error: Could not save file");
								}
							break;

							case JFileChooser.CANCEL_OPTION:
								output.setText("canceled save file selection");
							break;

							case JFileChooser.ERROR_OPTION:
								output.setText("some error occured saving file");
							break;
						}
					}
				);

				file_menu.add(save_menu);
				file_menu.add(file_open);

				menubar.add(file_menu);


				app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				app.setSize(900, 600);

				app.setJMenuBar(menubar);

				build.addActionListener(
					(ae) ->
					{
						new Thread(
							() ->
						{
						final var build_file_name = build_file_label.getText();
						if (build_file_name.isEmpty())
						{
							output.setText("Build Error: no build file context");
							return;
						}

						final var build_file_directory = build_file_name.substring(0, build_file_name.lastIndexOf("/"));
						final var build_process = new ProcessBuilder("javac", "-Xlint", "-Werror", "-d", build_file_directory, build_file_name);
						final var tmp_bfile_err = new File("tmp_build_err");

						build_process.redirectError(tmp_bfile_err);
						try
						{
							final var process = build_process.start();

							process.waitFor();

							try (final var bfile_err = new BufferedReader(new FileReader(tmp_bfile_err)))
							{
								rb_output.setText("");
								var line = "";
								while ((line = bfile_err.readLine()) != null)
								{
									rb_output.append(line);
									rb_output.append("\n");
								}
							}
							catch (final FileNotFoundException e)
							{
								output.setText("Build Error: unable to get build results");
							}
						}
						catch (final IOException e)
						{
							output.setText("Build Error: uknown IO Error");
						}
						catch (final InterruptedException e)
						{
							output.setText("Build error: build process interrupted");
						}
						}
						).start();
					}
				);


				run.addActionListener(
					(ae) ->
					{
						new Thread(
							() ->
						{
						final var build_file_name = build_file_label.getText();
						if (build_file_name.isEmpty())
						{
							output.setText("Run Error: no run file context");
							return;
						}
						var run_file_name = build_file_name.substring(build_file_name.lastIndexOf("/") + 1);
						run_file_name = run_file_name.substring(0, run_file_name.lastIndexOf("."));
						final var run_path = build_file_name.substring(0, build_file_name.lastIndexOf("/"));

						final var run_process = new ProcessBuilder("java", "-cp", run_path, run_file_name);

						final var tmp_rfile_err = new File("tmp_rfile_err");
						run_process.redirectError(tmp_rfile_err);
						run_process.redirectInput(ProcessBuilder.Redirect.INHERIT);
						run_process.redirectOutput(ProcessBuilder.Redirect.INHERIT);
						try
						{
							final var process = run_process.start();

							process.waitFor();

							try (final var rfile_err = new BufferedReader(new FileReader(tmp_rfile_err)))
							{
								var line = "";
								while ((line = rfile_err.readLine()) != null)
								{
									rb_output.append(line);
									rb_output.append("\n");
								}
							}
							catch(final FileNotFoundException e)
							{
								output.setText("Run Error: unable to open tmp run err log file");
							}
						}
						catch (final IOException e)
						{
							output.setText("Run Error: uknown IO error");
						}
						catch (final InterruptedException e)
						{
							output.setText("Run Error: run interrupted");
						}
						}
						).start();
					}
				);

				app.add(output, BorderLayout.SOUTH);

				app.setVisible(true);
			}
		);

	}

	private AJIDE()
	{
		throw new AssertionError();
	}
}
