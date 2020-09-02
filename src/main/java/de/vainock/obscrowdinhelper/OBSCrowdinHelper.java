package de.vainock.obscrowdinhelper;

import de.vainock.obscrowdinhelper.crowdin.*;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class OBSCrowdinHelper {

  public static Scanner scanner = new Scanner(System.in);

  public static void main(String[] args) {
    try {
      // open terminal
      if (args.length == 0) {
        String path = new File(
            OBSCrowdinHelper.class.getProtectionDomain().getCodeSource().getLocation()
                .toURI()).getPath();
        Runtime.getRuntime().exec(
            new String[]{"cmd", "/c", "start", "cmd", "/c", "java", "-jar", "\"" + path + "\"",
                "terminal"});
      }

      runCommand("cmd /c title OBSCrowdinHelper");
      File root = new File(new File("").getAbsolutePath());

      // authentication
      boolean run = true;
      int read;
      File tokenFile = new File(root, "Token");
      if (tokenFile.exists()) {
        out("Checking saved token...");
        FileReader tokenFr = new FileReader(tokenFile);
        StringBuilder tokenSb = new StringBuilder();
        while ((read = tokenFr.read()) != -1) {
          tokenSb.append(Character.valueOf((char) read));
        }
        tokenFr.close();
        CrowdinRequest.setToken(tokenSb.toString());

        if (isAccountOkay()) {
          run = false;
        } else {
          tokenFile.delete();
          out("This token seems to be invalid or the account isn't project manager or higher.");
        }
      } else {
        out("Please now enter your personal access token generated from within your account settings.");
        out("Don't worry: The program will save this token to automatically use it next time.");
      }
      out("----------");

      while (run) {
        out("Personal Access Token:");
        String token = scanner.nextLine();
        CrowdinRequest.setToken(token);
        out("----------");
        out("Checking...");
        if (isAccountOkay()) {
          run = false;
          FileOutputStream tokenFos = new FileOutputStream(tokenFile);
          tokenFos.write(token.getBytes());
          tokenFos.flush();
          tokenFos.close();
        } else {
          cls();
          out("This token seems to be invalid or the account isn't project manager or higher.");
        }
      }

      // clean-up
      cls();
      out("Press Enter to start collecting the data.");
      scanner.nextLine();
      out(" - removing old files");
      for (File file : Objects.requireNonNull(root.listFiles())) {
        if (file.getName().equals("Translators.txt") || file.getName().equals("Translations")) {
          deleteFile(file);
        }
      }

      // project languages
      out(" - requesting project languages");
      out(" - generating top member reports");
      List<CrowdinRequest> requests = new ArrayList<>();
      for (Object lang : (JSONArray) ((JSONObject) new CrowdinRequest().setPath("projects/51028")
          .setRequestMethod(CrowdinRequestMethod.GET).send().getBody().get("data"))
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
            .setPath("projects/51028/reports").setBody(body));
      }
      CrowdinRequest.send(requests, false);
      out(" - waiting for top member reports to generate");
      Thread.sleep(10000);

      // download and read reports
      out(" - downloading top member reports");
      requests.clear();
      for (CrowdinResponse response : CrowdinResponse.getResponses(true)) {
        requests.add(new CrowdinRequest().setRequestMethod(CrowdinRequestMethod.GET).setPath(
            "projects/51028/reports/" + ((JSONObject) response.getBody().get("data"))
                .get("identifier") + "/download"));
      }
      Map<String, JSONArray> topMembers = new HashMap<>();
      for (CrowdinResponse response : CrowdinRequest.send(requests, true)) {
        BufferedInputStream input = new BufferedInputStream(
            new URL(((JSONObject) response.getBody().get("data")).get("url").toString())
                .openStream());
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
          result.write(buffer, 0, bytesRead);
        }
        JSONObject report = (JSONObject) new JSONParser().parse(result.toString());
        topMembers.put(((JSONObject) report.get("language")).get("name").toString(),
            (JSONArray) report.get("data"));
      }

      Writer translatorsWriter = new BufferedWriter(new OutputStreamWriter(
          new FileOutputStream(new File(root, "Translators.txt")), StandardCharsets.UTF_8));
      {
        // generating and saving Translators.txt
        out(" - sorting data and generating Translators.txt");
        SortedSet<String> sortedLangs = new TreeSet<>(topMembers.keySet());
        translatorsWriter.append("Translators:\n");
        for (String lang : sortedLangs) {
          JSONArray members = topMembers.get(lang);
          if (members == null) {
            continue;
          }
          translatorsWriter.append(' ').append(lang).append(":\n");
          for (Object member : members) {
            String username =
                ((JSONObject) ((JSONObject) member).get("user")).get("fullName").toString();
            if (username.equals("REMOVED_USER")) {
              continue;
            }
            translatorsWriter.append("  - ");
            translatorsWriter.append(username);
            translatorsWriter.append('\n');
          }
        }
      }
      translatorsWriter.flush();
      translatorsWriter.close();

      // build project
      out(" - building OBS Studio project");
      JSONObject body = new JSONObject();
      body.put("skipUntranslatedStrings", true);
      body.put("exportApprovedOnly", false);
      long buildId = (long) ((JSONObject) new CrowdinRequest()
          .setPath("projects/51028/translations/builds")
          .setRequestMethod(CrowdinRequestMethod.POST).setBody(body).send().getBody()
          .get("data")).get("id");
      while (true) {
        JSONObject bodyData = (JSONObject) new CrowdinRequest().setPath(
            "projects/51028/translations/builds/" + buildId)
            .setRequestMethod(CrowdinRequestMethod.GET).send()
            .getBody().get("data");
        if (bodyData.get("status").equals("finished")) {
          break;
        }
        Thread.sleep(3000);
      }

      // download build
      out(" - downloading newest build");
      BufferedInputStream input = new BufferedInputStream(
          new URL(((JSONObject) new CrowdinRequest().setRequestMethod(CrowdinRequestMethod.GET)
              .setPath("projects/51028/translations/builds/" + buildId + "/download").send()
              .getBody().get("data")).get("url").toString())
              .openStream());
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = input.read(buffer)) != -1) {
        result.write(buffer, 0, bytesRead);
      }

      // unzip build
      out(" - unzipping build and deleting empty files");
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

      out("----------");
      out("Finished!");
      out("Press Enter to close the program.");
    } catch (Exception e) {
      error(e);
    }
    scanner.nextLine();
    System.exit(0);
    scanner.close();
  }

  private static boolean isAccountOkay() {
    if (new CrowdinRequest().setPath("user").setRequestMethod(CrowdinRequestMethod.GET).send()
        .getCode() == 200) {
      return ((JSONObject) new CrowdinRequest().setPath("projects/51028")
          .setRequestMethod(CrowdinRequestMethod.GET).send().getBody().get("data"))
          .get("translateDuplicates") != null;
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

  private static void out(String text) {
    System.out.println(text);
  }

  private static void cls() {
    try {
      runCommand("cmd /c cls");
    } catch (Exception e) {
      error(e);
    }
  }

  public static void error(Exception e) {
    out("----- An error occured: -----");
    out("Please close the program and try again. If this doesn't help, contact the developer: contact.vainock@gmail.com");
    out("Please also provide the following error message which will help to identify and fix the bug:");
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    out(sw.toString());
    scanner.nextLine();
    System.exit(0);
    scanner.close();
  }

  private static void runCommand(String command) throws Exception {
    new ProcessBuilder(command.split(" ")).inheritIO().start().waitFor();
  }
}
