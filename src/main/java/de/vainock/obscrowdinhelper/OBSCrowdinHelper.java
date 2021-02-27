package de.vainock.obscrowdinhelper;

import de.vainock.obscrowdinhelper.crowdin.CrowdinRequest;
import de.vainock.obscrowdinhelper.crowdin.CrowdinRequestMethod;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class OBSCrowdinHelper {

	private static final MyFrame frame = new MyFrame();
	private static final File root = new File(new File("").getAbsolutePath());

	// valid args: --skip-build, --skip-authors
	public static void main(String[] args) {
		try {
			// authentication
			int read;
			{
				boolean run = true;
				File tokenFile = new File(root, "Token.txt");
				if (tokenFile.exists()) {
					status("Checking saved token");
					frame.jButton.setEnabled(false);
					frame.jPasswordField.setEnabled(false);
					frame.jButton.setText("Checking Token");
					FileReader tokenFr = new FileReader(tokenFile);
					StringBuilder tokenSb = new StringBuilder();
					while ((read = tokenFr.read()) != -1) {
						tokenSb.append(Character.valueOf((char) read));
					}
					tokenFr.close();
					frame.jPasswordField.setText(tokenSb.toString());
					CrowdinRequest.setToken(tokenSb.toString());

					if (isAccountOkay()) {
						run = false;
						frame.jButton.setEnabled(true);
						frame.jButton.setText("Collect Data");
					} else {
						tokenFile.delete();
						frame.jButton.setEnabled(true);
						frame.jPasswordField.setEnabled(true);
						frame.jPasswordField.setText("");
						frame.jButton.setText("Check Token");
						status("Saved token invalid");
					}
				}

				while (run) {
					frame.jButton.setText("Check Token");
					frame.waitForButtonPress();
					String token = new String(frame.jPasswordField.getPassword());
					CrowdinRequest.setToken(token);
					status("Checking entered token");
					frame.jPasswordField.setEnabled(false);
					frame.jButton.setEnabled(false);
					frame.jButton.setText("Checking Token");
					if (isAccountOkay()) {
						run = false;
						FileOutputStream tokenFos = new FileOutputStream(tokenFile);
						tokenFos.write(token.getBytes());
						tokenFos.flush();
						tokenFos.close();
					} else {
						JOptionPane.showMessageDialog(frame,
								"This token seems to be invalid or the account isn't project manager or higher.",
								"Token invalid", JOptionPane.INFORMATION_MESSAGE);
						status("Token invalid");
						frame.jPasswordField.setText("");
						frame.jPasswordField.setEnabled(true);
					}
					frame.jButton.setEnabled(true);
				}
			}

			status("Waiting to collect data");
			frame.jButton.setText("Collect Data");
			frame.jPasswordField.setEnabled(false);
			frame.waitForButtonPress();
			frame.jButton.setText("Executing");
			frame.jButton.setEnabled(false);
			status("Removing previous files");
			for (File file : Objects.requireNonNull(root.listFiles())) {
				if (file.getName().equals("AUTHORS") || file.getName().equals("Translations")) {
					deleteFile(file);
				}
			}

			// AUTHORS file
			if (!Arrays.asList(args).contains("--skip-authors")) {
				Writer authorsWriter = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(new File(root, "AUTHORS")), StandardCharsets.UTF_8));
				authorsWriter.append(
						"Original Author: Hugh Bailey (\"Jim\")\n\nContributors are sorted by their amount of commits / translated words.\n\nContributors:\n");
				authorsWriter.flush();
				// get git contributors
				status("Getting git contributors");
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setDialogTitle("Open the .git directory");
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fileChooser.setAcceptAllFileFilterUsed(false);
				fileChooser.setFileHidingEnabled(false);
				frame.jButton.setText("Select Directory");
				frame.jButton.setEnabled(true);
				File selectedFile;
				while (true) {
					frame.waitForButtonPress();
					if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION && fileChooser
							.getSelectedFile().getName().equals(".git")) {
						selectedFile = fileChooser.getSelectedFile();
						break;
					}
					status("Wrong directory, try again");
				}
				frame.jButton.setEnabled(false);
				frame.jButton.setText("Executing");
				Process process = new ProcessBuilder().directory(selectedFile.getParentFile())
						.command("git", "shortlog", "--all", "-sn", "--no-merges")
						.start();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
				String line;
				while ((line = reader.readLine()) != null) {
					authorsWriter.append(' ').append(line.substring(line.indexOf('\t') + 1)).append('\n');
				}
				reader.close();
				authorsWriter.flush();

				// get blocked users
				List<String> blockedUsers = new ArrayList<>();
				for (Object obj : (JSONArray) new CrowdinRequest().setPath("members?role=blocked")
						.setRequestMethod(CrowdinRequestMethod.GET).send().body.get("data")) {
					blockedUsers.add(((JSONObject) ((JSONObject) obj).get("data")).get("id").toString());
				}

				// generate and parse top member report
				status("Generating top member report");
				JSONObject body = new JSONObject();
				body.put("name", "top-members");
				JSONObject schema = new JSONObject();
				schema.put("unit", "words");
				schema.put("format", "json");
				schema.put("dateFrom", "2014-01-01T00:00:00+00:00");
				schema.put("dateTo", "2030-01-01T00:00:00+00:00");
				body.put("schema", schema);
				Map<String, List<String>> topMembers = new HashMap<>();
				for (Object obj : (JSONArray) new CrowdinRequest()
						.setRequestMethod(CrowdinRequestMethod.GET).setUrl(((JSONObject) new CrowdinRequest()
								.setRequestMethod(CrowdinRequestMethod.GET)
								.setPath("reports/" + ((JSONObject) new CrowdinRequest()
										.setRequestMethod(CrowdinRequestMethod.POST).setPath("reports").setBody(body)
										.send().body.get("data")).get("identifier").toString() + "/download")
								.setDelay(7500).send().body.get("data")).get("url").toString()).send().body
						.get("data")) {
					JSONObject dataEntry = (JSONObject) obj;
					JSONObject userEntry = (JSONObject) dataEntry.get("user");
					String username = userEntry.get("fullName").toString();
					if (username.equals("REMOVED_USER") || blockedUsers
							.contains(userEntry.get("id").toString())) {
						continue;
					}
					for (Object langObj : (JSONArray) dataEntry.get("languages")) {
						String languageName = ((JSONObject) langObj).get("name").toString();
						List<String> members;
						if (topMembers.containsKey(languageName)) {
							members = topMembers.get(languageName);
						} else {
							members = new ArrayList<>();
						}
						members.add(username);
						topMembers.put(languageName, members);
					}
				}

				// save file
				authorsWriter.append("\nTranslators:\n");
				for (String lang : new TreeSet<>(topMembers.keySet())) {
					authorsWriter.append(' ').append(lang).append(":\n");
					for (String username : topMembers.get(lang)) {
						authorsWriter.append("  ").append(username).append('\n');
					}
				}
				authorsWriter.flush();
				authorsWriter.close();
			}

			// build project
			long newestBuildId;
			if (!Arrays.asList(args).contains("--skip-build")) {
				status("Building project");
				JSONObject body = new JSONObject();
				body.put("skipUntranslatedStrings", true);
				newestBuildId = (long) ((JSONObject) new CrowdinRequest()
						.setPath("translations/builds")
						.setRequestMethod(CrowdinRequestMethod.POST).setBody(body).send().body
						.get("data")).get("id");
				while (true) {
					JSONObject bodyData = (JSONObject) new CrowdinRequest().setDelay(1000).setPath(
							"translations/builds/" + newestBuildId)
							.setRequestMethod(CrowdinRequestMethod.GET).send()
							.body.get("data");
					if (bodyData.get("status").equals("finished")) {
						break;
					}
				}
			} else {
				status("Getting newest build");
				newestBuildId = (long) ((JSONObject) ((JSONObject) ((JSONArray) new CrowdinRequest()
						.setPath("translations/builds").setRequestMethod(CrowdinRequestMethod.GET).send().body
						.get("data")).get(0)).get("data")).get("id");
			}

			// download build
			status("Downloading newest build");
			BufferedInputStream input = new BufferedInputStream(
					new URL(((JSONObject) new CrowdinRequest().setRequestMethod(CrowdinRequestMethod.GET)
							.setPath("translations/builds/" + newestBuildId + "/download")
							.send()
							.body.get("data")).get("url").toString())
							.openStream());
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			byte[] readBuffer = new byte[1024];
			while ((read = input.read(readBuffer)) != -1) {
				result.write(readBuffer, 0, read);
			}

			// unpack build and only save non-empty files
			status("Saving non-empty files");
			ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(result.toByteArray()));
			ZipEntry entry = zipIn.getNextEntry();
			while (entry != null) {
				File file = new File(root, "Translations" + File.separator + entry.getName());
				if (entry.isDirectory()) {
					file.mkdirs();
				} else {
					FileOutputStream entryFos = new FileOutputStream(file);
					while ((read = zipIn.read(readBuffer, 0, readBuffer.length)) != -1) {
						entryFos.write(readBuffer, 0, read);
					}
					entryFos.flush();
					entryFos.close();
					FileReader emptyFilesFr = new FileReader(file);
					StringBuilder emptyFilesSb = new StringBuilder();
					while ((read = emptyFilesFr.read()) != -1) {
						emptyFilesSb.append(Character.valueOf((char) read));
					}
					emptyFilesFr.close();
					if (emptyFilesSb.toString().replaceAll("[\\r\\n]", "").length() == 0) {
						file.delete();
					}
				}
				zipIn.closeEntry();
				entry = zipIn.getNextEntry();
			}
			zipIn.close();

			status("Finished");
			frame.jButton.setText("Close");
			frame.jButton.setEnabled(true);
			frame.waitForButtonPress();
		} catch (Exception e) {
			error(e);
		}
		System.exit(0);
	}

	public static void error(Exception e) {
		try {
			File errorFile = new File(root, "Error-" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
					.format(new Timestamp((System.currentTimeMillis()))) + ".txt");
			FileWriter fw = new FileWriter(errorFile);
			PrintWriter pw = new PrintWriter(fw);
			e.printStackTrace(pw);
			fw.close();
			pw.close();
			JOptionPane.showMessageDialog(frame,
					"An unexpected error occurred and the program needs to be closed.\n\n"
							+ "The error file '" + errorFile.getName() + "' was created.",
					"An error occurred",
					JOptionPane.WARNING_MESSAGE);
		} catch (Exception e2) {
			e2.printStackTrace();
		}
	}

	private static boolean isAccountOkay() {
		if (new CrowdinRequest().setPath("/user").setRequestMethod(CrowdinRequestMethod.GET).send()
				.code == 200) {
			boolean accountOkay = ((JSONObject) new CrowdinRequest().setPath("")
					.setRequestMethod(CrowdinRequestMethod.GET).send().body.get("data"))
					.containsKey("translateDuplicates");
			if (accountOkay) {
				frame.jButton.setText("Collect Data");
				frame.jPasswordField.setEnabled(false);
			}
			return accountOkay;
		} else {
			return false;
		}
	}

	private static void deleteFile(File file) {
		if (!file.isFile()) {
			for (File subFile : Objects.requireNonNull(file.listFiles())) {
				deleteFile(subFile);
			}
		}
		if (file.exists()) {
			file.delete();
		}
	}

	private static void status(String status) {
		frame.setTitle("OBSCrowdinHelper: " + status);
	}
}
