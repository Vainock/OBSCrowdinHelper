package de.vainock.obscrowdinhelper;

import de.vainock.obscrowdinhelper.crowdin.CrowdinRequest;
import de.vainock.obscrowdinhelper.crowdin.CrowdinRequestMethod;
import de.vainock.obscrowdinhelper.crowdin.CrowdinResponse;
import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class OBSCrowdinHelper {

  public static final int PROJECT_ID = 51028;
  public static final String PROJECT_DOMAIN = "crowdin.com";
  private static final MyFrame frame = new MyFrame();
  private static final File root = new File(new File("").getAbsolutePath());

  public static void main(String[] args) throws IOException {
    try {
      // authentication
      boolean run = true;
      int read;
      File tokenFile = new File(root, "Token");
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

      status("Waiting to collect data");
      frame.jButton.setText("Collect Data");
      frame.jPasswordField.setEnabled(false);
      frame.waitForButtonPress();
      frame.jButton.setText("Executing");
      frame.jButton.setEnabled(false);
      status("Removing previous files");
      for (File file : Objects.requireNonNull(root.listFiles())) {
        if (file.getName().equals("Translators.txt") || file.getName().equals("Translations")) {
          deleteFile(file);
        }
      }

      // project languages
      status("Generating top member reports");
      List<CrowdinRequest> requests = new ArrayList<>();
      for (Object lang : (JSONArray) ((JSONObject) new CrowdinRequest()
          .setPath("projects/" + PROJECT_ID)
          .setRequestMethod(CrowdinRequestMethod.GET).send().body.get("data"))
          .get("targetLanguageIds")) {

        // generate reports
        JSONObject body = new JSONObject();
        body.put("name", "top-members");
        JSONObject schema = new JSONObject();
        schema.put("unit", "words");
        schema.put("languageId", lang);
        schema.put("format", "json");
        schema.put("dateFrom", "2014-01-01T00:00:00+00:00");
        schema.put("dateTo", "2030-01-01T00:00:00+00:00");
        body.put("schema", schema);
        requests.add(new CrowdinRequest().setRequestMethod(CrowdinRequestMethod.POST)
            .setPath("projects/" + PROJECT_ID + "/reports").setBody(body));
      }
      CrowdinRequest.send(requests, false);
      status("Waiting for top member reports to generate");
      Thread.sleep(10000);

      // download and read reports
      status("Downloading top member reports");
      requests.clear();
      for (CrowdinResponse response : CrowdinResponse.getResponses(true)) {
        requests.add(new CrowdinRequest().setRequestMethod(CrowdinRequestMethod.GET).setPath(
            "projects/" + PROJECT_ID + "/reports/" + ((JSONObject) response.body.get("data"))
                .get("identifier") + "/download"));
      }
      Map<String, List<String>> topMembers = new HashMap<>();
      for (CrowdinResponse response : CrowdinRequest.send(requests, true)) {
        JSONObject report = new CrowdinRequest()
            .setUrl(((JSONObject) response.body.get("data")).get("url").toString())
            .setRequestMethod(CrowdinRequestMethod.GET).removeAuth().send().body;
        List<String> users = new ArrayList<>();
        JSONArray members = (JSONArray) report.get("data");
        if (members == null) {
          continue;
        }
        for (Object member : members) {
          String username =
              ((JSONObject) ((JSONObject) member).get("user")).get("fullName").toString();
          if (username.equals("REMOVED_USER")) {
            continue;
          }
          users.add(username);
        }
        if (users.size() != 0) {
          topMembers.put(((JSONObject) report.get("language")).get("name").toString(), users);
        }
      }

      // generating and saving Translators.txt
      {
        Writer translatorsWriter = new BufferedWriter(new OutputStreamWriter(
            new FileOutputStream(new File(root, "Translators.txt")), StandardCharsets.UTF_8));
        translatorsWriter.append("Translators:\n");
        for (String lang : new TreeSet<>(topMembers.keySet())) {
          translatorsWriter.append(' ').append(lang).append(":\n");
          for (String username : topMembers.get(lang)) {
            translatorsWriter.append("  - ").append(username).append('\n');
          }
        }
        translatorsWriter.flush();
        translatorsWriter.close();
      }

      // build project
      status("Building project");
      JSONObject body = new JSONObject();
      body.put("skipUntranslatedStrings", true);
      body.put("exportApprovedOnly", false);
      long buildId = (long) ((JSONObject) new CrowdinRequest()
          .setPath("projects/" + PROJECT_ID + "/translations/builds")
          .setRequestMethod(CrowdinRequestMethod.POST).setBody(body).send().body
          .get("data")).get("id");
      while (true) {
        JSONObject bodyData = (JSONObject) new CrowdinRequest().setPath(
            "projects/" + PROJECT_ID + "/translations/builds/" + buildId)
            .setRequestMethod(CrowdinRequestMethod.GET).send()
            .body.get("data");
        if (bodyData.get("status").equals("finished")) {
          break;
        }
        Thread.sleep(3000);
      }

      // download build
      status("Downloading newest build");
      BufferedInputStream input = new BufferedInputStream(
          new URL(((JSONObject) new CrowdinRequest().setRequestMethod(CrowdinRequestMethod.GET)
              .setPath("projects/" + PROJECT_ID + "/translations/builds/" + buildId + "/download")
              .send()
              .body.get("data")).get("url").toString())
              .openStream());
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = input.read(buffer)) != -1) {
        result.write(buffer, 0, bytesRead);
      }

      // unzip build
      status("Unzipping build and deleting empty files");
      ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(result.toByteArray()));
      ZipEntry entry = zipIn.getNextEntry();
      while (entry != null) {
        File file = new File(root, "Translations" + File.separator + entry.getName());
        if (entry.isDirectory()) {
          file.mkdirs();
        } else {
          FileOutputStream entryFos = new FileOutputStream(file);
          while ((read = zipIn.read(buffer, 0, buffer.length)) != -1) {
            entryFos.write(buffer, 0, read);
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
      File errorFile = new File(root, "Error-" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
          .format(new Timestamp((System.currentTimeMillis()))) + ".txt");
      FileWriter fw = new FileWriter(errorFile);
      PrintWriter pw = new PrintWriter(fw);
      e.printStackTrace(pw);
      fw.close();
      pw.close();
      JOptionPane.showMessageDialog(frame,
          "An unexpected error occurred and the program needs to be closed.\n\n"
              + "An error file was created that contains what went wrong.\n"
              + "Feel free to contact 'contact.vainock@gmail.com' for more information.",
          "An error occurred",
          JOptionPane.WARNING_MESSAGE);
    }
    System.exit(0);
  }

  private static boolean isAccountOkay() {
    if (new CrowdinRequest().setPath("user").setRequestMethod(CrowdinRequestMethod.GET).send()
        .code == 200) {
      boolean accountOkay = ((JSONObject) new CrowdinRequest().setPath("projects/" + PROJECT_ID)
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

class MyFrame extends JFrame {

  final JPasswordField jPasswordField = new JPasswordField(20);
  final JButton jButton = new JButton();
  private final Thread currentThread = Thread.currentThread();

  public MyFrame() {
    setTitle("OBSCrowdinHelper");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Dimension minSize = new Dimension(600, 100);
    setSize(minSize);
    setMinimumSize(minSize);
    JPanel panel = new JPanel();
    panel.add(new JLabel("Personal Access Token:"));
    panel.add(jPasswordField);
    panel.add(jButton);
    getContentPane().add(panel);
    setVisible(true);
  }

  void waitForButtonPress() throws Exception {
    new Thread(() -> this.jButton.addActionListener(e -> {
      synchronized (currentThread) {
        currentThread.notify();
      }
    })).start();
    synchronized (currentThread) {
      currentThread.wait();
    }
  }
}
